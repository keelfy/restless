package dev.keelfy.restless.spirit.land;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class RemainsTest {
    @Test
    void bonesLieFiftyToAHundredFiftyBlocksFromTheGraveAndStayPut() {
        for (long seed : LongStream.range(0, 500).toArray()) {
            int[] spot = Quest.bonesOffset(seed, 1000, -500);
            assertArrayEquals(spot, Quest.bonesOffset(seed, 1000, -500));
            double distance = Math.hypot(spot[0], spot[1]);
            assertTrue(distance >= 49 && distance <= 151, "distance " + distance);
        }
    }
}
