package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.HauntSounds;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.event.kit.Eyes;
import dev.keelfy.restless.haunt.event.kit.Nearby;
import dev.keelfy.restless.haunt.event.kit.PacketEntity;
import dev.keelfy.restless.haunt.event.kit.Place;
import dev.keelfy.restless.haunt.event.kit.SimpleEvent;
import dev.keelfy.restless.haunt.recall.Said;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Scares the player sets off by something they do or where they stand: looking down a pit, coming up from a dive,
 * lowering a shield, looking away from far figures, walking up to a stair. Each polls the player for a while and ends
 * quietly if the moment never comes; see docs/scary-events.md. The figures are the {@link Jolts} kind, seen by this
 * player only.
 */
final class Frights {
    private Frights() {}

    static List<HauntEvent> all() {
        return List.of(
                // The view pulls out behind the player; a figure stands close behind them, and is there when it snaps
                // back.
                SimpleEvent.waiting(
                        "third_person",
                        EventTag.PRESENCE,
                        player -> Eyes.steady(player) && behind(player, Config.event("third_person")) != null,
                        Frights::thirdPerson),
                // Looking down into a deep drop: a figure at the bottom rises into the face.
                SimpleEvent.waiting(
                        "pit_face", EventTag.PRESENCE, player -> pit(player, false) != null, Frights::pitFace),
                // Coming up from a dive: a figure stands on the water ahead.
                SimpleEvent.waiting(
                        "surface_break", EventTag.WATER, player -> player.isInWater(), Frights::surfaceBreak),
                // A shield held up long enough is knocked on three times; lowered, a figure is at the face.
                SimpleEvent.waiting(
                        "shield_knocks",
                        EventTag.PRESENCE,
                        player -> player.getMainHandItem().is(Items.SHIELD)
                                || player.getOffhandItem().is(Items.SHIELD),
                        Frights::shieldKnocks),
                // A figure far off in the dark for every look away, five, then all gone and one close behind.
                SimpleEvent.waiting(
                        "tree_line",
                        EventTag.PRESENCE,
                        player -> player.level().dimension() == Level.OVERWORLD
                                && player.level().isDarkOutside()
                                && Nearby.any(player, "tree_line", state -> state.is(BlockTags.LOGS)),
                        Frights::treeLine),
                // A low figure at the top of a flight of stairs; near it, it is gone and steps sound behind.
                SimpleEvent.waiting(
                        "stairs_sitter", EventTag.PRESENCE, player -> sitter(player) != null, Frights::stairsSitter));
    }

    // ---- third_person ----

    /** Room for a figure close behind the player. */
    private static @Nullable BlockPos behind(ServerPlayer player, Config.Section config) {
        return Place.standingAhead(player, config.num("distance"), 150, 180);
    }

    /**
     * The {@code out_of_body} view, with the figure in it, close behind the player's head. When the view is over the
     * figure is shown again with the bang, and unseen until the player turns ({@link Jolts#show}).
     */
    private static void thirdPerson(ServerPlayer player) {
        Config.Section config = Config.event("third_person");
        BlockPos spot = behind(player, config);
        if (spot == null || !Cameras.outOfBody(player)) {
            return;
        }
        UUID spirit = Said.current();
        Vec3 at = Vec3.atBottomCenterOf(spot);
        PacketEntity figure =
                PacketEntity.show(player, Content.SILHOUETTE.get(), at, PacketEntity.yawToward(at, player.position()));
        Jolts.shape(player, figure);
        Scheduler.later(player, Config.event("out_of_body").integer("ticks") + 1, p -> {
            figure.remove(p);
            BlockPos now = behind(p, config);
            if (now != null && p.isAlive()) {
                Jolts.show(
                        p,
                        config,
                        now,
                        true,
                        config.integer("wait_ticks"),
                        seen -> Director.landed(seen, "third_person", spirit));
            }
        });
    }

    // ---- pit_face ----

