package dev.keelfy.restless.haunt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RarityTest {
    @BeforeEach
    void forget() {
        Rarity.reset();
    }

    @Test
    void alwaysAvailableEventKeepsItsWeight() {
        assertEquals(1.0, Rarity.boost("minecraft:footsteps"));
        for (int i = 0; i < 200; i++) {
            Rarity.observe("minecraft:footsteps", true);
        }
        assertEquals(1.0, Rarity.boost("minecraft:footsteps"), 1e-9);
    }

    @Test
    void seldomAvailableEventIsBoostedUpToTheCap() {
        for (int i = 0; i < 200; i++) {
            Rarity.observe("minecraft:harvest", i % 10 == 0);
            Rarity.observe("minecraft:shrieker", i == 199);
        }
        double harvest = Rarity.boost("minecraft:harvest");
        assertTrue(harvest > 2.0 && harvest <= 4.0, "harvest boost " + harvest);
        assertEquals(4.0, Rarity.boost("minecraft:shrieker"), 1e-9);
    }
}
