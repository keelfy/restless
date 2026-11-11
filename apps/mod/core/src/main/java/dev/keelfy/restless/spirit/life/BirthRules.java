package dev.keelfy.restless.spirit.life;

import java.util.SplittableRandom;

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
        /** Another territorial spirit is too close to an effigy's place. */
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
     * Whether the structure of {@code id} that starts in chunk ({@code chunkX}, {@code chunkZ}) is haunted. Only the
     * world seed, the id and the chunk decide, so a seed always haunts the same structures.
     */
    static boolean haunted(long seed, String id, int chunkX, int chunkZ, double chance) {
        long key = seed;
        key = key * 31 + id.hashCode();
        key = key * 31 + chunkX;
        key = key * 31 + chunkZ;
        return new SplittableRandom(key).nextDouble() < chance;
    }

    /**
     * Whether a territorial spirit may be born at a place (an effigy): not if one already stands within
     * {@code spacing} blocks.
     *
     * @param nearestSqr squared distance to the nearest anchor in the dimension; {@link Long#MAX_VALUE} for none
     * @return {@link Verdict#BORN} or {@link Verdict#CROWDED}
     */
    static Verdict settle(long nearestSqr, int spacing) {
        return nearestSqr < (long) spacing * spacing ? Verdict.CROWDED : Verdict.BORN;
    }
}
