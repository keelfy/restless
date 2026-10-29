package dev.keelfy.restless.haunt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.keelfy.restless.spirit.Clue;
import dev.keelfy.restless.spirit.Trait;
import dev.keelfy.restless.spirit.Traits;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CluesTest {
    private static Trait trait(String id, boolean lies, Clue... clues) {
        return new Trait(id, Map.of(), 1.0, Trait.Condition.ANY, 0.0, List.of(), Set.of(clues), List.of("word"), id, lies);
    }

    @Test
    void aSpiritShowsOnlyTheCluesItsTraitsLeave() {
        Trait cold = trait("cold", false, Clue.SILHOUETTE);
        List<Trait> catalog = List.of(cold, trait("other", false, Clue.VOICE));
        assertEquals(cold, Clues.reveal(List.of(cold), catalog, Clue.SILHOUETTE, new Random(1)).orElseThrow());
        assertFalse(Clues.reveal(List.of(cold), catalog, Clue.VOICE, new Random(1)).isPresent());
    }

    @Test
    void aLiarSometimesNamesATraitItDoesNotHave() {
        Trait liar = trait("liar", true, Clue.VOICE);
        Trait other = trait("other", false, Clue.VOICE);
        List<Trait> catalog = List.of(liar, other);
        Random random = new Random(3);
        long lies = java.util.stream.IntStream.range(0, 200)
            .filter(i -> Clues.reveal(List.of(liar), catalog, Clue.VOICE, random).orElseThrow() == other).count();
        assertTrue(lies > 50 && lies < 150, "lies: " + lies);
    }

    @Test
    void aSilhouetteIsNeverALie() {
        Trait liar = trait("liar", true, Clue.SILHOUETTE);
        List<Trait> catalog = List.of(liar, trait("other", false, Clue.SILHOUETTE));
        for (int i = 0; i < 50; i++) {
            assertEquals(liar, Clues.reveal(List.of(liar), catalog, Clue.SILHOUETTE, new Random(i)).orElseThrow());
        }
    }

    @Test
    void everyBundledTraitLeavesAClueAndHasAHint() {
        for (Trait trait : Traits.all()) {
            assertFalse(trait.clues().isEmpty(), trait.id());
            assertFalse(trait.hint().isBlank(), trait.id());
        }
    }
}
