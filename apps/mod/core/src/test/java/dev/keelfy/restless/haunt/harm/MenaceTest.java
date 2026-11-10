package dev.keelfy.restless.haunt.harm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.keelfy.restless.haunt.Ticks;
import org.junit.jupiter.api.Test;

class MenaceTest {
    private static final long COOLDOWN = 3 * Ticks.DAY;

    @Test
    void lethalNeedsAWarningAndRestsAfterwards() {
        Menace fresh = Menace.NONE;
        assertFalse(fresh.lethalOpen(1000, COOLDOWN), "no brink event yet");
        Menace warned = fresh.brink(1000, 1200);
        assertTrue(warned.lethalOpen(1000, COOLDOWN));
        Menace killed = warned.lethal(5000);
        assertFalse(killed.lethalOpen(5000, COOLDOWN), "the warning is spent");
        Menace rewarned = killed.brink(6000, 1200);
        assertFalse(rewarned.lethalOpen(5000 + COOLDOWN - 1, COOLDOWN), "still resting");
        assertTrue(rewarned.lethalOpen(5000 + COOLDOWN, COOLDOWN));
    }

    @Test
    void mercyEndsOnItsTick() {
        Menace warned = Menace.NONE.brink(1000, 1200);
        assertTrue(warned.merciful(2199));
        assertFalse(warned.merciful(2200));
        assertEquals(1000, warned.lethal(1000).lastLethalAt());
    }
}
