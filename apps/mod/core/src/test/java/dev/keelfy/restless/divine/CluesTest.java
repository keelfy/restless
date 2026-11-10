package dev.keelfy.restless.divine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.keelfy.restless.spirit.Clue;
import dev.keelfy.restless.spirit.Focus;
import dev.keelfy.restless.spirit.Trait;
import dev.keelfy.restless.spirit.Traits;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CluesTest {
    private static Trait trait(String id, boolean lies, Clue... clues) {
        return new Trait(
                id,
                Map.of(),
                1.0,
                Trait.Condition.ANY,
                0.0,
                List.of(),
                Set.of(clues),
                List.of("word"),
                List.of("phrase"),
                lies,
                Focus.IRON);
    }

    @Test
    void everyToolAnswersWhateverTheTraitsLeave() {
        Trait cold = trait("cold", false, Clue.SILHOUETTE);
        List<Trait> catalog = List.of(cold, trait("other", false, Clue.VOICE));
        assertEquals(
                cold,
                Clues.reveal(List.of(cold), catalog, Clue.VOICE, new Random(1)).orElseThrow());
        assertTrue(Clues.reveal(List.of(), catalog, Clue.VOICE, new Random(1)).isEmpty());
    }

    @Test
    void aTraitThatLeavesTheClueIsLikelierToShow() {
        Trait seen = trait("seen", false, Clue.SILHOUETTE);
        Trait quiet = trait("quiet", false, Clue.VOICE);
        List<Trait> owned = List.of(seen, quiet);
        Random random = new Random(5);
        long shown = java.util.stream.IntStream.range(0, 1000)
                .filter(i -> Clues.reveal(owned, owned, Clue.SILHOUETTE, random).orElseThrow() == seen)
                .count();
        // weight 3 against 1: three in four
        assertTrue(shown > 680 && shown < 820, "shown: " + shown);
    }

    @Test
    void aLiarSometimesNamesATraitItDoesNotHave() {
        Trait liar = trait("liar", true, Clue.VOICE);
        Trait other = trait("other", false, Clue.VOICE);
        List<Trait> catalog = List.of(liar, other);
        Random random = new Random(3);
        long lies = java.util.stream.IntStream.range(0, 200)
                .filter(i ->
                        Clues.reveal(List.of(liar), catalog, Clue.VOICE, random).orElseThrow() == other)
                .count();
        assertTrue(lies > 50 && lies < 150, "lies: " + lies);
    }

    @Test
    void aSilhouetteIsNeverALie() {
        Trait liar = trait("liar", true, Clue.SILHOUETTE);
        List<Trait> catalog = List.of(liar, trait("other", false, Clue.SILHOUETTE));
        for (int i = 0; i < 50; i++) {
            assertEquals(
                    liar,
                    Clues.reveal(List.of(liar), catalog, Clue.SILHOUETTE, new Random(i))
                            .orElseThrow());
        }
    }

    @Test
    void everyBundledTraitLeavesAClue() {
        for (Trait trait : Traits.all()) {
            assertFalse(trait.clues().isEmpty(), trait.id());
        }
    }
}
