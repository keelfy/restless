package dev.keelfy.restless.banish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HallowTest {
    private static final double NONE = Double.POSITIVE_INFINITY;

    @Test
    void near_the_grave_at_any_hour() {
        assertTrue(Hallow.hallowed(4.0, 8.0, 6000, 17000, 19000));
        assertTrue(Hallow.hallowed(8.0, 8.0, 6000, 17000, 19000), "the edge counts");
        assertFalse(Hallow.hallowed(8.5, 8.0, 6000, 17000, 19000));
    }

    @Test
    void at_midnight_anywhere_on_any_day() {
        assertTrue(Hallow.hallowed(NONE, 8.0, 18000, 17000, 19000));
        assertTrue(Hallow.hallowed(NONE, 8.0, 24000L * 5 + 17000, 17000, 19000), "any day");
        assertFalse(Hallow.hallowed(NONE, 8.0, 19000, 17000, 19000), "the end is past midnight");
        assertFalse(Hallow.hallowed(NONE, 8.0, 12000, 17000, 19000));
    }

    @Test
    void a_hallowed_ritual_leaves_one_more_essence() {
        assertEquals(1, Hallow.essences(true));
        assertEquals(0, Hallow.essences(false));
    }
}
