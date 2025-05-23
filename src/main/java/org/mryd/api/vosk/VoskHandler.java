package org.mryd.api.vosk;

import lombok.extern.slf4j.Slf4j;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import static org.mryd.VoiceScript.instance;

@Slf4j
public class VoskHandler {

    private static final float SAMPLE_RATE = 48000.0f;
    private static final int BUFFER_SIZE = 4096;
    private static Model model;

    public static void init() {
        try {
            String modelPath = instance.getDataPath() + "/" + "model";
            model = new Model(new File(modelPath).getCanonicalPath());
            log.info("Vosk model loaded successfully from: {}", modelPath);
        } catch (IOException e) {
            log.error("Failed to load Vosk model: {}", e.getMessage(), e);
            // optionally throw a RuntimeException if model loading is critical
            throw new ExceptionInInitializerError(e);
        }
    }

    public static CompletableFuture<String> recognizeFromBytesAsync(byte[] audioBytes) {
        return recognizeFromPcmStreamAsync(new ByteArrayInputStream(audioBytes));
    }

    public static CompletableFuture<String> recognizeFromShortArrayAsync(short[] pcmSamples) {
        byte[] byteData = shortsToBytes(pcmSamples);
        return recognizeFromPcmStreamAsync(new ByteArrayInputStream(byteData));
    }

    private static CompletableFuture<String> recognizeFromPcmStreamAsync(InputStream pcmStream) {
        return CompletableFuture.supplyAsync(() -> {
            try (Recognizer recognizer = new Recognizer(model, SAMPLE_RATE);
                 AudioInputStream audioInputStream = new AudioInputStream(
                         pcmStream,
                         getPcmAudioFormat(),
                         AudioSystem.NOT_SPECIFIED)) {

                byte[] buffer = new byte[BUFFER_SIZE];
                while (audioInputStream.read(buffer) != -1) {
                    recognizer.acceptWaveForm(buffer, buffer.length);
                }
                return recognizer.getFinalResult();

            } catch (IOException e) {
                log.error("Error during recognition: {}", e.getMessage(), e);
                return null;
            }
        });
    }

    private static AudioFormat getPcmAudioFormat() {
        return new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    }

    public static byte[] shortsToBytes(short[] samples) {
        ByteBuffer buffer = ByteBuffer.allocate(samples.length * 2);
        for (short sample : samples) {
            buffer.putShort(sample);
        }
        return buffer.array();
    }
}
