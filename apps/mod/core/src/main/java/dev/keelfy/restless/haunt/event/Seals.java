package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.Ticks;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The player is shut in: walled where they stand, or the way back sealed. The blocks are the ones around, so a dig out
 * pays nothing new; they go back through {@link Restore#swap} and stay under hardcore.
 */
final class Seals {
    private Seals() {}

    static List<HauntEvent> all() {
        return List.of(
                // 116: knocking on every side, then walls of the stone around, a roof, and mining fatigue; the player's
                // own two blocks stay open
                SimpleEvent.harmful("walled_in", EventTag.BLOCKS, Seals::wallable, Seals::wallIn),
                // 188: the tunnel a few blocks behind the player is filled in, wall to wall
                SimpleEvent.harmful("sealed_tunnel", EventTag.BLOCKS, player -> plug(player) != null, Seals::seal));
    }

    private static boolean wallable(ServerPlayer player) {
        return player.onGround()
                && !player.isInWater()
                && !player.isPassenger()
                && shell(player).size() >= Config.event("walled_in").integer("min_blocks");
    }

    /** The air around the player's feet and head and over the head, with nobody standing in it. */
    private static List<BlockPos> shell(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos feet = player.blockPosition();
        List<BlockPos> shell = new ArrayList<>();
        for (Direction side : Direction.Plane.HORIZONTAL) {
            shell.add(feet.relative(side));
            shell.add(feet.above().relative(side));
        }
        shell.add(feet.above(2));
        shell.removeIf(pos -> !level.isEmptyBlock(pos) || occupied(level, pos));
        return shell;
    }

    private static void wallIn(ServerPlayer player) {
        Config.Section config = Config.event("walled_in");
        Config.Tone tone = config.tone("sound");
        int knocks = config.integer("knocks");
        Scheduler.repeat(player, 0, config.integer("knock_ticks"), (p, n) -> {
            ServerLevel level = p.level();
            BlockPos feet = p.blockPosition();
            BlockState material =
                    material(level, BlockPos.betweenClosed(feet.offset(-2, -1, -2), feet.offset(2, 2, 2)));
            if (n < knocks) {
                Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(p.getRandom());
                level.playSound(
                        null,
                        feet.above().relative(side),
                        material.getSoundType().getHitSound(),
                        SoundSource.HOSTILE,
                        tone.volume(),
                        tone.pitch());
                return true;
            }
            if (!p.isAlive() || p.isInWater()) {
                return false;
            }
            // Pulled to the middle of the block, so the walls close on every side.
            p.teleportTo(feet.getX() + 0.5, p.getY(), feet.getZ() + 0.5);
            int restore = config.integer("restore_ticks");
            shell(p).forEach(pos -> Restore.swap(level, pos, material, restore));
            level.playSound(
                    null,
                    feet,
                    material.getSoundType().getPlaceSound(),
                    SoundSource.BLOCKS,
                    tone.volume(),
                    tone.pitch());
            p.addEffect(
                    new MobEffectInstance(
                            MobEffects.MINING_FATIGUE,
                            Ticks.SECOND * config.integer("fatigue_seconds"),
                            config.integer("amplifier")),
                    null);
            return false;
        });
    }

    /** The air of the tunnel's cross-section a few blocks straight behind the player, with open tunnel between. */
    private static @Nullable Set<BlockPos> plug(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos feet = player.blockPosition();
        // The heightmap rather than sky light: light is recomputed lazily and a roof placed this tick is not in it yet.
        if (!player.onGround()
                || player.isInWater()
                || level.getHeight(Heightmap.Types.MOTION_BLOCKING, feet.getX(), feet.getZ()) <= feet.getY() + 2) {
            return null;
        }
        Config.Section config = Config.event("sealed_tunnel");
        Direction back = player.getDirection().getOpposite();
        for (int d = 1; d <= config.integer("max_distance"); d++) {
            BlockPos low = feet.relative(back, d);
            if (!level.isEmptyBlock(low) || !level.isEmptyBlock(low.above())) {
                return null;
            }
            if (d >= config.integer("min_distance")) {
                Set<BlockPos> section = section(level, low, back, config.integer("max_blocks"));
                if (section != null) {
                    return section;
                }
            }
        }
        return null;
    }

    /**
     * Flood fill of the air across the tunnel at {@code start}; null when it is not shut in by full blocks within the
     * budget.
     */
    private static @Nullable Set<BlockPos> section(ServerLevel level, BlockPos start, Direction along, int max) {
        Direction side = along.getClockWise();
        List<Direction> across = List.of(Direction.UP, Direction.DOWN, side, side.getOpposite());
        Set<BlockPos> air = new LinkedHashSet<>(List.of(start));
        Deque<BlockPos> open = new ArrayDeque<>(air);
        while (!open.isEmpty()) {
            BlockPos at = open.poll();
            if (occupied(level, at)) {
                return null;
            }
            for (Direction way : across) {
                BlockPos next = at.relative(way);
                if (air.contains(next)) {
                    continue;
                }
                if (level.isEmptyBlock(next)) {
                    if (air.size() >= max) {
                        return null;
                    }
                    air.add(next);
                    open.add(next);
                } else if (!level.getBlockState(next).isCollisionShapeFullBlock(level, next)) {
                    return null;
                }
            }
        }
        return air;
    }

    private static void seal(ServerPlayer player) {
        Set<BlockPos> plug = plug(player);
        if (plug == null) {
            return;
        }
        ServerLevel level = player.level();
        Config.Section config = Config.event("sealed_tunnel");
        List<BlockPos> walls = new ArrayList<>();
        plug.forEach(pos -> Direction.stream()
                .map(pos::relative)
                .filter(p -> !plug.contains(p))
                .forEach(walls::add));
        BlockState material = material(level, walls);
        plug.forEach(pos -> Restore.swap(level, pos, material, config.integer("restore_ticks")));
        Config.Tone tone = config.tone("sound");
        level.playSound(
                null,
                plug.iterator().next(),
                material.getSoundType().getPlaceSound(),
                SoundSource.BLOCKS,
                tone.volume(),
                tone.pitch());
    }

    private static boolean occupied(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty();
    }

    /**
     * The commonest plain block among {@code around}, one that will not fall or pay out when dug; stone if there is
     * none.
     */
    private static BlockState material(ServerLevel level, Iterable<BlockPos> around) {
        Map<Block, Integer> counts = new HashMap<>();
        for (BlockPos pos : around) {
            BlockState state = level.getBlockState(pos);
            if (Place.solid(level, pos) && !(state.getBlock() instanceof FallingBlock) && !valuable(state)) {
                counts.merge(state.getBlock(), 1, Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().defaultBlockState())
                .orElse(Blocks.STONE.defaultBlockState());
    }

    private static boolean valuable(BlockState state) {
        return state.is(BlockTags.ORES) || state.is(BlockTags.BEACON_BASE_BLOCKS) || state.is(Blocks.ANCIENT_DEBRIS);
    }
}
