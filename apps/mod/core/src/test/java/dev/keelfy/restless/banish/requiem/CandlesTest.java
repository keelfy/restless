package dev.keelfy.restless.banish.requiem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class CandlesTest {
    private static Candles<Integer> fourteen() {
        return new Candles<>(IntStream.range(0, 14).boxed().toList(), 7);
    }

    @Test
    void stands_while_seven_of_fourteen_are_lit() {
        Candles<Integer> candles = fourteen();
        assertTrue(candles.standing());
        candles.snuff(7, n -> 0);
        assertEquals(7, candles.lit().size());
        assertTrue(candles.standing(), "seven lit is enough");
        candles.snuff(n -> 0);
        assertFalse(candles.standing(), "six lit is not");
    }

    @Test
    void snuffs_the_candle_the_pick_names_and_remembers_the_order() {
        Candles<Integer> candles = fourteen();
        assertEquals(5, candles.snuff(n -> 5));
        assertEquals(0, candles.snuff(n -> 0));
        assertEquals(13, candles.snuff(n -> n - 1), "the pick is an index among the candles still lit");
        assertEquals(List.of(5, 0, 13), candles.out());
        assertFalse(candles.lit().contains(5));
        assertTrue(candles.isOut(0));
        assertFalse(candles.isOut(1));
    }

    @Test
    void snuffing_more_than_are_lit_stops_at_none() {
        Candles<Integer> candles = new Candles<>(List.of(1, 2), 1);
        assertEquals(List.of(1, 2), candles.snuff(5, n -> 0));
        assertNull(candles.snuff(n -> 0));
        assertFalse(candles.standing());
    }
}
