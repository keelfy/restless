package dev.keelfy.restless.haunt.requiem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import org.jspecify.annotations.Nullable;

/**
 * The Requiem's life bar: the candles that were lit when it began, and which of them are out. Pure bookkeeping; the
 * rite puts the world in line with it. The Requiem stands while at least {@code needed} candles are lit.
 */
public final class Candles<T> {
    private final List<T> lit;
    private final List<T> out = new ArrayList<>();
    private final int needed;

    public Candles(List<T> all, int needed) {
        this.lit = new ArrayList<>(all);
        this.needed = needed;
    }

    public List<T> lit() {
        return List.copyOf(lit);
    }

    /** The candles that are out, in the order they went out. */
    public List<T> out() {
        return List.copyOf(out);
    }

    public boolean isOut(T candle) {
        return out.contains(candle);
    }

    /** Whether enough candles are lit for the Requiem to stand. */
    public boolean standing() {
        return lit.size() >= needed;
    }

    /**
     * Snuffs the lit candle at the index {@code pick} gives for the number of lit ones.
     *
     * @return the candle, or null if none was lit
     */
    public @Nullable T snuff(IntUnaryOperator pick) {
        if (lit.isEmpty()) {
            return null;
        }
        T candle = lit.remove(pick.applyAsInt(lit.size()));
        out.add(candle);
        return candle;
    }

    /** Snuffs {@code count} candles, as {@link #snuff} does one; @return those that went out */
    public List<T> snuff(int count, IntUnaryOperator pick) {
        List<T> gone = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            T candle = snuff(pick);
            if (candle != null) {
                gone.add(candle);
            }
        }
        return gone;
    }
}
