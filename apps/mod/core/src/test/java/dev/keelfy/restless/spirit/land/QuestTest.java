package dev.keelfy.restless.spirit.land;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.spirit.Story;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class QuestTest {
    @Test
    void sameSeedSameQuestAndEveryKindTurnsUp() {
        Set<Quest> seen = EnumSet.noneOf(Quest.class);
        for (long seed : LongStream.range(0, 2000).toArray()) {
            assertEquals(Quest.of(seed), Quest.of(seed));
            seen.add(Quest.of(seed));
        }
        assertEquals(EnumSet.allOf(Quest.class), seen);
    }

    @Test
    void causeOfDeathLeansTheQuest() {
        Config.Section causes = Config.section("grave").section("cause_quests");
        long matched = 0;
        long total = 0;
        for (long seed : LongStream.range(0, 4000).toArray()) {
            String cause = Story.of(seed).cause();
            if (causes.has(cause)) {
                total++;
                if (Quest.of(seed).id().equals(causes.text(cause).toLowerCase(Locale.ROOT))) {
                    matched++;
                }
            }
        }
        // cause_chance of them follow the cause, the rest roll by weight and may land on it too.
        assertTrue(matched > total * Config.section("grave").num("cause_chance"), matched + " of " + total);
    }
}
