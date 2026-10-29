package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Restore;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** A whole tree, trunk and crown, is somewhere else than it was a moment ago. */
final class Trees {
    private Trees() {}

    static List<HauntEvent> all() {
        return List.of(
            // 108: the tree nearby takes a few steps to the side; under hardcore it stays there
            SimpleEvent.light("moving_tree", EventTag.BLOCKS, player -> Nearby.any(player, "moving_tree", state -> state.is(BlockTags.LOGS)), Trees::move)
        );
    }

    private static void move(ServerPlayer player) {
        Config.Section config = Config.event("moving_tree");
        ServerLevel level = player.level();
        List<BlockPos> logs = Nearby.all(player, "moving_tree", state -> state.is(BlockTags.LOGS));
        Collections.shuffle(logs, new java.util.Random(player.getRandom().nextLong()));
        for (BlockPos log : logs.subList(0, Math.min(logs.size(), config.integer("tries")))) {
            Map<BlockPos, BlockState> tree = tree(level, log, config.integer("max_blocks"));
            if (tree == null) {
                continue;
            }
            List<Direction> ways = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
            Collections.shuffle(ways, new java.util.Random(player.getRandom().nextLong()));
            int shift = config.integer("min_shift") + player.getRandom().nextInt(config.integer("max_shift") - config.integer("min_shift") + 1);
            for (Direction way : ways) {
                BlockPos by = BlockPos.ZERO.relative(way, shift);
                if (fits(level, tree, by)) {
                    shift(level, tree, by, config.integer("restore_ticks"));
                    Nearby.sound(player, SoundEvents.AZALEA_LEAVES_BREAK, Vec3.atCenterOf(log), config.tone("sound"));
                    return;
                }
            }
        }
    }

    /** The logs and the leaves that grew with them, connected to {@code start}; null if it is too big or has no crown. */
    private static @Nullable Map<BlockPos, BlockState> tree(ServerLevel level, BlockPos start, int max) {
        Map<BlockPos, BlockState> found = new LinkedHashMap<>();
        Deque<BlockPos> queue = new ArrayDeque<>(List.of(start));
        found.put(start, level.getBlockState(start));
        boolean crown = false;
        while (!queue.isEmpty()) {
            BlockPos at = queue.poll();
            for (BlockPos next : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1))) {
                if (found.containsKey(next) || !level.isLoaded(next)) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                boolean leaves = state.is(BlockTags.LEAVES) && state.hasProperty(LeavesBlock.PERSISTENT) && !state.getValue(LeavesBlock.PERSISTENT);
                if (state.is(BlockTags.LOGS) || leaves) {
                    if (found.size() >= max) {
                        return null;
                    }
                    found.put(next.immutable(), state);
                    queue.add(next.immutable());
                    crown |= leaves;
                }
            }
        }
        return crown ? found : null;
    }

    /** Every spot the tree would take is free or its own, and its foot stands on earth. */
    private static boolean fits(ServerLevel level, Map<BlockPos, BlockState> tree, BlockPos by) {
        int lowest = tree.keySet().stream().mapToInt(BlockPos::getY).min().orElse(0);
        for (Map.Entry<BlockPos, BlockState> part : tree.entrySet()) {
            BlockPos to = part.getKey().offset(by);
            if (!level.isLoaded(to) || !(tree.containsKey(to) || level.isEmptyBlock(to))) {
                return false;
            }
            boolean foot = part.getKey().getY() == lowest && part.getValue().is(BlockTags.LOGS);
            if (foot && !level.getBlockState(to.below()).is(BlockTags.DIRT)) {
                return false;
            }
        }
        return true;
    }

    private static void shift(ServerLevel level, Map<BlockPos, BlockState> tree, BlockPos by, int restoreTicks) {
        move(level, tree, by);
        Map<BlockPos, BlockState> moved = new LinkedHashMap<>();
        tree.forEach((pos, state) -> moved.put(pos.offset(by), state));
        Restore.revert(level, restoreTicks, force -> {
            if (!force && !moved.keySet().stream().allMatch(level::isLoaded)) {
                return false;
            }
            // A tree that has been cut or built on meanwhile stays as it is.
            if (moved.entrySet().stream().allMatch(e -> level.getBlockState(e.getKey()) == e.getValue())
                && tree.keySet().stream().allMatch(pos -> moved.containsKey(pos) || level.isEmptyBlock(pos))) {
                move(level, moved, by.multiply(-1));
            }
            return true;
        });
    }

    private static void move(ServerLevel level, Map<BlockPos, BlockState> parts, BlockPos by) {
        parts.keySet().forEach(pos -> Restore.quiet(level, pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()));
        parts.forEach((pos, state) -> Restore.quiet(level, pos.offset(by), state));
    }
}
