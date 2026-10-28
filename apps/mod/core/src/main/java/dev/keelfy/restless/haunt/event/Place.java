package dev.keelfy.restless.haunt.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Finds somewhere valid for an event to act: a wall, a ledge, open floor, room to stand. */
final class Place {
    private static final int TRIES = 16;

    private Place() {}

    /** A one-block-thick wall two blocks high, with the player in front of it and free standing room behind. */
    record Wall(BlockPos low, BlockPos high, BlockPos behind) {}

    static @Nullable Wall wall(ServerPlayer player) {
        ServerLevel level = player.level();
        for (Direction d : horizontals(player)) {
            for (int k = 1; k <= 6; k++) {
                BlockPos low = player.blockPosition().relative(d, k);
                BlockPos high = low.above();
                if (level.isEmptyBlock(low) && level.isEmptyBlock(high)) {
                    continue;
                }
                BlockPos behind = low.relative(d);
                if (k >= 2 && solid(level, low) && solid(level, high) && free(level, behind) && level.getBlockState(behind.below()).isSolidRender()) {
                    return new Wall(low, high, behind);
                }
                break;
            }
        }
        return null;
    }

    /** Standing room (floor and two free blocks) at a random bearing {@code min} to {@code max} blocks away. */
    static @Nullable BlockPos spawnSpot(ServerPlayer player, int min, int max) {
        ServerLevel level = player.level();
        for (int i = 0; i < TRIES; i++) {
            BlockPos base = scatter(player, min, max);
            for (int dy = 2; dy >= -3; dy--) {
                BlockPos pos = base.above(dy);
                if (level.isLoaded(pos) && free(level, pos) && level.getBlockState(pos.below()).isSolidRender()) {
                    return pos;
                }
            }
        }
        return null;
    }

    /** The centre of a plus of five free blocks lying on solid ground, {@code min} to {@code max} blocks away. */
    static @Nullable BlockPos cross(ServerPlayer player, int min, int max) {
        ServerLevel level = player.level();
        for (int i = 0; i < TRIES; i++) {
            BlockPos base = scatter(player, min, max);
            for (int dy = 2; dy >= -2; dy--) {
                BlockPos centre = base.above(dy);
                if (crossCells(centre).stream().allMatch(pos -> level.isLoaded(pos) && level.isEmptyBlock(pos) && level.getBlockState(pos.below()).isSolidRender())) {
                    return centre;
                }
            }
        }
        return null;
    }

    static List<BlockPos> crossCells(BlockPos centre) {
        List<BlockPos> cells = new ArrayList<>(List.of(centre));
        for (Direction d : Direction.Plane.HORIZONTAL) {
            cells.add(centre.relative(d));
        }
        return cells;
    }

    /** A direction in which the player stands at the edge of a drop of 4 to 7 blocks onto solid, dry ground. */
    static @Nullable Direction ledge(ServerPlayer player) {
        ServerLevel level = player.level();
        if (!player.onGround() || player.isInWater()) {
            return null;
        }
        for (Direction d : horizontals(player)) {
            BlockPos edge = player.blockPosition().relative(d);
            if (!free(level, edge)) {
                continue;
            }
            int depth = 0;
            BlockPos below = edge.below();
            while (depth <= 8 && level.isEmptyBlock(below)) {
                depth++;
                below = below.below();
            }
            if (depth >= 4 && depth <= 7 && level.isLoaded(below) && level.getBlockState(below).isSolidRender()) {
                return d;
            }
        }
        return null;
    }

    /** Room for a piston, a block and a power source behind the player, pushing into the player's own spot. */
    record Piston(Direction facing, BlockPos base, BlockPos block, BlockPos power, BlockPos target) {}

    static @Nullable Piston piston(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos at = player.blockPosition();
        for (Direction d : horizontals(player)) {
            BlockPos base = at.relative(d, -2);
            BlockPos block = at.relative(d, -1);
            BlockPos power = base.above();
            BlockPos ahead = at.relative(d);
            boolean room = level.isEmptyBlock(base) && level.isEmptyBlock(block) && level.isEmptyBlock(power)
                && level.isEmptyBlock(at) && level.isEmptyBlock(at.above());
            // The shove carries the player one block on: that must be dry standing room, not a drop.
            boolean landing = free(level, ahead) && level.getBlockState(ahead.below()).isSolidRender();
            if (room && landing) {
                return new Piston(d, base, block, power, at);
            }
        }
        return null;
    }

    static boolean solid(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isSolidRender() && !state.hasBlockEntity() && state.getDestroySpeed(level, pos) >= 0.0F;
    }

    private static boolean free(ServerLevel level, BlockPos pos) {
        return level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above());
    }

    private static List<Direction> horizontals(ServerPlayer player) {
        List<Direction> dirs = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
        Collections.shuffle(dirs, new Random(player.getRandom().nextLong()));
        return dirs;
    }

    private static BlockPos scatter(ServerPlayer player, int min, int max) {
        double angle = player.getRandom().nextDouble() * Math.PI * 2;
        double distance = min + player.getRandom().nextDouble() * (max - min);
        return BlockPos.containing(player.getX() + Math.cos(angle) * distance, player.getY(), player.getZ() + Math.sin(angle) * distance);
    }
}
