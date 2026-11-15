package dev.keelfy.restless.ward;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.block.Altar;
import dev.keelfy.restless.haunt.Scan;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.state.HauntData;
import dev.keelfy.restless.state.Session;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Blessing a house. Water on a complete altar inside a closed salt ring, with {@code blessing.candles} candles lit
 * around it, begins a long ritual of {@code blessing.waves} waves instead of making holy water. Each wave one of the
 * player's spirits may snuff a candle; at the end of a wave the altar must stand, the ring be closed and the player
 * stay near, and at the end of the last every candle must be lit. Then a {@link Spells.Kind#BLESSING} lasts
 * {@code blessing.ticks}: no event runs near it ({@link #quiet}). When it ends the owner's spirits come back angrier
 * ({@link #release}). Rituals live in memory only, like every {@link Scheduler} task; a failed one costs only the
 * water.
 */
public final class Blessing {
    /** The altars with a blessing under way, and the game time it ends by: a player who leaves frees it silently. */
    private static final Map<BlockPos, Long> RUNNING = Session.SERVER.keep(new HashMap<>());

    private Blessing() {}

    /** The lit candles around the altar, nearest first. */
    private static List<BlockPos> candles(ServerLevel level, BlockPos altar) {
        List<BlockPos> candles = new ArrayList<>();
        Scan.each(level, altar, Config.section("ritual").integer("candle_radius"), (pos, state) -> {
            if (AbstractCandleBlock.isLit(state)) {
                candles.add(pos.immutable());
            }
        });
        candles.sort(Comparator.comparingDouble(pos -> pos.distSqr(altar)));
        return candles;
    }

    /** Whether water on this altar blesses the house: the salt ring is closed and enough candles are lit. */
    public static boolean ready(ServerLevel level, BlockPos altar) {
        return Wards.ringed(level, altar)
                && candles(level, altar).size() >= Config.section("blessing").integer("candles");
    }

    /** @return whether the blessing began; if not, the player was told why */
    public static boolean begin(ServerPlayer player, ServerLevel level, BlockPos altar) {
        return begin(player, level, altar, Config.section("ritual").integer("wave_ticks"));
    }

    /** As {@link #begin(ServerPlayer, ServerLevel, BlockPos)}, with each wave {@code ticks} long. */
    public static boolean begin(ServerPlayer player, ServerLevel level, BlockPos altar, int ticks) {
        int waves = Config.section("blessing").integer("waves");
        int wave = Math.max(1, ticks);
        if (running(level, altar)) {
            player.sendSystemMessage(Component.translatable("message.restless.blessing_busy"), true);
            return false;
        }
        RUNNING.put(altar.immutable(), level.getGameTime() + (long) waves * wave + wave);
        List<BlockPos> candles =
                candles(level, altar).subList(0, Config.section("blessing").integer("candles"));
        level.playSound(null, altar, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 0.8f);
        player.sendSystemMessage(Component.translatable("message.restless.blessing_begins"));
        Scheduler.repeat(player, wave, wave, (p, n) -> {
            if (!holds(p, level, altar)) {
                RUNNING.remove(altar);
                p.sendSystemMessage(Component.translatable("message.restless.blessing_failed"), true);
                return false;
            }
            if (n + 1 < waves) {
                snuff(p, level, candles);
                return true;
            }
            RUNNING.remove(altar);
            if (candles.stream().anyMatch(pos -> !AbstractCandleBlock.isLit(level.getBlockState(pos)))) {
                p.sendSystemMessage(Component.translatable("message.restless.blessing_failed"), true);
                return false;
            }
            bless(p, level, altar);
            return false;
        });
        return true;
    }

    /** Whether the altar stands, the ring is closed and the player is near. */
    private static boolean holds(ServerPlayer player, ServerLevel level, BlockPos altar) {
        double reach = Config.section("ritual").integer("radius");
        return player.level() == level
                && Altar.isComplete(level, altar)
                && Wards.ringed(level, altar)
                && player.blockPosition().distSqr(altar) <= reach * reach;
    }

    /** A spirit of the player, if it has any, may put out one of the candles; the player must light it again. */
    private static void snuff(ServerPlayer player, ServerLevel level, List<BlockPos> candles) {
        List<HauntData.Haunting> spirits = HauntData.get(level.getServer()).get(player.getUUID());
        if (spirits == null
                || spirits.isEmpty()
                || player.getRandom().nextDouble() >= Config.section("blessing").num("snuff_chance")) {
            return;
        }
        BlockPos candle = candles.get(player.getRandom().nextInt(candles.size()));
        BlockState state = level.getBlockState(candle);
        if (AbstractCandleBlock.isLit(state)) {
            AbstractCandleBlock.extinguish(null, state, level, candle);
            player.sendSystemMessage(Component.translatable("message.restless.candle_snuffed"), true);
        }
    }

    private static void bless(ServerPlayer player, ServerLevel level, BlockPos altar) {
        Spells.get(level.getServer())
                .add(new Spells.Spell(
                        Spells.Kind.BLESSING,
                        level.dimension(),
                        altar,
                        level.getGameTime() + Config.section("blessing").integer("ticks"),
                        Optional.of(player.getUUID())));
        level.sendParticles(
                ParticleTypes.END_ROD,
                altar.getX() + 0.5,
                altar.getY() + 1.0,
                altar.getZ() + 0.5,
                40,
                0.8,
                0.6,
                0.8,
                0.03);
        level.playSound(null, altar, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 0.6f);
        player.sendSystemMessage(Component.translatable("message.restless.blessing_done"));
    }

    /** Whether a blessing is under way at this altar. */
    public static boolean running(ServerLevel level, BlockPos altar) {
        return RUNNING.getOrDefault(altar, 0L) > level.getGameTime();
    }

    /** Frees the altar of its blessing record; the ritual's own task dies with its player. */
    public static void forget(BlockPos altar) {
        RUNNING.remove(altar);
    }

    /** Whether a live blessing lies within {@code blessing.radius} of the player: no event runs there. */
    public static boolean quiet(ServerPlayer player) {
        ServerLevel level = player.level();
        return !Spells.get(level.getServer())
                .near(
                        Spells.Kind.BLESSING,
                        level.dimension(),
                        player.blockPosition(),
                        Config.section("blessing").integer("radius"),
                        level.getGameTime())
                .isEmpty();
    }

    /** Call every tick; every {@code blessing.check_ticks} the ended blessings are released. */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % Math.max(1, Config.section("blessing").integer("check_ticks")) == 0) {
            release(server);
        }
    }

    /** Ends each blessing whose time is up: every spirit of its owner is angrier, and the owner is told. */
    public static void release(MinecraftServer server) {
        Spells spells = Spells.get(server);
        long now = server.overworld().getGameTime();
        double anger = Config.section("blessing").num("return_anger");
        HauntData data = HauntData.get(server);
        for (Spells.Spell spell : spells.all(Spells.Kind.BLESSING)) {
            if (spell.until() > now) {
                continue;
            }
            spells.remove(spell);
            UUID owner = spell.owner().orElse(null);
            if (owner == null) {
                continue;
            }
            List<HauntData.Haunting> spirits = data.get(owner);
            for (HauntData.Haunting h : spirits == null ? List.<HauntData.Haunting>of() : List.copyOf(spirits)) {
                data.change(h.uuid(), s -> s.angered(anger));
            }
            ServerPlayer player = server.getPlayerList().getPlayer(owner);
            if (player != null) {
                player.sendSystemMessage(Component.translatable("message.restless.blessing_ended"));
            }
        }
    }
}
