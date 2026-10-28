package dev.keelfy.restless;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import dev.keelfy.restless.command.HauntCommand;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.platform.Platform;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

/**
 * Loader-agnostic entry point. Each loader module calls {@link #init()} once at mod construction
 * and forwards its server tick and command registration events to the hooks below.
 */
public final class Restless {
    public static final String MOD_ID = "restless";
    public static final Logger LOGGER = LogUtils.getLogger();

    private Restless() {
    }

    public static void init() {
        LOGGER.info("Restless initialising on {}", Platform.get().loaderName());
    }

    /** Call once per server tick, after the world has ticked. */
    public static void onServerTick(MinecraftServer server) {
        Scheduler.tick(server);
        Director.tick(server);
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        HauntCommand.register(dispatcher);
    }
}
