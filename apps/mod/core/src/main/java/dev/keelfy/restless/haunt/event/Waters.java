package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** What a water spirit does: holds the player under, calls them to the water, empties it of fish and breaks what floats. */
final class Waters {
    private Waters() {}

    static List<HauntEvent> all() {
        return List.of(
            // 149: held under while swimming, down at the surface
            SimpleEvent.harmful("undertow", EventTag.WATER, player -> Assaults.healthy(player, "undertow") && player.isInWater(), Waters::undertow),
            // 149: drawn along the ground toward the nearest water
            SimpleEvent.harmful("water_call", EventTag.WATER, player -> Assaults.healthy(player, "water_call") && !player.isInWater() && waterTarget(player) != null, Waters::call),
            // 149: squid and fish nearby die
            SimpleEvent.light("drowned_fish", EventTag.WATER, player -> !fish(player).isEmpty(), Waters::drownFish),
            // 149: a boat nearby comes apart
            SimpleEvent.light("broken_boat", EventTag.WATER, player -> !boats(player).isEmpty(), Waters::breakBoat)
        );
    }

    /** The player is shoved down every step while they swim, and ends when they leave the water or are hurt enough. */
    private static void undertow(ServerPlayer player) {
        Config.Section config = Config.event("undertow");
        Scheduler.repeat(player, 1, config.integer("interval_ticks"), (p, n) -> {
            if (!p.isInWater() || p.getHealth() <= Assaults.safeHealth() || n * config.integer("interval_ticks") >= config.integer("max_ticks")) {
                return false;
            }
            p.setDeltaMovement(p.getDeltaMovement().x, -config.num("pull"), p.getDeltaMovement().z);
            p.connection.send(new ClientboundSetEntityMotionPacket(p));
            return true;
        });
    }

    private static @Nullable Vec3 waterTarget(ServerPlayer player) {
        BlockPos here = player.blockPosition();
        return Nearby.all(player, "water_call", state -> state.getFluidState().is(FluidTags.WATER)).stream()
            .min(Comparator.comparingDouble(pos -> pos.distSqr(here))).map(Vec3::atBottomCenterOf).orElse(null);
    }

    private static void call(ServerPlayer player) {
        Vec3 target = waterTarget(player);
        if (target == null) {
            return;
        }
        Config.Section config = Config.event("water_call");
        Nearby.sound(player, SoundEvents.AMBIENT_UNDERWATER_ENTER, player.position(), config.tone("sound"));
        Scheduler.repeat(player, config.integer("delay_ticks"), config.integer("interval_ticks"), (p, n) -> {
            double dx = target.x - p.getX();
            double dz = target.z - p.getZ();
            double left = Math.max(Math.hypot(dx, dz), 1.0E-4);
            if (p.isInWater() || p.getHealth() <= Assaults.safeHealth() || left <= config.num("arrive_distance")
                || n * config.integer("interval_ticks") >= config.integer("max_ticks")) {
                return false;
            }
            p.level().sendParticles(ParticleTypes.SPLASH, p.getX(), p.getY() + 0.2, p.getZ(), 4, 0.3, 0.1, 0.3, 0.01);
            if (p.onGround()) {
                double pull = config.num("pull");
                p.setDeltaMovement(dx / left * pull, p.getDeltaMovement().y, dz / left * pull);
                p.connection.send(new ClientboundSetEntityMotionPacket(p));
            }
            return true;
        });
    }

    private static List<Entity> fish(ServerPlayer player) {
        double radius = Config.event("drowned_fish").num("radius");
        return List.copyOf(player.level().getEntitiesOfClass(Entity.class, player.getBoundingBox().inflate(radius), e -> e instanceof Squid || e instanceof AbstractFish));
    }

    private static void drownFish(ServerPlayer player) {
        Config.Section config = Config.event("drowned_fish");
        List<Entity> found = fish(player);
        Nearby.sound(player, SoundEvents.ELDER_GUARDIAN_CURSE, player.position(), config.tone("sound"));
        for (Entity animal : found.subList(0, Math.min(found.size(), config.integer("max_animals")))) {
            player.level().sendParticles(ParticleTypes.SQUID_INK, animal.getX(), animal.getY() + 0.2, animal.getZ(), 8, 0.2, 0.2, 0.2, 0.02);
            animal.kill(player.level());
        }
    }

    private static List<AbstractBoat> boats(ServerPlayer player) {
        double radius = Config.event("broken_boat").num("radius");
        AABB area = player.getBoundingBox().inflate(radius);
        return player.level().getEntitiesOfClass(AbstractBoat.class, area, boat -> !boat.isVehicle());
    }

    private static void breakBoat(ServerPlayer player) {
        List<AbstractBoat> found = boats(player);
        AbstractBoat boat = found.get(player.getRandom().nextInt(found.size()));
        Nearby.sound(player, SoundEvents.WOOD_BREAK, boat.position(), Config.event("broken_boat").tone("sound"));
        boat.hurtServer(player.level(), player.level().damageSources().generic(), (float) Config.event("broken_boat").num("damage"));
    }
}
