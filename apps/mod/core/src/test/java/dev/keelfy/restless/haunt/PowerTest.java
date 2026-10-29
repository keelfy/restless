package dev.keelfy.restless.haunt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.keelfy.restless.spirit.Spirits;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PowerTest {
    @Test
    void newSpiritIsCalmAndAngerOrAgeRaisesIt() {
        assertEquals(0.0, Power.of(0, 0));
        assertEquals(0.6, Power.of(30 * Ticks.DAY, 0), 1e-9);
        assertEquals(0.4, Power.of(0, 1), 1e-9);
        assertEquals(1.0, Power.of(1000 * Ticks.DAY, 1), 1e-9);
    }

    @Test
    void harmfulEventsNeverRollForNewSpirit() {
        assertEquals(0.0, Power.weight(HauntTier.HARMFUL, Set.of(EventTag.ASSAULT), 1, Spirits.SHADE, 0));
    }

    @Test
    void progressionShiftsWeightFromLightToHarmful() {
        double lightEarly = Power.tierFactor(HauntTier.LIGHT, 0.1);
        double lightLate = Power.tierFactor(HauntTier.LIGHT, 0.9);
        double harmfulEarly = Power.tierFactor(HauntTier.HARMFUL, 0.1);
        double harmfulLate = Power.tierFactor(HauntTier.HARMFUL, 0.9);
        assertTrue(lightLate < lightEarly);
        assertTrue(harmfulLate > harmfulEarly);
    }

    @Test
    void poltergeistPrefersDoorsOverAffliction() {
        double doors = Power.weight(HauntTier.LIGHT, Set.of(EventTag.DOORS), 1, Spirits.POLTERGEIST, 0.5);
        double affliction = Power.weight(HauntTier.LIGHT, Set.of(EventTag.AFFLICTION), 1, Spirits.POLTERGEIST, 0.5);
        assertTrue(doors > affliction * 4);
    }

    @Test
    void cooldownShrinksWithPowerAndActivity() {
        long calm = Power.cooldownTicks(0, 1, 0.5, 1);
        long late = Power.cooldownTicks(1, 1, 0.5, 1);
        long active = Power.cooldownTicks(0, 2, 0.5, 1);
        assertEquals(4 * 1.5 * 60 * 20, calm);
        assertTrue(late < calm);
        assertEquals(calm / 2, active);
    }

    @Test
    void openDaylightStretchesCooldown() {
        assertEquals(1.0, Power.daylightFactor(false, false, false));
        assertEquals(1.2, Power.daylightFactor(true, false, false), 1e-9);
        assertEquals(1.2 * 1.2 * 1.2, Power.daylightFactor(true, true, true), 1e-9);
        assertEquals((long) (calmTicks() * 1.2), Power.cooldownTicks(0, 1, 0.5, 1.2), 1);
    }

    private static long calmTicks() {
        return Power.cooldownTicks(0, 1, 0.5, 1);
    }
}
