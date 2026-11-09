package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** What the biomes do when the player is in them: the jungle, the pale garden, the dark forest, the deep dark. */
final class Habitats {
    /** Forests with nothing odd about them. */
    @SuppressWarnings("unchecked")
    private static final ResourceKey<Biome>[] PLAIN_FORESTS =
            new ResourceKey[] {Biomes.FOREST, Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST
            };

    /** The forests that creaking hearts and resin come from, and the plain ones. */
    @SuppressWarnings("unchecked")
    private static final ResourceKey<Biome>[] ALL_FORESTS = new ResourceKey[] {
        Biomes.FOREST,
        Biomes.FLOWER_FOREST,
        Biomes.BIRCH_FOREST,
        Biomes.OLD_GROWTH_BIRCH_FOREST,
        Biomes.DARK_FOREST,
        Biomes.PALE_GARDEN
    };

    private Habitats() {}

    static List<HauntEvent> all() {
        return List.of(
                // 480: out at sea, something holds the boat still under the player and drowned groan below it
                SimpleEvent.harmful(
                        "boat_grabbed",
                        EventTag.WATER,
                        player -> Where.biome(player, BiomeTags.IS_OCEAN) && floatingBoat(player) != null,
                        Habitats::grabBoat),
                // 481: in the jungle every parrot nearby that has no owner takes wing and flies away
                SimpleEvent.light(
                        "parrots_scatter",
                        EventTag.MOBS,
                        player -> Where.biome(player, BiomeTags.IS_JUNGLE)
                                && !wildParrots(player).isEmpty(),
                        Habitats::scatterParrots),
                // 482: in the jungle vines grow on the air beside the glass of a window; they go again unless hardcore
                SimpleEvent.light(
                        "window_vines",
                        EventTag.BLOCKS,
                        player -> (Where.biome(player, BiomeTags.IS_JUNGLE)
                                        || Where.biome(player, Biomes.SWAMP, Biomes.MANGROVE_SWAMP))
                                && !vineSpots(player).isEmpty(),
                        Habitats::growVines),
                // 483: on the mushroom fields the red mooshrooms turn brown, then red again unless hardcore
                SimpleEvent.light(
                        "mooshroom_brown",
                        EventTag.MOBS,
                        player -> Where.biome(player, Biomes.MUSHROOM_FIELDS)
                                && !redMooshrooms(player).isEmpty(),
                        Habitats::browning),
                // 484: in the pale garden closed eyeblossoms behind the player's back are seen open when they turn
                SimpleEvent.light(
                        "eyeblossoms_open",
                        EventTag.BLOCKS,
                        player -> !closedEyes(player).isEmpty(),
                        Habitats::openEyes),
                // 485: in an ordinary forest a creaking heart sounds from the trees
                SimpleEvent.light(
                        "creaking_heart_call",
                        EventTag.SOUND,
                        player -> Where.biome(player, PLAIN_FORESTS),
                        Habitats::callHeart),
                // 486: in the forests resin clumps grow on the wall of a house; they go again unless hardcore
                SimpleEvent.light(
                        "resin_clumps",
                        EventTag.BLOCKS,
                        player -> Where.biome(player, ALL_FORESTS)
                                && !resinSpots(player).isEmpty(),
                        Habitats::growResin),
                // 487: in the cherry grove the petals that fall are black
                SimpleEvent.light(
                        "black_petals",
                        EventTag.PRESENCE,
                        player -> Where.biome(player, Biomes.CHERRY_GROVE),
                        Habitats::fallBlack),
                // 488: in the dark forest the gaps in the canopy overhead are shown filled with leaves
                SimpleEvent.light(
                        "canopy_closes",
                        EventTag.BLOCKS,
                        player -> Where.biome(player, Biomes.DARK_FOREST, Biomes.PALE_GARDEN)
                                && gaps(player).size()
                                        >= Config.event("canopy_closes").integer("min_blocks"),
                        Habitats::closeCanopy),
                // 489: in the deep dark sculk spreads over the floor toward the player, a block at a time
                SimpleEvent.light(
                        "sculk_creep",
                        EventTag.BLOCKS,
                        player -> Where.biome(player, Biomes.DEEP_DARK)
                                && !sculkStarts(player).isEmpty(),
                        Habitats::creepSculk),
                // 490: in the lush caves the glow berries go dark one after another, then glow again unless hardcore
                SimpleEvent.light(
                        "berries_dark",
                        EventTag.LIGHTS,
                        player -> Nearby.any(player, "berries_dark", Habitats::berries),
                        Habitats::darkenBerries));
    }

