package dev.keelfy.restless.spirit.land;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class StripTest {
    @Test
    void stripRunsFromAnchorTowardTargetNoFurtherThanReach() {
        Random random = new Random(7);
        for (int i = 0; i < 500; i++) {
            Strip.Offset at = Strip.offset(40, 0, 64, 5, random.nextDouble(), random.nextDouble());
            assertTrue(at.x() >= 0 && at.x() <= 40, "along the line, never past the target: " + at);
            assertTrue(Math.abs(at.z()) <= 3, "within the strip width: " + at);
            Strip.Offset far = Strip.offset(-200, 0, 64, 5, random.nextDouble(), random.nextDouble());
            assertTrue(far.x() <= 0 && far.x() >= -64, "never past the walk reach: " + far);
        }
    }

    @Test
    void stripFollowsADiagonalAndAnEmptyTargetStaysAtAnchor() {
        Strip.Offset diagonal = Strip.offset(30, 30, 64, 4, 1.0, 0.5);
        assertEquals(new Strip.Offset(30, 30), diagonal);
        assertEquals(new Strip.Offset(0, 0), Strip.offset(0, 0, 64, 4, 0.5, 0.5));
    }
}
