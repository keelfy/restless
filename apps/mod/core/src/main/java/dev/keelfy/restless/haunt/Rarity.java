package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import java.util.HashMap;
import java.util.Map;

/**
 * How seldom each event can run, learned from the director's own rolls. An event that needs a particular place (a ripe
 * field, a door, a shrieker) is rarely available, so when it is, its weight is boosted by the inverse of how often it
 * is, up to {@code director.rarity_boost_max}: the world's specific moments win over what can happen anywhere. Events
 * that can always run keep their weight. Pure math on ids, so it is unit-testable; server-thread only.
 */
public final class Rarity {
    private static final Map<String, Double> AVAILABILITY = new HashMap<>();

    private Rarity() {}

    /**
     * Records whether the event could run on this roll; a moving average with {@code director.rarity_memory} as the
     * step.
     */
    public static void observe(String id, boolean ready) {
        double sample = ready ? 1.0 : 0.0;
        double step = Config.section("director").num("rarity_memory");
        AVAILABILITY.merge(id, sample, (old, now) -> old + step * (now - old));
    }

    /** At least 1: the inverse of the event's availability, capped; 1 for an event never seen. */
    public static double boost(String id) {
        return Math.min(Config.section("director").num("rarity_boost_max"), 1.0 / AVAILABILITY.getOrDefault(id, 1.0));
    }

    static void reset() {
        AVAILABILITY.clear();
    }
}
