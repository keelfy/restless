package dev.keelfy.restless.haunt.event.kit;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A swarm of tiny critters only one player's client is told of (a {@link PacketEntity} each, shrunk by the scale
 * attribute): they pour in from around, run to the player, circle at their feet (or, when climbing, spiral up their
 * body) and scatter. Nothing exists on the server, so nothing is left to revert.
 */
public final class Swarm {
    private Swarm() {}

    /** What one swarm is made of; the numbers come from {@code events.<id>}. */
    public record Spec(
            EntityType<?> type,
            boolean climb,
            int count,
            double scale,
            double speed,
            double startMin,
            double startMax,
            double orbitMin,
            double orbitMax,
            int ticks,
            int fleeTicks,
            SoundEvent step,
            Config.Tone tone) {
        public static Spec of(String id, EntityType<?> type, boolean climb, SoundEvent step) {
            Config.Section config = Config.event(id);
            return new Spec(
                    type,
                    climb,
                    config.integer("count"),
                    config.num("scale"),
                    config.num("speed"),
                    config.num("start_min"),
                    config.num("start_max"),
                    config.num("orbit_min"),
                    config.num("orbit_max"),
                    config.integer("ticks"),
                    config.integer("flee_ticks"),
                    step,
                    config.tone("sound"));
        }

        Spec lasting(int total) {
            return new Spec(
                    type, climb, count, scale, speed, startMin, startMax, orbitMin, orbitMax, total, fleeTicks, step,
                    tone);
        }
    }

    /** The spiders of {@code boggart_spiders}, for the given time; the Requiem's boggart movement sends them too. */
    public static void spiders(ServerPlayer player, int ticks) {
        run(
                player,
                Spec.of("boggart_spiders", EntityTypes.SPIDER, false, SoundEvents.SPIDER_STEP)
                        .lasting(ticks));
    }

    /** Whether a swarm has room to start: the player is not in water. */
    public static boolean canRun(ServerPlayer player) {
        return !player.isInWater() && !player.isSpectator();
    }

    private static final class Critter {
        final PacketEntity entity;
        Vec3 at;
        double angle;
        final double radius;
        final int dir;
        boolean orbiting;
        int age;

        Critter(PacketEntity entity, Vec3 at, double radius, int dir) {
            this.entity = entity;
            this.at = at;
            this.radius = radius;
            this.dir = dir;
        }
    }

    public static void run(ServerPlayer player, Spec spec) {
        ServerLevel level = player.level();
        RandomSource random = player.getRandom();
        List<Critter> critters = new ArrayList<>();
        for (int i = 0; i < spec.count(); i++) {
            Vec3 from = start(player, spec);
            if (from == null) {
                continue;
            }
            float yaw = PacketEntity.yawToward(from, player.position());
            PacketEntity entity = PacketEntity.show(player, spec.type(), from, yaw);
            dress(player, entity, spec);
            double orbit = spec.orbitMin() + random.nextDouble() * (spec.orbitMax() - spec.orbitMin());
            critters.add(new Critter(entity, from, orbit, random.nextBoolean() ? 1 : -1));
        }
        if (critters.isEmpty()) {
            return;
        }
        Nearby.sound(player, SoundEvents.SPIDER_AMBIENT, player.position(), spec.tone());
        Scheduler.repeat(player, 1, 1, (p, n) -> {
            if (n >= spec.ticks() || p.level() != level || !p.isAlive()) {
                critters.forEach(c -> c.entity.remove(p));
                return false;
            }
            boolean fleeing = n >= spec.ticks() - spec.fleeTicks();
            for (Critter c : critters) {
                step(p, spec, c, fleeing);
            }
            if (n % 3 == 0) {
                Critter heard = critters.get(random.nextInt(critters.size()));
                Nearby.sound(p, spec.step(), heard.at, spec.tone());
            }
            return true;
        });
    }

