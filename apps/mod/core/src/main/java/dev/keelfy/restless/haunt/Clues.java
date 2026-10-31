package dev.keelfy.restless.haunt;

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
     * @param owned the spirit's traits
     * @param catalog every trait, where a lie is taken from
     * @return empty when none of the spirit's traits leaves this clue
     */
    public static Optional<Trait> reveal(List<Trait> owned, List<Trait> catalog, Clue clue, Random random) {
        List<Trait> leaving =
                owned.stream().filter(t -> t.clues().contains(clue)).toList();
        if (leaving.isEmpty()) {
            return Optional.empty();
        }
        boolean speaks = clue == Clue.VOICE || clue == Clue.PLANCHETTE;
        if (speaks
                && owned.stream().anyMatch(Trait::lies)
                && random.nextDouble() < Config.section("clues").num("lie_chance")) {
            List<Trait> others =
                    catalog.stream().filter(t -> !owned.contains(t)).toList();
            if (!others.isEmpty()) {
                return Optional.of(others.get(random.nextInt(others.size())));
            }
        }
        return Optional.of(leaving.get(random.nextInt(leaving.size())));
    }
}
