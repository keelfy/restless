package dev.keelfy.restless.haunt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DrawTest {
    private static final int ITEMS = 60;
    private static final int DRAWS = 200_000;

    /**
     * The odds of the old roll (weights of the usable events only, every one checked) against drawing first: no item's
     * share moves by more than a fifth.
     */
    @Test
    void sharesMatchADrawAmongTheUsableOnes() {
        Random random = new Random(42);
        List<Integer> items = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        Set<Integer> usable = new HashSet<>();
        for (int i = 0; i < ITEMS; i++) {
            items.add(i);
            weights.add(0.1 + random.nextDouble() * 10);
            if (random.nextDouble() < 0.4) {
                usable.add(i);
            }
        }
        double usableTotal = 0;
        for (int i : usable) {
            usableTotal += weights.get(i);
        }
        Map<Integer, Integer> drawn = new HashMap<>();
        for (int n = 0; n < DRAWS; n++) {
            drawn.merge(
                    Draw.first(items, weights, usable::contains, random::nextDouble)
                            .orElseThrow(),
                    1,
                    Integer::sum);
        }
        for (int i = 0; i < ITEMS; i++) {
            double expected = usable.contains(i) ? weights.get(i) / usableTotal : 0;
            double share = drawn.getOrDefault(i, 0) / (double) DRAWS;
            if (expected == 0) {
                assertEquals(0, share, "item " + i + " cannot be used and must never be drawn");
            } else {
                assertTrue(
                        Math.abs(share - expected) <= expected / 5,
                        "item " + i + " share " + share + " against " + expected);
            }
        }
    }

    @Test
    void asksEachItemAtMostOnceAndGivesUpWhenNoneIsUsable() {
        Map<String, Integer> asked = new HashMap<>();
        Optional<String> none = Draw.first(
                List.of("a", "b", "c"),
                List.of(1.0, 2.0, 3.0),
                item -> {
                    asked.merge(item, 1, Integer::sum);
                    return false;
                },
                new Random(1)::nextDouble);
        assertTrue(none.isEmpty());
        assertEquals(Map.of("a", 1, "b", 1, "c", 1), asked);
    }

    @Test
    void stopsAtTheFirstUsableDraw() {
        List<String> asked = new ArrayList<>();
        Optional<String> one = Draw.first(
                List.of("a", "b"),
                List.of(1.0, 1.0),
                item -> {
                    asked.add(item);
                    return true;
                },
                () -> 0.0);
        assertEquals(Optional.of("a"), one);
        assertEquals(List.of("a"), asked);
    }
}
