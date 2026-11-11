package dev.keelfy.restless.spirit.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.keelfy.restless.haunt.Ticks;
import dev.keelfy.restless.spirit.life.BirthRules.Verdict;
import org.junit.jupiter.api.Test;

class BirthRulesTest {
    private static final long COOLDOWN = 3 * Ticks.DAY;
    private static final long NOW = 100 * Ticks.DAY;

    @Test
    void firstBirthNeedsOnlyRoom() {
        assertEquals(Verdict.BORN, BirthRules.decide(false, 0, 0, NOW, COOLDOWN, 6));
    }

    @Test
    void youngNewestSpiritBlocksABirthUntilTheCooldownIsOver() {
        assertEquals(Verdict.COOLDOWN, BirthRules.decide(false, 2, NOW - COOLDOWN + 1, NOW, COOLDOWN, 6));
        assertEquals(Verdict.BORN, BirthRules.decide(false, 2, NOW - COOLDOWN, NOW, COOLDOWN, 6));
    }

    @Test
    void fullPlayerGetsNoMoreSpirits() {
        assertEquals(Verdict.FULL, BirthRules.decide(false, 2, 0, NOW, COOLDOWN, 2));
    }

    @Test
    void sameTypeIsFedEvenDuringCooldownOrWhenFull() {
        assertEquals(Verdict.FED, BirthRules.decide(true, 2, NOW, NOW, COOLDOWN, 2));
    }

    @Test
    void aSeedHauntsTheSameStructuresEveryTime() {
        int haunted = 0;
        for (int x = 0; x < 1000; x++) {
            boolean first = BirthRules.haunted(42L, "minecraft:shipwreck", x, -x, 0.3);
            assertEquals(first, BirthRules.haunted(42L, "minecraft:shipwreck", x, -x, 0.3));
            haunted += first ? 1 : 0;
        }
        assertTrue(haunted > 250 && haunted < 350, "chance 0.3 must haunt about 30% of 1000, was " + haunted);
        assertTrue(BirthRules.haunted(1L, "minecraft:igloo", 5, 5, 1.0));
        assertTrue(!BirthRules.haunted(1L, "minecraft:igloo", 5, 5, 0.0));
    }

    @Test
    void anEffigyIsFreeOnlyOutsideTheSpacing() {
        assertEquals(Verdict.BORN, BirthRules.settle(Long.MAX_VALUE, 96));
        assertEquals(Verdict.CROWDED, BirthRules.settle(95L * 95, 96));
        assertEquals(Verdict.BORN, BirthRules.settle(96L * 96, 96));
    }
}
