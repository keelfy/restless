package dev.keelfy.restless.haunt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * The point ({@link Location}) needs Minecraft's {@code Vec3}; the GameTest {@code delayed_step_keeps_the_spirit_point}
 * covers it.
 */
class HauntContextTest {
    private static final UUID OUTER = UUID.randomUUID();
    private static final UUID INNER = UUID.randomUUID();

    @Test
    void aNestedActLeavesTheOuterSpeakerInPlace() {
        Said.during(OUTER, () -> {
            Said.during(INNER, () -> assertEquals(INNER, Said.current()));
            assertEquals(OUTER, Said.current());
        });
        assertNull(Said.current());
    }

    @Test
    void capturedContextComesBackLaterWhateverIsRunningThen() {
        HauntContext[] captured = new HauntContext[1];
        Said.during(
                OUTER,
                () -> ScenarioContext.start(() -> {
                    ScenarioContext.nickname(() -> "Alex");
                    captured[0] = HauntContext.capture();
                }));

        Said.during(
                INNER,
                () -> captured[0].during(() -> {
                    assertEquals(OUTER, Said.current());
                    assertEquals("Alex", ScenarioContext.speaker("???"));
                }));
        assertNull(Said.current());
    }

    @Test
    void theOuterContextIsRestoredWhenTheActionThrows() {
        HauntContext empty = HauntContext.capture();
        Said.during(OUTER, () -> {
            assertThrows(
                    IllegalStateException.class,
                    () -> empty.during(() -> {
                        throw new IllegalStateException("boom");
                    }));
            assertEquals(OUTER, Said.current());
        });
    }
}
