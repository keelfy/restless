package dev.keelfy.restless.divine;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.spirit.Clue;
import dev.keelfy.restless.spirit.Trait;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/** What a clue shows of a spirit: one of its traits, or, for a lying spirit, sometimes a trait it does not have. */
public final class Clues {
    private Clues() {}

    /**
     * Any tool answers while a spirit has a trait: the traits that leave this clue are only likelier, by
     * {@code clues.preferred_weight}, to be the one shown.
     *
     * @param owned the spirit's traits
     * @param catalog every trait, where a lie is taken from
     * @return empty only when {@code owned} is
     */
    public static Optional<Trait> reveal(List<Trait> owned, List<Trait> catalog, Clue clue, Random random) {
        return reveal(owned, catalog, clue, random, false);
    }

    /** Whether the tool behind {@code clue} speaks, and so may lie. */
    public static boolean speaks(Clue clue) {
        return clue == Clue.VOICE || clue == Clue.PLANCHETTE;
    }

    /** As above; {@code honest} makes this one reveal tell no lie. */
    public static Optional<Trait> reveal(
            List<Trait> owned, List<Trait> catalog, Clue clue, Random random, boolean honest) {
        if (owned.isEmpty()) {
            return Optional.empty();
        }
        boolean speaks = speaks(clue) && !honest;
        if (speaks
                && owned.stream().anyMatch(Trait::lies)
                && random.nextDouble() < Config.section("clues").num("lie_chance")) {
            List<Trait> others =
                    catalog.stream().filter(t -> !owned.contains(t)).toList();
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
