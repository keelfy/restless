package dev.keelfy.restless.spirit;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Spirits {
    public static final SpiritType POLTERGEIST = of("poltergeist");
    public static final SpiritType SHADE = of("shade");

    private static final List<SpiritType> ALL = List.of(POLTERGEIST, SHADE);

    private Spirits() {}

    public static List<SpiritType> all() {
        return ALL;
    }

    public static Optional<SpiritType> byId(String id) {
        return ALL.stream().filter(s -> s.id().equals(id)).findFirst();
    }

    /** Affinities and activity come from the {@code spirits.<id>} config section. */
    private static SpiritType of(String id) {
        Config.Section config = Config.section("spirits." + id);
        Config.Section affinity = config.section("affinity");
        Map<EventTag, Double> tags = new EnumMap<>(EventTag.class);
        for (String tag : affinity.keys()) {
            tags.put(EventTag.valueOf(tag), affinity.num(tag));
        }
        return new SpiritType(id, Map.copyOf(tags), config.num("activity"));
    }
}
