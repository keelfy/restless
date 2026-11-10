package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.event.kit.Nearby;
import dev.keelfy.restless.haunt.event.kit.Place;
import dev.keelfy.restless.haunt.event.kit.SimpleEvent;
import dev.keelfy.restless.haunt.event.kit.Swaps;
import dev.keelfy.restless.haunt.event.kit.Where;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The surroundings are rebuilt in this player's view alone, mostly where they are not looking; see {@link Swaps}. Out
 * of view the blocks are shown changed and put back for them the moment they turn towards the change. The server keeps
 * its own blocks throughout (unless the swaps setting is {@code REAL}, when they are changed for a short while
 * instead).
 */
final class Remodels {
    private Remodels() {}

    static List<HauntEvent> all() {
        return List.of(
                // 284: stone and earth behind the player turn to netherrack until they look round
                SimpleEvent.light(
                        "netherrack_behind",
                        EventTag.BLOCKS,
                        player -> !rockBehind(player).isEmpty(),
                        Remodels::turnNetherrack),
                // 289: indoors the roof is shown gone, open sky above
                SimpleEvent.light(
                        "roof_gone",
                        EventTag.BLOCKS,
                        player ->
                                roof(player).size() >= Config.event("roof_gone").integer("min_blocks"),
                        Remodels::openRoof),
                // 300: the floor of the way just walked is shown in another order until they look back
                SimpleEvent.light(
                        "path_rearranged",
                        EventTag.BLOCKS,
                        player -> !rearranged(player).isEmpty(),
                        Remodels::rearrange),
                // 303: the tree near the player's bed is shown gone until they come near
                SimpleEvent.light(
                        "landmark_tree_gone",
                        EventTag.BLOCKS,
                        player -> landmark(player) != null,
                        Remodels::removeLandmark),
                // 305: the tunnel behind the player is shown filled in with stone until they look back
                SimpleEvent.light(
                        "tunnel_filled",
                        EventTag.BLOCKS,
                        player -> !fill(player).isEmpty(),
                        Remodels::fillTunnel),
                // 309: indoors, stairs, slabs and a bed are shown upside down under the ceiling
                SimpleEvent.light(
                        "ceiling_furniture",
                        EventTag.BLOCKS,
                        player -> Household.indoors(player)
                                && !ceilingSpots(player).isEmpty(),
                        Remodels::furnishCeiling),
                // 479: in the ocean, lights drift under the water on a straight line
                SimpleEvent.light(
                        "sea_lights",
                        EventTag.WATER,
                        player -> Where.biome(player, BiomeTags.IS_OCEAN)
                                && !seaPath(player).isEmpty(),
                        Remodels::driftLights));
    }

    // Out of view

    /** How squarely the player looks at the block: 1 dead ahead, 0 to the side, -1 behind. */
    private static double facing(ServerPlayer player, BlockPos pos) {
        Vec3 to = Vec3.atCenterOf(pos).subtract(player.getEyePosition());
        double length = to.length();
        return length < 1.0E-6 ? 1.0 : player.getLookAngle().dot(to) / length;
    }

    /**
     * Shows {@code shown} to the player and tells them the truth again when they look towards any of it, or after
     * {@code ticks}.
     */
    private static void unseen(ServerPlayer player, Map<BlockPos, BlockState> shown, Config.Section config) {
        ServerLevel level = player.level();
        int ticks = config.integer("ticks");
        if (shown.isEmpty()) {
            return;
        }
        if (Swaps.real(level)) {
            Swaps.show(player, shown, ticks);
            return;
        }
        shown.forEach((pos, state) -> player.connection.send(new ClientboundBlockUpdatePacket(pos, state)));
        List<BlockPos> where = List.copyOf(shown.keySet());
        double seen = config.num("view_dot");
        int interval = config.integer("interval_ticks");
        Scheduler.repeat(player, interval, interval, (p, n) -> {
            if (p.level() != level) {
                return false;
            }
            boolean over = (n + 1) * interval >= ticks || where.stream().anyMatch(pos -> facing(p, pos) > seen);
            if (over) {
                where.forEach(pos -> p.connection.send(new ClientboundBlockUpdatePacket(level, pos)));
            }
            return !over;
        });
    }

