package dev.keelfy.restless.banish;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class PhaseTest {
    @Test
    void furyLoopIsHeardOnlyDuringTheFury() {
        assertTrue(Phase.furyHeard(Optional.of(Phase.FURY)));
        assertFalse(Phase.furyHeard(Optional.of(Phase.CALL)));
        assertFalse(Phase.furyHeard(Optional.of(Phase.BARGAIN)));
        assertFalse(Phase.furyHeard(Optional.of(Phase.LEAVING)));
        assertFalse(Phase.furyHeard(Optional.empty()), "the end of the ritual (-1) stops it");
    }
}
