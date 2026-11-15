package dev.keelfy.restless.spirit;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.event.EventTag;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/** The trait catalog from the {@code traits} config section, and the roll that gives each spirit its own few. */
public final class Traits {
    /** Mixed into the seed so the hidden habit and the grown one do not follow the roll's own draws. */
    private static final long HIDDEN = 0x5EC12E7L;

    private static final long GROWN = 0x6120A7L;

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
     * Leanings are {@code director.desires} desires, then {@code director.leanings} fears or hates; last, with the
     * chance {@code director.veil_chance}, one veil. The same seed always gives the same traits while the catalog is
     * unchanged, so a save from before traits existed gets them from its seed on load.
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
        draw(rolled, catalog, t -> t.leans() == Trait.Lean.DESIRE, director.integer("desires"), random);
        draw(
                rolled,
                catalog,
                t -> t.kind() == Trait.Kind.LEANING && t.leans() != Trait.Lean.DESIRE,
                director.integer("leanings"),
                random);
        if (random.nextDouble() < director.num("veil_chance")) {
            draw(rolled, catalog, t -> t.kind() == Trait.Kind.VEIL, 1, random);
        }
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
        Config.Section section = Config.section("traits");
        Catalog catalog = cached;
        if (catalog == null || catalog.from() != section) {
            catalog = new Catalog(
                    section, section.keys().stream().sorted().map(Traits::byId).toList());
            cached = catalog;
        }
        return catalog.traits();
    }

    /** The catalog as built from one load of the config; rebuilt when the config is loaded again. */
    private record Catalog(Config.Section from, List<Trait> traits) {}

    private static volatile @Nullable Catalog cached;

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

    /**
     * The habit that every tool keeps quiet about until its player is shaken or the spirit has evolved; picked by the
     * seed. Null for a spirit with fewer than two habits, so its only habit is never hidden.
     */
    public static @Nullable Trait hidden(long seed, List<Trait> traits) {
        List<Trait> habits =
                traits.stream().filter(t -> t.kind() == Trait.Kind.HABIT).toList();
        return habits.size() < 2 ? null : habits.get(new Random(seed ^ HIDDEN).nextInt(habits.size()));
    }

    /**
     * The habit a spirit gains when it evolves: one of its own nature that it lacks, picked by the seed, so the nature
     * stays the majority; null when it has them all.
     */
    public static @Nullable String grown(long seed, List<Trait> traits) {
        Focus nature = weakness(traits);
        List<String> pool = all().stream()
                .filter(t -> t.kind() == Trait.Kind.HABIT && t.weakness() == nature && !traits.contains(t))
                .map(Trait::id)
                .toList();
        return pool.isEmpty() ? null : pool.get(new Random(seed ^ GROWN).nextInt(pool.size()));
    }

    /** The spirit's fear, if it has one: its events do not run beside the feared thing. */
    public static Optional<Trait> fear(List<Trait> traits) {
        return leaning(traits, Trait.Lean.FEAR);
    }

    /** The spirit's desire, if it has one: what calms it when offered. A spirit born before leanings has none. */
    public static Optional<Trait> desire(List<Trait> traits) {
        return leaning(traits, Trait.Lean.DESIRE);
    }

    /** The spirit's hate, if it has one: what angers it when its player uses it. */
    public static Optional<Trait> hate(List<Trait> traits) {
        return leaning(traits, Trait.Lean.HATE);
    }

    private static Optional<Trait> leaning(List<Trait> traits, Trait.Lean lean) {
        return traits.stream().filter(t -> t.leans() == lean).findFirst();
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
                config.has("weakness") ? Focus.valueOf(config.text("weakness").toUpperCase(Locale.ROOT)) : null,
                config.has("leans") ? Trait.Lean.valueOf(config.text("leans").toUpperCase(Locale.ROOT)) : null,
                config.has("things") ? config.texts("things") : List.of(),
                config.has("foils") ? Clue.valueOf(config.text("foils").toUpperCase(Locale.ROOT)) : null);
    }
}
