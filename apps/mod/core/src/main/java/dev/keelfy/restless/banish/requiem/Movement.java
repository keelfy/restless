package dev.keelfy.restless.banish.requiem;

/**
 * One movement of the Requiem: a short test of what the player learned against one type of spirit. The rite calls
 * {@link #start}, then {@link #tick} every tick, then {@link #passed} when the movement's time is out, and always
 * {@link #end} last, also when the rite is cut short, so a movement puts back everything it changed.
 */
public interface Movement {
    void start(Rite rite);

    /** @param n ticks since the movement began */
    default void tick(Rite rite, int n) {}

    /** Whether the player met the test; read once, when the time is out. */
    boolean passed(Rite rite);

    /** Removes what the movement spawned or changed. */
    default void end(Rite rite) {}
}
