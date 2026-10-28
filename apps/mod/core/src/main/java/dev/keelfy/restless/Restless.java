package dev.keelfy.restless;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import dev.keelfy.restless.command.HauntCommand;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.DoorLocks;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.platform.Platform;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
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
        Restore.tick(server);
        Director.tick(server);
    }

    /** Call when the server starts to stop, before the worlds are saved. */
    public static void onServerStopping() {
        Restore.flush();
    }

    /** Call before a player uses a block; true means the use must be cancelled. */
    public static boolean onUseBlock(Player player, InteractionHand hand, BlockPos pos) {
        // The locks live on the server; the client only sees the open attempt snap back.
        if (player.level().isClientSide() || !DoorLocks.isLocked(player.level(), pos)) {
            return false;
        }
        if (hand == InteractionHand.MAIN_HAND) {
            player.level().playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
        return true;
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        HauntCommand.register(dispatcher);
    }
}
