package org.mryd.api.simple;

import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import lombok.Getter;

@Getter
public class DecoderWrapper {
    private static final long DECODER_TIMEOUT_MS = 1000;

    private final OpusDecoder decoder;
    private long lastUsed;

    public DecoderWrapper(OpusDecoder decoder) {
        this.decoder = decoder;
        updateLastUsed();
    }

    public void updateLastUsed() {
        lastUsed = System.currentTimeMillis();
    }

    public boolean isExpired() {
        return isExpired(System.currentTimeMillis());
    }

    public boolean isExpired(long now) {
        return now - lastUsed > DECODER_TIMEOUT_MS;
    }
}