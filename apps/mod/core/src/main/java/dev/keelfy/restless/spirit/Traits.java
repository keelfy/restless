package dev.keelfy.restless.spirit;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** The trait catalog from the {@code traits} config section, and the roll that gives each spirit its own few. */
public final class Traits {
    private Traits() {}

    /**
     * The same seed always gives the same traits while the catalog is unchanged, so a save from before traits existed
     * gets them from its seed on load.
     */
    public static List<String> roll(long seed) {
        Config.Section director = Config.section("director");
        List<String> pool = new ArrayList<>(Config.section("traits").keys().stream().sorted().toList());
        Random random = new Random(seed);
        int min = director.integer("traits_min");
        int count = Math.min(pool.size(), min + random.nextInt(director.integer("traits_max") - min + 1));
        List<String> rolled = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            rolled.add(pool.remove(random.nextInt(pool.size())));
        }
        return rolled;
    }

    /** The traits behind the saved ids; ids the catalog no longer has are skipped. */
    public static List<Trait> of(List<String> ids) {
        Set<String> known = Config.section("traits").keys();
        return ids.stream().filter(known::contains).map(Traits::byId).toList();
    }

    public static double affinity(List<Trait> traits, EventTag tag) {
        double factor = 1.0;
        for (Trait trait : traits) {
            factor *= trait.affinity(tag);
        }
        return factor;
    }

    public static double activity(List<Trait> traits, Set<Trait.Condition> now) {
        double factor = 1.0;
        for (Trait trait : traits) {
            factor *= trait.activity(now);
        }
        return factor;
    }

    /** Ticks sooner a noise-fed spirit's next event comes after its player made {@code sounds} sounds. */
    public static double noise(List<Trait> traits, int sounds) {
        return traits.stream().mapToDouble(Trait::noise).sum() * sounds;
    }

    public static boolean reacts(List<Trait> traits, Trait.Trigger trigger) {
        return traits.stream().anyMatch(t -> t.reacts().contains(trigger));
    }

    private static Trait byId(String id) {
        Config.Section config = Config.section("traits." + id);
        Config.Section affinity = config.section("affinity");
        Map<EventTag, Double> tags = new EnumMap<>(EventTag.class);
        for (String tag : affinity.keys()) {
            tags.put(EventTag.valueOf(tag), affinity.num(tag));
        }
        Trait.Condition when = config.has("when") ? Trait.Condition.valueOf(config.text("when").toUpperCase(Locale.ROOT)) : Trait.Condition.ANY;
        List<Trait.Trigger> reacts = config.has("reacts")
            ? config.texts("reacts").stream().map(t -> Trait.Trigger.valueOf(t.toUpperCase(Locale.ROOT))).toList() : List.of();
        return new Trait(id, Map.copyOf(tags), config.has("activity") ? config.num("activity") : 1.0, when,
            config.has("noise") ? config.num("noise") : 0.0, reacts);
    }
}