    private static List<BlockPos> nearest(ServerPlayer player, List<BlockPos> found, int limit) {
        return found.stream()
                .sorted(Comparator.comparingDouble(pos -> pos.distSqr(player.blockPosition())))
                .limit(limit)
                .toList();
    }

    private static <T extends Entity> List<T> near(
            ServerPlayer player, Class<T> type, String event, Predicate<T> match) {
        return player.level()
                .getEntitiesOfClass(type, new AABB(player.blockPosition()).inflate(Nearby.radius(event)), match);
    }

    // 480

    private static @Nullable AbstractBoat floatingBoat(ServerPlayer player) {
        return player.getVehicle() instanceof AbstractBoat boat
                        && boat.level().getFluidState(boat.blockPosition()).is(FluidTags.WATER)
                ? boat
                : null;
    }

    /** The boat is stopped dead every tick while the drowned groan under it, and let go when it is empty. */
    private static void grabBoat(ServerPlayer player) {
        Config.Section config = Config.event("boat_grabbed");
        int interval = config.integer("sound_interval_ticks");
        Scheduler.repeat(player, 0, 1, (p, n) -> {
            if (!(p.getVehicle() instanceof AbstractBoat boat) || n >= config.integer("hold_ticks")) {
                return false;
            }
            boat.setDeltaMovement(Vec3.ZERO);
            p.connection.send(new ClientboundSetEntityMotionPacket(boat));
            if (n % interval == 0) {
                Nearby.sound(
                        p,
                        n / interval % 2 == 0 ? SoundEvents.DROWNED_AMBIENT_WATER : SoundEvents.DROWNED_HURT_WATER,
                        boat.position().add(0.0, -config.num("sound_depth"), 0.0),
                        config.tone("sound"));
            }
            return true;
        });
    }

    // 481

    private static List<Parrot> wildParrots(ServerPlayer player) {
        return near(player, Parrot.class, "parrots_scatter", parrot -> parrot.isAlive() && !parrot.isTame());
    }

