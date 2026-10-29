package dev.keelfy.restless.spirit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntTier;
import dev.keelfy.restless.haunt.Power;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class TraitsTest {
    @Test
    void sameSeedRollsSameDistinctTraitsInRange() {
        for (long seed : LongStream.range(0, 200).toArray()) {
            List<String> traits = Traits.roll(seed);
            assertEquals(traits, Traits.roll(seed));
            assertEquals(traits.size(), new HashSet<>(traits).size());
            assertTrue(traits.size() >= 3 && traits.size() <= 5, traits.toString());
            assertEquals(traits.size(), Traits.of(traits).size());
        }
    }

    @Test
    void seedsDifferInTraits() {
        assertTrue(LongStream.range(0, 50).mapToObj(Traits::roll).distinct().count() > 10);
    }

    @Test
    void traitsShiftWhatTheSameTypeRolls() {
        List<Trait> doors = Traits.of(List.of("door_slammer"));
        double plain = Power.weight(HauntTier.LIGHT, Set.of(EventTag.DOORS), 1, Spirits.SHADE, List.of(), 0.5);
        double slammer = Power.weight(HauntTier.LIGHT, Set.of(EventTag.DOORS), 1, Spirits.SHADE, doors, 0.5);
        assertEquals(plain * 2.5, slammer, 1e-9);
    }

    @Test
    void activityTraitAppliesOnlyWhileItsConditionHolds() {
        List<Trait> rain = Traits.of(List.of("rain_walker"));
        assertEquals(1.0, Traits.activity(rain, EnumSet.of(Trait.Condition.ANY)));
        assertEquals(2.0, Traits.activity(rain, EnumSet.of(Trait.Condition.ANY, Trait.Condition.RAIN)));
        assertNotEquals(1.0, Traits.activity(Traits.of(List.of("rain_walker", "night_stalker")), EnumSet.of(Trait.Condition.NIGHT)));
    }

    @Test
    void noiseAndReactionsComeFromTheirTraitsOnly() {
        List<Trait> plain = Traits.of(List.of("door_slammer"));
        List<Trait> fed = Traits.of(List.of("door_slammer", "noise_feeder", "chest_watcher"));
        assertEquals(0.0, Traits.noise(plain, 10));
        assertEquals(60.0, Traits.noise(fed, 10));
        assertTrue(Traits.reacts(fed, Trait.Trigger.CHEST));
        assertFalse(Traits.reacts(fed, Trait.Trigger.KILL));
        assertFalse(Traits.reacts(plain, Trait.Trigger.CHEST));
    }

    @Test
    void theSpiritIsWeakToWhatMostOfItsTraitsName() {
        // light_eater, cold_touch: glowstone; door_slammer: iron
        assertEquals(Focus.GLOWSTONE, Traits.weakness(Traits.of(List.of("door_slammer", "light_eater", "cold_touch"))));
        // a tie goes to the focus declared first
        assertEquals(Focus.IRON, Traits.weakness(Traits.of(List.of("light_eater", "door_slammer"))));
    }

    @Test
    void everyFocusIsSomeTraitsWeakness() {
        Set<Focus> named = EnumSet.noneOf(Focus.class);
        Traits.all().forEach(t -> named.add(t.weakness()));
        assertEquals(EnumSet.allOf(Focus.class), named);
    }
}
