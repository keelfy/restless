package dev.keelfy.restless.banish;

/**
 * The parts of a ritual, in order: the call (the spirit comes in quietly), the bargain (it offers to stop for a gift),
 * the fury (it fights for the candles), the leaving (it lets go). Each part's rules live in its own class beside
 * {@link Ritual}: {@link Call}, {@link Bargain}, {@link Fury}, {@link Leaving}.
 */
public enum Phase {
    CALL,
    BARGAIN,
    FURY,
    LEAVING
}
