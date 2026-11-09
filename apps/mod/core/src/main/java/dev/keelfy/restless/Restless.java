package dev.keelfy.restless;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import dev.keelfy.restless.command.HauntCommand;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.config.Lang;
import dev.keelfy.restless.content.Advanced;
import dev.keelfy.restless.content.Charms;
import dev.keelfy.restless.content.Consumables;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.Relics;
import dev.keelfy.restless.haunt.Anger;
import dev.keelfy.restless.haunt.Bands;
import dev.keelfy.restless.haunt.Births;
import dev.keelfy.restless.haunt.Candle;
import dev.keelfy.restless.haunt.Compass;
import dev.keelfy.restless.haunt.Corruption;
import dev.keelfy.restless.haunt.Deeds;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.Divination;
import dev.keelfy.restless.haunt.Earshot;
import dev.keelfy.restless.haunt.DoorLocks;
import dev.keelfy.restless.haunt.Echoes;
import dev.keelfy.restless.haunt.Evolution;
import dev.keelfy.restless.haunt.Graves;
import dev.keelfy.restless.haunt.Kills;
import dev.keelfy.restless.haunt.Overheard;
import dev.keelfy.restless.haunt.Reactions;
import dev.keelfy.restless.haunt.Requiem;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Sanity;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.Session;
import dev.keelfy.restless.haunt.Trail;
import dev.keelfy.restless.haunt.Wakes;
import dev.keelfy.restless.haunt.Wards;
import dev.keelfy.restless.haunt.event.Breaks;
import dev.keelfy.restless.haunt.event.DoubleEvent;
import dev.keelfy.restless.haunt.event.Machines;
import dev.keelfy.restless.haunt.event.Swaps;
import dev.keelfy.restless.platform.Platform;
import dev.keelfy.restless.spirit.Trait;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Loader-agnostic entry point. Each loader module calls {@link #init()} once at mod construction and forwards its
 * server tick and command registration events to the hooks below.
 */
public final class Restless {
    public static final String MOD_ID = "restless";
    public static final Logger LOGGER = LogUtils.getLogger();

    private Restless() {}

    public static void init() {
        Config.load(Platform.get().configDir());
        Lang.load(Platform.get().configDir());
        Content.init();
        LOGGER.info("Restless initialising on {}", Platform.get().loaderName());
    }

    /** Call once per server tick, after the world has ticked. */
    public static void onServerTick(MinecraftServer server) {
        Scheduler.tick(server);
        Requiem.tick(server);
        Restore.tick(server);
        Director.tick(server);
        Candle.tick(server);
        Compass.tick(server);
        Graves.tick(server);
        Sanity.tick(server);
        Corruption.tick(server);
        Evolution.tick(server);
        Charms.tick(server);
        Deeds.tick(server);
        Trail.tick(server);
        Consumables.tick(server);
    }

    /**
     * Call when a chunk loads for the first time, so each structure start in it is met exactly once. A territorial
     * spirit may be born for each configured structure. Off the server thread, the work waits for it.
     */
    public static void onNewChunk(ServerLevel level, LevelChunk chunk) {
        var server = level.getServer();
        if (!server.isSameThread()) {
            server.execute(() -> onNewChunk(level, chunk));
            return;
        }
        for (var start : chunk.getAllStarts().values()) {
            if (start.isValid()) {
                Births.generated(level, start, true);
            }
        }
    }

    /** Call when the server starts to stop, before the worlds are saved. */
    public static void onServerStopping(MinecraftServer server) {
        Requiem.abortAll();
        Restore.flush();
        Advanced.endWalks(server);
        Session.SERVER.end();
    }

    /** Call when a living thing is about to die; true means it must not. */
    public static boolean onDeath(LivingEntity dying, DamageSource source) {
        return Relics.ferried(dying, source);
    }

    /** Call when a living thing dies; {@code killer} is whoever dealt the blow, if anyone. */
    public static void onKill(@Nullable Entity killer, LivingEntity dead) {
        Deeds.killed(killer, dead);
        Advanced.killed(killer, dead);
        if (dead instanceof ServerPlayer victim) {
            Bands.died(victim);
            DoubleEvent.dismiss(victim);
            Charms.died(victim);
        }
        if (killer instanceof ServerPlayer player) {
            Kills.killed(player, dead);
            if (dead instanceof Mob) {
                Reactions.trigger(player, Trait.Trigger.KILL);
            }
            if (dead instanceof Cat) {
                Anger.provoke(player, Anger.Source.KILL_CAT, dead.blockPosition());
            }
        }
    }

    /** Call when a player breaks a block, before it is gone; {@code state} is what stood there. */
    public static void onBreakBlock(ServerPlayer player, BlockState state, BlockPos pos) {
        Anger.broke(player, state, pos);
        Breaks.broke(player, state);
        Charms.noise(player);
    }

    /** Call before a player takes damage; returns the amount to apply, capped if a spirit's creature dealt it. */
    public static float onHurt(Entity victim, DamageSource source, float amount) {
        float dealt = Charms.hurt(victim, source, amount);
        return victim instanceof ServerPlayer player ? Bands.cap(source, player, dealt) : dealt;
    }

    /** Call before a living thing takes damage; true means the blow must not land (a ghost double cannot be struck). */
    public static boolean onStrike(LivingEntity victim, DamageSource source) {
        return Charms.passes(victim, source) || DoubleEvent.spared(victim, source);
    }

    /** Call when a player swings at an entity, or a projectile meets one; true means it passes through, untouched. */
    public static boolean onMeet(@Nullable Entity striker, Entity target) {
        return Charms.passes(striker, target);
    }

    /** Call when a living thing's drops are settled but not yet dropped; the list may be changed. */
    public static void onDrops(@Nullable Entity killer, LivingEntity dead, Collection<ItemEntity> drops) {
        Charms.drops(killer, dead, drops);
    }

    /** Call when a living thing is about to take a new target; true means it must not. */
    public static boolean onTarget(LivingEntity looker, @Nullable LivingEntity target) {
        return target != null && Charms.unseen(looker, target);
    }

    /** Call when a player is back after dying. */
    public static void onRespawn(ServerPlayer player) {
        Charms.respawned(player);
    }

    /** Call before a mob is let into the world; true means it must not be. */
    public static boolean onSpawn(ServerLevel level, Mob mob, EntitySpawnReason reason) {
        return Consumables.spawn(level, mob, reason);
    }

    /** Call when an anvil is asked what two stacks make; an empty stack is no result of ours. */
    public static ItemStack onAnvil(ItemStack left, ItemStack right) {
        return Consumables.anvil(left, right);
    }

    /** Call when a player tries a bed and the game finds a problem; true means the problem is waived. */
    public static boolean onSleepProblem(ServerPlayer player) {
        return Consumables.sleepsAnyway(player);
    }

    /** Call when a player wakes. */
    public static void onWake(ServerPlayer player) {
        Relics.woke(player);
        Wakes.woke(player);
    }

    /** Call when the sleepers would skip the night; true means it must not pass. */
    public static boolean onSleepFinished(ServerLevel level) {
        return Relics.keepsNight(level);
    }

    /** Call when a respawning player's destination is chosen; a non-null result replaces it. */
    public static @Nullable TeleportTransition onRespawnPosition(ServerPlayer player, TeleportTransition chosen) {
        return Relics.respawn(player, chosen);
    }

    /** Call when a player says something in chat. */
    public static void onChat(ServerPlayer player, String line) {
        Divination.ask(player);
        Machines.said(player, line);
        Overheard.heard(player, line);
    }

    /** Call when a player finishes a phrase on voice chat: a spirit box answers it as it does a chat line. */
    public static void onVoice(ServerPlayer player) {
        Divination.ask(player);
    }

    /** Call after a living thing finishes eating or drinking; {@code eaten} is the stack as it was before. */
    public static void onEat(LivingEntity eater, ItemStack eaten) {
        if (eater instanceof ServerPlayer player) {
            Sanity.ate(player, eaten);
        }
    }

    /** Call when a player starts to break a block. */
    public static void onStartBreak(ServerPlayer player, BlockPos pos) {
        Swaps.reveal(player, pos);
    }

    /** Call before a player uses a block; true means the use must be cancelled. */
    public static boolean onUseBlock(Player player, InteractionHand hand, BlockPos pos) {
        if (Relics.faded(player)) {
            return true;
        }
        Level level = player.level();
        if (player instanceof ServerPlayer server && hand == InteractionHand.MAIN_HAND) {
            Reactions.used(server, level.getBlockState(pos));
            Charms.used(server, pos);
            // Cake is eaten at the block, only by a hungry player whose hand does not take the click first.
            if (level.getBlockState(pos).is(Blocks.CAKE)
                    && server.canEat(false)
                    && !(server.isSecondaryUseActive() && !server.getMainHandItem().isEmpty())) {
                Sanity.sweet(server, "minecraft:cake");
            }
        }
        if (level instanceof ServerLevel server
                && hand == InteractionHand.MAIN_HAND
                && level.getBlockState(pos).is(Blocks.BELL)) {
            Wards.rung(server, pos);
            Requiem.rung(server, pos);
        }
        if (player instanceof ServerPlayer server
                && isFire(player.getItemInHand(hand))
                && (level.getBlockState(pos).is(BlockTags.LOGS)
                        || level.getBlockState(pos).is(BlockTags.LEAVES))) {
            Anger.provoke(server, Anger.Source.BURN_FOREST, pos);
        }
        Config.Tone tone = DoorLocks.denial(level, pos);
        if (tone == null) {
            return false;
        }
        // The client refuses on its own, so the server hears nothing and only the client plays the sound.
        if (hand == InteractionHand.MAIN_HAND) {
            if (level.isClientSide()) {
                level.playLocalSound(
                        pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, tone.volume(), tone.pitch(), false);
            } else {
                level.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, tone.volume(), tone.pitch());
            }
        }
        return true;
    }

    private static boolean isFire(ItemStack stack) {
        return stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE);
    }

    /** Call whenever a level is about to play a sound at a point; the sound itself is left alone. */
    /** @return whether the sound must not be broadcast: a personal spirit's sound goes to its player alone */
    public static boolean onPlaySound(
            Level level, Vec3 at, @Nullable Holder<SoundEvent> sound, SoundSource source, float volume, float pitch) {
        Echoes.hear(level, at, sound, source, volume, pitch);
        return Earshot.keep(level, at, sound, source, volume, pitch);
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        HauntCommand.register(dispatcher);
    }
}
