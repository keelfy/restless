package dev.keelfy.restless.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class SettingsTest {
    @Test
    void defaultsKeepTheBehaviourTheDataFilesHad() {
        Settings defaults = Settings.DEFAULTS;
        assertEquals(Settings.HardcoreMode.AUTO, defaults.hardcore());
        assertEquals(Settings.SwapMode.ILLUSION, defaults.swaps());
        assertEquals(true, defaults.journalOnJoin());
    }

    @Test
    void useReplacesTheWholeRecord() {
        Settings before = Settings.get();
        Settings chosen = new Settings(Settings.HardcoreMode.ON, Settings.SwapMode.REAL, false);
        try {
            Settings.use(chosen);
            assertSame(chosen, Settings.get());
        } finally {
            Settings.use(before);
        }
    }
}
