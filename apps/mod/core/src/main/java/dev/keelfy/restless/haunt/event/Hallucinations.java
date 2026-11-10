package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * More mobs that only one player's client is told of, like {@code hallucination} in {@link Illusions}: each is a
 * {@link PacketEntity} with an id no server entity has, so nothing is on the server and nothing needs reverting. A few
 * are dressed (a baby, a sitting pose, a name that turns a mob upside down) by filling the entity data of a model
 * entity that is never added to the world and sending that to the client.
 */
final class Hallucinations {
    private Hallucinations() {}

    static List<HauntEvent> all() {
        return List.of(
                // 392: a creeper right behind the player, gone a few ticks after they turn and see it
                SimpleEvent.light(
                        "creeper_behind",
                        EventTag.MOBS,
                        player -> creeperSpot(player) != null,
                        Hallucinations::creeperBehind),
                // 394: a drowned rises out of a pond, then sinks again
                SimpleEvent.light(
                        "drowned_rises",
                        EventTag.WATER,
                        player -> !ponds(player).isEmpty(),
                        Hallucinations::drownedRises),
                // 395: a riderless horse gallops past along a line
                SimpleEvent.light(
                        "ghost_horse",
                        EventTag.MOBS,
                        player -> lane(player, "ghost_horse", false) != null,
                        player -> run(player, "ghost_horse", EntityTypes.HORSE, null)),
                // 396: a cat sits on a bed, facing the wall
                SimpleEvent.light(
                        "bed_cat", EventTag.MOBS, player -> !beds(player).isEmpty(), Hallucinations::bedCat),
                // 397: a villager child runs through the house
                SimpleEvent.light(
                        "child_runs",
                        EventTag.MOBS,
                        player -> !player.level().canSeeSky(player.blockPosition())
                                && lane(player, "child_runs", false) != null,
                        player -> run(
                                player, "child_runs", EntityTypes.VILLAGER, mob -> ((AgeableMob) mob).setBaby(true))),
                // 398: an enderman stands in a doorway, gone with the teleport sound when looked at
                SimpleEvent.light(
                        "doorway_enderman",
                        EventTag.MOBS,
                        player -> !doorways(player).isEmpty(),
                        Hallucinations::doorwayEnderman),
                // 399: wolves watch from a forest, all facing the player
                SimpleEvent.light(
                        "forest_wolves",
                        EventTag.MOBS,
                        player -> inForest(player) && pack(player).size() >= minWolves(),
                        Hallucinations::forestWolves),
                // 400: a spider upside down under the ceiling of a room
                SimpleEvent.light(
                        "ceiling_spider",
                        EventTag.MOBS,
                        player -> ceiling(player) != null,
                        Hallucinations::ceilingSpider),
                // 401: bats fly through the player, wall or no wall
                SimpleEvent.light("bat_swarm", EventTag.MOBS, SimpleEvent.ALWAYS, Hallucinations::batSwarm),
                // 402: a zombie outside a window, facing in
                SimpleEvent.light(
                        "window_zombie",
                        EventTag.MOBS,
                        player -> !windows(player).isEmpty(),
                        Hallucinations::windowZombie),
                // 403: a ghast drifts high over a forest
                SimpleEvent.light(
                        "ghast_drift",
                        EventTag.PRESENCE,
                        player -> player.level().dimension() == Level.OVERWORLD
                                && player.level().canSeeSky(player.blockPosition())
                                && inForest(player),
                        Hallucinations::ghastDrift));
    }

    // 392

    private static @Nullable BlockPos creeperSpot(ServerPlayer player) {
        Config.Section config = Config.event("creeper_behind");
        return Place.standingAhead(player, config.num("distance"), config.num("min_angle"), config.num("max_angle"));
    }

    private static void creeperBehind(ServerPlayer player) {
        BlockPos spot = creeperSpot(player);
        if (spot != null) {
            Vec3 at = Vec3.atBottomCenterOf(spot);
            PacketEntity creeper = show(player, EntityTypes.CREEPER, at, null);
            linger(player, "creeper_behind", List.of(creeper), List.of(at), null);
        }
    }

