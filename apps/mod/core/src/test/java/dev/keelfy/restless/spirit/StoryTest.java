package dev.keelfy.restless.spirit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.keelfy.restless.config.Config;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class StoryTest {
    @Test
    void sameSeedTellsSameStoryWithinTheConfiguredYears() {
        for (long seed : LongStream.range(0, 200).toArray()) {
            Story story = Story.of(seed);
            assertEquals(story, Story.of(seed));
            assertTrue(story.daysDead() >= 3 && story.daysDead() <= 400, story.toString());
            assertFalse(story.name().isBlank(), story.toString());
        }
    }

    @Test
    void nicknamesVaryAcrossManySeeds() {
        assertTrue(LongStream.range(0, 1000)
                        .mapToObj(seed -> Story.of(seed).name())
                        .distinct()
                        .count()
                > 900);
    }

    @Test
    void seedsTellDifferentStories() {
        assertTrue(LongStream.range(0, 50).mapToObj(Story::of).distinct().count() > 30);
    }

    @Test
    void everyDeathHasSpiritBoxWordsTheTraitsAlreadyVoice() {
        Set<String> voiced =
                Traits.all().stream().flatMap(t -> t.words().stream()).collect(Collectors.toSet());
        for (String cause : Config.section("story").texts("causes")) {
            var words = Config.section("story").section("words").texts(cause);
            assertFalse(words.isEmpty(), cause);
            words.forEach(w -> assertTrue(voiced.contains(w), cause + ": " + w + " has no recorded clip"));
        }
    }

    @Test
    void everyDeathNamesAFormFourCausesEachAndThingsToWantBack() {
        java.util.Map<Form, Integer> per = new java.util.EnumMap<>(Form.class);
        for (String cause : Config.section("story").texts("causes")) {
            Story story = new Story("x", 1, cause);
            per.merge(story.form(), 1, Integer::sum);
            assertFalse(story.wants().isEmpty(), cause);
        }
        assertEquals(java.util.Map.of(Form.WHOLE, 4, Form.BIT, 4, Form.CRAFTED, 4), per);
    }
}
