package dev.keelfy.restless.haunt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ScenarioContextTest {
    @Test
    void carriesNameIntoDelayedWorkAndRestoresScope() {
        ScenarioContext[] captured = new ScenarioContext[1];
        ScenarioContext.start(() -> {
            assertEquals("Alex", ScenarioContext.nickname(() -> "Alex"));
            captured[0] = ScenarioContext.current();
        });
        assertEquals("???", ScenarioContext.speaker("???"));
        ScenarioContext.during(captured[0], () -> {
            assertEquals("Alex", ScenarioContext.nickname(() -> "Steve"));
            assertEquals("Alex", ScenarioContext.speaker("???"));
        });
        assertEquals("???", ScenarioContext.speaker("???"));
    }

    @Test
    void isolatesRunsAndRestoresAfterFailure() {
        ScenarioContext.start(() -> {
            ScenarioContext.nickname(() -> "Alex");
            assertThrows(
                    IllegalStateException.class,
                    () -> ScenarioContext.start(() -> {
                        assertEquals("???", ScenarioContext.speaker("???"));
                        ScenarioContext.nickname(() -> "Steve");
                        throw new IllegalStateException();
                    }));
            assertEquals("Alex", ScenarioContext.speaker("???"));
        });
        assertNull(ScenarioContext.current());
        assertEquals("Steve", ScenarioContext.nickname(() -> "Steve"));
    }
}
