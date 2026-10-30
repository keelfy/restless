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
    public static final SpiritType NIX = of("nix");
    public static final SpiritType MIMIC = of("mimic");
    public static final SpiritType WRAITH = of("wraith");
    public static final SpiritType MARA = of("mara");
    public static final SpiritType BANSHEE = of("banshee");
    public static final SpiritType REVENANT = of("revenant");
    public static final SpiritType LOST_ONE = of("lost_one");
    public static final SpiritType DOMOVOI = of("domovoi");
    public static final SpiritType HOARDER = of("hoarder");
    public static final SpiritType DEMON = of("demon");
    public static final SpiritType TRICKSTER = of("trickster");

    private static final List<SpiritType> ALL = List.of(POLTERGEIST, SHADE, PHANTOM, NIX, MIMIC, WRAITH, MARA, BANSHEE, REVENANT,
        LOST_ONE, DOMOVOI, HOARDER, DEMON, TRICKSTER);

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

    /** Kind, affinities, activity and reactions come from the {@code spirits.<id>} config section. */
    private static SpiritType of(String id) {
        Config.Section config = Config.section("spirits." + id);
        Config.Section affinity = config.section("affinity");
        Map<EventTag, Double> tags = new EnumMap<>(EventTag.class);
        for (String tag : affinity.keys()) {
            tags.put(EventTag.valueOf(tag), affinity.num(tag));
        }
        Trait.Condition when = config.has("when") ? Trait.Condition.valueOf(config.text("when").toUpperCase(Locale.ROOT)) : Trait.Condition.ANY;
        List<Trait.Trigger> reacts = config.has("reacts")
            ? config.texts("reacts").stream().map(t -> Trait.Trigger.valueOf(t.toUpperCase(Locale.ROOT))).toList() : List.of();
        return new SpiritType(id, SpiritType.Kind.valueOf(config.text("kind").toUpperCase(Locale.ROOT)), Map.copyOf(tags), config.num("activity"),
            when, reacts);
    }
}
