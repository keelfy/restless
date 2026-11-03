package dev.keelfy.restless.haunt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.keelfy.restless.haunt.BirthRules.Verdict;
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
    void generatedStructureIsFreeOnlyOutsideTheSpacing() {
        assertEquals(Verdict.BORN, BirthRules.settle(Long.MAX_VALUE, 96));
        assertEquals(Verdict.CROWDED, BirthRules.settle(95L * 95, 96));
        assertEquals(Verdict.BORN, BirthRules.settle(96L * 96, 96));
    }
}
