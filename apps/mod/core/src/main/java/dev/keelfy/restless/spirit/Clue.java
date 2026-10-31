package dev.keelfy.restless.spirit;

/**
 * A way a spirit can be told apart; each trait lists the ones it leaves, and a tool that reads a clue the spirit does
 * not leave finds nothing.
 */
public enum Clue {
    /** Ash on the floor shows prints. */
    FOOTPRINTS,
    /** The camera catches a silhouette. */
    SILHOUETTE,
    /** The spirit box answers. */
    VOICE,
    /** The ouija board's planchette moves. */
    PLANCHETTE
}
