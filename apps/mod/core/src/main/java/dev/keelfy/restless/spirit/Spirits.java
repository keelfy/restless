package dev.keelfy.restless.spirit;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class Spirits {
    public static final SpiritType POLTERGEIST = of("poltergeist");
    public static final SpiritType SHADE = of("shade");
    public static final SpiritType PHANTOM = of("phantom");

    private static final List<SpiritType> ALL = List.of(POLTERGEIST, SHADE, PHANTOM);

    private Spirits() {}

    public static List<SpiritType> all() {
        return ALL;
    }

    /** The types a player can be bound to; first-join binding draws from these. */
    public static List<SpiritType> personal() {
        return ALL.stream().filter(s -> s.kind() == SpiritType.Kind.PERSONAL).toList();
    }

    public static Optional<SpiritType> byId(String id) {
        return ALL.stream().filter(s -> s.id().equals(id)).findFirst();
    }

    /** Kind, affinities and activity come from the {@code spirits.<id>} config section. */
    private static SpiritType of(String id) {
        Config.Section config = Config.section("spirits." + id);
        Config.Section affinity = config.section("affinity");
        Map<EventTag, Double> tags = new EnumMap<>(EventTag.class);
        for (String tag : affinity.keys()) {
            tags.put(EventTag.valueOf(tag), affinity.num(tag));
        }
        return new SpiritType(id, SpiritType.Kind.valueOf(config.text("kind").toUpperCase(Locale.ROOT)), Map.copyOf(tags), config.num("activity"));
    }
}
