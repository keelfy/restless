package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Mobs only this player's client is told of, more of what {@link Illusions} does with a {@link PacketEntity}: nothing
 * on the server, so nothing to put back, and a mob that is shown and then removed leaves nothing behind.
 */
final class Apparitions {
    /** Samples per block along the path of a vex. */
    private static final double SAMPLES_PER_BLOCK = 2.0;

    private Apparitions() {}

    static List<HauntEvent> all() {
        return List.of(
                // 405: a vex flies straight through a wall, as if it were not there
                SimpleEvent.light(
                        "vex_through_wall",
                        EventTag.MOBS,
                        player -> wallPath(player) != null,
                        Apparitions::vexThroughWall),
                // 409: a phantom dives at the player and is gone on contact
                SimpleEvent.light(
                        "phantom_swoop",
                        EventTag.MOBS,
                        player -> player.level().canSeeSky(player.blockPosition()),
                        Apparitions::phantomSwoop),
                // 413: a dead mob lies on the path, as it fell, until the player comes near
                SimpleEvent.light(
                        "dead_on_path", EventTag.MOBS, player -> deadSpot(player) != null, Apparitions::deadOnPath),
                // 414: llamas with no trader cross in front of the player in a line
                SimpleEvent.light(
                        "llama_caravan", EventTag.MOBS, player -> caravanSpot(player) != null, Apparitions::caravan),
                // 415: a huge slime hops about out in the field
                SimpleEvent.light(
                        "giant_slime", EventTag.MOBS, player -> slimeSpot(player) != null, Apparitions::giantSlime));
    }

    private record Path(Vec3 from, Vec3 to) {}

    /**
     * A straight line at eye height, centred {@code distance} blocks away at a random bearing, that starts and ends in
     * the open and passes through {@code min_solid} to {@code max_solid} solid samples between.
     */
    private static @Nullable Path wallPath(ServerPlayer player) {
        Config.Section config = Config.event("vex_through_wall");
        ServerLevel level = player.level();
        double half = config.num("length") / 2;
        int samples = (int) (config.num("length") * SAMPLES_PER_BLOCK);
        for (int i = 0; i < config.integer("tries"); i++) {
            Vec3 centre = Nearby.randomAround(player, config.num("distance"));
            double bearing = player.getRandom().nextDouble() * Math.PI;
            Vec3 along = new Vec3(Math.cos(bearing), 0, Math.sin(bearing)).scale(half);
            Vec3 from = new Vec3(centre.x - along.x, player.getEyeY(), centre.z - along.z);
            Vec3 to = new Vec3(centre.x + along.x, player.getEyeY(), centre.z + along.z);
            int solid = 0;
            boolean open = true;
            for (int k = 0; k <= samples; k++) {
                Vec3 at = from.lerp(to, k / (double) samples);
                BlockPos pos = BlockPos.containing(at);
                boolean block = level.isLoaded(pos) && level.getBlockState(pos).isSolidRender();
                open &= !((k == 0 || k == samples) && block);
                solid += block ? 1 : 0;
            }
            if (open && solid >= config.integer("min_solid") && solid <= config.integer("max_solid")) {
                return player.getRandom().nextBoolean() ? new Path(from, to) : new Path(to, from);
            }
        }
        return null;
    }

    private static void vexThroughWall(ServerPlayer player) {
        Path path = wallPath(player);
        if (path != null) {
            fly(
                    player,
                    EntityTypes.VEX,
                    path.from(),
                    path.to(),
                    Config.event("vex_through_wall").integer("ticks"));
        }
    }

    /** A packet {@code type} that goes from {@code from} to {@code to} in {@code ticks}, then is gone. */
    private static void fly(ServerPlayer player, EntityType<?> type, Vec3 from, Vec3 to, int ticks) {
        float yaw = PacketEntity.yawToward(from, to);
        PacketEntity mob = PacketEntity.show(player, type, from, yaw);
        ServerLevel level = player.level();
        Scheduler.repeat(player, 1, 1, (p, n) -> {
            if (n >= ticks || p.level() != level) {
                mob.remove(p);
                return false;
            }
            mob.move(p, from.lerp(to, (n + 1) / (double) ticks), yaw);
            return true;
        });
    }

