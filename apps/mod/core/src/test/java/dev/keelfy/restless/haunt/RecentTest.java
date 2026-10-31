package dev.keelfy.restless.haunt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecentTest {
    private static final UUID ALEX = UUID.randomUUID();
    private static final UUID STEVE = UUID.randomUUID();
    private static final int GAP = 20;

    @BeforeEach
    void forget() {
        Recent.reset();
    }

    @Test
    void eventIsBlockedUntilTwentyOthersHappen() {
        assertFalse(Recent.blocks(ALEX, "restless:harvest"), "never happened");
        Recent.record(ALEX, "restless:harvest", GAP);
        for (int i = 0; i < 19; i++) {
            Recent.record(ALEX, "restless:other_" + i, GAP);
            assertTrue(Recent.blocks(ALEX, "restless:harvest"), "after " + (i + 1) + " others");
        }
        Recent.record(ALEX, "restless:other_19", GAP);
        assertFalse(Recent.blocks(ALEX, "restless:harvest"), "after 20 others");
    }

    @Test
    void historyIsPerPlayer() {
        Recent.record(ALEX, "restless:harvest", GAP);
        assertFalse(Recent.blocks(STEVE, "restless:harvest"));
    }
}