    /**
     * The floor of a drop of {@code depth} or more beside the player, within {@code reach} blocks, with the player
     * looking down steeply (when {@code looking}).
     */
    private static @Nullable BlockPos pit(ServerPlayer player, boolean looking) {
        Config.Section config = Config.event("pit_face");
        ServerLevel level = player.level();
        if (!player.onGround() || player.isInWater() || looking && player.getXRot() < config.num("pitch")) {
            return null;
        }
        int reach = config.integer("reach");
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (dx == 0 && dz == 0 || dx * dx + dz * dz > reach * reach) {
                    continue;
                }
                BlockPos floor = bottom(level, player.blockPosition().offset(dx, -1, dz), config.integer("depth"));
                if (floor != null) {
                    return floor;
                }
            }
        }
        return null;
    }

    /** The first solid block under {@code top} if it lies {@code depth} or more down through open space. */
    private static @Nullable BlockPos bottom(ServerLevel level, BlockPos top, int depth) {
        int open = 0;
        for (BlockPos pos = top;
                pos.getY() > level.getMinY() && open <= Config.event("pit_face").integer("max_depth");
                pos = pos.below()) {
            if (!level.isLoaded(pos)) {
                return null;
            }
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return open >= depth && level.getFluidState(pos.above()).isEmpty() ? pos : null;
            }
            open++;
        }
        return null;
    }

    /**
     * Polls for the player looking steeply down at a pit. The figure stands at its floor, rises to {@code gap} blocks
     * below their eyes in {@code rise_ticks}, bangs when it gets there, and is gone {@code hold_ticks} later.
     */
    private static void pitFace(ServerPlayer player) {
        Config.Section config = Config.event("pit_face");
        ServerLevel level = player.level();
        UUID spirit = Said.current();
        Scheduler.repeat(player, 1, 1, (p, n) -> {
            if (p.level() != level || n >= config.integer("max_ticks")) {
                return false;
            }
            BlockPos floor = pit(p, true);
            if (floor == null) {
                return true;
            }
            Vec3 from = Vec3.atBottomCenterOf(floor.above());
            Vec3 to = new Vec3(from.x, p.getEyeY() - config.num("gap") - 1.8, from.z);
            PacketEntity figure =
                    PacketEntity.show(p, Content.SILHOUETTE.get(), from, PacketEntity.yawToward(from, p.position()));
            Jolts.shape(p, figure);
            int rise = config.integer("rise_ticks");
            Scheduler.repeat(p, 1, 1, (q, t) -> {
                if (q.level() == level && t < rise) {
                    figure.move(q, from.lerp(to, (t + 1.0) / rise), PacketEntity.yawToward(to, q.position()));
                    if (t + 1 == rise) {
                        Director.landed(q, "pit_face", spirit);
                        Nearby.sound(q, HauntSounds.JOLT, to.add(0, 1.0, 0), config.tone("sound"));
                    }
                    return true;
                }
                boolean over = q.level() != level || t >= rise + config.integer("hold_ticks");
                if (over) {
                    figure.remove(q);
                }
                return !over;
            });
            return false;
        });
    }

    // ---- surface_break ----

    /**
     * Polls for the head coming out of the water after {@code dive_ticks} under it. A figure then stands on the water
     * {@code distance} blocks ahead, with a splash and a bang, for {@code hold_ticks}.
     */
    private static void surfaceBreak(ServerPlayer player) {
        Config.Section config = Config.event("surface_break");
        ServerLevel level = player.level();
        UUID spirit = Said.current();
        int[] under = {0};
        Scheduler.repeat(player, 1, 1, (p, n) -> {
            if (p.level() != level || n >= config.integer("max_ticks") || !p.isInWater() && !p.isUnderWater()) {
                return false;
            }
            if (p.isUnderWater()) {
                under[0]++;
                return true;
            }
            if (under[0] < config.integer("dive_ticks")) {
                under[0] = 0;
                return true;
            }
            Vec3 on = surface(p, config.num("distance"));
            if (on != null) {
                Director.landed(p, "surface_break", spirit);
                Nearby.sound(p, SoundEvents.PLAYER_SPLASH, on, config.tone("splash"));
                Scares.flash(p, on, -1, config.tone("sound"), config.integer("hold_ticks"));
            }
            return false;
        });
    }

    /** The top of the water {@code distance} blocks ahead of the player, with open air over it. */
    private static @Nullable Vec3 surface(ServerPlayer player, double distance) {
        ServerLevel level = player.level();
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        Vec3 ahead = player.position().add(flat.scale(distance));
        BlockPos column = BlockPos.containing(ahead.x, player.getEyeY(), ahead.z);
        for (int dy = -2; dy <= 2; dy++) {
            BlockPos water = column.above(dy);
            if (level.getFluidState(water).isSource()
                    && level.getFluidState(water.above()).isEmpty()
                    && level.isEmptyBlock(water.above())
                    && level.isEmptyBlock(water.above(2))) {
                return new Vec3(ahead.x, water.getY() + 1.0, ahead.z);
            }
        }
        return null;
    }

    // ---- shield_knocks ----

    private static boolean hostilesNear(ServerPlayer player, double radius) {
        return !player.level()
                .getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(radius), Monster::isAlive)
                .isEmpty();
    }

    /**
     * Polls for a raised shield. After {@code block_ticks} of holding it with no hostile within {@code quiet_radius},
     * it is knocked {@code knocks} times from the front, {@code knock_gap_ticks} apart. When it is lowered after that a
     * figure is right ahead, with a bang.
     */
    private static void shieldKnocks(ServerPlayer player) {
        Config.Section config = Config.event("shield_knocks");
        ServerLevel level = player.level();
        UUID spirit = Said.current();
        int[] held = {0};
        int[] knocked = {0};
        Scheduler.repeat(player, 1, 1, (p, n) -> {
            if (p.level() != level || n >= config.integer("max_ticks")) {
                return false;
            }
            if (p.isBlocking()) {
                if (hostilesNear(p, config.num("quiet_radius"))) {
                    held[0] = 0;
                    knocked[0] = 0;
                    return true;
                }
                int t = ++held[0] - config.integer("block_ticks");
                if (t >= 0 && t % config.integer("knock_gap_ticks") == 0 && knocked[0] < config.integer("knocks")) {
                    knocked[0]++;
                    Nearby.sound(
                            p,
                            SoundEvents.SHIELD_BLOCK.value(),
                            Nearby.around(p, config.num("knock_distance"), 0),
                            config.tone("knock"));
                }
                return true;
            }
            if (knocked[0] == 0) {
                held[0] = 0;
                return true;
            }
            BlockPos spot =
                    Place.standingAhead(p, config.num("distance"), config.num("min_angle"), config.num("max_angle"));
            if (spot != null) {
                Director.landed(p, "shield_knocks", spirit);
                Scares.flash(p, Vec3.atBottomCenterOf(spot), -1, config.tone("sound"), config.integer("hold_ticks"));
            }
            return false;
        });
    }

    // ---- tree_line ----

    private static @Nullable BlockPos darkSpot(ServerPlayer player, Config.Section config) {
        return Place.standingAhead(
                player,
                config.num("distance"),
                config.num("min_angle"),
                config.num("max_angle"),
                pos -> player.level().getBrightness(LightLayer.BLOCK, pos) <= config.integer("dark_max"));
    }

    /**
     * A figure far ahead in the dark. Each time the player looks at any and then away, another stands somewhere else
     * ahead of them, up to {@code figures}. The look away after that removes all and puts one {@code behind_distance}
     * behind them, unseen until they turn ({@link Jolts#show}).
     */
    private static void treeLine(ServerPlayer player) {
        Config.Section config = Config.event("tree_line");
        ServerLevel level = player.level();
        UUID spirit = Said.current();
        List<Vec3> spots = new ArrayList<>();
        List<PacketEntity> figures = new ArrayList<>();
        boolean[] seen = {false};
        Scheduler.repeat(player, 1, 1, (p, n) -> {
            if (p.level() != level || n >= config.integer("max_ticks")) {
                figures.forEach(f -> f.remove(p));
                return false;
            }
            if (spots.stream().anyMatch(at -> Jolts.watched(p, at.add(0, 1.5, 0), config.num("look_angle")))) {
                seen[0] = true;
                return true;
            }
            if (!seen[0] && !spots.isEmpty()) {
                return true;
            }
            if (spots.size() < config.integer("figures")) {
                BlockPos spot = darkSpot(p, config);
                if (spot != null) {
                    seen[0] = false;
                    Vec3 at = Vec3.atBottomCenterOf(spot);
                    spots.add(at);
                    PacketEntity figure = PacketEntity.show(
                            p, Content.SILHOUETTE.get(), at, PacketEntity.yawToward(at, p.position()));
                    Jolts.shape(p, figure);
                    figures.add(figure);
                }
                return true;
            }
            BlockPos behind = Place.standingAhead(p, config.num("behind_distance"), 150, 180);
            if (behind == null) {
                return true;
            }
            figures.forEach(f -> f.remove(p));
            Jolts.show(
                    p,
                    config,
                    behind,
                    true,
                    config.integer("wait_ticks"),
                    s -> Director.landed(s, "tree_line", spirit));
            return false;
        });
    }

    // ---- stairs_sitter ----

    /** The block at the top of a flight of at least {@code steps} stairs in view, within the distance bounds. */
    private static @Nullable BlockPos sitter(ServerPlayer player) {
        Config.Section config = Config.event("stairs_sitter");
        ServerLevel level = player.level();
        for (BlockPos base : Nearby.all(player, "stairs_sitter", Frights::stair)) {
            Direction up = level.getBlockState(base).getValue(StairBlock.FACING);
            if (stairAt(level, base.relative(up.getOpposite()).below(), up)) {
                continue;
            }
            BlockPos top = base;
            int steps = 1;
            while (stairAt(level, top.relative(up).above(), up)) {
                top = top.relative(up).above();
                steps++;
            }
            Vec3 at = Vec3.atBottomCenterOf(top.above());
            double away = player.position().distanceTo(at);
            if (steps >= config.integer("steps")
                    && away >= config.num("min_distance")
                    && away <= config.num("max_distance")
                    && level.isEmptyBlock(top.above())
                    && level.isEmptyBlock(top.above(2))
                    && Jolts.watched(player, at.add(0, 1.0, 0), config.num("look_angle"))) {
                return top;
            }
        }
        return null;
    }

    private static boolean stair(BlockState state) {
        return state.getBlock() instanceof StairBlock && state.getValue(StairBlock.HALF) == Half.BOTTOM;
    }

    private static boolean stairAt(ServerLevel level, BlockPos pos, Direction up) {
        BlockState state = level.getBlockState(pos);
        return stair(state) && state.getValue(StairBlock.FACING) == up;
    }

    /**
     * A low figure on the top step. When the player comes within {@code trigger_distance} of it, it is gone and
     * {@code steps_heard} steps sound {@code step_distance} blocks behind them, {@code step_gap_ticks} apart.
     */
    private static void stairsSitter(ServerPlayer player) {
        Config.Section config = Config.event("stairs_sitter");
        BlockPos top = sitter(player);
        if (top == null) {
            return;
        }
        ServerLevel level = player.level();
        UUID spirit = Said.current();
        Vec3 at = Vec3.atBottomCenterOf(top.above());
        PacketEntity figure =
                PacketEntity.show(player, Content.SILHOUETTE.get(), at, PacketEntity.yawToward(at, player.position()));
        Jolts.shape(player, figure, 1);
        Scheduler.repeat(player, 1, 1, (p, n) -> {
            if (p.level() != level || n >= config.integer("max_ticks")) {
                figure.remove(p);
                return false;
            }
            if (p.position().distanceTo(at) > config.num("trigger_distance")) {
                return true;
            }
            figure.remove(p);
            Director.landed(p, "stairs_sitter", spirit);
            Scheduler.repeat(p, 1, config.integer("step_gap_ticks"), (q, t) -> {
                Nearby.sound(
                        q,
                        SoundEvents.RAVAGER_STEP,
                        Nearby.around(q, config.num("step_distance"), 180),
                        config.tone("step_sound"));
                return t + 1 < config.integer("steps_heard");
            });
            return false;
        });
    }
}
