package dev.keelfy.restless.spirit;

import dev.keelfy.restless.haunt.EventTag;
import java.util.Map;

/**
 * @param affinity weight multiplier per event tag; missing tags count as 1
 * @param activity how often the spirit acts; 2 means twice as often
 */
public record SpiritType(String id, Map<EventTag, Double> affinity, double activity) {
    public double affinity(EventTag tag) {
        return affinity.getOrDefault(tag, 1.0);
    }
}
