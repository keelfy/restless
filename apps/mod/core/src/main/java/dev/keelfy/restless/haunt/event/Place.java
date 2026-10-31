package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Finds somewhere valid for an event to act: a wall, a ledge, open floor, room to stand. */
final class Place {
    private Place() {}

    /** A one-block-thick wall two blocks high, with the player in front of it and free standing room behind. */
    record Wall(BlockPos low, BlockPos high, BlockPos behind) {}

    static @Nullable Wall wall(ServerPlayer player) {
        ServerLevel level = player.level();
        int min = config().integer("wall_min");
        for (Direction d : horizontals(player)) {
            for (int k = 1; k <= config().integer("wall_max"); k++) {
                BlockPos low = player.blockPosition().relative(d, k);
                BlockPos high = low.above();
                if (level.isEmptyBlock(low) && level.isEmptyBlock(high)) {
                    continue;
                }
                BlockPos behind = low.relative(d);
                if (k >= min
                        && solid(level, low)
                        && solid(level, high)
                        && free(level, behind)
                        && level.getBlockState(behind.below()).isSolidRender()) {
                    return new Wall(low, high, behind);
                }
                break;
            }
        }
        return null;
    }

    /** Standing room (floor and two free blocks) at a random bearing {@code min} to {@code max} blocks away. */
    static @Nullable BlockPos spawnSpot(ServerPlayer player, int min, int max) {
        for (int i = 0; i < config().integer("tries"); i++) {
            BlockPos found = standing(player.level(), scatter(player, min, max));
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Standing room {@code distance} blocks away, {@code minAngle} to {@code maxAngle} degrees to either side of where
     * the player looks, that also passes {@code ok}. A single column often has none (a tree, a hill, water), so several
     * bearings are tried.
     */
    static @Nullable BlockPos standingAhead(
            ServerPlayer player, double distance, double minAngle, double maxAngle, Predicate<BlockPos> ok) {
        for (int i = 0; i < config().integer("tries"); i++) {
            double angle = minAngle + player.getRandom().nextDouble() * (maxAngle - minAngle);
            double side = player.getRandom().nextBoolean() ? 1.0 : -1.0;
            BlockPos found = standingAt(player, distance, side * angle);
            if (found != null && ok.test(found)) {
                return found;
            }
        }
        return null;
    }

    static @Nullable BlockPos standingAhead(ServerPlayer player, double distance, double minAngle, double maxAngle) {
        return standingAhead(player, distance, minAngle, maxAngle, pos -> true);
    }

    /** Standing room {@code distance} blocks away, {@code yawOffset} degrees to the side of where the player looks. */
    private static @Nullable BlockPos standingAt(ServerPlayer player, double distance, double yawOffset) {
        Vec3 at = Nearby.around(player, distance, yawOffset);
        return standing(player.level(), BlockPos.containing(at.x, player.getY(), at.z));
    }

    /**
     * The first spot in the column of {@code base}, a few blocks up to a few down, with a floor and two free blocks.
     */
    static @Nullable BlockPos standing(ServerLevel level, BlockPos base) {
        for (int dy = config().integer("spawn_climb"); dy >= -config().integer("spawn_drop"); dy--) {
            BlockPos pos = base.above(dy);
            if (level.isLoaded(pos)
                    && free(level, pos)
                    && level.getBlockState(pos.below()).isSolidRender()) {
                return pos;
            }
        }
        return null;
    }

    /**
     * An empty block {@code min} to {@code max} blocks from {@code centre}, a layer up or down, that passes {@code ok}.
     */
    static @Nullable BlockPos near(ServerPlayer player, BlockPos centre, int min, int max, Predicate<BlockPos> ok) {
        for (int i = 0; i < config().integer("tries"); i++) {
            BlockPos base = scatter(player, Vec3.atBottomCenterOf(centre), min, max);
            for (int dy = 1; dy >= -1; dy--) {
                if (ok.test(base.above(dy))) {
                    return base.above(dy);
                }
            }
        }
        return null;
    }

    /** Loaded air on a solid floor. */
    static boolean floored(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos)
                && level.isEmptyBlock(pos)
                && level.getBlockState(pos.below()).isSolidRender();
    }

    /** The centre of a plus of five free blocks lying on solid ground, {@code min} to {@code max} blocks away. */
    static @Nullable BlockPos cross(ServerPlayer player, int min, int max) {
        ServerLevel level = player.level();
        for (int i = 0; i < config().integer("tries"); i++) {
            BlockPos base = scatter(player, min, max);
            for (int dy = config().integer("cross_climb"); dy >= -config().integer("cross_drop"); dy--) {
                BlockPos centre = base.above(dy);
                if (crossCells(centre).stream()
                        .allMatch(pos -> level.isLoaded(pos)
                                && level.isEmptyBlock(pos)
                                && level.getBlockState(pos.below()).isSolidRender())) {
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

    /** A direction in which the player stands at the edge of a drop of the configured depth onto solid, dry ground. */
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
            int minDrop = config().integer("ledge_min_drop");
            int maxDrop = config().integer("ledge_max_drop");
            BlockPos below = edge.below();
            while (depth <= maxDrop + 1 && level.isEmptyBlock(below)) {
                depth++;
                below = below.below();
            }
            if (depth >= minDrop
                    && depth <= maxDrop
                    && level.isLoaded(below)
                    && level.getBlockState(below).isSolidRender()) {
                return d;
            }
        }
        return null;
    }

    /**
     * A ragged patch around {@code centre}: each position within the radius (a few layers up or down) that passes
     * {@code ok} is kept with a chance that falls off towards the edge, so the shape is a scatter, never a disc. The
     * centre is always kept.
     */
    static List<BlockPos> patch(
            ServerPlayer player, BlockPos centre, int radius, double density, Predicate<BlockPos> ok) {
        List<BlockPos> kept = new ArrayList<>();
        int layers = config().integer("patch_layers");
        for (BlockPos pos : BlockPos.betweenClosed(
                centre.offset(-radius, -layers, -radius), centre.offset(radius, layers, radius))) {
            double edge = Math.sqrt(pos.distSqr(centre)) / (radius + 1.0);
            if (edge < 1.0
                    && ok.test(pos)
                    && (pos.equals(centre) || player.getRandom().nextDouble() < density * (1.0 - edge * edge))) {
                kept.add(pos.immutable());
            }
        }
        return kept;
    }

    static boolean solid(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isSolidRender() && !state.hasBlockEntity() && state.getDestroySpeed(level, pos) >= 0.0F;
    }

    private static boolean free(ServerLevel level, BlockPos pos) {
        return open(level, pos) && open(level, pos.above());
    }

    /**
     * Nothing to bump into: air, or grass, flowers, a torch; not water. Fields are full of plants, so air alone is
     * rare.
     */
    static boolean open(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty()
                && state.getFluidState().isEmpty()
                && !state.hasBlockEntity();
    }

    private static List<Direction> horizontals(ServerPlayer player) {
        List<Direction> dirs =
                new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
        Collections.shuffle(dirs, new Random(player.getRandom().nextLong()));
        return dirs;
    }

    private static Config.Section config() {
        return Config.section("place");
    }

    private static BlockPos scatter(ServerPlayer player, int min, int max) {
        return scatter(player, player.position(), min, max);
    }

    private static BlockPos scatter(ServerPlayer player, Vec3 around, int min, int max) {
        double angle = player.getRandom().nextDouble() * Math.PI * 2;
        double distance = min + player.getRandom().nextDouble() * (max - min);
        return BlockPos.containing(
                around.x + Math.cos(angle) * distance, around.y, around.z + Math.sin(angle) * distance);
    }
}
