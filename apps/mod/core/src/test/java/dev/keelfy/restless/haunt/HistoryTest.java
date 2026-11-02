package dev.keelfy.restless.haunt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HistoryTest {
    @BeforeEach
    void forget() {
        History.reset();
    }

    @Test
    void newestFirstWithTheSpiritBehindEach() {
        History.record(10, "Alex", "restless:harvest", "poltergeist \"x\"");
        History.record(20, "Alex", "restless:whisper", null);
        List<History.Entry> all = History.last(null, 5);
        assertEquals("restless:whisper", all.get(0).event());
        assertNull(all.get(0).spirit());
        assertEquals("poltergeist \"x\"", all.get(1).spirit());
    }

    @Test
    void filtersByPlayerAndCapsTheCount() {
        History.record(1, "Alex", "restless:a", null);
        History.record(2, "Steve", "restless:b", null);
        History.record(3, "Alex", "restless:c", null);
        assertEquals(2, History.last("Alex", 5).size());
        assertEquals("restless:c", History.last("Alex", 1).get(0).event());
    }

    @Test
    void keepsOnlyTheLastHundred() {
        for (int i = 0; i < 150; i++) {
            History.record(i, "Alex", "restless:e" + i, null);
        }
        List<History.Entry> all = History.last(null, 1000);
        assertEquals(100, all.size());
        assertEquals("restless:e149", all.get(0).event());
    }
}
