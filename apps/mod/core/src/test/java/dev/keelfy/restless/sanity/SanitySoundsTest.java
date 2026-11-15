package dev.keelfy.restless.sanity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SanitySoundsTest {
    @Test
    void bedStartsBelowThresholdAndStopsWhenSanityRecovers() {
        assertEquals(0, SanitySounds.bedVolume(0.6, 0.6, 0.6));
        assertEquals(0, SanitySounds.bedVolume(0.9, 0.6, 0.6));
        double shallow = SanitySounds.bedVolume(0.5, 0.6, 0.6);
        double deep = SanitySounds.bedVolume(0.1, 0.6, 0.6);
        assertTrue(shallow > 0);
        assertTrue(deep > shallow);
        assertEquals(0.6, SanitySounds.bedVolume(0.0, 0.6, 0.6), 1e-9);
        assertEquals(0, SanitySounds.bedVolume(0.7, 0.6, 0.6));
    }

    @Test
    void heartbeatOnlyBelowItsThreshold() {
        assertTrue(SanitySounds.heartbeat(0.1, 0.2));
        assertFalse(SanitySounds.heartbeat(0.2, 0.2));
        assertFalse(SanitySounds.heartbeat(0.5, 0.2));
    }
}