    /** Standing spots {@code min_distance} to {@code max_distance} behind the player, {@code width} to either side. */
    private static List<BlockPos> behindSpots(ServerPlayer player, Config.Section config) {
        double yaw = Math.toRadians(player.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 side = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        int width = config.integer("width");
        Set<BlockPos> found = new LinkedHashSet<>();
        for (int k = config.integer("min_distance"); k <= config.integer("max_distance"); k++) {
            for (int l = -width; l <= width; l++) {
                Vec3 at = player.position().subtract(forward.scale(k)).add(side.scale(l));
                BlockPos spot = Place.standing(player.level(), BlockPos.containing(at.x, player.getY(), at.z));
                if (spot != null) {
                    found.add(spot);
                }
            }
        }
        return List.copyOf(found);
    }

    // 284

    /** Natural blocks with a free face within reach, behind the player; the nearest first. */
    private static List<BlockPos> rockBehind(ServerPlayer player) {
        Config.Section config = Config.event("netherrack_behind");
        ServerLevel level = player.level();
        double hidden = config.num("hide_dot");
        double least = config.integer("min_distance");
        return Nearby.all(
                        player,
                        "netherrack_behind",
                        state -> state.isSolidRender() && !state.hasBlockEntity() && !state.is(Blocks.NETHERRACK))
                .stream()
                .filter(pos -> pos.distSqr(player.blockPosition()) >= least * least
                        && facing(player, pos) < hidden
                        && exposed(level, pos))
                .sorted(Comparator.comparingDouble(pos -> pos.distSqr(player.blockPosition())))
                .limit(config.integer("max_blocks"))
                .toList();
    }

    private static boolean exposed(ServerLevel level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (level.isEmptyBlock(pos.relative(d))) {
                return true;
            }
        }
        return false;
    }

    private static void turnNetherrack(ServerPlayer player) {
        Map<BlockPos, BlockState> shown = new LinkedHashMap<>();
        rockBehind(player).forEach(pos -> shown.put(pos, Blocks.NETHERRACK.defaultBlockState()));
        unseen(player, shown, Config.event("netherrack_behind"));
    }

    // 289