    // 394

    /** Source water at least three deep with air above it, not too near. */
    private static List<BlockPos> ponds(ServerPlayer player) {
        ServerLevel level = player.level();
        double min = Config.event("drowned_rises").num("min_distance");
        return Nearby.all(
                        player,
                        "drowned_rises",
                        state -> state.getFluidState().isSource()
                                && state.getFluidState().is(FluidTags.WATER))
                .stream()
                .filter(pos -> level.getBlockState(pos.above()).isAir()
                        && water(level, pos.below())
                        && water(level, pos.below(2))
                        && water(level, pos.below(3))
                        && player.position().distanceToSqr(Vec3.atCenterOf(pos)) >= min * min)
                .toList();
    }

    private static boolean water(ServerLevel level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER);
    }

    private static void drownedRises(ServerPlayer player) {
        Config.Section config = Config.event("drowned_rises");
        List<BlockPos> found = ponds(player);
        if (found.isEmpty()) {
            return;
        }
        BlockPos pond = found.get(player.getRandom().nextInt(found.size()));
        Vec3 surface = Vec3.atBottomCenterOf(pond);
        double low = config.num("low");
        double high = config.num("high");
        PacketEntity drowned = show(player, EntityTypes.DROWNED, surface.add(0, low, 0), null);
        float yaw = PacketEntity.yawToward(surface, player.position());
        // Up and down again in one go: 0 at both ends, 1 half way.
        travel(
                player,
                drowned,
                config.integer("ticks"),
                yaw,
                t -> surface.add(0, Mth.lerp(1.0 - Math.abs(2 * t - 1), low, high), 0));
    }

    // 395, 397

    private record Lane(Vec3 from, Vec3 to) {}

    /** A line across the ground ahead, from one side of where the player looks to the other. */
    private static @Nullable Lane lane(ServerPlayer player, String id, boolean flip) {
        Config.Section config = Config.event(id);
        double angle = flip ? -config.num("angle") : config.num("angle");
        Vec3 from = groundAt(player, config, angle);
        Vec3 to = groundAt(player, config, -angle);
        return from == null || to == null ? null : new Lane(from, to);
    }

    private static @Nullable Vec3 groundAt(ServerPlayer player, Config.Section config, double angle) {
        Vec3 at = Nearby.around(player, config.num("distance"), angle);
        Integer y = Walk.ground(
                player.level(),
                at.x,
                at.z,
                player.getBlockY() + config.integer("climb"),
                config.integer("climb") + config.integer("drop"));
        return y == null ? null : new Vec3(at.x, y, at.z);
    }

    /** The mob crosses the lane in {@code ticks}, on the ground as it rises and falls. */
    private static void run(ServerPlayer player, String id, EntityType<?> type, @Nullable Consumer<Entity> dress) {
        Lane lane = lane(player, id, player.getRandom().nextBoolean());
        if (lane == null) {
            return;
        }
        Config.Section config = Config.event(id);
        ServerLevel level = player.level();
        float yaw = PacketEntity.yawToward(lane.from(), lane.to());
        PacketEntity mob = show(player, type, lane.from(), dress, yaw);
        int climb = config.integer("climb");
        int depth = climb + config.integer("drop");
        travel(player, mob, config.integer("ticks"), yaw, t -> {
            Vec3 at = lane.from().lerp(lane.to(), t);
            Integer y = Walk.ground(level, at.x, at.z, Mth.floor(at.y) + climb, depth);
            return y == null ? at : new Vec3(at.x, y, at.z);
        });
    }

    // 396

    /** The heads of beds, not too near. */
    private static List<BlockPos> beds(ServerPlayer player) {
        double min = Config.event("bed_cat").num("min_distance");
        return Nearby.all(
                        player,
                        "bed_cat",
                        state -> state.is(BlockTags.BEDS)
                                && state.hasProperty(BlockStateProperties.BED_PART)
                                && state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD)
                .stream()
                .filter(pos -> player.position().distanceToSqr(Vec3.atCenterOf(pos)) >= min * min)
                .toList();
    }

    private static void bedCat(ServerPlayer player) {
        List<BlockPos> found = beds(player);
        if (found.isEmpty()) {
            return;
        }
        BlockPos bed = found.get(player.getRandom().nextInt(found.size()));
        // The head of a bed faces the way to its headboard, which is against the wall.
        float yaw = player.level()
                .getBlockState(bed)
                .getValue(BlockStateProperties.HORIZONTAL_FACING)
                .toYRot();
        Vec3 at = Vec3.atBottomCenterOf(bed).add(0, Config.event("bed_cat").num("height"), 0);
        PacketEntity cat = show(player, EntityTypes.CAT, at, mob -> ((TamableAnimal) mob).setInSittingPose(true), yaw);
        linger(player, "bed_cat", List.of(cat), List.of(at), null);
    }

    // 398

    private static List<BlockPos> doorways(ServerPlayer player) {
        double min = Config.event("doorway_enderman").num("min_distance");
        return Nearby.all(
                        player,
                        "doorway_enderman",
                        state -> state.is(BlockTags.DOORS)
                                && state.hasProperty(DoorBlock.HALF)
                                && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER)
                .stream()
                .filter(pos -> player.position().distanceToSqr(Vec3.atCenterOf(pos)) >= min * min)
                .toList();
    }

    private static void doorwayEnderman(ServerPlayer player) {
        List<BlockPos> found = doorways(player);
        if (found.isEmpty()) {
            return;
        }
        Vec3 at = Vec3.atBottomCenterOf(found.get(player.getRandom().nextInt(found.size())));
        PacketEntity enderman = show(player, EntityTypes.ENDERMAN, at, null);
        linger(player, "doorway_enderman", List.of(enderman), List.of(at), SoundEvents.ENDERMAN_TELEPORT);
    }

    // 399

    private static boolean inForest(ServerPlayer player) {
        return player.level().getBiome(player.blockPosition()).is(BiomeTags.IS_FOREST);
    }

    private static int minWolves() {
        return Config.event("forest_wolves").integer("min_wolves");
    }

    private static List<Vec3> pack(ServerPlayer player) {
        Config.Section config = Config.event("forest_wolves");
        Set<BlockPos> spots = new LinkedHashSet<>();
        for (int i = 0; i < config.integer("wolves"); i++) {
            BlockPos spot = Place.standingAhead(
                    player,
                    config.num("distance") + player.getRandom().nextDouble() * config.num("spread"),
                    0.0,
                    config.num("max_angle"));
            if (spot != null) {
                spots.add(spot);
            }
        }
        return spots.stream().map(Vec3::atBottomCenterOf).toList();
    }

    private static void forestWolves(ServerPlayer player) {
        List<Vec3> pack = pack(player);
        if (pack.size() >= minWolves()) {
            linger(
                    player,
                    "forest_wolves",
                    pack.stream()
                            .map(at -> show(player, EntityTypes.WOLF, at, null))
                            .toList(),
                    pack,
                    null);
        }
    }

    // 400

    /** Where a spider hangs under the ceiling a few blocks above some floor near the player. */
    private static @Nullable Vec3 ceiling(ServerPlayer player) {
        Config.Section config = Config.event("ceiling_spider");
        ServerLevel level = player.level();
        BlockPos floor = Place.spawnSpot(player, config.integer("min_distance"), config.integer("max_distance"));
        if (floor == null) {
            return null;
        }
        for (int dy = 2; dy <= config.integer("max_height"); dy++) {
            BlockPos pos = floor.above(dy);
            if (Place.solid(level, pos)) {
                return new Vec3(floor.getX() + 0.5, pos.getY() - EntityTypes.SPIDER.getHeight(), floor.getZ() + 0.5);
            }
            if (!Place.open(level, pos)) {
                return null;
            }
        }
        return null;
    }

    /** Upside down by the name vanilla flips mobs for; the name itself is not shown. */
    private static void ceilingSpider(ServerPlayer player) {
        Vec3 at = ceiling(player);
        if (at != null) {
            String name = Config.event("ceiling_spider").text("name");
            PacketEntity spider =
                    show(player, EntityTypes.SPIDER, at, mob -> mob.setCustomName(Component.literal(name)));
            linger(player, "ceiling_spider", List.of(spider), List.of(at), null);
        }
    }

    // 401

    /** A few bats along a line through the player at a random bearing, a little apart, all in the same time. */
    private static void batSwarm(ServerPlayer player) {
        Config.Section config = Config.event("bat_swarm");
        RandomSource random = player.getRandom();
        double bearing = random.nextDouble() * Math.PI * 2;
        Vec3 along = new Vec3(Math.cos(bearing), 0, Math.sin(bearing));
        Vec3 across = new Vec3(-along.z, 0, along.x);
        Vec3 centre = player.position().add(0, config.num("height"), 0);
        double reach = config.num("length") / 2;
        for (int i = 0; i < config.integer("bats"); i++) {
            double lag = random.nextDouble() * config.num("lag");
            Vec3 shift = across.scale((random.nextDouble() * 2 - 1) * config.num("spread"))
                    .add(0, (random.nextDouble() * 2 - 1) * config.num("spread"), 0);
            Vec3 from = centre.add(shift).subtract(along.scale(reach + lag));
            Vec3 to = centre.add(shift).add(along.scale(reach - lag));
            float yaw = PacketEntity.yawToward(from, to);
            travel(
                    player,
                    show(player, EntityTypes.BAT, from, null, yaw),
                    config.integer("ticks"),
                    yaw,
                    t -> from.lerp(to, t));
        }
    }

    // 402

    /** Standing room just outside a pane, below its top, with the player on the other side of it. */
    private static List<Vec3> windows(ServerPlayer player) {
        Config.Section config = Config.event("window_zombie");
        ServerLevel level = player.level();
        double min = config.num("min_distance");
        List<Vec3> found = new ArrayList<>();
        for (BlockPos pane : Nearby.all(player, "window_zombie", Place::glass)) {
            if (player.position().distanceToSqr(Vec3.atCenterOf(pane)) < min * min) {
                continue;
            }
            for (Direction out : Direction.Plane.HORIZONTAL) {
                BlockPos outside = pane.relative(out);
                BlockPos inside = pane.relative(out.getOpposite());
                if (!Place.open(level, inside)
                        || !Place.open(level, outside)
                        || player.position().distanceToSqr(Vec3.atCenterOf(inside))
                                >= player.position().distanceToSqr(Vec3.atCenterOf(outside))) {
                    continue;
                }
                BlockPos stand = Place.standing(level, outside);
                if (stand != null && stand.getY() <= pane.getY() && stand.getY() >= pane.getY() - 2) {
                    found.add(Vec3.atBottomCenterOf(stand));
                }
            }
        }
        return found;
    }

    private static void windowZombie(ServerPlayer player) {
        List<Vec3> found = windows(player);
        if (!found.isEmpty()) {
            Vec3 at = found.get(player.getRandom().nextInt(found.size()));
            linger(player, "window_zombie", List.of(show(player, EntityTypes.ZOMBIE, at, null)), List.of(at), null);
        }
    }

    // 403

    /** A straight, slow line high over the player at a random bearing, a little to one side. */
    private static void ghastDrift(ServerPlayer player) {
        Config.Section config = Config.event("ghast_drift");
        double bearing = player.getRandom().nextDouble() * Math.PI * 2;
        Vec3 along = new Vec3(Math.cos(bearing), 0, Math.sin(bearing));
        Vec3 side =
                new Vec3(-along.z, 0, along.x).scale((player.getRandom().nextDouble() * 2 - 1) * config.num("side"));
        Vec3 over = player.position().add(side).add(0, config.num("height"), 0);
        Vec3 from = over.subtract(along.scale(config.num("length") / 2));
        Vec3 to = over.add(along.scale(config.num("length") / 2));
        float yaw = PacketEntity.yawToward(from, to);
        PacketEntity ghast = show(player, EntityTypes.GHAST, from, null, yaw);
        sound(player, SoundEvents.GHAST_AMBIENT, over, config.tone("sound"));
        travel(player, ghast, config.integer("ticks"), yaw, t -> from.lerp(to, t));
    }

    // shared

    /** Shown at {@code at}, facing the player. */
    private static PacketEntity show(
            ServerPlayer player, EntityType<?> type, Vec3 at, @Nullable Consumer<Entity> dress) {
        return show(player, type, at, dress, PacketEntity.yawToward(at, player.position()));
    }

    /**
     * Shown at {@code at}; {@code dress} edits a model entity of the same type, and what it changes in the entity data
     * is sent for the shown one.
     */
    private static PacketEntity show(
            ServerPlayer player, EntityType<?> type, Vec3 at, @Nullable Consumer<Entity> dress, float yaw) {
        PacketEntity mob = PacketEntity.show(player, type, at, yaw);
        if (dress != null) {
            Entity model = type.create(player.level(), EntitySpawnReason.EVENT);
            if (model != null) {
                dress.accept(model);
                List<SynchedEntityData.DataValue<?>> values =
                        model.getEntityData().getNonDefaultValues();
                if (values != null && !values.isEmpty()) {
                    player.connection.send(new ClientboundSetEntityDataPacket(mob.id(), values));
                }
            }
        }
        return mob;
    }

    /**
     * Keeps the mobs where they are until {@code max_ticks} are up, the player comes within {@code vanish_distance} of
     * one, or (with {@code look_angle}) they have been in view for {@code look_ticks} checks, when {@code vanish} is
     * played to them. Checked every {@code interval_ticks}.
     */
    private static void linger(
            ServerPlayer player, String id, List<PacketEntity> mobs, List<Vec3> at, @Nullable SoundEvent vanish) {
        Config.Section config = Config.event(id);
        ServerLevel level = player.level();
        int interval = config.integer("interval_ticks");
        double near = config.has("vanish_distance") ? config.num("vanish_distance") : 0.0;
        double look = config.has("look_angle") ? config.num("look_angle") : 0.0;
        int[] seen = {0};
        Scheduler.repeat(player, interval, interval, (p, n) -> {
            boolean looked = look > 0.0
                    && looking(p, at.get(0).add(0, 1.0, 0), look)
                    && ++seen[0] >= config.integer("look_ticks");
            boolean over = looked
                    || p.level() != level
                    || (n + 1) * interval >= config.integer("max_ticks")
                    || (near > 0.0 && at.stream().anyMatch(spot -> p.position().distanceToSqr(spot) <= near * near));
            if (over) {
                mobs.forEach(mob -> mob.remove(p));
                if (looked && vanish != null) {
                    sound(p, vanish, at.get(0), config.tone("sound"));
                }
            }
            return !over;
        });
    }

    /** Whether {@code at} is within {@code degrees} of where the player looks. */
    private static boolean looking(ServerPlayer player, Vec3 at, double degrees) {
        Vec3 to = at.subtract(player.getEyePosition()).normalize();
        return player.getLookAngle().dot(to) >= Math.cos(Math.toRadians(degrees));
    }

    /**
     * Moves the mob along {@code path}, called with how far along it is in (0, 1], for {@code ticks}, then removes it.
     */
    private static void travel(
            ServerPlayer player, PacketEntity mob, int ticks, float yaw, Function<Double, Vec3> path) {
        ServerLevel level = player.level();
        Scheduler.repeat(player, 1, 1, (p, n) -> {
            if (n >= ticks || p.level() != level) {
                mob.remove(p);
                return false;
            }
            mob.move(p, path.apply((n + 1) / (double) ticks), yaw);
            return true;
        });
    }

    private static void sound(ServerPlayer player, SoundEvent sound, Vec3 at, Config.Tone tone) {
        player.connection.send(new ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                SoundSource.HOSTILE,
                at.x,
                at.y,
                at.z,
                tone.volume(),
                tone.pitch(),
                player.getRandom().nextLong()));
    }
}
