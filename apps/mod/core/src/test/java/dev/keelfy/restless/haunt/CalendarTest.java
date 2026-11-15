package dev.keelfy.restless.haunt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CalendarTest {
    @Test
    void theFullMoonComesEveryEighthDayFromTheFirst() {
        assertTrue(Calendar.Date.of(0).fullMoon());
        assertFalse(Calendar.Date.of(1).fullMoon());
        assertFalse(Calendar.Date.of(7).fullMoon());
        assertTrue(Calendar.Date.of(8).fullMoon());
        assertTrue(Calendar.Date.of(-8).fullMoon());
    }

    @Test
    void aBloodNightFallsOnEverySeventhDay() {
        assertTrue(Calendar.Date.of(6).bloodNight());
        assertTrue(Calendar.Date.of(13).bloodNight());
        assertFalse(Calendar.Date.of(0).bloodNight());
        assertFalse(Calendar.Date.of(7).bloodNight());
    }

    @Test
    void theDayOfTheDeadComesOnceASeasonAndWinsOverABloodNight() {
        // Day 27 is both the 28th day of the season and a multiple of seven less one.
        Calendar.Date dead = Calendar.Date.of(27);
        assertTrue(dead.dayOfTheDead());
        assertFalse(dead.bloodNight());
        assertTrue(Calendar.Date.of(55).dayOfTheDead());
        assertFalse(Calendar.Date.of(26).dayOfTheDead());
        assertEquals(false, Calendar.Date.of(1).special());
    }
}
