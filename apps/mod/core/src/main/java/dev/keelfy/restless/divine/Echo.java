package dev.keelfy.restless.divine;

import java.util.Random;

/**
 * A word the asker said, as the spirit box gives it back in the asker's own voice: cut short, stuttered or whole as the
 * text shows it, then lower and slower, through a narrow band, crushed and under static.
 */
final class Echo {
    static final int CUT = 0;
    static final int STUTTER = 1;
    static final int WHOLE = 2;
    /** How the word is mangled is picked among this many ways. */
    static final int WAYS = 3;

    private static final double LOW = 300;
    private static final double HIGH = 3000;
    /** A stutter repeats this much of the word's start, then pauses half as long. */
    private static final double STUTTER_SECONDS = 0.12;

    private Echo() {}

    /** The word's text as the box says it. */
    static String text(String word, int how) {
        return switch (how) {
            case CUT -> word.substring(0, word.length() / 2 + 1) + "—";
            case STUTTER -> word.charAt(0) + "-" + word;
            default -> word;
        };
    }

    /** The word's sound, mono at {@code rate}, as 16-bit little endian samples at {@link SpiritBoxPayload#RATE}. */
    static byte[] voice(short[] word, int rate, int how, Random random) {
        float[] in = shape(word, rate, how);
        // Read slower than it was said: lower and slower at once, as a tape dragging.
        double step = (double) rate / SpiritBoxPayload.RATE * (0.78 + 0.1 * random.nextDouble());
        float[] out = new float[(int) (in.length / step)];
        double dt = 1.0 / SpiritBoxPayload.RATE;
        double highRc = 1 / (2 * Math.PI * LOW);
        double lowRc = 1 / (2 * Math.PI * HIGH);
        double highAlpha = highRc / (highRc + dt);
        double lowAlpha = dt / (lowRc + dt);
        double high = 0;
        double low = 0;
        double last = 0;
        float peak = 1e-6f;
        for (int i = 0; i < out.length; i++) {
            double at = i * step;
            int j = (int) at;
            double next = j + 1 < in.length ? in[j + 1] : 0;
            double sample = in[j] + (next - in[j]) * (at - j);
            high = highAlpha * (high + sample - last);
            last = sample;
            low += lowAlpha * (high - low);
            out[i] = (float) low;
            peak = Math.max(peak, Math.abs(out[i]));
        }
        byte[] pcm = new byte[out.length * 2];
        float held = 0;
        for (int i = 0; i < out.length; i++) {
            if (i % 2 == 0) {
                // Crushed: every other sample held, 64 steps loud to quiet.
                held = Math.round(out[i] / peak * 32) / 32f;
            }
            double hiss = (random.nextDouble() - 0.5) * 0.12;
            int value = (int) Math.max(-32768, Math.min(32767, (held * 0.8 + hiss) * 32767));
            pcm[2 * i] = (byte) value;
            pcm[2 * i + 1] = (byte) (value >> 8);
        }
        return pcm;
    }

    private static float[] shape(short[] word, int rate, int how) {
        int stutter = Math.min(word.length, (int) (rate * STUTTER_SECONDS));
        int length = switch (how) {
            case CUT -> word.length / 2 + word.length / 10;
            case STUTTER -> stutter + stutter / 2 + word.length;
            default -> word.length;
        };
        float[] out = new float[Math.max(1, length)];
        int from = how == STUTTER ? stutter + stutter / 2 : 0;
        if (how == STUTTER) {
            for (int i = 0; i < stutter; i++) {
                out[i] = word[i] / 32768f;
            }
        }
        for (int i = 0; from + i < length && i < word.length; i++) {
            out[from + i] = word[i] / 32768f;
        }
        return out;
    }
}
