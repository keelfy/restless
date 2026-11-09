package dev.keelfy.restless.haunt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StartleTest {
    @Test
    void jumpScareHoldsTheNextOneBackForTheGap() {
        assertFalse(Startle.held(null, 5000, 12000), "none has run yet");
        assertTrue(Startle.held(1000L, 1001, 12000), "just after one");
        assertTrue(Startle.held(1000L, 12999, 12000), "one tick before the gap ends");
        assertFalse(Startle.held(1000L, 13000, 12000), "gap over");
    }
}
