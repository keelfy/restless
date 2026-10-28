package dev.keelfy.restless.haunt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.keelfy.restless.spirit.Spirits;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DangerTest {
    @Test
    void freshWorldIsCalmAndMaxesOut() {
        assertEquals(0.0, Danger.of(0, 0));
        assertEquals(1.0, Danger.of(1000, 1000), 1e-9);
    }

    @Test
    void harmfulEventsNeverRollOnFreshWorld() {
        assertEquals(0.0, Danger.weight(HauntTier.HARMFUL, Set.of(EventTag.ASSAULT), 1, Spirits.SHADE, 0));
    }

    @Test
    void progressionShiftsWeightFromLightToHarmful() {
        double lightEarly = Danger.tierFactor(HauntTier.LIGHT, 0.1);
        double lightLate = Danger.tierFactor(HauntTier.LIGHT, 0.9);
        double harmfulEarly = Danger.tierFactor(HauntTier.HARMFUL, 0.1);
        double harmfulLate = Danger.tierFactor(HauntTier.HARMFUL, 0.9);
        assertTrue(lightLate < lightEarly);
        assertTrue(harmfulLate > harmfulEarly);
    }

    @Test
    void poltergeistPrefersDoorsOverAffliction() {
        double doors = Danger.weight(HauntTier.LIGHT, Set.of(EventTag.DOORS), 1, Spirits.POLTERGEIST, 0.5);
        double affliction = Danger.weight(HauntTier.LIGHT, Set.of(EventTag.AFFLICTION), 1, Spirits.POLTERGEIST, 0.5);
        assertTrue(doors > affliction * 4);
    }

    @Test
    void cooldownShrinksWithDangerAndActivity() {
        long calm = Danger.cooldownTicks(0, 1, 0.5, 1);
        long late = Danger.cooldownTicks(1, 1, 0.5, 1);
        long active = Danger.cooldownTicks(0, 2, 0.5, 1);
        assertEquals(4 * 1.5 * 60 * 20, calm);
        assertTrue(late < calm);
        assertEquals(calm / 2, active);
    }

    @Test
    void openDaylightStretchesCooldown() {
        assertEquals(1.0, Danger.daylightFactor(false, false, false));
        assertEquals(1.2, Danger.daylightFactor(true, false, false), 1e-9);
        assertEquals(1.2 * 1.2 * 1.2, Danger.daylightFactor(true, true, true), 1e-9);
        assertEquals((long) (calmTicks() * 1.2), Danger.cooldownTicks(0, 1, 0.5, 1.2), 1);
    }

    private static long calmTicks() {
        return Danger.cooldownTicks(0, 1, 0.5, 1);
    }
}
