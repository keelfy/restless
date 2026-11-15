package dev.keelfy.restless.divine;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.spirit.Clue;
import dev.keelfy.restless.spirit.Trait;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * What a clue shows of a spirit: one of its traits on the tool's own axis (see {@link #reads}), or, for a lying spirit,
 * sometimes a trait of that axis it does not have.
 */
public final class Clues {
    private Clues() {}

    /**
     * A tool answers while a spirit has a trait on its axis: the traits that leave this clue are only likelier, by
     * {@code clues.preferred_weight}, to be the one shown.
     *
     * @param owned the spirit's traits
     * @param catalog every trait, where a lie is taken from
     * @return empty when {@code owned} has no trait the tool reads, or a veil in it hides the spirit from this tool
     */
    public static Optional<Trait> reveal(List<Trait> owned, List<Trait> catalog, Clue clue, Random random) {
        return reveal(owned, catalog, clue, random, false);
    }

    /** Whether a veil among {@code owned} hides the spirit from the tool behind {@code clue}. */
    public static boolean foiled(List<Trait> owned, Clue clue) {
        return owned.stream().anyMatch(t -> t.foils() == clue);
    }

    /**
     * Whether the tool behind {@code clue} reads {@code trait}: the spirit box hears what the spirit wants or fears
     * (leanings), the ouija board spells what it does (habits), ash shows when it walks (conditions) and the camera,
     * which only names the type, any trait.
     */
    public static boolean reads(Clue clue, Trait trait) {
        return switch (clue) {
            case VOICE -> trait.kind() == Trait.Kind.LEANING;
            case PLANCHETTE -> trait.kind() == Trait.Kind.HABIT;
            case FOOTPRINTS -> trait.kind() == Trait.Kind.CONDITION;
            case SILHOUETTE -> true;
        };
    }

    /** Whether the tool behind {@code clue} speaks, and so may lie. */
    public static boolean speaks(Clue clue) {
        return clue == Clue.VOICE || clue == Clue.PLANCHETTE;
    }

    /** As above; {@code honest} makes this one reveal tell no lie. */
    public static Optional<Trait> reveal(
            List<Trait> all, List<Trait> catalog, Clue clue, Random random, boolean honest) {
        List<Trait> owned = all.stream().filter(t -> reads(clue, t)).toList();
        if (owned.isEmpty() || foiled(all, clue)) {
            return Optional.empty();
        }
        boolean speaks = speaks(clue) && !honest;
        if (speaks
                && all.stream().anyMatch(Trait::lies)
                && random.nextDouble() < Config.section("clues").num("lie_chance")) {
            List<Trait> others = catalog.stream()
                    .filter(t -> reads(clue, t) && !all.contains(t))
                    .toList();
            if (!others.isEmpty()) {
                return Optional.of(others.get(random.nextInt(others.size())));
            }
        }
        int preferred = Config.section("clues").integer("preferred_weight");
        int total = owned.stream().mapToInt(t -> weight(t, clue, preferred)).sum();
        int pick = random.nextInt(total);
        for (Trait trait : owned) {
            pick -= weight(trait, clue, preferred);
            if (pick < 0) {
                return Optional.of(trait);
            }
        }
        throw new IllegalStateException("unreachable");
    }

    private static int weight(Trait trait, Clue clue, int preferred) {
        return trait.clues().contains(clue) ? preferred : 1;
    }
}
