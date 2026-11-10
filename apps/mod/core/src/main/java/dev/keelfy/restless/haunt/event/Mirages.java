package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.block.UnlitLights;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Blocks that are not what they seem, for this player alone: doors walled up, windows boarded, torches burnt out, a
 * pit, a lava pool that looks like water. All go through {@link Swaps} (the lava pool through packets, since it must
 * never be real), so the world is unchanged and a click shows the truth.
 */
final class Mirages {
    private Mirages() {}

    static List<HauntEvent> all() {
        return List.of(
                // 282: a door in the house is bricked up for a moment, in what the wall around it is made of
                SimpleEvent.light(
                        "bricked_door",
                        EventTag.BLOCKS,
                        player -> Nearby.any(player, "bricked_door", Mirages::lowerDoor),
                        Mirages::brickDoor),
                // 283: the windows look boarded up from outside
                SimpleEvent.light(
                        "boarded_windows",
                        EventTag.BLOCKS,
                        player -> player.level().canSeeSky(player.blockPosition())
                                && Nearby.any(player, "boarded_windows", Place::glass),
                        player -> {
                            Config.Section config = Config.event("boarded_windows");
                            Swaps.show(
                                    player,
                                    nearest(
                                            player,
                                            Nearby.all(player, "boarded_windows", Place::glass),
                                            "boarded_windows"),
                                    block(config.text("board")).defaultBlockState(),
                                    config.integer("ticks"));
                        }),
                // 285: a staircase leads into a wall
                SimpleEvent.light(
                        "stair_wall",
                        EventTag.BLOCKS,
                        player -> !stairEnds(player).isEmpty(),
                        Mirages::wallUpStair),
                // 286: a pit opens in the yard, far enough that the client never walks into it
                SimpleEvent.light("yard_pit", EventTag.BLOCKS, player -> pitSpot(player) != null, Mirages::openPit),
                // 287: the torches nearby look burnt out
                SimpleEvent.light(
                        "unlit_torches",
                        EventTag.LIGHTS,
                        player -> Nearby.any(player, "unlit_torches", UnlitLights::isTorch),
                        player -> {
                            Map<BlockPos, BlockState> shown = new LinkedHashMap<>();
                            for (BlockPos pos : nearest(
                                    player,
                                    Nearby.all(player, "unlit_torches", UnlitLights::isTorch),
                                    "unlit_torches")) {
                                shown.put(
                                        pos,
                                        UnlitLights.extinguished(player.level().getBlockState(pos)));
                            }
                            Swaps.show(
                                    player, shown, Config.event("unlit_torches").integer("ticks"));
                        }),
                // 288: an ore in the cave wall that is only stone; a click shows the stone
                SimpleEvent.light(
                        "fake_ore",
                        EventTag.BLOCKS,
                        player -> !player.level().canSeeSky(player.blockPosition())
                                && !oreSpots(player).isEmpty(),
                        Mirages::plantOre),
                // 290: a ladder ends halfway up
                SimpleEvent.light(
                        "ladder_stub",
                        EventTag.BLOCKS,
                        player -> !ladderTops(player).isEmpty(),
                        Mirages::cutLadder),
                // 291: a second door in a plain wall
                SimpleEvent.light(
                        "second_door", EventTag.DOORS, player -> Place.wall(player) != null, Mirages::addDoor),
                // 292: crops look ripe; harvested they were green
                SimpleEvent.light(
                        "false_harvest",
                        EventTag.BLOCKS,
                        player -> Nearby.any(player, "false_harvest", Mirages::unripe),
                        player -> {
                            ServerLevel level = player.level();
                            Map<BlockPos, BlockState> shown = new LinkedHashMap<>();
                            for (BlockPos pos : nearest(
                                    player, Nearby.all(player, "false_harvest", Mirages::unripe), "false_harvest")) {
                                CropBlock crop =
                                        (CropBlock) level.getBlockState(pos).getBlock();
                                shown.put(pos, crop.getStateForAge(crop.getMaxAge()));
                            }
                            Swaps.show(
                                    player, shown, Config.event("false_harvest").integer("ticks"));
                        }),
                // 293: the pond turns to lava for a moment
                SimpleEvent.light(
                        "lava_pond",
                        EventTag.WATER,
                        player -> Nearby.any(player, "lava_pond", Mirages::surfaceWater),
                        player -> {
                            ServerLevel level = player.level();
                            List<BlockPos> surface = Nearby.all(player, "lava_pond", Mirages::surfaceWater).stream()
                                    .filter(pos -> level.isEmptyBlock(pos.above()))
                                    .toList();
                            Swaps.show(
                                    player,
                                    nearest(player, surface, "lava_pond"),
                                    Blocks.LAVA.defaultBlockState(),
                                    Config.event("lava_pond").integer("ticks"));
                        }),
                // 294: a lava pool looks like water until the player is 8 blocks from it; a sizzle gives it away
                SimpleEvent.harmful(
                        "lava_as_water",
                        EventTag.WATER,
                        player -> Assaults.healthy(player, "lava_as_water")
                                && !farLava(player).isEmpty(),
                        Mirages::lavaAsWater),
                // 295: snow lies on the roofs of the player's house in summer
                SimpleEvent.light(
                        "roof_snow",
                        EventTag.BLOCKS,
                        player -> player.level().canSeeSky(player.blockPosition())
                                && !roofs(player).isEmpty(),
                        player -> {
                            Config.Section config = Config.event("roof_snow");
                            BlockState snow = Blocks.SNOW
                                    .defaultBlockState()
                                    .setValue(SnowLayerBlock.LAYERS, config.integer("layers"));
                            Swaps.show(
                                    player, nearest(player, roofs(player), "roof_snow"), snow, config.integer("ticks"));
                        }),
                // 296: the house glass turns black: tinted glass, and black panes
                SimpleEvent.light(
                        "black_glass",
                        EventTag.BLOCKS,
                        player -> Nearby.any(player, "black_glass", Place::glass),
                        player -> {
                            ServerLevel level = player.level();
                            Map<BlockPos, BlockState> shown = new LinkedHashMap<>();
                            for (BlockPos pos :
                                    nearest(player, Nearby.all(player, "black_glass", Place::glass), "black_glass")) {
                                BlockState state = level.getBlockState(pos);
                                shown.put(
                                        pos,
                                        state.is(BlockTags.IMPERMEABLE)
                                                ? Blocks.TINTED_GLASS.defaultBlockState()
                                                : Blocks.STAINED_GLASS_PANE
                                                        .black()
                                                        .defaultBlockState()
                                                        .withPropertiesOf(state));
                            }
                            Swaps.show(
                                    player, shown, Config.event("black_glass").integer("ticks"));
                        }));
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
    }

