package dev.keelfy.restless.spirit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertTrue(LongStream.range(0, 1000).mapToObj(seed -> Story.of(seed).name()).distinct().count() > 900);
    }

    @Test
    void seedsTellDifferentStories() {
        assertTrue(LongStream.range(0, 50).mapToObj(Story::of).distinct().count() > 30);
    }
}
