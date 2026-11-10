package dev.keelfy.restless.content.item.charm;

import dev.keelfy.restless.content.Content;

/**
 * The run of the trickster's coin: wins in a row as of {@code at}, a game time. A win pays one more than the one
 * before; the run wears off by one for every day the coin is not tossed. No Minecraft types, so the math is tested on
 * its own; the codecs are in {@link Content#LUCK}.
 */
public record Luck(int streak, long at) {
    public static final Luck NONE = new Luck(0, 0L);

    private static final long DAY = 24000L;

    /** The streak that is left at {@code now}. */
    public int live(long now) {
        return (int) Math.max(0L, streak - Math.max(0L, now - at) / DAY);
    }

    /** What a win at {@code now} pays: the drops are multiplied by this. */
    public int payout(long now) {
        return 2 + live(now);
    }

    public Luck won(long now, int cap) {
        return new Luck(Math.min(cap, live(now) + 1), now);
    }

    public Luck lost(long now) {
        return new Luck(0, now);
    }
}
