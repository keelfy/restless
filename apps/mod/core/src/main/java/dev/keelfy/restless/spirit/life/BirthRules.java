package dev.keelfy.restless.spirit.life;

/** What a deed does to a player's spirits, apart from the world: pure, so it can be tested without Minecraft. */
public final class BirthRules {
    /** What a deed did. */
    public enum Verdict {
        /** A new spirit was bound. */
        BORN,
        /** The player already had a spirit of that type; the deed angered it. */
        FED,
        /** The newest spirit is too young. */
        COOLDOWN,
        /** The player has as many personal spirits as allowed. */
        FULL,
        /** Another territorial spirit is too close to a generated structure. */
        CROWDED,
        /** The chance roll failed or the player cannot be haunted. */
        NOTHING
    }

    private BirthRules() {}

    /**
     * In order: a spirit of the type already there is fed; else the newest spirit must be at least {@code cooldown}
     * old; else there must be room. The cooldown reads {@code bornAt}, so a restart cannot reset it.
     *
     * @param newestBornAt {@code bornAt} of the player's newest spirit; ignored when {@code count} is 0
     */
    static Verdict decide(boolean hasType, int count, long newestBornAt, long now, long cooldown, int max) {
        if (hasType) {
            return Verdict.FED;
        }
        if (count > 0 && now - newestBornAt < cooldown) {
            return Verdict.COOLDOWN;
        }
        return count >= max ? Verdict.FULL : Verdict.BORN;
    }

    /**
     * Whether a territorial spirit may be born at a generated structure: not if one already stands within
     * {@code spacing} blocks.
     *
     * @param nearestSqr squared distance to the nearest anchor in the dimension; {@link Long#MAX_VALUE} for none
     * @return {@link Verdict#BORN} or {@link Verdict#CROWDED}
     */
    static Verdict settle(long nearestSqr, int spacing) {
        return nearestSqr < (long) spacing * spacing ? Verdict.CROWDED : Verdict.BORN;
    }
}
