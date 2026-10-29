package dev.keelfy.restless.spirit;

import dev.keelfy.restless.haunt.EventTag;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A quirk that sets two spirits of one type apart.
 *
 * @param affinity weight multiplier per event tag; missing tags count as 1
 * @param activity how much more often the spirit acts while {@code when} holds; 2 means twice as often
 * @param noise ticks the next event comes sooner for every sound its player makes; 0 for a spirit that ignores noise
 * @param reacts player actions that can make the spirit act right away
 */
public record Trait(String id, Map<EventTag, Double> affinity, double activity, Condition when, double noise, List<Trigger> reacts) {
    /** What the spirit's surroundings must be like for its activity modifier to apply. */
    public enum Condition {
        ANY,
        RAIN,
        NIGHT,
        DARK
    }

    /** A player action a spirit can react to. */
    public enum Trigger {
        /** Opens a chest or barrel. */
        CHEST,
        /** Uses a bed. */
        BED,
        /** Goes below y 0. */
        DEEP,
        /** Kills a mob. */
        KILL
    }

    public double affinity(EventTag tag) {
        return affinity.getOrDefault(tag, 1.0);
    }

    public double activity(Set<Condition> now) {
        return when == Condition.ANY || now.contains(when) ? activity : 1.0;
    }
}
