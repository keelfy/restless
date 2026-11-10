package dev.keelfy.restless.content.item.charm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LuckTest {
    private static final long DAY = 24000L;

    @Test
    void aFreshCoinPaysDouble() {
        assertEquals(2, Luck.NONE.payout(0));
    }

    @Test
    void everyWinRaisesThePayoutUpToTheCap() {
        Luck luck = Luck.NONE;
        int[] paid = new int[5];
        for (int i = 0; i < paid.length; i++) {
            paid[i] = luck.payout(0);
            luck = luck.won(0, 3);
        }
        assertEquals(2, paid[0]);
        assertEquals(3, paid[1]);
        assertEquals(4, paid[2]);
        assertEquals(5, paid[3]);
        assertEquals(5, paid[4]);
    }

    @Test
    void aLossResetsTheRun() {
        Luck luck = new Luck(3, 0).lost(10);
        assertEquals(0, luck.live(10));
        assertEquals(2, luck.payout(10));
    }

    @Test
    void theRunWearsOffByOneForEveryDayUntossed() {
        Luck luck = new Luck(3, 1000);
        assertEquals(3, luck.live(1000 + DAY - 1));
        assertEquals(2, luck.live(1000 + DAY));
        assertEquals(0, luck.live(1000 + 5 * DAY));
        assertEquals(3, luck.live(0));
    }

    @Test
    void aWinAfterAGapBuildsOnWhatIsLeft() {
        Luck luck = new Luck(3, 0).won(2 * DAY, 3);
        assertEquals(2, luck.streak());
        assertEquals(2 * DAY, luck.at());
    }
}