    /** The {@code max_blocks} of {@code found} nearest the player. */
    private static List<BlockPos> nearest(ServerPlayer player, List<BlockPos> found, String event) {
        return found.stream()
                .sorted(Comparator.comparingDouble(pos -> pos.distSqr(player.blockPosition())))
                .limit(Config.event(event).integer("max_blocks"))
                .toList();
    }

    // 282
    private static boolean lowerDoor(BlockState state) {
        return state.getBlock() instanceof DoorBlock
                && DoorBlock.isWoodenDoor(state)
                && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
    }

    private static void brickDoor(ServerPlayer player) {
        Config.Section config = Config.event("bricked_door");
        ServerLevel level = player.level();
        BlockPos low = Nearby.random(player, "bricked_door", Mirages::lowerDoor);
        if (low == null) {
            return;
        }
        Map<BlockState, Integer> counts = new HashMap<>();
        for (BlockPos part : List.of(low, low.above())) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockState next = level.getBlockState(part.relative(d));
                if (next.isSolidRender() && !next.hasBlockEntity() && !next.is(BlockTags.DOORS)) {
                    counts.merge(next, 1, Integer::sum);
                }
            }
        }
        BlockState wall = counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(block(config.text("fallback")).defaultBlockState());
        Swaps.show(player, List.of(low, low.above()), wall, config.integer("ticks"));
    }

    // 285: for each bottom stair that is the last step of a flight, the two blocks above it
    private static List<BlockPos> stairEnds(ServerPlayer player) {
        ServerLevel level = player.level();
        List<BlockPos> ends = new ArrayList<>();
        for (BlockPos pos : Nearby.all(
                player,
                "stair_wall",
                s -> s.getBlock() instanceof StairBlock && s.getValue(StairBlock.HALF) == Half.BOTTOM)) {
            Direction up = level.getBlockState(pos).getValue(StairBlock.FACING);
            BlockPos ahead = pos.relative(up);
            if (level.getBlockState(pos.relative(up.getOpposite()).below()).getBlock() instanceof StairBlock
                    && Place.solid(level, ahead)
                    && level.isEmptyBlock(ahead.above())
                    && level.isEmptyBlock(pos.above())
                    && level.isEmptyBlock(pos.above(2))) {
                ends.add(pos);
            }
        }
        return ends;
    }

    private static void wallUpStair(ServerPlayer player) {
        ServerLevel level = player.level();
        List<BlockPos> ends = stairEnds(player);
        if (ends.isEmpty()) {
            return;
        }
        BlockPos pos = ends.get(player.getRandom().nextInt(ends.size()));
        BlockState wall =
                level.getBlockState(pos.relative(level.getBlockState(pos).getValue(StairBlock.FACING)));
        Swaps.show(
                player,
                List.of(pos.above(), pos.above(2)),
                wall,
                Config.event("stair_wall").integer("ticks"));
    }

    // 286
    private static @Nullable BlockPos pitSpot(ServerPlayer player) {
        Config.Section config = Config.event("yard_pit");
        BlockPos spot = Place.spawnSpot(player, config.integer("min_distance"), config.integer("max_distance"));
        return spot != null && player.level().canSeeSky(spot) ? spot : null;
    }

    private static void openPit(ServerPlayer player) {
        Config.Section config = Config.event("yard_pit");
        ServerLevel level = player.level();
        BlockPos spot = pitSpot(player);
        if (spot == null) {
            return;
        }
        int radius = config.integer("radius");
        BlockState air = Blocks.AIR.defaultBlockState();
        Map<BlockPos, BlockState> shown = new LinkedHashMap<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos ground = spot.below().offset(dx, 0, dz);
                for (int depth = 0; depth < config.integer("depth"); depth++) {
                    if (Place.solid(level, ground.below(depth))) {
                        shown.put(ground.below(depth), air);
                    }
                }
                // Grass or flowers over the hole would hang in the air.
                BlockPos over = ground.above();
                if (shown.containsKey(ground) && !level.isEmptyBlock(over)) {
                    shown.put(over, air);
                }
            }
        }
        Swaps.show(player, shown, config.integer("ticks"));
    }

    // 288: stone (or deepslate) with air against it, shown as the ore listed for it
    private static List<BlockPos> oreSpots(ServerPlayer player) {
        ServerLevel level = player.level();
        Config.Section ores = Config.event("fake_ore").section("ores");
        return Nearby.all(
                        player,
                        "fake_ore",
                        s -> ores.has(
                                BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString()))
                .stream()
                .filter(pos -> {
                    for (Direction d : Direction.values()) {
                        if (level.isEmptyBlock(pos.relative(d))) {
                            return true;
                        }
                    }
                    return false;
                })
                .toList();
    }

    private static void plantOre(ServerPlayer player) {
        Config.Section config = Config.event("fake_ore");
        List<BlockPos> spots = oreSpots(player);
        if (spots.isEmpty()) {
            return;
        }
        BlockPos pos = spots.get(player.getRandom().nextInt(spots.size()));
        String stone = BuiltInRegistries.BLOCK
                .getKey(player.level().getBlockState(pos).getBlock())
                .toString();
        Swaps.show(player, pos, block(config.section("ores").text(stone)).defaultBlockState(), config.integer("ticks"));
    }

    // 290: the topmost ladder of each column at least min_length high
    private static List<BlockPos> ladderTops(ServerPlayer player) {
        ServerLevel level = player.level();
        int min = Config.event("ladder_stub").integer("min_length");
        return Nearby.all(player, "ladder_stub", s -> s.is(Blocks.LADDER)).stream()
                .filter(pos -> !level.getBlockState(pos.above()).is(Blocks.LADDER) && columnHeight(level, pos) >= min)
                .toList();
    }

    private static int columnHeight(ServerLevel level, BlockPos top) {
        int height = 0;
        while (level.getBlockState(top.below(height)).is(Blocks.LADDER)) {
            height++;
        }
        return height;
    }

    private static void cutLadder(ServerPlayer player) {
        List<BlockPos> tops = ladderTops(player);
        if (tops.isEmpty()) {
            return;
        }
        BlockPos top = tops.get(player.getRandom().nextInt(tops.size()));
        List<BlockPos> upper = new ArrayList<>();
        for (int i = 0; i < columnHeight(player.level(), top) / 2; i++) {
            upper.add(top.below(i));
        }
        Swaps.show(
                player,
                upper,
                Blocks.AIR.defaultBlockState(),
                Config.event("ladder_stub").integer("ticks"));
    }

    // 291
    private static void addDoor(ServerPlayer player) {
        Place.Wall wall = Place.wall(player);
        if (wall == null) {
            return;
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (wall.low().relative(d).equals(wall.behind())) {
                BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, d);
                Swaps.show(
                        player,
                        Map.of(wall.low(), door, wall.high(), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER)),
                        Config.event("second_door").integer("ticks"));
                return;
            }
        }
    }

    // 292
    private static boolean unripe(BlockState state) {
        return state.getBlock() instanceof CropBlock crop && !crop.isMaxAge(state);
    }

    // 293
    private static boolean surfaceWater(BlockState state) {
        return state.is(Blocks.WATER) && state.getFluidState().isSource();
    }

    // 294: lava more than trigger_distance away, nearest first; none when any lava is already close
    private static List<BlockPos> farLava(ServerPlayer player) {
        Config.Section config = Config.event("lava_as_water");
        double near = config.num("trigger_distance");
        Vec3 at = player.position();
        List<BlockPos> lava = Nearby.all(player, "lava_as_water", s -> s.is(Blocks.LAVA));
        if (lava.size() < config.integer("min_blocks")
                || lava.stream().anyMatch(pos -> at.distanceToSqr(Vec3.atCenterOf(pos)) <= (near + 1) * (near + 1))) {
            return List.of();
        }
        return nearest(player, lava, "lava_as_water");
    }

    private static void lavaAsWater(ServerPlayer player) {
        Config.Section config = Config.event("lava_as_water");
        ServerLevel level = player.level();
        List<BlockPos> pool = farLava(player);
        if (pool.isEmpty()) {
            return;
        }
        for (BlockPos pos : pool) {
            player.connection.send(new ClientboundBlockUpdatePacket(
                    pos, Blocks.WATER.defaultBlockState().withPropertiesOf(level.getBlockState(pos))));
        }
        Nearby.sound(player, SoundEvents.LAVA_EXTINGUISH, Vec3.atCenterOf(pool.get(0)), config.tone("sound"));
        double near = config.num("trigger_distance");
        int interval = config.integer("interval_ticks");
        Scheduler.repeat(player, interval, interval, (p, n) -> {
            boolean close = p.level() == level
                    && pool.stream().anyMatch(pos -> p.position().distanceToSqr(Vec3.atCenterOf(pos)) <= near * near);
            boolean over = close || p.level() != level || n * interval >= config.integer("max_ticks");
            if (over && p.level() == level) {
                pool.forEach(pos -> p.connection.send(new ClientboundBlockUpdatePacket(level, pos)));
            }
            return !over;
        });
    }

    // 295: the top of each column within the radius that is a roof of planks or the listed blocks, above the player
    private static List<BlockPos> roofs(ServerPlayer player) {
        Config.Section config = Config.event("roof_snow");
        ServerLevel level = player.level();
        List<String> kinds = config.texts("roofs");
        int radius = config.integer("radius");
        int lowest = player.getBlockY() + config.integer("min_height");
        List<BlockPos> found = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = player.getBlockX() + dx;
                int z = player.getBlockZ() + dz;
                if (!level.isLoaded(new BlockPos(x, player.getBlockY(), z))) {
                    continue;
                }
                BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
                BlockState state = level.getBlockState(top);
                if (top.getY() >= lowest
                        && (state.is(BlockTags.PLANKS)
                                || kinds.contains(BuiltInRegistries.BLOCK
                                        .getKey(state.getBlock())
                                        .toString()))
                        && level.isEmptyBlock(top.above())) {
                    found.add(top.above());
                }
            }
        }
        return found;
    }
}
