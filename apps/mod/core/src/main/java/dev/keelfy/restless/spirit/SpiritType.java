package dev.keelfy.restless.spirit;

import dev.keelfy.restless.haunt.EventTag;
import java.util.Map;

/**
 * @param kind who the spirit is bound to
 * @param affinity weight multiplier per event tag; missing tags count as 1
 * @param activity how often the spirit acts; 2 means twice as often
 */
public record SpiritType(String id, Kind kind, Map<EventTag, Double> affinity, double activity) {
    /** Personal spirits follow one player; territorial ones stay at an anchor and act on whoever is near it. */
    public enum Kind {
        PERSONAL,
        TERRITORIAL
    }

    public double affinity(EventTag tag) {
        return affinity.getOrDefault(tag, 1.0);
    }
}
