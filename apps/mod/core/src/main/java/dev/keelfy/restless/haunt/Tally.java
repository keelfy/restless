package dev.keelfy.restless.haunt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * How often each event id was rolled, ran, was stopped by a ward and could not run, kept with the world for /haunt
 * stats; unlike {@link History} it is never trimmed. Server-thread only.
 */
public final class Tally {
    /** What happened to an event. */
    public enum Kind {
        /** A spirit's roll chose it. */
        ROLLED,
        /** It ran (a waiting event: it started). */
        RAN,
        /** A ward stopped it. */
        WARDED,
        /** It was tried and could not run: it failed. */
        FAILED
    }

    public record Counts(int rolled, int ran, int warded, int failed) {
        public static final Counts NONE = new Counts(0, 0, 0, 0);

        Counts plus(Kind kind) {
            return switch (kind) {
                case ROLLED -> new Counts(rolled + 1, ran, warded, failed);
                case RAN -> new Counts(rolled, ran + 1, warded, failed);
                case WARDED -> new Counts(rolled, ran, warded + 1, failed);
                case FAILED -> new Counts(rolled, ran, warded, failed + 1);
            };
        }
    }

    private final Map<String, Counts> counts = new HashMap<>();

    public void add(String event, Kind kind) {
        counts.merge(event, Counts.NONE.plus(kind), (old, one) -> old.plus(kind));
    }

    public Counts of(String event) {
        return counts.getOrDefault(event, Counts.NONE);
    }

    public Map<String, Counts> all() {
        return Map.copyOf(counts);
    }

    public void load(Map<String, Counts> saved) {
        counts.putAll(saved);
    }

    /** The ids, most runs first and by id among equals. */
    public List<String> ranked(Collection<String> ids) {
        List<String> sorted = new ArrayList<>(ids);
        sorted.sort(Comparator.comparingInt((String id) -> -of(id).ran()).thenComparing(Comparator.naturalOrder()));
        return sorted;
    }

    /** The ids that never ran, by id. */
    public List<String> never(Collection<String> ids) {
        return ids.stream().filter(id -> of(id).ran() == 0).sorted().toList();
    }

    /** One CSV line per id (header first), most runs first. */
    public String csv(Collection<String> ids) {
        StringBuilder out = new StringBuilder("event,rolled,ran,warded,failed\n");
        for (String id : ranked(ids)) {
            Counts c = of(id);
            out.append(id)
                    .append(',')
                    .append(c.rolled())
                    .append(',')
                    .append(c.ran())
                    .append(',')
                    .append(c.warded())
                    .append(',')
                    .append(c.failed())
                    .append('\n');
        }
        return out.toString();
    }
}
