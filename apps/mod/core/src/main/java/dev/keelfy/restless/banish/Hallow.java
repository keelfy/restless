package dev.keelfy.restless.banish;

/**
 * Place and hour: a ritual held near the spirit's gravestone, or begun around midnight, is hallowed. Its fury runs
 * longer and the banished spirit leaves one more essence. Pure rules only, so they test without a world.
 */
public final class Hallow {
    private Hallow() {}

    /**
     * @param graveDistance blocks from the altar to the spirit's gravestone, infinite when it has none
     * @param dayTime overworld day time; midnight is 18000
     * @param midnightFrom first tick of the day (0..23999) that counts as midnight
     * @param midnightTo first tick after it
     */
    public static boolean hallowed(
            double graveDistance, double graveRadius, long dayTime, int midnightFrom, int midnightTo) {
        long hour = Math.floorMod(dayTime, 24000L);
        return graveDistance <= graveRadius || hour >= midnightFrom && hour < midnightTo;
    }

    /** One more essence for a hallowed ritual. */
    public static int essences(boolean hallowed) {
        return hallowed ? 1 : 0;
    }
}
