package dev.keelfy.restless.spirit;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.event.EventTag;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** The trait catalog from the {@code traits} config section, and the roll that gives each spirit its own few. */
public final class Traits {
    private Traits() {}

    /** The roll with every nature equally likely: the rule for a save whose spirit was born before types leaned. */
    public static List<String> roll(long seed) {
        return roll(seed, Map.of());
    }

    /**
     * The roll of a spirit born now: its nature is drawn by the weights in {@code spirits.<type>.natures}, so a type
     * leans to a nature; a type without them, and a nature it leaves out, weighs 1.
     */
    public static List<String> roll(long seed, String spirit) {
        Config.Section type = Config.section("spirits." + spirit);
        Map<Focus, Double> natures = new EnumMap<>(Focus.class);
        if (type.has("natures")) {
            Config.Section weights = type.section("natures");
            for (String focus : weights.keys()) {
                natures.put(Focus.valueOf(focus), weights.num(focus));
            }
        }
        return roll(seed, natures);
    }

    /**
     * The spirit's nature is picked first, by {@code natures} (a nature missing weighs 1). Each axis is then drawn on
     * its own: habits all but one from the nature's group and the last from any other, so the nature is always the
     * strict majority of the habits (see {@link #weakness}); then conditions; then leanings. An axis the catalog has
     * too few traits for gives what it has, and the foreign habit joins only beside at least two of the nature's own.
     * The same seed always gives the same traits while the catalog is unchanged, so a save from before traits existed
     * gets them from its seed on load.
     */
    private static List<String> roll(long seed, Map<Focus, Double> natures) {
        Config.Section director = Config.section("director");
        Random random = new Random(seed);
        List<Trait> catalog = all();
        Focus nature = nature(random, natures);
        List<String> rolled = new ArrayList<>();
        draw(
                rolled,
                catalog,
                t -> t.kind() == Trait.Kind.HABIT && t.weakness() == nature,
                count(random, director, "habits_min", "habits_max") - 1,
                random);
        if (rolled.size() >= 2) {
            draw(rolled, catalog, t -> t.kind() == Trait.Kind.HABIT && t.weakness() != nature, 1, random);
        }
        draw(
                rolled,
                catalog,
                t -> t.kind() == Trait.Kind.CONDITION,
                count(random, director, "conditions_min", "conditions_max"),
                random);
        draw(rolled, catalog, t -> t.kind() == Trait.Kind.LEANING, director.integer("leanings"), random);
        return rolled;
    }

    private static Focus nature(Random random, Map<Focus, Double> natures) {
        double total = 0;
        for (Focus focus : Focus.values()) {
            total += natures.getOrDefault(focus, 1.0);
        }
        double left = random.nextDouble() * total;
        for (Focus focus : Focus.values()) {
            left -= natures.getOrDefault(focus, 1.0);
            if (left < 0) {
                return focus;
            }
        }
        return Focus.values()[Focus.values().length - 1];
    }

    private static int count(Random random, Config.Section director, String min, String max) {
        int low = director.integer(min);
        return low + random.nextInt(director.integer(max) - low + 1);
    }

    /** Adds up to {@code count} distinct ids of the traits that pass {@code pick}, fewer when the pool runs out. */
    private static void draw(List<String> into, List<Trait> catalog, Predicate<Trait> pick, int count, Random random) {
        List<String> pool =
                new ArrayList<>(catalog.stream().filter(pick).map(Trait::id).toList());
        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            into.add(pool.remove(random.nextInt(pool.size())));
        }
    }

    /** The traits behind the saved ids; ids the catalog no longer has are skipped. */
    public static List<Trait> of(List<String> ids) {
        Set<String> known = Config.section("traits").keys();
        return ids.stream().filter(known::contains).map(Traits::byId).toList();
    }

    /** Every trait in the catalog. */
    public static List<Trait> all() {
        return Config.section("traits").keys().stream()
                .sorted()
                .map(Traits::byId)
                .toList();
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

    /**
     * The focus most of the habits name. A spirit rolled by {@link #roll} never ties: it is its nature. A tie, and a
     * spirit with no habit, fall back to the vote of every trait that carries a focus (conditions keep the one they had
     * before the axes existed), so a saved spirit keeps the nature it had wherever its habits allow; a tie that is left
     * goes to the focus declared first in {@link Focus}.
     */
    public static Focus weakness(List<Trait> traits) {
        int[] habits = new int[Focus.values().length];
        int[] all = new int[Focus.values().length];
        for (Trait trait : traits) {
            if (trait.weakness() != null) {
                all[trait.weakness().ordinal()]++;
                if (trait.kind() == Trait.Kind.HABIT) {
                    habits[trait.weakness().ordinal()]++;
                }
            }
        }
        Focus best = Focus.values()[0];
        for (Focus focus : Focus.values()) {
            int i = focus.ordinal();
            int b = best.ordinal();
            if (habits[i] > habits[b] || habits[i] == habits[b] && all[i] > all[b]) {
                best = focus;
            }
        }
        return best;
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
        Trait.Condition when = config.has("when")
                ? Trait.Condition.valueOf(config.text("when").toUpperCase(Locale.ROOT))
                : Trait.Condition.ANY;
        List<Trait.Trigger> reacts = config.has("reacts")
                ? config.texts("reacts").stream()
                        .map(t -> Trait.Trigger.valueOf(t.toUpperCase(Locale.ROOT)))
                        .toList()
                : List.of();
        Set<Clue> clues = config.has("clues")
                ? config.texts("clues").stream()
                        .map(c -> Clue.valueOf(c.toUpperCase(Locale.ROOT)))
                        .collect(Collectors.toCollection(() -> EnumSet.noneOf(Clue.class)))
                : EnumSet.noneOf(Clue.class);
        return new Trait(
                id,
                Trait.Kind.valueOf(config.text("kind").toUpperCase(Locale.ROOT)),
                Map.copyOf(tags),
                config.has("activity") ? config.num("activity") : 1.0,
                when,
                config.has("noise") ? config.num("noise") : 0.0,
                reacts,
                Set.copyOf(clues),
                config.texts("words"),
                config.texts("planchette"),
                config.has("lies") && config.num("lies") > 0,
                config.has("weakness") ? Focus.valueOf(config.text("weakness").toUpperCase(Locale.ROOT)) : null);
    }
}
