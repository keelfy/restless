package dev.keelfy.restless.spirit;

import dev.keelfy.restless.haunt.EventTag;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @param kind who the spirit is bound to
 * @param affinity weight multiplier per event tag; missing tags count as 1
 * @param activity how often the spirit acts while {@code when} holds; 2 means twice as often
 * @param when what the surroundings must be like for {@code activity} to apply; otherwise the spirit acts at 1
 * @param reacts player actions that can make a personal spirit of this type act right away, as a trait's do
 */
public record SpiritType(String id, Kind kind, Map<EventTag, Double> affinity, double activity, Trait.Condition when,
                         List<Trait.Trigger> reacts) {
    /** Personal spirits follow one player; territorial ones stay at an anchor and act on whoever is near it. */
    public enum Kind {
        PERSONAL,
        TERRITORIAL
    }

    public double affinity(EventTag tag) {
        return affinity.getOrDefault(tag, 1.0);
    }

    public double activity(Set<Trait.Condition> now) {
        return when == Trait.Condition.ANY || now.contains(when) ? activity : 1.0;
    }
}
