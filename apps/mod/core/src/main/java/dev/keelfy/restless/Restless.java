package dev.keelfy.restless;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import dev.keelfy.restless.command.HauntCommand;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.config.Lang;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.Echoes;
import dev.keelfy.restless.haunt.DoorLocks;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.platform.Platform;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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
        Config.load(Platform.get().configDir());
        Lang.load(Platform.get().configDir());
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
        Level level = player.level();
        if (!DoorLocks.isLocked(level, pos)) {
            return false;
        }
        // The client refuses on its own, so the server hears nothing and only the client plays the sound.
        if (hand == InteractionHand.MAIN_HAND) {
            Config.Tone tone = Config.event("locked_door").tone("denied_sound");
            if (level.isClientSide()) {
                level.playLocalSound(pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, tone.volume(), tone.pitch(), false);
            } else {
                level.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, tone.volume(), tone.pitch());
            }
        }
        return true;
    }

    /** Call whenever a level is about to play a sound at a point; the sound itself is left alone. */
    public static void onPlaySound(Level level, Vec3 at, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch) {
        Echoes.hear(level, at, sound, source, volume, pitch);
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        HauntCommand.register(dispatcher);
    }
}