    /** From high up at a random bearing, straight at the player's eyes, as they move, until it is within reach. */
    private static void phantomSwoop(ServerPlayer player) {
        Config.Section config = Config.event("phantom_swoop");
        Vec3 flat = Nearby.randomAround(player, config.num("distance"));
        Vec3[] at = {new Vec3(flat.x, player.getY() + config.num("height"), flat.z)};
        PacketEntity mob =
                PacketEntity.show(player, EntityTypes.PHANTOM, at[0], PacketEntity.yawToward(at[0], player.position()));
        Config.Tone tone = config.tone("sound");
        player.connection.send(new ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.PHANTOM_SWOOP),
                SoundSource.HOSTILE,
                at[0].x,
                at[0].y,
                at[0].z,
                tone.volume(),
                tone.pitch(),
                player.getRandom().nextLong()));
        ServerLevel level = player.level();
        double speed = config.num("speed");
        double contact = config.num("contact");
        Scheduler.repeat(player, 1, 1, (p, n) -> {
            Vec3 target = p.getEyePosition();
            Vec3 delta = target.subtract(at[0]);
            if (n >= config.integer("max_ticks") || p.level() != level || delta.length() <= contact) {
                mob.remove(p);
                return false;
            }
            at[0] = at[0].add(delta.normalize().scale(speed));
            mob.move(p, at[0], PacketEntity.yawToward(at[0], target));
            return true;
        });
    }

    private static @Nullable BlockPos deadSpot(ServerPlayer player) {
        Config.Section config = Config.event("dead_on_path");
        return Place.standingAhead(player, config.num("distance"), 0.0, config.num("max_angle"));
    }

    /** The mob is shown, then told it died: the client lays it down by itself, and it stays so. */
    private static void deadOnPath(ServerPlayer player) {
        Config.Section config = Config.event("dead_on_path");
        BlockPos spot = deadSpot(player);
        List<String> mobs = config.texts("mobs");
        EntityType<?> type = type(mobs.get(player.getRandom().nextInt(mobs.size())));
        if (spot == null || type == null) {
            return;
        }
        Vec3 at = Vec3.atBottomCenterOf(spot);
        PacketEntity mob =
                PacketEntity.show(player, type, at, player.getRandom().nextFloat() * 360.0F);
        Entity stand = type.create(player.level(), EntitySpawnReason.EVENT);
        if (stand == null) {
            mob.remove(player);
            return;
        }
        stand.setId(mob.id());
        player.connection.send(new ClientboundEntityEventPacket(stand, EntityEvent.DEATH));
        linger(player, mob, at, config.integer("max_ticks"), config.num("vanish_distance"), null);
    }

    /**
     * Keeps {@code mob} for up to {@code maxTicks}, or until the player comes within {@code near} of {@code at}. If
     * {@code pose} is given it is where the mob is each tick.
     */
    private static void linger(
            ServerPlayer player,
            PacketEntity mob,
            Vec3 at,
            int maxTicks,
            double near,
            @Nullable IntFunction<Vec3> pose) {
        ServerLevel level = player.level();
        Scheduler.repeat(player, 1, 1, (p, n) -> {
            boolean over = n >= maxTicks || p.level() != level || p.position().distanceToSqr(at) <= near * near;
            if (over) {
                mob.remove(p);
                return false;
            }
            if (pose != null) {
                mob.move(p, pose.apply(n), PacketEntity.yawToward(pose.apply(n), p.position()));
            }
            return true;
        });
    }

    private static @Nullable BlockPos caravanSpot(ServerPlayer player) {
        Config.Section config = Config.event("llama_caravan");
        return Place.standingAhead(player, config.num("distance"), 0.0, config.num("max_angle"));
    }

    /**
     * A line of llamas walks across, at right angles to where the player looks: each is shown when its turn comes and
     * removed past the end of the path, and follows the ground as it goes.
     */
    private static void caravan(ServerPlayer player) {
        Config.Section config = Config.event("llama_caravan");
        BlockPos spot = caravanSpot(player);
        EntityType<?> type = type(config.text("mob"));
        if (spot == null || type == null) {
            return;
        }
        Vec3 centre = Vec3.atBottomCenterOf(spot);
        Vec3 toSpot = centre.subtract(player.position());
        double reach = Math.max(Math.hypot(toSpot.x, toSpot.z), 1.0E-4);
        double sign = player.getRandom().nextBoolean() ? 1.0 : -1.0;
        Vec3 side = new Vec3(-toSpot.z / reach * sign, 0, toSpot.x / reach * sign);
        double length = config.num("length");
        Vec3 from = centre.subtract(side.scale(length / 2));
        float yaw = PacketEntity.yawToward(Vec3.ZERO, side);
        int count = config.integer("count");
        double spacing = config.num("spacing");
        double speed = config.num("speed");
        int climb = config.integer("climb");
        int drop = config.integer("drop");
        PacketEntity[] llamas = new PacketEntity[count];
        boolean[] gone = new boolean[count];
        double[] height = new double[count];
        Arrays.fill(height, centre.y);
        ServerLevel level = player.level();
        int total = (int) Math.ceil((length + (count - 1) * spacing) / speed);
        Scheduler.repeat(player, 1, 1, (p, n) -> {
            boolean over = n > total || p.level() != level;
            for (int i = 0; i < count; i++) {
                double along = n * speed - i * spacing;
                boolean past = over || along > length;
                if (llamas[i] != null && !gone[i] && past) {
                    llamas[i].remove(p);
                    gone[i] = true;
                }
                if (past || along < 0) {
                    continue;
                }
                Vec3 at = from.add(side.scale(along));
                Integer ground = Walk.ground(level, at.x, at.z, Mth.floor(height[i]) + climb, climb + drop);
                height[i] = ground != null ? ground : height[i];
                at = new Vec3(at.x, height[i], at.z);
                if (llamas[i] == null) {
                    llamas[i] = PacketEntity.show(p, type, at, yaw);
                } else {
                    llamas[i].move(p, at, yaw);
                }
            }
            return !over;
        });
    }

    private static @Nullable BlockPos slimeSpot(ServerPlayer player) {
        Config.Section config = Config.event("giant_slime");
        return Place.standingAhead(player, config.num("distance"), 0.0, config.num("max_angle"));
    }

    /** Shown at a size no slime is born with, hopping on the spot, until the player comes near. */
    private static void giantSlime(ServerPlayer player) {
        Config.Section config = Config.event("giant_slime");
        BlockPos spot = slimeSpot(player);
        Slime stand = EntityTypes.SLIME.create(player.level(), EntitySpawnReason.EVENT);
        if (spot == null || stand == null) {
            return;
        }
        Vec3 at = Vec3.atBottomCenterOf(spot);
        PacketEntity mob =
                PacketEntity.show(player, EntityTypes.SLIME, at, PacketEntity.yawToward(at, player.position()));
        stand.setId(mob.id());
        stand.setSize(config.integer("size"), false);
        List<SynchedEntityData.DataValue<?>> data = stand.getEntityData().getNonDefaultValues();
        if (data != null) {
            player.connection.send(new ClientboundSetEntityDataPacket(mob.id(), data));
        }
        int hop = config.integer("hop_ticks");
        double rise = config.num("hop_height");
        linger(
                player,
                mob,
                at,
                config.integer("max_ticks"),
                config.num("vanish_distance"),
                n -> at.add(0, Math.sin((n % hop) / (double) hop * Math.PI) * rise, 0));
    }

    private static @Nullable EntityType<?> type(String id) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id)).orElse(null);
    }
}
