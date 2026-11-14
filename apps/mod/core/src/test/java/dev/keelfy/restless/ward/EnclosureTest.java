package dev.keelfy.restless.ward;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EnclosureTest {
    /** '#' wall, 's' salt, '.' open; anything off the grid is open floor. */
    private static Enclosure.Floor grid(String... rows) {
        return (x, z) -> {
            if (z < 0 || z >= rows.length || x < 0 || x >= rows[z].length()) {
                return Enclosure.Cell.OPEN;
            }
            return switch (rows[z].charAt(x)) {
                case '#' -> Enclosure.Cell.WALL;
                case 's' -> Enclosure.Cell.SALT;
                case 'w' -> Enclosure.Cell.WORN;
                default -> Enclosure.Cell.OPEN;
            };
        };
    }

    @Test
    void closedSaltRingShelters() {
        assertTrue(Enclosure.sealed(2, 2, grid(".sss.", ".s.s.", ".s.s.", ".sss."), 100));
    }

    @Test
    void gapLetsTheFillEscape() {
        assertFalse(Enclosure.sealed(2, 2, grid(".sss.", ".s.s.", ".s...", ".sss."), 100));
    }

    @Test
    void wallsCloseTheLoopButAloneAreNoWard() {
        assertTrue(Enclosure.sealed(2, 1, grid("#####", "#...#", "##s##"), 100));
        assertFalse(Enclosure.sealed(2, 1, grid("#####", "#...#", "#####"), 100));
    }

    @Test
    void bigRoomsPastTheBudgetDoNotCount() {
        assertFalse(Enclosure.sealed(
                3, 3, grid("sssssss", "s.....s", "s.....s", "s.....s", "s.....s", "s.....s", "sssssss"), 10));
    }

    @Test
    void ringNamesTheSaltThatShutsTheCellIn() {
        assertEquals(4, Enclosure.ring(1, 1, grid("sss.", "s.s.", "sss."), 100).size());
        assertTrue(Enclosure.ring(2, 2, grid(".sss.", ".s.s.", ".s...", ".sss."), 100)
                .isEmpty());
    }

    @Test
    void wornSaltIsAGapAndShelterIsGone() {
        assertTrue(Enclosure.sealed(2, 2, grid(".sss.", ".s.s.", ".s.s.", ".sss."), 100));
        assertFalse(Enclosure.sealed(2, 2, grid(".sws.", ".s.s.", ".s.s.", ".sss."), 100));
        assertFalse(Enclosure.sealed(2, 2, grid(".www.", ".w.w.", ".w.w.", ".www."), 100));
    }

    @Test
    void ringWearsOutAtTheLimit() {
        assertFalse(Enclosure.wornOut(11, 12));
        assertTrue(Enclosure.wornOut(12, 12));
    }
}
