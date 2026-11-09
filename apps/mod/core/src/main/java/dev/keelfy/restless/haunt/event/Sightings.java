package dev.keelfy.restless.haunt.event;

import com.mojang.datafixers.util.Pair;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.DimPayload;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Scheduler;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Things the player's client is told it sees, each a {@link PacketEntity} (an id no server entity has) in a place or a
 * moment that makes it worse: a roof, a window, a flash of lightning, the eyepiece of a spyglass. Nothing is on the
 * server, so nothing needs putting back.
 */
final class Sightings {
    private Sightings() {}

    static List<HauntEvent> all() {
        return List.of(
                // 393: a skeleton on a roof draws its bow, with no arrow in it
                SimpleEvent.light(
                        "roof_skeleton", EventTag.MOBS, player -> !roofs(player).isEmpty(), Sightings::roofSkeleton),
                // 404: a guardian under the water throws its beam at the player
                SimpleEvent.light(
                        "water_guardian",
                        EventTag.WATER,
                        player -> BeamTarget.ID != null && !ponds(player).isEmpty(),
                        Sightings::waterGuardian),
                // 406: a warden far away rises out of the ground and digs back in
                SimpleEvent.light(
                        "far_warden",
                        EventTag.MOBS,
                        player -> Where.overworld(player) && wardenSpot(player) != null,
                        Sightings::farWarden),
                // 407: a witch looks out of a window in a swamp
                SimpleEvent.light(
                        "swamp_witch",
                        EventTag.MOBS,
                        player -> Where.biome(player, Biomes.SWAMP, Biomes.MANGROVE_SWAMP)
                                && !windows(player).isEmpty(),
                        Sightings::swampWitch),
                // 408: a shulker opens its shell on the wall of a room
                SimpleEvent.light(
                        "wall_shulker",
                        EventTag.MOBS,
                        player -> !alcoves(player).isEmpty(),
                        Sightings::wallShulker),
                // 410: an allay flies off with something the player has in the pack
                SimpleEvent.light(
                        "allay_thief", EventTag.ITEMS, player -> !stuff(player).isEmpty(), Sightings::allayThief),
                // 411: a creaking in an ordinary forest comes closer only while nobody looks at it
                SimpleEvent.light(
                        "forest_creaking",
                        EventTag.MOBS,
                        player -> Where.biome(player, BiomeTags.IS_FOREST) && creakingSpot(player) != null,
                        Sightings::forestCreaking),
                // 412: a herd of cows in the field is gone while the view blinks
                SimpleEvent.light(
                        "cow_herd_blink",
                        EventTag.MOBS,
                        player -> herd(player).size() >= minCows(),
                        Sightings::cowHerd),
                // 417: a figure stands far off, but only in the spyglass
                SimpleEvent.light(
                        "spyglass_figure", EventTag.PRESENCE, Sightings::hasSpyglass, Sightings::spyglassFigure),
                // 491: in a storm, each flash of lightning finds a figure standing closer
                SimpleEvent.light(
                        "flash_figure",
                        EventTag.PRESENCE,
                        player -> Where.overworld(player)
                                && player.level().isRaining()
                                && player.level().canSeeSky(player.blockPosition()),
                        Sightings::flashFigure));
    }

    // 393

    /** The top of a stair or a slab with open sky and two free blocks over it, higher than the player. */
    private static List<Vec3> roofs(ServerPlayer player) {
        Config.Section config = Config.event("roof_skeleton");
        ServerLevel level = player.level();
        double min = config.num("min_distance");
        return Nearby.all(player, "roof_skeleton", state -> state.is(BlockTags.STAIRS) || state.is(BlockTags.SLABS))
                .stream()
                .filter(pos -> pos.getY() - player.getBlockY() >= config.integer("min_rise")
                        && level.canSeeSky(pos.above())
                        && Place.open(level, pos.above())
                        && Place.open(level, pos.above(2))
                        && player.position().distanceToSqr(Vec3.atCenterOf(pos)) >= min * min)
                .map(pos -> new Vec3(
                        pos.getX() + 0.5,
                        pos.getY()
                                + level.getBlockState(pos)
                                        .getCollisionShape(level, pos)
                                        .max(Direction.Axis.Y),
                        pos.getZ() + 0.5))
                .toList();
    }

