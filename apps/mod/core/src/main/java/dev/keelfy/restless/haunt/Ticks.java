package dev.keelfy.restless.haunt;

/** Fixed game-time units; everything tunable lives in the config instead. */
public final class Ticks {
    public static final int SECOND = 20;
    public static final int MINUTE = 60 * SECOND;
    public static final long DAY = 24000L;

    private Ticks() {}
}
