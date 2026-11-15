package dev.keelfy.restless.banish;

import java.util.Optional;

/**
 * The parts of a ritual, in order: the call (the spirit comes in quietly), the bargain (it offers to stop for a gift),
 * the fury (it fights for the candles), the leaving (it lets go). Each part's rules live in its own class beside
 * {@link Ritual}: {@link Call}, {@link Bargain}, {@link Fury}, {@link Leaving}.
 */
public enum Phase {
    CALL,
    BARGAIN,
    FURY,
    LEAVING;

    /** Whether the fury's loop should be heard: only while the fury runs, not in another phase or after the end. */
    public static boolean furyHeard(Optional<Phase> phase) {
        return phase.filter(now -> now == FURY).isPresent();
    }
}
