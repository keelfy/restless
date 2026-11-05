package dev.keelfy.restless.neoforge;

import dev.keelfy.restless.config.Settings;
import dev.keelfy.restless.config.Settings.HardcoreMode;
import dev.keelfy.restless.config.Settings.SwapMode;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The file a player edits, {@code config/restless/settings.toml}, and its screen in the mod list. It only holds the
 * choices; core reads them as {@link Settings}, which this hands every load and reload of the file. The numbers that
 * tune single events are not here: they are the data files under {@code config/restless/}.
 */
final class RestlessSettings {
    private static final String KEY = "restless.configuration.";
    private static final ModConfigSpec.EnumValue<HardcoreMode> HARDCORE;
    private static final ModConfigSpec.EnumValue<SwapMode> SWAPS;
    private static final ModConfigSpec.BooleanValue JOURNAL_ON_JOIN;
    private static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        HARDCORE = builder.comment(
                        "Hardcore haunting: what an event changes stays changed, and tricks that would only be shown come true.",
                        "AUTO follows the world's own hardcore flag. A world keeps the answer it got the first time it was asked.")
                .translation(KEY + "hardcore")
                .defineEnum("hardcore", HardcoreMode.AUTO);
        SWAPS = builder.comment(
                        "ILLUSION: swapped blocks are shown only to the haunted player. REAL: they are changed in the world for"
                                + " everyone for a short while.",
                        "Hardcore worlds always change them for good.")
                .translation(KEY + "swaps")
                .defineEnum("swaps", SwapMode.ILLUSION);
        JOURNAL_ON_JOIN = builder.comment("Give every player a journal when they join.")
                .translation(KEY + "journal_on_join")
                .define("journal_on_join", true);
        SPEC = builder.build();
    }

    private RestlessSettings() {}

    static void register(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.LOCAL, SPEC, "restless/settings.toml");
        modBus.addListener((ModConfigEvent.Loading e) -> apply(e.getConfig()));
        modBus.addListener((ModConfigEvent.Reloading e) -> apply(e.getConfig()));
    }

    private static void apply(ModConfig config) {
        if (config.getSpec() == SPEC) {
            Settings.use(new Settings(HARDCORE.get(), SWAPS.get(), JOURNAL_ON_JOIN.get()));
        }
    }
}