    /** The blocks over the player's head, in the columns around them, up to the sky. Empty if the roof is too thick. */
    private static List<BlockPos> roof(ServerPlayer player) {
        if (!Household.indoors(player)) {
            return List.of();
        }
        Config.Section config = Config.event("roof_gone");
        ServerLevel level = player.level();
        BlockPos from = player.blockPosition();
        int lowest = from.getY() + 2;
        int radius = config.integer("radius");
        if (level.getHeight(Heightmap.Types.WORLD_SURFACE, from.getX(), from.getZ()) - lowest
                > config.integer("max_depth")) {
            return List.of();
        }
        List<BlockPos> roof = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = from.getX() + dx;
                int z = from.getZ() + dz;
                int top = Math.min(
                        level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), lowest + config.integer("max_depth"));
                for (int y = lowest; y < top; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.isLoaded(pos) && !level.isEmptyBlock(pos)) {
                        roof.add(pos);
                    }
                }
            }
        }
        return roof;
    }

    private static void openRoof(ServerPlayer player) {
        Swaps.show(
                player,
                roof(player),
                Blocks.AIR.defaultBlockState(),
                Config.event("roof_gone").integer("ticks"));
    }

    // 300

    /** The floor of the way behind the player, with the blocks of it in another order; empty if all are alike. */
    private static Map<BlockPos, BlockState> rearranged(ServerPlayer player) {
        Config.Section config = Config.event("path_rearranged");
        ServerLevel level = player.level();
        List<BlockPos> floor = new ArrayList<>();
        for (BlockPos spot : behindSpots(player, config)) {
            BlockState state = level.getBlockState(spot.below());
            if (state.isSolidRender() && !state.hasBlockEntity()) {
                floor.add(spot.below());
            }
        }
        List<BlockState> states =
                new ArrayList<>(floor.stream().map(level::getBlockState).toList());
        if (floor.size() < config.integer("min_blocks")
                || states.stream().distinct().count() < 2) {
            return Map.of();
        }
        Collections.shuffle(states, new Random(player.getRandom().nextLong()));
        Map<BlockPos, BlockState> shown = new LinkedHashMap<>();
        for (int i = 0; i < floor.size(); i++) {
            if (!states.get(i).equals(level.getBlockState(floor.get(i)))) {
                shown.put(floor.get(i), states.get(i));
            }
        }
        return shown;
    }

    private static void rearrange(ServerPlayer player) {
        unseen(player, rearranged(player), Config.event("path_rearranged"));
    }

    // 303

    /** The tallest tree found by the player's bed, or null if there is none, or the player is too near or too far. */
    private static @Nullable List<BlockPos> landmark(ServerPlayer player) {
        ServerPlayer.RespawnConfig home = player.getRespawnConfig();
        ServerLevel level = player.level();
        if (home == null || home.respawnData().dimension() != level.dimension()) {
            return null;
        }
        Config.Section config = Config.event("landmark_tree_gone");
        BlockPos base = home.respawnData().pos();
        int search = config.integer("search");
        List<BlockPos> best = null;
        for (int i = 0; i < config.integer("tries"); i++) {
            int x = base.getX() + player.getRandom().nextInt(search * 2 + 1) - search;
            int z = base.getZ() + player.getRandom().nextInt(search * 2 + 1) - search;
            if (!level.isLoaded(new BlockPos(x, base.getY(), z))) {
                continue;
            }
            BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
            if (level.getBlockState(top).is(BlockTags.LOGS)) {
                List<BlockPos> tree = tree(level, top, config);
                if (tree != null && (best == null || tree.size() > best.size())) {
                    best = tree;
                }
            }
        }
        if (best == null) {
            return null;
        }
        double away = Math.sqrt(player.position().distanceToSqr(Vec3.atCenterOf(best.getFirst())));
        return away >= config.num("min_distance") && away <= config.num("max_distance") ? best : null;
    }

    /**
     * The logs connected to {@code start}, standing on earth, and the leaves within {@code crown} steps of them;
     * {@code start} first. Null if it is too big or has too few leaves to be a tree.
     */
    private static @Nullable List<BlockPos> tree(ServerLevel level, BlockPos start, Config.Section config) {
        int max = config.integer("max_blocks");
        Set<BlockPos> found = new LinkedHashSet<>(List.of(start));
        Deque<BlockPos> queue = new ArrayDeque<>(List.of(start));
        while (!queue.isEmpty()) {
            BlockPos at = queue.poll();
            for (BlockPos next : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1))) {
                if (!found.contains(next)
                        && level.isLoaded(next)
                        && level.getBlockState(next).is(BlockTags.LOGS)) {
                    if (found.size() >= max) {
                        return null;
                    }
                    found.add(next.immutable());
                    queue.add(next.immutable());
                }
            }
        }
        List<BlockPos> logs = List.copyOf(found);
        int lowest = logs.stream().mapToInt(BlockPos::getY).min().orElse(0);
        if (logs.stream()
                .noneMatch(log ->
                        log.getY() == lowest && level.getBlockState(log.below()).is(BlockTags.SUBSTRATE_OVERWORLD))) {
            return null;
        }
        List<BlockPos> ring = logs;
        for (int step = 0; step < config.integer("crown") && !ring.isEmpty(); step++) {
            List<BlockPos> next = new ArrayList<>();
            for (BlockPos at : ring) {
                for (Direction d : Direction.values()) {
                    BlockPos pos = at.relative(d);
                    if (!found.contains(pos)
                            && level.isLoaded(pos)
                            && level.getBlockState(pos).is(BlockTags.LEAVES)) {
                        if (found.size() >= max) {
                            return null;
                        }
                        found.add(pos);
                        next.add(pos);
                    }
                }
            }
            ring = next;
        }
        return found.size() - logs.size() >= config.integer("min_leaves") ? List.copyOf(found) : null;
    }

    private static void removeLandmark(ServerPlayer player) {
        Config.Section config = Config.event("landmark_tree_gone");
        List<BlockPos> tree = landmark(player);
        if (tree == null) {
            return;
        }
        ServerLevel level = player.level();
        int ticks = config.integer("ticks");
        Swaps.show(player, tree, Blocks.AIR.defaultBlockState(), ticks);
        if (Swaps.real(level)) {
            return;
        }
        // It is back for them as soon as they come near it.
        Vec3 at = Vec3.atCenterOf(tree.getFirst());
        double near = config.num("vanish_distance");
        int interval = config.integer("interval_ticks");
        Scheduler.repeat(player, interval, interval, (p, n) -> {
            if (p.level() != level) {
                return false;
            }
            if (p.position().distanceToSqr(at) <= near * near) {
                tree.forEach(pos -> p.connection.send(new ClientboundBlockUpdatePacket(level, pos)));
                return false;
            }
            return (n + 1) * interval < ticks;
        });
    }

    // 305

    /**
     * Both blocks of the walkway behind the player where stone stands overhead and the sky is not seen: a tunnel, dug
     * or natural.
     */
    private static Map<BlockPos, BlockState> fill(ServerPlayer player) {
        ServerLevel level = player.level();
        if (level.canSeeSky(player.blockPosition())) {
            return Map.of();
        }
        Config.Section config = Config.event("tunnel_filled");
        BlockState stone = BuiltInRegistries.BLOCK
                .getValue(Identifier.parse(config.text("block")))
                .defaultBlockState();
        Map<BlockPos, BlockState> shown = new LinkedHashMap<>();
        int cells = 0;
        for (BlockPos spot : behindSpots(player, config)) {
            if (!level.canSeeSky(spot) && level.getBlockState(spot.above(2)).isSolidRender()) {
                shown.put(spot, stone);
                shown.put(spot.above(), stone);
                cells++;
            }
        }
        return cells >= config.integer("min_cells") ? shown : Map.of();
    }

    private static void fillTunnel(ServerPlayer player) {
        unseen(player, fill(player), Config.event("tunnel_filled"));
    }

    // 309

    /** Air blocks under the ceiling of the room, a little way from the player. */
    private static List<BlockPos> ceilingSpots(ServerPlayer player) {
        ServerLevel level = player.level();
        int lowest = player.getBlockY() + 1;
        return Nearby.all(player, "ceiling_furniture", BlockState::isAir).stream()
                .filter(pos -> pos.getY() >= lowest
                        && ceiling(level, pos)
                        && level.isEmptyBlock(pos.below())
                        && Math.hypot(pos.getX() + 0.5 - player.getX(), pos.getZ() + 0.5 - player.getZ()) >= 1.5)
                .toList();
    }

    private static boolean ceiling(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos.above()).isSolidRender();
    }

    private static void furnishCeiling(ServerPlayer player) {
        Config.Section config = Config.event("ceiling_furniture");
        ServerLevel level = player.level();
        List<BlockPos> spots = new ArrayList<>(ceilingSpots(player));
        Collections.shuffle(spots, new Random(player.getRandom().nextLong()));
        List<String> pieces = config.texts("pieces");
        Map<BlockPos, BlockState> shown = new LinkedHashMap<>();
        for (BlockPos spot : spots) {
            if (shown.size() >= config.integer("count") || shown.containsKey(spot)) {
                continue;
            }
            Block block = BuiltInRegistries.BLOCK.getValue(
                    Identifier.parse(pieces.get(player.getRandom().nextInt(pieces.size()))));
            Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(player.getRandom());
            BlockState state = block.defaultBlockState();
            if (state.hasProperty(BlockStateProperties.HALF)) {
                state = state.setValue(BlockStateProperties.HALF, Half.TOP);
            }
            if (state.hasProperty(BlockStateProperties.SLAB_TYPE)) {
                state = state.setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP);
            }
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
            }
            if (state.hasProperty(BlockStateProperties.BED_PART)) {
                // A bed is two blocks long: its head needs a spot of its own under the ceiling.
                BlockPos head = spot.relative(facing);
                if (shown.containsKey(head) || !level.isEmptyBlock(head) || !ceiling(level, head)) {
                    continue;
                }
                shown.put(head, state.setValue(BlockStateProperties.BED_PART, BedPart.HEAD));
                state = state.setValue(BlockStateProperties.BED_PART, BedPart.FOOT);
            }
            shown.put(spot, state);
        }
        Swaps.show(player, shown, config.integer("ticks"));
    }

    // 479

    /**
     * A straight line of water blocks {@code depth} under the surface, starting {@code min_distance} to
     * {@code max_distance} blocks away at a random bearing and running off at another; empty if none was found.
     */
    private static List<BlockPos> seaPath(ServerPlayer player) {
        Config.Section config = Config.event("sea_lights");
        ServerLevel level = player.level();
        for (int i = 0; i < config.integer("tries"); i++) {
            double bearing = player.getRandom().nextDouble() * Math.PI * 2;
            double away = config.num("min_distance")
                    + player.getRandom().nextDouble() * (config.num("max_distance") - config.num("min_distance"));
            int x = (int) Math.floor(player.getX() + Math.cos(bearing) * away);
            int z = (int) Math.floor(player.getZ() + Math.sin(bearing) * away);
            int surface = surface(level, x, player.getBlockY(), z);
            if (surface == Integer.MIN_VALUE) {
                continue;
            }
            double run = player.getRandom().nextDouble() * Math.PI * 2;
            List<BlockPos> path = new ArrayList<>();
            for (int k = 0; k < config.integer("length"); k++) {
                BlockPos pos =
                        new BlockPos((int) Math.floor(x + Math.cos(run) * k), surface - config.integer("depth"), (int)
                                Math.floor(z + Math.sin(run) * k));
                if (!level.isLoaded(pos) || !level.getBlockState(pos).is(Blocks.WATER)) {
                    break;
                }
                if (path.isEmpty() || !path.getLast().equals(pos)) {
                    path.add(pos);
                }
            }
            if (path.size() >= config.integer("min_length")) {
                return path;
            }
        }
        return List.of();
    }

    /** The y of the top water block in the column near {@code y}, or {@code Integer.MIN_VALUE}. */
    private static int surface(ServerLevel level, int x, int y, int z) {
        for (int at = y + 3; at >= y - 6; at--) {
            BlockPos pos = new BlockPos(x, at, z);
            if (level.isLoaded(pos)
                    && level.getBlockState(pos).is(Blocks.WATER)
                    && !level.getBlockState(pos.above()).is(Blocks.WATER)) {
                return at;
            }
        }
        return Integer.MIN_VALUE;
    }

    /** A sea lantern lights up cell by cell along the path, trailing {@code trail} cells, each put back behind it. */
    private static void driftLights(ServerPlayer player) {
        Config.Section config = Config.event("sea_lights");
        List<BlockPos> path = seaPath(player);
        ServerLevel level = player.level();
        BlockState lantern = Blocks.SEA_LANTERN.defaultBlockState();
        int trail = config.integer("trail");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            if (p.level() != level || path.isEmpty()) {
                return false;
            }
            if (n < path.size()) {
                p.connection.send(new ClientboundBlockUpdatePacket(path.get(n), lantern));
            }
            int gone = n - trail;
            if (gone >= 0 && gone < path.size()) {
                p.connection.send(new ClientboundBlockUpdatePacket(level, path.get(gone)));
            }
            return gone + 1 < path.size();
        });
    }
}
