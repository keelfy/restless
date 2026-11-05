package dev.keelfy.restless.config;

/**
 * What a player or server owner chooses: how the mod plays, not how each event is tuned (that is {@link Config}, the
 * data files). Core only reads the current value; the loader module owns the file and the screen and hands every change
 * to {@link #use}. Values are immutable, so a reload swaps the whole record. Before a loader has set any, and in unit
 * tests, the defaults apply.
 */
public record Settings(HardcoreMode hardcore, SwapMode swaps, boolean journalOnJoin) {
    /** Whether a world runs hardcore haunting; {@link #AUTO} follows the world's own hardcore flag. */
    public enum HardcoreMode {
        AUTO,
        ON,
        OFF
    }

    /** Whether blocks that events swap are only shown to the haunted player, or changed in the world. */
    public enum SwapMode {
        ILLUSION,
        REAL
    }

    public static final Settings DEFAULTS = new Settings(HardcoreMode.AUTO, SwapMode.ILLUSION, true);

    private static volatile Settings current = DEFAULTS;

    public static Settings get() {
        return current;
    }

    public static void use(Settings settings) {
        current = settings;
    }
}