    private static void roofSkeleton(ServerPlayer player) {
        List<Vec3> found = roofs(player);
        if (found.isEmpty()) {
            return;
        }
        Vec3 at = found.get(player.getRandom().nextInt(found.size()));
        PacketEntity skeleton = show(player, EntityTypes.SKELETON, at, yawToPlayer(player, at), mob -> {
            if (mob instanceof Mob m) {
                m.setAggressive(true);
            }
        });
        player.connection.send(new ClientboundSetEquipmentPacket(
                skeleton.id(), List.of(Pair.of(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW)))));
        linger(player, "roof_skeleton", List.of(skeleton), List.of(at), null);
    }

    // 404

    /** The beam's target, which the guardian keeps in a private field. */
    private static final class BeamTarget {
        static final @Nullable EntityDataAccessor<Integer> ID = find();

        @SuppressWarnings("unchecked")
        private static @Nullable EntityDataAccessor<Integer> find() {
            try {
                Field field = Guardian.class.getDeclaredField("DATA_ID_ATTACK_TARGET");
                field.setAccessible(true);
                return (EntityDataAccessor<Integer>) field.get(null);
            } catch (ReflectiveOperationException | RuntimeException e) {
                return null;
            }
        }
    }

    /** Source water at least four deep with air above it, not too near. */
    private static List<BlockPos> ponds(ServerPlayer player) {
        ServerLevel level = player.level();
        double min = Config.event("water_guardian").num("min_distance");
        return Nearby.all(
                        player,
                        "water_guardian",
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

    private static void waterGuardian(ServerPlayer player) {
        EntityDataAccessor<Integer> target = BeamTarget.ID;
        List<BlockPos> found = ponds(player);
        if (target == null || found.isEmpty()) {
            return;
        }
        Config.Section config = Config.event("water_guardian");
        Vec3 at = Vec3.atBottomCenterOf(
                found.get(player.getRandom().nextInt(found.size())).below(config.integer("depth")));
        PacketEntity guardian = show(player, EntityTypes.GUARDIAN, at, yawToPlayer(player, at), null);
        player.connection.send(new ClientboundSetEntityDataPacket(
                guardian.id(), List.of(SynchedEntityData.DataValue.create(target, player.getId()))));
        sound(player, SoundEvents.GUARDIAN_ATTACK, at, config.tone("sound"));
        Scheduler.later(player, config.integer("ticks"), guardian::remove);
    }

    // 406

    private static @Nullable BlockPos wardenSpot(ServerPlayer player) {
        Config.Section config = Config.event("far_warden");
        return Place.spawnSpot(player, config.integer("min_distance"), config.integer("max_distance"));
    }

    /** Up with its emerge animation and sound, still for a while, then down with its dig animation. */
    private static void farWarden(ServerPlayer player) {
        BlockPos spot = wardenSpot(player);
        if (spot == null) {
            return;
        }
        Config.Section config = Config.event("far_warden");
        ServerLevel level = player.level();
        Vec3 at = Vec3.atBottomCenterOf(spot);
        PacketEntity warden =
                show(player, EntityTypes.WARDEN, at, yawToPlayer(player, at), model -> model.setPose(Pose.EMERGING));
        sound(player, SoundEvents.WARDEN_EMERGE, at, config.tone("sound"));
        int stay = config.integer("emerge_ticks") + config.integer("hold_ticks");
        Scheduler.later(player, stay, p -> {
            if (p.level() == level) {
                dress(p, warden, EntityTypes.WARDEN, model -> model.setPose(Pose.DIGGING));
                sound(p, SoundEvents.WARDEN_DIG, at, config.tone("sound"));
            }
        });
        Scheduler.later(player, stay + config.integer("dig_ticks"), warden::remove);
    }

    // 407

    /**
     * Where a witch stands behind a window, as seen from the player's side: a pane or glass block, or a one-block gap
     * over a sill of planks with a wall around it, with standing room inside, one below the gap, and the player nearer
     * the outside.
     */
    private static List<Vec3> windows(ServerPlayer player) {
        Config.Section config = Config.event("swamp_witch");
        ServerLevel level = player.level();
        double min = config.num("min_distance");
        List<Vec3> found = new ArrayList<>();
        for (BlockPos base :
                Nearby.all(player, "swamp_witch", state -> Place.glass(state) || state.is(BlockTags.PLANKS))) {
            boolean glass = Place.glass(level.getBlockState(base));
            BlockPos cell = glass ? base : base.above();
            if (player.position().distanceToSqr(Vec3.atCenterOf(cell)) < min * min
                    || (!glass && !(level.getBlockState(cell).isAir() && Place.solid(level, cell.above())))) {
                continue;
            }
            for (Direction out : Direction.Plane.HORIZONTAL) {
                BlockPos outside = cell.relative(out);
                BlockPos inside = cell.relative(out.getOpposite());
                BlockPos stand = inside.below();
                if (!Place.open(level, outside)
                        || !Place.open(level, inside)
                        || (!glass
                                && !(Place.solid(level, cell.relative(out.getClockWise()))
                                        && Place.solid(level, cell.relative(out.getCounterClockWise()))))
                        || !Place.open(level, stand)
                        || !Place.open(level, stand.above())
                        || !level.getBlockState(stand.below()).isSolidRender()
                        || player.position().distanceToSqr(Vec3.atCenterOf(outside))
                                >= player.position().distanceToSqr(Vec3.atCenterOf(inside))) {
                    continue;
                }
                // A little toward the window, so the head is in it.
                found.add(Vec3.atBottomCenterOf(stand).add(out.getStepX() * 0.3, 0, out.getStepZ() * 0.3));
            }
        }
        return found;
    }

    private static void swampWitch(ServerPlayer player) {
        List<Vec3> found = windows(player);
        if (!found.isEmpty()) {
            Vec3 at = found.get(player.getRandom().nextInt(found.size()));
            PacketEntity witch = show(player, EntityTypes.WITCH, at, yawToPlayer(player, at), null);
            linger(player, "swamp_witch", List.of(witch), List.of(at), null);
        }
    }

    // 408

    /**
     * Shulker with its shell and the face it sits on reachable: the keys are protected, so a subclass hands them out.
     */
    private static final class Peek extends Shulker {
        private Peek(EntityType<? extends Shulker> type, Level level) {
            super(type, level);
        }

        static EntityDataAccessor<Byte> amount() {
            return DATA_PEEK_ID;
        }

        static EntityDataAccessor<Direction> face() {
            return DATA_ATTACH_FACE_ID;
        }
    }

    private record Alcove(Vec3 at, Direction face) {}

    /** Free floored air indoors against a solid wall block, at the player's height or near it, not too near. */
    private static List<Alcove> alcoves(ServerPlayer player) {
        Config.Section config = Config.event("wall_shulker");
        ServerLevel level = player.level();
        double min = config.num("min_distance");
        List<Alcove> found = new ArrayList<>();
        for (BlockPos wall : Nearby.all(player, "wall_shulker", BlockState::isSolidRender)) {
            for (Direction toward : Direction.Plane.HORIZONTAL) {
                BlockPos cell = wall.relative(toward);
                if (Math.abs(cell.getY() - player.getBlockY()) <= config.integer("height")
                        && Place.floored(level, cell)
                        && !level.canSeeSky(cell)
                        && Place.solid(level, wall)
                        && player.position().distanceToSqr(Vec3.atCenterOf(cell)) >= min * min) {
                    found.add(new Alcove(Vec3.atBottomCenterOf(cell), toward.getOpposite()));
                }
            }
        }
        return found;
    }

    private static void wallShulker(ServerPlayer player) {
        List<Alcove> found = alcoves(player);
        if (found.isEmpty()) {
            return;
        }
        Config.Section config = Config.event("wall_shulker");
        Alcove alcove = found.get(player.getRandom().nextInt(found.size()));
        PacketEntity shulker = PacketEntity.show(player, EntityTypes.SHULKER, alcove.at(), 0.0F);
        player.connection.send(new ClientboundSetEntityDataPacket(
                shulker.id(), List.of(SynchedEntityData.DataValue.create(Peek.face(), alcove.face()))));
        ServerLevel level = player.level();
        int open = config.integer("open_after");
        int shut = open + config.integer("open_ticks");
        Scheduler.later(player, open, p -> shell(p, shulker, level, (byte) config.integer("peek"), alcove.at()));
        Scheduler.later(player, shut, p -> shell(p, shulker, level, (byte) 0, alcove.at()));
        Scheduler.later(player, shut + config.integer("leave_ticks"), shulker::remove);
    }

    /** Opens the shell to {@code amount} (0 shuts it) with the matching sound. */
    private static void shell(ServerPlayer player, PacketEntity shulker, ServerLevel level, byte amount, Vec3 at) {
        if (player.level() == level) {
            player.connection.send(new ClientboundSetEntityDataPacket(
                    shulker.id(), List.of(SynchedEntityData.DataValue.create(Peek.amount(), amount))));
            sound(
                    player,
                    amount > 0 ? SoundEvents.SHULKER_OPEN : SoundEvents.SHULKER_CLOSE,
                    at,
                    Config.event("wall_shulker").tone("sound"));
        }
    }

    // 410

    private static List<ItemStack> stuff(ServerPlayer player) {
        List<ItemStack> found = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty()) {
                found.add(stack);
            }
        }
        return found;
    }

    /** An allay shown a little ahead holding a copy of one of the player's stacks, off in a straight line. */
    private static void allayThief(ServerPlayer player) {
        List<ItemStack> found = stuff(player);
        if (found.isEmpty()) {
            return;
        }
        Config.Section config = Config.event("allay_thief");
        ItemStack held = found.get(player.getRandom().nextInt(found.size())).copyWithCount(1);
        double side = (player.getRandom().nextDouble() * 2 - 1) * config.num("angle");
        Vec3 near = Nearby.around(player, config.num("distance"), side);
        Vec3 from = new Vec3(near.x, player.getEyeY(), near.z);
        Vec3 flat = new Vec3(near.x - player.getX(), 0, near.z - player.getZ()).normalize();
        Vec3 to = from.add(flat.scale(config.num("length"))).add(0, config.num("rise"), 0);
        float yaw = PacketEntity.yawToward(from, to);
        PacketEntity allay = PacketEntity.show(player, EntityTypes.ALLAY, from, yaw);
        player.connection.send(
                new ClientboundSetEquipmentPacket(allay.id(), List.of(Pair.of(EquipmentSlot.MAINHAND, held))));
        sound(player, SoundEvents.ALLAY_AMBIENT_WITH_ITEM, from, config.tone("sound"));
        travel(player, allay, config.integer("ticks"), yaw, t -> from.lerp(to, t));
    }

    // 411

    private static @Nullable BlockPos creakingSpot(ServerPlayer player) {
        Config.Section config = Config.event("forest_creaking");
        return Place.spawnSpot(player, config.integer("min_distance"), config.integer("max_distance"));
    }

    /**
     * A creaking, as {@link ShadowEvent} chases, but only in the player's client: it walks to the player while they do
     * not look at it, stands while they do, and is gone on arrival, after a while, or when it cannot go on.
     */
    private static void forestCreaking(ServerPlayer player) {
        BlockPos spot = creakingSpot(player);
        if (spot == null) {
            return;
        }
        Config.Section config = Config.event("forest_creaking");
        ServerLevel level = player.level();
        Vec3[] at = {Vec3.atBottomCenterOf(spot)};
        PacketEntity creaking = PacketEntity.show(player, EntityTypes.CREAKING, at[0], yawToPlayer(player, at[0]));
        int interval = config.integer("interval_ticks");
        int[] stuck = {0};
        Scheduler.repeat(player, interval, interval, (p, n) -> {
            Vec3 toPlayer = p.position().subtract(at[0]);
            double reach = Math.max(Math.hypot(toPlayer.x, toPlayer.z), 1.0E-4);
            boolean over = p.level() != level
                    || n * interval >= config.integer("max_ticks")
                    || stuck[0] >= config.integer("stuck_steps")
                    || reach <= config.num("touch_distance");
            if (over) {
                creaking.remove(p);
                return false;
            }
            if (!watched(p, at[0].add(0, config.num("eye_height"), 0), config.num("watch_degrees"))) {
                double stride = Math.min(config.num("speed"), reach);
                double x = at[0].x + toPlayer.x / reach * stride;
                double z = at[0].z + toPlayer.z / reach * stride;
                int climb = config.integer("climb");
                Integer y = Walk.ground(level, x, z, Mth.floor(at[0].y) + climb, climb + config.integer("drop"));
                if (y == null) {
                    stuck[0]++;
                } else {
                    stuck[0] = 0;
                    at[0] = new Vec3(x, y, z);
                    creaking.move(p, at[0], yawToPlayer(p, at[0]));
                }
            }
            return true;
        });
    }

    // 412

    private static int minCows() {
        return Config.event("cow_herd_blink").integer("min_cows");
    }

    /** Standing room on grass under open sky, ahead, scattered a little. */
    private static List<Vec3> herd(ServerPlayer player) {
        Config.Section config = Config.event("cow_herd_blink");
        ServerLevel level = player.level();
        Set<BlockPos> spots = new LinkedHashSet<>();
        for (int i = 0; i < config.integer("cows"); i++) {
            BlockPos spot = Place.standingAhead(
                    player,
                    config.num("distance") + player.getRandom().nextDouble() * config.num("spread"),
                    0.0,
                    config.num("max_angle"),
                    pos -> level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK) && level.canSeeSky(pos));
            if (spot != null) {
                spots.add(spot);
            }
        }
        return spots.stream().map(Vec3::atBottomCenterOf).toList();
    }

    /**
     * The herd stands until the player has looked at it for {@code look_ticks} checks; then the view blinks and the
     * herd is gone at the darkest point. Without a look it is gone after {@code max_ticks}, or when they come near.
     */
    private static void cowHerd(ServerPlayer player) {
        List<Vec3> spots = herd(player);
        if (spots.size() < minCows()) {
            return;
        }
        Config.Section config = Config.event("cow_herd_blink");
        ServerLevel level = player.level();
        List<PacketEntity> cows = spots.stream()
                .map(at -> PacketEntity.show(
                        player, EntityTypes.COW, at, player.getRandom().nextFloat() * 360.0F))
                .toList();
        int interval = config.integer("interval_ticks");
        int[] seen = {0};
        double near = config.num("vanish_distance");
        Scheduler.repeat(player, interval, interval, (p, n) -> {
            boolean looked = watched(p, spots.get(0).add(0, 1.0, 0), config.num("look_angle"))
                    && ++seen[0] >= config.integer("look_ticks");
            boolean over = looked
                    || p.level() != level
                    || n * interval >= config.integer("max_ticks")
                    || spots.stream().anyMatch(spot -> p.position().distanceToSqr(spot) <= near * near);
            if (!over) {
                return true;
            }
            if (looked) {
                int fall = config.integer("fall_ticks");
                p.connection.send(new ClientboundCustomPayloadPacket(
                        new DimPayload(fall, config.integer("hold_ticks"), config.integer("rise_ticks"), 1, (float)
                                config.num("peak"))));
                Scheduler.later(p, fall + 1, q -> cows.forEach(cow -> cow.remove(q)));
            } else {
                cows.forEach(cow -> cow.remove(p));
            }
            return false;
        });
    }

    // 417

    private static boolean hasSpyglass(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(Items.SPYGLASS)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Waits for the player to raise a spyglass, then shows a mob that far off along where they look; it is removed as
     * soon as they lower it (or after {@code max_ticks}), and if they never raise it nothing happens.
     */
    private static void spyglassFigure(ServerPlayer player) {
        Config.Section config = Config.event("spyglass_figure");
        EntityType<?> type = type(config.text("mob"));
        if (type == null) {
            return;
        }
        ServerLevel level = player.level();
        int interval = config.integer("interval_ticks");
        PacketEntity[] figure = {null};
        int[] shown = {0};
        Scheduler.repeat(player, interval, interval, (p, n) -> {
            if (p.level() != level) {
                return false;
            }
            if (figure[0] == null) {
                if (n * interval >= config.integer("wait_ticks")) {
                    return false;
                }
                if (p.isScoping()) {
                    Vec3 look = p.getLookAngle();
                    Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
                    double x = p.getX() + flat.x * config.num("distance");
                    double z = p.getZ() + flat.z * config.num("distance");
                    int climb = config.integer("climb");
                    Integer y = Walk.ground(level, x, z, p.getBlockY() + climb, climb + config.integer("drop"));
                    if (y != null) {
                        Vec3 at = new Vec3(x, y, z);
                        figure[0] = PacketEntity.show(p, type, at, yawToPlayer(p, at));
                    }
                }
                return true;
            }
            if (!p.isScoping() || ++shown[0] * interval >= config.integer("max_ticks")) {
                figure[0].remove(p);
                return false;
            }
            return true;
        });
    }

    // 491

    /**
     * Lightning with nothing burning, {@code flashes} times; at each the figure is shown for a few ticks, a step closer
     * than at the last, along one bearing. The bolt strikes beyond it, so it stands against the flash.
     */
    private static void flashFigure(ServerPlayer player) {
        Config.Section config = Config.event("flash_figure");
        EntityType<?> type = type(config.text("mob"));
        if (type == null) {
            return;
        }
        double bearing =
                Math.toRadians(player.getYRot() + (player.getRandom().nextDouble() * 2 - 1) * config.num("angle"));
        Vec3 away = new Vec3(-Math.sin(bearing), 0, Math.cos(bearing));
        flash(player, player.level(), type, away, 0, config.num("distance"));
    }

    private static void flash(
            ServerPlayer player, ServerLevel level, EntityType<?> type, Vec3 away, int index, double distance) {
        Config.Section config = Config.event("flash_figure");
        int wait = index == 0 ? config.integer("first_ticks") : gap(player, config);
        Scheduler.later(player, wait, p -> {
            if (p.level() != level) {
                return;
            }
            double x = p.getX() + away.x * distance;
            double z = p.getZ() + away.z * distance;
            int climb = config.integer("climb");
            Integer y = Walk.ground(level, x, z, p.getBlockY() + climb, climb + config.integer("drop"));
            Vec3 at = new Vec3(x, y != null ? y : p.getY(), z);
            Assaults.strike(level, BlockPos.containing(at.add(away.scale(config.num("behind")))), true);
            PacketEntity figure = PacketEntity.show(p, type, at, yawToPlayer(p, at));
            Scheduler.later(p, config.integer("flash_ticks"), figure::remove);
            if (index + 1 < config.integer("flashes")) {
                double closer = Math.max(config.num("min_distance"), distance - config.num("step"));
                flash(p, level, type, away, index + 1, closer);
            }
        });
    }

    private static int gap(ServerPlayer player, Config.Section config) {
        int min = config.integer("gap_min_ticks");
        return min + player.getRandom().nextInt(config.integer("gap_max_ticks") - min + 1);
    }

    // shared

    private static float yawToPlayer(ServerPlayer player, Vec3 at) {
        return PacketEntity.yawToward(at, player.position());
    }

    private static @Nullable EntityType<?> type(String id) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id)).orElse(null);
    }

    /** Shown at {@code at}; {@code dress} edits a model of the same type whose entity data is then sent. */
    private static PacketEntity show(
            ServerPlayer player, EntityType<?> type, Vec3 at, float yaw, @Nullable Consumer<Entity> dress) {
        PacketEntity mob = PacketEntity.show(player, type, at, yaw);
        dress(player, mob, type, dress);
        return mob;
    }

    private static void dress(
            ServerPlayer player, PacketEntity mob, EntityType<?> type, @Nullable Consumer<Entity> dress) {
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
    }

    /** Whether {@code at} is within {@code degrees} of where the player looks, with nothing solid between. */
    private static boolean watched(ServerPlayer player, Vec3 at, double degrees) {
        Vec3 eye = player.getEyePosition();
        return player.getLookAngle().dot(at.subtract(eye).normalize()) >= Math.cos(Math.toRadians(degrees))
                && player.level()
                                .clip(new ClipContext(
                                        eye, at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
                                .getType()
                        == HitResult.Type.MISS;
    }

    /**
     * Keeps the mobs until {@code max_ticks} are up, the player comes within {@code vanish_distance} of one, or they
     * have looked at the first for {@code look_ticks} checks, when {@code vanish} is played. Checked every
     * {@code interval_ticks}.
     */
    private static void linger(
            ServerPlayer player, String id, List<PacketEntity> mobs, List<Vec3> at, @Nullable SoundEvent vanish) {
        Config.Section config = Config.event(id);
        ServerLevel level = player.level();
        int interval = config.integer("interval_ticks");
        double near = config.num("vanish_distance");
        double look = config.num("look_angle");
        int[] seen = {0};
        Scheduler.repeat(player, interval, interval, (p, n) -> {
            boolean looked = watched(p, at.get(0).add(0, 1.0, 0), look) && ++seen[0] >= config.integer("look_ticks");
            boolean over = looked
                    || p.level() != level
                    || (n + 1) * interval >= config.integer("max_ticks")
                    || at.stream().anyMatch(spot -> p.position().distanceToSqr(spot) <= near * near);
            if (over) {
                mobs.forEach(mob -> mob.remove(p));
                if (looked && vanish != null) {
                    sound(p, vanish, at.get(0), config.tone("sound"));
                }
            }
            return !over;
        });
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
