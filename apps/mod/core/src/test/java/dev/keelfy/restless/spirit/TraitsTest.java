package dev.keelfy.restless.spirit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.keelfy.restless.haunt.event.EventTag;
import dev.keelfy.restless.haunt.event.HauntTier;
import dev.keelfy.restless.spirit.life.Power;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
            assertEquals(traits.size(), Traits.of(traits).size());
            long habits = count(Traits.of(traits), Trait.Kind.HABIT);
            long conditions = count(Traits.of(traits), Trait.Kind.CONDITION);
            assertTrue(habits >= 1 && habits <= 4, traits.toString());
            assertTrue(conditions >= 1 && conditions <= 2, traits.toString());
            // one desire, then one fear or hate
            assertEquals(2, count(Traits.of(traits), Trait.Kind.LEANING));
            assertTrue(Traits.desire(Traits.of(traits)).isPresent(), traits.toString());
            assertTrue(Traits.fear(Traits.of(traits)).isPresent()
                    ^ Traits.hate(Traits.of(traits)).isPresent());
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
        assertNotEquals(
                1.0,
                Traits.activity(Traits.of(List.of("rain_walker", "night_stalker")), EnumSet.of(Trait.Condition.NIGHT)));
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
    void theSpiritIsWeakToWhatMostOfItsHabitsName() {
        // light_eater, cold_touch: glowstone; door_slammer: iron
        assertEquals(Focus.GLOWSTONE, Traits.weakness(Traits.of(List.of("door_slammer", "light_eater", "cold_touch"))));
        // a tie goes to the focus declared first
        assertEquals(Focus.IRON, Traits.weakness(Traits.of(List.of("light_eater", "door_slammer"))));
        // conditions never outvote a habit
        assertEquals(Focus.IRON, Traits.weakness(Traits.of(List.of("door_slammer", "night_stalker", "dark_dweller"))));
        // a habit tie goes to the old vote of every trait
        assertEquals(
                Focus.GLOWSTONE, Traits.weakness(Traits.of(List.of("door_slammer", "light_eater", "night_stalker"))));
        // no habit at all: the old vote decides
        assertEquals(
                Focus.HOLY_WATER, Traits.weakness(Traits.of(List.of("night_stalker", "rain_walker", "deep_caller"))));
    }

    @Test
    void aRolledSpiritsNatureIsTheStrictMajorityOfItsHabitsAndConditionsNeverChangeIt() {
        Map<Focus, Integer> natures = new EnumMap<>(Focus.class);
        List<Trait> conditions = Traits.all().stream()
                .filter(t -> t.kind() == Trait.Kind.CONDITION)
                .toList();
        Random seeds = new Random(42);
        for (int i = 0; i < 1000; i++) {
            List<Trait> traits = Traits.of(Traits.roll(seeds.nextLong()));
            Focus nature = Traits.weakness(traits);
            List<Trait> habits =
                    traits.stream().filter(t -> t.kind() == Trait.Kind.HABIT).toList();
            long own = habits.stream().filter(t -> t.weakness() == nature).count();
            assertTrue(own > habits.size() - own, traits.toString());
            assertTrue(habits.size() - own <= 1, traits.toString());
            assertEquals(nature, Traits.weakness(habits), traits.toString());
            List<Trait> every = new ArrayList<>(habits);
            every.addAll(conditions);
            assertEquals(nature, Traits.weakness(every), traits.toString());
            natures.merge(nature, 1, Integer::sum);
        }
        for (Focus focus : Focus.values()) {
            assertTrue(natures.get(focus) > 180 && natures.get(focus) < 320, natures.toString());
        }
    }

    @Test
    void aSpiritRolledBeforeTheAxesKeepsItsNatureWhereverItsHabitsAllow() {
        Random seeds = new Random(7);
        int kept = 0;
        for (int i = 0; i < 1000; i++) {
            Random random = new Random(seeds.nextLong());
            // the roll as it was: the nature's group took every trait voting for it, conditions included
            Focus before = Focus.values()[random.nextInt(Focus.values().length)];
            List<String> own = new ArrayList<>(Traits.all().stream()
                    .filter(t -> t.weakness() == before)
                    .map(Trait::id)
                    .toList());
            List<String> foreign = new ArrayList<>(Traits.all().stream()
                    .filter(t -> t.weakness() != null && t.weakness() != before)
                    .map(Trait::id)
                    .toList());
            int size = 3 + random.nextInt(3);
            List<String> ids = new ArrayList<>();
            for (int j = 0; j < size - 1 && !own.isEmpty(); j++) {
                ids.add(own.remove(random.nextInt(own.size())));
            }
            ids.add(foreign.remove(random.nextInt(foreign.size())));
            List<Trait> traits = Traits.of(ids);
            int[] votes = new int[Focus.values().length];
            traits.stream()
                    .filter(t -> t.kind() == Trait.Kind.HABIT)
                    .forEach(t -> votes[t.weakness().ordinal()]++);
            boolean allowed = Arrays.stream(votes).allMatch(v -> v <= votes[before.ordinal()]);
            if (allowed) {
                assertEquals(before, Traits.weakness(traits), ids.toString());
                kept++;
            }
        }
        assertTrue(kept > 500, "kept " + kept);
    }

    @Test
    void aTypeLeansToItsNature() {
        Map<Focus, Integer> poltergeist = new EnumMap<>(Focus.class);
        Map<Focus, Integer> even = new EnumMap<>(Focus.class);
        Random seeds = new Random(11);
        for (int i = 0; i < 4000; i++) {
            long seed = seeds.nextLong();
            poltergeist.merge(Traits.weakness(Traits.of(Traits.roll(seed, "poltergeist"))), 1, Integer::sum);
            even.merge(Traits.weakness(Traits.of(Traits.roll(seed))), 1, Integer::sum);
        }
        // weights 4.5 : 1 : 1 : 1, so 60% objects and about 13% each of the rest
        assertTrue(poltergeist.get(Focus.IRON) > 2200 && poltergeist.get(Focus.IRON) < 2600, poltergeist.toString());
        for (Focus focus : List.of(Focus.GLOWSTONE, Focus.HOLY_WATER, Focus.MIRROR)) {
            assertTrue(poltergeist.get(focus) > 380 && poltergeist.get(focus) < 700, poltergeist.toString());
        }
        for (Focus focus : Focus.values()) {
            assertTrue(even.get(focus) > 850 && even.get(focus) < 1150, even.toString());
        }
    }

    @Test
    void everyTypeLeansToOneNature() {
        for (String type : Spirits.all().stream().map(SpiritType::id).toList()) {
            Map<Focus, Integer> natures = new EnumMap<>(Focus.class);
            Random seeds = new Random(type.hashCode());
            for (int i = 0; i < 1000; i++) {
                long seed = seeds.nextLong();
                natures.merge(Traits.weakness(Traits.of(Traits.roll(seed, type))), 1, Integer::sum);
            }
            assertTrue(natures.values().stream().anyMatch(n -> n > 500), type + " " + natures);
        }
    }

    @Test
    void onlyHabitsAndOldVotersCarryAWeakness() {
        for (Trait trait : Traits.all()) {
            if (trait.kind() == Trait.Kind.HABIT) {
                assertNotNull(trait.weakness(), trait.id());
            }
            assertFalse(trait.kind() == Trait.Kind.LEANING && trait.weakness() != null, trait.id());
        }
    }

    private static long count(List<Trait> traits, Trait.Kind kind) {
        return traits.stream().filter(t -> t.kind() == kind).count();
    }

    @Test
    void theCatalogHasSixHabitsPerNatureAndTenConditions() {
        for (Focus focus : Focus.values()) {
            assertTrue(
                    Traits.all().stream()
                                    .filter(t -> t.kind() == Trait.Kind.HABIT && t.weakness() == focus)
                                    .count()
                            >= 6,
                    focus.name());
        }
        assertTrue(
                Traits.all().stream().filter(t -> t.kind() == Trait.Kind.HABIT).count() >= 24);
        assertTrue(Traits.all().stream()
                        .filter(t -> t.kind() == Trait.Kind.CONDITION)
                        .count()
                >= 10);
    }

    @Test
    void everyLeaningIsAFearADesireOrAHateAboutSomething() {
        Set<Trait.Lean> leans = EnumSet.noneOf(Trait.Lean.class);
        for (Trait trait : Traits.all()) {
            assertEquals(trait.kind() == Trait.Kind.LEANING, trait.leans() != null, trait.id());
            assertEquals(trait.leans() != null, !trait.things().isEmpty(), trait.id());
            if (trait.leans() != null) {
                leans.add(trait.leans());
                assertFalse(trait.clues().isEmpty(), trait.id());
            }
        }
        assertEquals(EnumSet.allOf(Trait.Lean.class), leans);
        assertTrue(Traits.all().stream()
                        .filter(t -> t.kind() == Trait.Kind.LEANING)
                        .count()
                >= 10);
    }

    @Test
    void leaningsDoNotChangeTheHabitsOrConditionsASeedRolls() {
        for (long seed : LongStream.range(0, 100).toArray()) {
            List<Trait> rolled = Traits.of(Traits.roll(seed));
            List<Trait> early = rolled.stream()
                    .filter(t -> t.kind() == Trait.Kind.HABIT || t.kind() == Trait.Kind.CONDITION)
                    .toList();
            assertEquals(early, rolled.subList(0, early.size()));
        }
    }

    @Test
    void aSpiritHasAtMostOneVeilAndEveryToolButTheBoardHasOne() {
        Set<Clue> foiled = EnumSet.noneOf(Clue.class);
        for (Trait trait : Traits.all()) {
            assertEquals(trait.kind() == Trait.Kind.VEIL, trait.foils() != null, trait.id());
            if (trait.foils() != null) {
                foiled.add(trait.foils());
            }
        }
        assertEquals(EnumSet.of(Clue.SILHOUETTE, Clue.FOOTPRINTS, Clue.VOICE), foiled);
        int veiled = 0;
        for (long seed : LongStream.range(0, 400).toArray()) {
            long veils = count(Traits.of(Traits.roll(seed)), Trait.Kind.VEIL);
            assertTrue(veils <= 1);
            veiled += (int) veils;
        }
        assertTrue(veiled > 40 && veiled < 160, "veiled " + veiled);
    }

    @Test
    void oneOfTwoOrMoreHabitsIsHiddenAndEvolvingAddsOneOfTheNature() {
        for (long seed : LongStream.range(0, 200).toArray()) {
            List<Trait> rolled = Traits.of(Traits.roll(seed));
            Trait hidden = Traits.hidden(seed, rolled);
            assertNotNull(hidden);
            assertEquals(Trait.Kind.HABIT, hidden.kind());
            assertEquals(hidden, Traits.hidden(seed, rolled));
            String grown = Traits.grown(seed, rolled);
            assertNotNull(grown);
            assertFalse(rolled.stream().anyMatch(t -> t.id().equals(grown)));
            List<Trait> after = new ArrayList<>(rolled);
            after.addAll(Traits.of(List.of(grown)));
            assertEquals(Traits.weakness(rolled), Traits.weakness(after));
        }
        assertEquals(null, Traits.hidden(1, Traits.of(List.of("vengeful", "grudge"))));
    }

    @Test
    void everyFocusIsSomeHabitsWeakness() {
        Set<Focus> named = EnumSet.noneOf(Focus.class);
        Traits.all().stream().filter(t -> t.kind() == Trait.Kind.HABIT).forEach(t -> named.add(t.weakness()));
        assertEquals(EnumSet.allOf(Focus.class), named);
    }
}
