package org.mryd.api.simple;

import java.util.ArrayList;
import java.util.List;

public class VoiceBuffer {
    private final List<short[]> chunks = new ArrayList<>();
    private long lastUpdate = System.nanoTime();

    synchronized void append(short[] pcmChunk) {
        chunks.add(pcmChunk);
        lastUpdate = System.nanoTime();
    }

    public synchronized boolean hasData() {
        return !chunks.isEmpty();
    }

    public synchronized long getLastUpdate() {
        return lastUpdate;
    }

    public synchronized short[] consumeBuffer() {
        int totalLength = chunks.stream().mapToInt(c -> c.length).sum();
        short[] full = new short[totalLength];
        int pos = 0;
        for (short[] chunk : chunks) {
            System.arraycopy(chunk, 0, full, pos, chunk.length);
            pos += chunk.length;
        }
        chunks.clear();
        return full;
    }
}