    /** Each parrot is thrown up and sent a long way from the player, over and over until the time is up. */
    private static void scatterParrots(ServerPlayer player) {
        Config.Section config = Config.event("parrots_scatter");
        List<Parrot> parrots = wildParrots(player);
        Nearby.sound(player, SoundEvents.PARROT_FLY, parrots.getFirst().position(), config.tone("sound"));
        for (Parrot parrot : parrots) {
            if (parrot.onGround()) {
                parrot.setDeltaMovement(0.0, config.num("lift"), 0.0);
            }
        }
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            for (Parrot parrot : parrots) {
                if (!parrot.isAlive()) {
                    continue;
                }
                Vec3 away = parrot.position().subtract(p.position()).multiply(1.0, 0.0, 1.0);
                away = away.lengthSqr() < 1.0E-4
                        ? new Vec3(Math.cos(parrot.getId()), 0.0, Math.sin(parrot.getId()))
                        : away.normalize();
                Vec3 to = parrot.position()
                        .add(away.scale(config.num("distance")))
                        .add(0.0, config.num("rise"), 0.0);
                parrot.getNavigation().moveTo(to.x, to.y, to.z, config.num("speed"));
            }
            return (n + 1) * config.integer("interval_ticks") < config.integer("fly_ticks");
        });
    }

    // 482

    /** Air beside a window, with the way to the glass: where a vine would hang. */
    private static Map<BlockPos, Direction> vineSpots(ServerPlayer player) {
        ServerLevel level = player.level();
        Map<BlockPos, Direction> spots = new LinkedHashMap<>();
        for (BlockPos glass : Nearby.all(player, "window_vines", Place::glass)) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos cell = glass.relative(d);
                if (level.isEmptyBlock(cell)) {
                    spots.putIfAbsent(cell, d.getOpposite());
                }
            }
        }
        return spots;
    }

    private static void growVines(ServerPlayer player) {
        Config.Section config = Config.event("window_vines");
        ServerLevel level = player.level();
        Map<BlockPos, Direction> spots = vineSpots(player);
        for (BlockPos cell : nearest(player, List.copyOf(spots.keySet()), config.integer("max_blocks"))) {
            Restore.swap(
                    level,
                    cell,
                    Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(spots.get(cell)), true),
                    config.integer("ticks"));
        }
    }

    // 483

    private static List<MushroomCow> redMooshrooms(ServerPlayer player) {
        return near(
                player,
                MushroomCow.class,
                "mooshroom_brown",
                cow -> cow.isAlive() && cow.getVariant() == MushroomCow.Variant.RED);
    }

    private static void browning(ServerPlayer player) {
        Config.Section config = Config.event("mooshroom_brown");
        List<MushroomCow> cows = redMooshrooms(player);
        cows = cows.subList(0, Math.min(cows.size(), config.integer("max_animals")));
        cows.forEach(cow -> cow.setComponent(DataComponents.MOOSHROOM_VARIANT, MushroomCow.Variant.BROWN));
        List<MushroomCow> browned = cows;
        Restore.revert(player.level(), config.integer("ticks"), force -> {
            browned.forEach(cow -> {
                if (!cow.isRemoved() && cow.getVariant() == MushroomCow.Variant.BROWN) {
                    cow.setComponent(DataComponents.MOOSHROOM_VARIANT, MushroomCow.Variant.RED);
                }
            });
            return true;
        });
    }

    // 484

    /** Closed eyeblossoms near the player. */
    private static List<BlockPos> closedEyes(ServerPlayer player) {
        return Nearby.all(player, "eyeblossoms_open", state -> state.is(Blocks.CLOSED_EYEBLOSSOM));
    }

    private static void openEyes(ServerPlayer player) {
        Config.Section config = Config.event("eyeblossoms_open");
        List<BlockPos> opened = nearest(player, closedEyes(player), config.integer("max_blocks"));
        Swaps.show(player, opened, Blocks.OPEN_EYEBLOSSOM.defaultBlockState(), config.integer("ticks"));
        Nearby.sound(player, SoundEvents.EYEBLOSSOM_OPEN, Vec3.atCenterOf(opened.getFirst()), config.tone("sound"));
    }

    // 485

    private static void callHeart(ServerPlayer player) {
        Config.Section config = Config.event("creaking_heart_call");
        Vec3 at = Nearby.source(player, config.num("distance"));
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            Nearby.sound(p, SoundEvents.CREAKING_HEART_IDLE, at, config.tone("sound"));
            return n + 1 < config.integer("count");
        });
    }

    // 486

    /** Air beside a wall block of the configured kinds, with the way to the wall: where a clump would stick. */
    private static Map<BlockPos, Direction> resinSpots(ServerPlayer player) {
        ServerLevel level = player.level();
        Set<Block> walls = new HashSet<>();
        for (String id : Config.event("resin_clumps").texts("walls")) {
            walls.add(BuiltInRegistries.BLOCK.getValue(Identifier.parse(id)));
        }
        Map<BlockPos, Direction> spots = new LinkedHashMap<>();
        for (BlockPos wall : Nearby.all(
                player, "resin_clumps", state -> state.is(BlockTags.PLANKS) || walls.contains(state.getBlock()))) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos cell = wall.relative(d);
                if (level.isEmptyBlock(cell)) {
                    spots.putIfAbsent(cell, d.getOpposite());
                }
            }
        }
        return spots;
    }

    /** The clumps grow in one cluster: the spots nearest a random one. */
    private static void growResin(ServerPlayer player) {
        Config.Section config = Config.event("resin_clumps");
        ServerLevel level = player.level();
        Map<BlockPos, Direction> spots = resinSpots(player);
        List<BlockPos> cells = List.copyOf(spots.keySet());
        BlockPos seed = cells.get(player.getRandom().nextInt(cells.size()));
        List<BlockPos> cluster = cells.stream()
                .sorted(Comparator.comparingDouble(pos -> pos.distSqr(seed)))
                .limit(config.integer("max_blocks"))
                .toList();
        for (BlockPos cell : cluster) {
            Restore.swap(
                    level,
                    cell,
                    Blocks.RESIN_CLUMP
                            .defaultBlockState()
                            .setValue(MultifaceBlock.getFaceProperty(spots.get(cell)), true),
                    config.integer("ticks"));
        }
        Nearby.sound(player, SoundEvents.RESIN_PLACE, Vec3.atCenterOf(seed), config.tone("sound"));
    }

    // 487

    /** Ash drifts down around the player like petals do. */
    private static void fallBlack(ServerPlayer player) {
        Config.Section config = Config.event("black_petals");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            p.level()
                    .sendParticles(
                            ParticleTypes.ASH,
                            p.getX(),
                            p.getY() + config.num("height"),
                            p.getZ(),
                            config.integer("count"),
                            config.num("spread"),
                            config.num("depth"),
                            config.num("spread"),
                            0.0);
            return (n + 1) * config.integer("interval_ticks") < config.integer("ticks");
        });
    }

    // 488

    /** Air above the player's head that touches a leaf: a gap in the canopy. */
    private static List<BlockPos> gaps(ServerPlayer player) {
        ServerLevel level = player.level();
        int lowest = player.getBlockY() + Config.event("canopy_closes").integer("min_height");
        Set<BlockPos> found = new LinkedHashSet<>();
        for (BlockPos leaf : Nearby.all(player, "canopy_closes", state -> state.is(BlockTags.LEAVES))) {
            for (Direction d : Direction.values()) {
                BlockPos cell = leaf.relative(d);
                if (cell.getY() >= lowest && level.isEmptyBlock(cell)) {
                    found.add(cell);
                }
            }
        }
        return List.copyOf(found);
    }

    private static void closeCanopy(ServerPlayer player) {
        Config.Section config = Config.event("canopy_closes");
        Swaps.show(
                player,
                nearest(player, gaps(player), config.integer("max_blocks")),
                Blocks.DARK_OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true),
                config.integer("ticks"));
    }

    // 489

    /** Natural ground the sculk can take, with air above it. */
    private static List<BlockPos> sculkGround(ServerPlayer player) {
        ServerLevel level = player.level();
        return Nearby.all(player, "sculk_creep", state -> state.is(BlockTags.SCULK_REPLACEABLE)).stream()
                .filter(pos -> level.isEmptyBlock(pos.above()))
                .toList();
    }

    /** Ground at least {@code min_distance} from the player: where the sculk can start. */
    private static List<BlockPos> sculkStarts(ServerPlayer player) {
        int least = Config.event("sculk_creep").integer("min_distance");
        return sculkGround(player).stream()
                .filter(pos -> pos.distSqr(player.blockPosition()) >= least * least)
                .toList();
    }

    /** A trail from far ground to near, each step a neighbour of the last that is closer to the player. */
    private static void creepSculk(ServerPlayer player) {
        Config.Section config = Config.event("sculk_creep");
        ServerLevel level = player.level();
        List<BlockPos> starts = sculkStarts(player);
        Set<BlockPos> ground = new HashSet<>(sculkGround(player));
        BlockPos here = player.blockPosition();
        int stop = config.integer("stop_distance");
        List<BlockPos> trail =
                new ArrayList<>(List.of(starts.get(player.getRandom().nextInt(starts.size()))));
        while (trail.size() < config.integer("steps") && trail.getLast().distSqr(here) > stop * stop) {
            BlockPos at = trail.getLast();
            List<BlockPos> closer = new ArrayList<>();
            for (BlockPos next : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1))) {
                if (ground.contains(next) && next.distSqr(here) < at.distSqr(here)) {
                    closer.add(next.immutable());
                }
            }
            if (closer.isEmpty()) {
                break;
            }
            trail.add(closer.get(player.getRandom().nextInt(closer.size())));
        }
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            BlockPos pos = trail.get(n);
            if (level.isLoaded(pos) && level.getBlockState(pos).is(BlockTags.SCULK_REPLACEABLE)) {
                Restore.swap(level, pos, Blocks.SCULK.defaultBlockState(), config.integer("ticks"));
                Nearby.sound(p, SoundEvents.SCULK_BLOCK_SPREAD, Vec3.atCenterOf(pos), config.tone("sound"));
            }
            return n + 1 < trail.size();
        });
    }

    // 490

    private static boolean berries(BlockState state) {
        return state.hasProperty(CaveVines.BERRIES) && state.getValue(CaveVines.BERRIES);
    }

    /** The berries nearest the player go dark first. */
    private static void darkenBerries(ServerPlayer player) {
        Config.Section config = Config.event("berries_dark");
        ServerLevel level = player.level();
        List<BlockPos> found =
                nearest(player, Nearby.all(player, "berries_dark", Habitats::berries), config.integer("max_blocks"));
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            BlockPos pos = found.get(n);
            BlockState state = level.getBlockState(pos);
            if (level.isLoaded(pos) && berries(state)) {
                Restore.swap(level, pos, state.setValue(CaveVines.BERRIES, false), config.integer("ticks"));
            }
            return n + 1 < found.size();
        });
    }
}
