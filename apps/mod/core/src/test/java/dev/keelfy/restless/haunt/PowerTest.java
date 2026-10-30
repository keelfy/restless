package dev.keelfy.restless.haunt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.keelfy.restless.spirit.SpiritType;
import dev.keelfy.restless.spirit.Spirits;
import dev.keelfy.restless.spirit.Trait;
import java.util.List;
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
    void evolvedSpiritKeepsItsFloorAndBlightGrowsToRadius() {
        assertEquals(0.7, Power.of(0, 0, true), 1e-9);
        assertEquals(1.0, Power.of(1000 * Ticks.DAY, 1, true), 1e-9);
        assertEquals(0.0, Power.of(0, 0, false));
        assertTrue(Power.grown(5 * Ticks.DAY) && !Power.grown(5 * Ticks.DAY - 1));
        assertEquals(6.0, Power.blightReach(0, 48), 1e-9);
        assertEquals(48.0, Power.blightReach(100 * Ticks.DAY, 48), 1e-9);
        assertEquals(4.0, Power.blightReach(100 * Ticks.DAY, 4), 1e-9);
    }

    @Test
    void harmfulEventsNeverRollForNewSpirit() {
        assertEquals(0.0, Power.weight(HauntTier.HARMFUL, Set.of(EventTag.ASSAULT), 1, Spirits.SHADE, List.of(), 0));
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
        double doors = Power.weight(HauntTier.LIGHT, Set.of(EventTag.DOORS), 1, Spirits.POLTERGEIST, List.of(), 0.5);
        double affliction = Power.weight(HauntTier.LIGHT, Set.of(EventTag.AFFLICTION), 1, Spirits.POLTERGEIST, List.of(), 0.5);
        assertTrue(doors > affliction * 4);
    }

    @Test
    void phantomPrefersOtherPlayersOverOtherPresence() {
        double player = Power.weight(HauntTier.LIGHT, Set.of(EventTag.PRESENCE, EventTag.PLAYER), 1, Spirits.PHANTOM, List.of(), 0.5);
        double presence = Power.weight(HauntTier.LIGHT, Set.of(EventTag.PRESENCE), 1, Spirits.PHANTOM, List.of(), 0.5);
        double shade = Power.weight(HauntTier.LIGHT, Set.of(EventTag.PRESENCE, EventTag.PLAYER), 1, Spirits.SHADE, List.of(), 0.5);
        assertTrue(player > presence * 5);
        assertTrue(player > shade * 5);
    }

    @Test
    void nixPrefersWaterOverEverythingElse() {
        double water = Power.weight(HauntTier.LIGHT, Set.of(EventTag.WATER), 1, Spirits.NIX, List.of(), 0.5);
        double doors = Power.weight(HauntTier.LIGHT, Set.of(EventTag.DOORS), 1, Spirits.NIX, List.of(), 0.5);
        assertTrue(water > doors * 5);
    }

    @Test
    void everyTypeLeansOnItsOwnTags() {
        assertTrue(lean(Spirits.MIMIC, EventTag.BLOCKS, EventTag.ITEMS) > 2);
        assertTrue(lean(Spirits.MARA, EventTag.AFFLICTION, EventTag.ITEMS) > 5);
        assertTrue(lean(Spirits.BANSHEE, EventTag.SOUND, EventTag.ASSAULT) > 5);
        assertTrue(lean(Spirits.REVENANT, EventTag.ASSAULT, EventTag.DOORS) > 5);
        assertTrue(lean(Spirits.LOST_ONE, EventTag.PRESENCE, EventTag.ASSAULT) > 5);
        assertTrue(lean(Spirits.DOMOVOI, EventTag.DOORS, EventTag.ASSAULT) > 5);
        assertTrue(lean(Spirits.HOARDER, EventTag.ITEMS, EventTag.LIGHTS) > 5);
        assertTrue(lean(Spirits.DEMON, EventTag.ASSAULT, EventTag.ITEMS) > 5);
        assertTrue(lean(Spirits.TRICKSTER, EventTag.BLOCKS, EventTag.ASSAULT) > 5);
    }

    @Test
    void typeActivityHoldsOnlyUnderItsCondition() {
        assertEquals(2.0, Spirits.WRAITH.activity(Set.of(Trait.Condition.ANY, Trait.Condition.DARK)));
        assertEquals(1.0, Spirits.WRAITH.activity(Set.of(Trait.Condition.ANY, Trait.Condition.NIGHT)));
        assertEquals(1.3, Spirits.POLTERGEIST.activity(Set.of(Trait.Condition.ANY)));
        assertTrue(Spirits.MARA.reacts().contains(Trait.Trigger.BED));
        assertTrue(Spirits.REVENANT.reacts().contains(Trait.Trigger.KILL));
    }

    @Test
    void newTypesKeepTheirKind() {
        assertEquals(SpiritType.Kind.PERSONAL, Spirits.WRAITH.kind());
        assertEquals(SpiritType.Kind.TERRITORIAL, Spirits.DOMOVOI.kind());
        assertTrue(Spirits.personal().containsAll(List.of(Spirits.MIMIC, Spirits.WRAITH, Spirits.MARA, Spirits.BANSHEE, Spirits.REVENANT, Spirits.LOST_ONE)));
        assertTrue(Spirits.personal().stream().noneMatch(List.of(Spirits.DOMOVOI, Spirits.HOARDER, Spirits.DEMON, Spirits.TRICKSTER)::contains));
    }

    private static double lean(SpiritType spirit, EventTag liked, EventTag other) {
        return Power.weight(HauntTier.LIGHT, Set.of(liked), 1, spirit, List.of(), 0.5)
            / Power.weight(HauntTier.LIGHT, Set.of(other), 1, spirit, List.of(), 0.5);
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
