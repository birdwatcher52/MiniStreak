package com.birdwatcher52.ministreak.util;

import javax.sound.sampled.*;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static javax.sound.sampled.LineEvent.Type.CLOSE;
import static javax.sound.sampled.LineEvent.Type.STOP;

/**
 * Tiny, dependency-free WAV player for short UI pips:
 * - Preloads WAVs to 16-bit PCM in memory.
 * - Creates a fresh Clip per play (overlap-safe) with a small cap.
 * - Per-key debounce window.
 * - No globals; make one instance per plugin/module.
 */
public final class TinyWav {
    private static final int DEFAULT_DEBOUNCE_MS = 90;
    private static final int MAX_CONCURRENT_CLIPS = 4;

    private final Map<String, PcmBuffer> cache = new ConcurrentHashMap<>();
    private final Map<String, Long> lastPlayNs = new ConcurrentHashMap<>();

    // --- Public API ----------------------------------------------------------

    /** Register a classpath WAV (e.g., "/com/you/sfx/hover.wav") under a key ("hover"). */
    public void registerResource(String key, String resourcePath) throws IOException, UnsupportedAudioFileException {
        URL url = TinyWav.class.getResource(resourcePath);
        if (url == null) throw new FileNotFoundException("Classpath WAV not found: " + resourcePath);
        cache.put(key, loadToPcm(url));
    }

    /** Optional convenience: derive key from filename (no extension). */
    public void registerResourceAutoKey(String resourcePath) throws IOException, UnsupportedAudioFileException {
        registerResource(basenameNoExt(resourcePath), resourcePath);
    }

    /** Also supports filesystem WAVs if you ever need it. */
    public void registerFile(String key, Path path) throws IOException, UnsupportedAudioFileException {
        if (!Files.exists(path)) throw new FileNotFoundException(path.toString());
        cache.put(key, loadToPcm(path.toUri().toURL()));
    }

    /** Play a registered key at 0–100 volume (per-call). Debounced by DEFAULT_DEBOUNCE_MS. */
    public void play(String key, int volumePercent) {
        play(key, volumePercent, DEFAULT_DEBOUNCE_MS);
    }

    /** Play with explicit debounce window (ms). */
    public void play(String key, int volumePercent, int debounceMs) {
        final PcmBuffer pcm = cache.get(key);
        if (pcm == null) return;                 // not registered
        if (volumePercent <= 0) return;          // muted
        if (!allowed(key, debounceMs)) return;   // per-key rate limit
        playOnce(pcm, volumePercent);
    }

    /** Optional cleanup (clips already playing will still finish). */
    public void clear() {
        cache.clear();
        lastPlayNs.clear();
    }

    // --- Internals -----------------------------------------------------------

    private boolean allowed(String key, int debounceMs) {
        final long win = Math.max(0, debounceMs) * 1_000_000L;
        final long now = System.nanoTime();
        return lastPlayNs.compute(key, (k, last) -> (last == null || now - last >= win) ? now : last) == now;
    }

    private void playOnce(PcmBuffer pcm, int volPercent) {
        if (pcm.getActive() >= MAX_CONCURRENT_CLIPS) return; // small safety cap

        try {
            Clip clip = AudioSystem.getClip();

            // Re-wrap PCM bytes in a fresh stream for this play
            ByteArrayInputStream bais = new ByteArrayInputStream(pcm.data);
            AudioInputStream ais = new AudioInputStream(bais, pcm.format, pcm.frameLength);

            clip.addLineListener(ev -> {
                if (ev.getType() == STOP || ev.getType() == CLOSE) {
                    try { clip.close(); } catch (Exception ignored) {}
                    pcm.decActive();
                }
            });

            clip.open(ais); // copies PCM into the Clip buffer
            try {
                setGain(clip, volPercent);
                pcm.incActive();
                clip.start(); // non-blocking
            } finally {
                try { ais.close(); } catch (IOException ignored) {}
            }
        } catch (LineUnavailableException | IllegalArgumentException e) {
            // mixer unavailable or no gain control — ignore for UI pips
        } catch (Exception ignored) {
            // keep helper fail-safe for gameplay
        }
    }

    private static void setGain(Clip clip, int volPercent) {
        try {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (volPercent >= 100) ? 0f
                    : (volPercent <= 0) ? gain.getMinimum()
                    : (float) (20.0 * Math.log10(Math.max(0.0001, volPercent / 100.0)));
            dB = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB));
            gain.setValue(dB);
        } catch (IllegalArgumentException ignored) {
            // no MASTER_GAIN on this mixer; let default volume play
        }
    }

    private static String basenameNoExt(String path) {
        String p = path.replace('\\', '/');
        int s = p.lastIndexOf('/');
        String f = (s >= 0) ? p.substring(s + 1) : p;
        int dot = f.lastIndexOf('.');
        return (dot >= 0) ? f.substring(0, dot) : f;
    }

    /** Load any WAV into 16-bit PCM in memory for instant Clip playback. */
    private static PcmBuffer loadToPcm(URL wavUrl) throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream in = AudioSystem.getAudioInputStream(wavUrl)) {
            AudioFormat src = in.getFormat();
            AudioFormat dst = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    src.getSampleRate(),
                    16,
                    src.getChannels(),
                    src.getChannels() * 2,
                    src.getSampleRate(),
                    false // little endian
            );
            AudioInputStream pcm = AudioSystem.getAudioInputStream(dst, in);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int r;
            while ((r = pcm.read(buf)) != -1) baos.write(buf, 0, r);

            byte[] data = baos.toByteArray();
            long frameLength = data.length / dst.getFrameSize();
            return new PcmBuffer(dst, data, frameLength);
        }
    }

    // --- Data holder ---------------------------------------------------------

    private static final class PcmBuffer {
        final AudioFormat format;
        final byte[] data;
        final long frameLength;
        private final AtomicInteger activeClips = new AtomicInteger(0);

        PcmBuffer(AudioFormat f, byte[] d, long frames) {
            this.format = f;
            this.data = d;
            this.frameLength = frames;
        }

        void incActive() { activeClips.incrementAndGet(); }
        void decActive() { activeClips.updateAndGet(v -> Math.max(0, v - 1)); }
        int  getActive() { return activeClips.get(); }
    }
}
