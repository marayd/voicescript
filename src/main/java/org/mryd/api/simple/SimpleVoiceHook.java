package org.mryd.api.simple;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.luaj.vm2.LuaValue;
import org.mryd.lua.wrap.events.LuaSpeechEventWrapper;

import javax.sound.sampled.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mryd.VoiceScript.instance;
import static org.mryd.VoiceScript.loader;

@Slf4j
public class SimpleVoiceHook implements VoicechatPlugin {

    @Getter
    public static VoicechatServerApi simpleApi;

    private final Map<UUID, DecoderSession> sessions = new ConcurrentHashMap<>();

    @Override
    public String getPluginId() {
        return "voicescript";
    }

    @Override
    public void initialize(VoicechatApi api) {
        log.info("Initializing SimpleVoiceHook");
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
        registration.registerEvent(VoicechatServerStoppedEvent.class, this::onServerStopped);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        simpleApi = event.getVoicechat();
    }

    private void onServerStopped(VoicechatServerStoppedEvent event) {
        sessions.values().forEach(DecoderSession::finalizeSession);
        sessions.clear();
    }

    private void onMicrophone(MicrophonePacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null || sender.getPlayer() == null) return;

        UUID playerId = sender.getPlayer().getUuid();
        DecoderSession session = sessions.computeIfAbsent(playerId,
                uuid -> new DecoderSession(simpleApi.createDecoder(), uuid));

        byte[] encoded = event.getPacket().getOpusEncodedData();
        session.process(encoded);
    }

    private static class DecoderSession {
        private static final int SAMPLE_RATE = 48000;
        private static final int CHANNELS = 1;
        private static final int SILENCE_TIMEOUT_MS = 500;

        private final UUID playerId;
        private final OpusDecoder decoder;
        private final List<File> clips = new ArrayList<>();
        private final ByteArrayOutputStream currentPhrase = new ByteArrayOutputStream();

        private long lastPacketTime = System.currentTimeMillis();
        private boolean speaking = false;

        private final ScheduledExecutorService silenceChecker = Executors.newSingleThreadScheduledExecutor();

        public DecoderSession(OpusDecoder decoder, UUID playerId) {
            this.decoder = decoder;
            this.playerId = playerId;
            startSilenceChecker();
        }

        public void process(byte[] encoded) {
            short[] decoded = decoder.decode(encoded);
            lastPacketTime = System.currentTimeMillis();

            speaking = true;

            for (short sample : decoded) {
                currentPhrase.write(sample & 0xFF);
                currentPhrase.write((sample >> 8) & 0xFF);
            }
        }

        private void startSilenceChecker() {
            silenceChecker.scheduleAtFixedRate(() -> {
                if (!speaking) return;

                long now = System.currentTimeMillis();
                if (now - lastPacketTime > SILENCE_TIMEOUT_MS) {
                    saveCurrentPhrase();
                    speaking = false;
                }
            }, 100, 100, TimeUnit.MILLISECONDS);
        }

        private void saveCurrentPhrase() {
            try {
                byte[] data = currentPhrase.toByteArray();
                if (data.length == 0) return;
                loader.getAllEngines().forEach((string, luaEngine) -> {
                    LuaValue globals = luaEngine.getGlobals();

                    LuaValue onSpeechEventFunc = globals.get("onSpeechEvent");
                    if (!onSpeechEventFunc.isnil()) {
                        LuaSpeechEventWrapper luaEvent = new LuaSpeechEventWrapper(Base64.getEncoder().encodeToString(data), Bukkit.getPlayer(playerId));
                        //
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                onSpeechEventFunc.call(luaEvent);
                            }
                        }.runTask(instance);
                        //
                    }
                });

                currentPhrase.reset();

            } catch (Exception e) {
                log.error("Error saving phrase for {}", playerId, e);
            }
        }

        private void writeWavFile(byte[] data, File file) throws IOException {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, CHANNELS, true, false);
            try (AudioInputStream stream = new AudioInputStream(
                    new ByteArrayInputStream(data), format, data.length / 2)) {
                AudioSystem.write(stream, AudioFileFormat.Type.WAVE, file);
            }
        }

        public void finalizeSession() {
            silenceChecker.shutdownNow();
            saveCurrentPhrase();
            decoder.close();

            try {
                File output = new File("voice_" + playerId + "_final.wav");
                mergeWavFiles(clips, output);
                log.info("Final WAV saved: {}", output.getAbsolutePath());
            } catch (Exception e) {
                log.error("Error merging WAV files for {}", playerId, e);
            }
            clips.forEach(File::delete);
        }

        private void mergeWavFiles(List<File> inputFiles, File outputFile) throws IOException, UnsupportedAudioFileException {
            if (inputFiles.isEmpty()) return;

            AudioInputStream appendedStream = null;
            for (File file : inputFiles) {
                AudioInputStream clipStream = AudioSystem.getAudioInputStream(file);
                if (appendedStream == null) {
                    appendedStream = clipStream;
                } else {
                    appendedStream = new AudioInputStream(
                            new SequenceInputStream(appendedStream, clipStream),
                            appendedStream.getFormat(),
                            appendedStream.getFrameLength() + clipStream.getFrameLength()
                    );
                }
            }

            if (appendedStream != null) {
                AudioSystem.write(appendedStream, AudioFileFormat.Type.WAVE, outputFile);
                appendedStream.close();
            }
        }
    }
}