    /**
     * Where a critter appears: on the floor {@code start_min..start_max} blocks out, with a clear line to the player.
     */
    private static Vec3 start(ServerPlayer player, Spec spec) {
        RandomSource random = player.getRandom();
        for (int tries = 0; tries < 8; tries++) {
            double bearing = random.nextDouble() * Math.PI * 2;
            double distance = spec.startMin() + random.nextDouble() * (spec.startMax() - spec.startMin());
            double x = player.getX() + Math.cos(bearing) * distance;
            double z = player.getZ() + Math.sin(bearing) * distance;
            double y = ground(player.level(), x, z, player.getY());
            if (Double.isNaN(y)) {
                continue;
            }
            Vec3 at = new Vec3(x, y, z);
            boolean clear = player.level()
                            .clip(new ClipContext(
                                    player.getEyePosition(),
                                    at.add(0, 0.3, 0),
                                    ClipContext.Block.COLLIDER,
                                    ClipContext.Fluid.NONE,
                                    player))
                            .getType()
                    == HitResult.Type.MISS;
            if (clear) {
                return at;
            }
        }
        return null;
    }

    /** Shrinks the critter (the scale attribute), and puts spiders on the wall-climbing legs when they climb. */
    private static void dress(ServerPlayer player, PacketEntity entity, Spec spec) {
        AttributeInstance size = new AttributeInstance(Attributes.SCALE, instance -> {});
        size.setBaseValue(spec.scale());
        player.connection.send(new ClientboundUpdateAttributesPacket(entity.id(), List.of(size)));
        if (spec.climb()) {
            Entity model = spec.type().create(player.level(), EntitySpawnReason.EVENT);
            if (model instanceof Spider spider) {
                spider.setClimbing(true);
                List<SynchedEntityData.DataValue<?>> values =
                        spider.getEntityData().getNonDefaultValues();
                if (values != null && !values.isEmpty()) {
                    player.connection.send(new ClientboundSetEntityDataPacket(entity.id(), values));
                }
            }
        }
    }

    /** One tick of a critter: run at the player, circle (and climb) once close, then flee outward at the end. */
    private static void step(ServerPlayer player, Spec spec, Critter c, boolean fleeing) {
        Vec3 centre = player.position();
        Vec3 next;
        if (fleeing) {
            Vec3 away = new Vec3(c.at.x - centre.x, 0, c.at.z - centre.z);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(Math.cos(c.angle), 0, Math.sin(c.angle)) : away.normalize();
            next = c.at.add(away.scale(spec.speed() * 1.3));
            c.orbiting = false;
        } else if (c.orbiting) {
            c.angle += c.dir * spec.speed() / c.radius;
            next = new Vec3(centre.x + Math.cos(c.angle) * c.radius, c.at.y, centre.z + Math.sin(c.angle) * c.radius);
            c.age++;
        } else {
            Vec3 to = new Vec3(centre.x - c.at.x, 0, centre.z - c.at.z);
            double distance = to.length();
            if (distance <= c.radius + spec.speed()) {
                c.orbiting = true;
                c.angle = Math.atan2(c.at.z - centre.z, c.at.x - centre.x);
                next = c.at;
            } else {
                next = c.at.add(to.scale(spec.speed() / distance));
            }
        }
        double floor = ground(player.level(), next.x, next.z, player.getY());
        double y = Double.isNaN(floor) ? c.at.y : floor;
        if (spec.climb() && c.orbiting) {
            y += Math.min(player.getBbHeight() - 0.2, 0.06 * c.age);
        }
        Vec3 moved = new Vec3(next.x, y, next.z);
        float yaw = c.at.distanceToSqr(moved) > 1.0E-6 ? PacketEntity.yawToward(c.at, moved) : 0.0F;
        c.entity.move(player, moved, yaw);
        c.at = moved;
    }

    /** The height of the floor in the column, within a few blocks of {@code ref}; NaN when there is none. */
    private static double ground(ServerLevel level, double x, double z, double ref) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        for (int y = (int) Math.floor(ref) + 2; y >= (int) Math.floor(ref) - 3; y--) {
            BlockPos pos = new BlockPos(bx, y, bz);
            if (!level.isLoaded(pos)) {
                return Double.NaN;
            }
            BlockState here = level.getBlockState(pos);
            BlockState below = level.getBlockState(pos.below());
            if (here.getCollisionShape(level, pos).isEmpty()
                    && !below.getCollisionShape(level, pos.below()).isEmpty()) {
                return pos.below().getY()
                        + below.getCollisionShape(level, pos.below()).max(Direction.Axis.Y);
            }
        }
        return Double.NaN;
    }
}
