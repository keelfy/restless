package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Direct physical harm. Every event leaves the player alive; lethal ones need a telegraph and come later. */
final class Assaults {
    /** Telegraphed events stop hurting once the player is down to this much health (3 hearts). */
    private static final float SAFE_HEALTH = 6.0F;

    private Assaults() {}

    static List<HauntEvent> all() {
        return List.of(
            // 53: freeze the legs for a second
            SimpleEvent.harmful("trip", EventTag.ASSAULT, 0.7, player -> !player.hasEffect(MobEffects.SLOWNESS), player -> {
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 6), null);
                player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_SMALL_FALL, SoundSource.HOSTILE, 1.0F, 0.7F);
            }),
            // 54
            SimpleEvent.harmful("scorch", EventTag.ASSAULT, 0.6,
                player -> player.getHealth() > 6.0F && !player.isInWater() && !player.isOnFire() && !player.fireImmune(),
                player -> player.igniteForSeconds(1.0F)),
            // 55: like cold_touch, harder
            SimpleEvent.harmful("wrath", EventTag.ASSAULT, 0.6, player -> player.getHealth() > 8.0F, player -> {
                float damage = Math.min(4.0F + player.getRandom().nextInt(4), player.getHealth() - 1.0F);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WARDEN_NEARBY_CLOSE, SoundSource.HOSTILE, 1.0F, 0.8F);
                player.hurtServer(player.level(), player.damageSources().magic(), damage);
            }),
            // 56
            SimpleEvent.harmful("snatch", EventTag.ASSAULT, 0.7, player -> !player.getMainHandItem().isEmpty(), Assaults::snatch),
            // 57
            SimpleEvent.harmful("suffocation", EventTag.ASSAULT, 0.5,
                player -> player.getHealth() > SAFE_HEALTH + 2 && !player.isInWater() && dark(player), Assaults::suffocate),
            // 52: always toward a drop of four to seven blocks onto solid ground
            SimpleEvent.harmful("shove", EventTag.ASSAULT, 0.5, player -> player.getHealth() > SAFE_HEALTH + 4 && Place.ledge(player) != null, Assaults::shove),
            // 59: straight up from solid ground, about two hearts of fall
            SimpleEvent.harmful("toss", EventTag.ASSAULT, 0.5,
                player -> player.getHealth() > SAFE_HEALTH + 4 && player.onGround() && !player.isInWater() && !player.isInLava(),
                Assaults::toss),
            // 58
            SimpleEvent.harmful("thunder_knocks", EventTag.ASSAULT, 0.5, player -> player.getHealth() > SAFE_HEALTH + 4, Assaults::thunderKnocks),
            // 60
            SimpleEvent.harmful("embrace", EventTag.ASSAULT, 0.5, player -> player.getHealth() > SAFE_HEALTH + 4, Assaults::embrace)
        );
    }

    private static void shove(ServerPlayer player) {
        Direction toward = Place.ledge(player);
        if (toward != null) {
            push(player, toward.getStepX() * 0.7, 0.2, toward.getStepZ() * 0.7);
        }
    }

    private static void toss(ServerPlayer player) {
        push(player, 0.0, 1.0, 0.0);
    }

    private static void push(ServerPlayer player, double x, double y, double z) {
        player.setDeltaMovement(x, y, z);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.HOSTILE, 1.0F, 0.6F);
    }

    private static void snatch(ServerPlayer player) {
        ItemStack held = player.getMainHandItem().copy();
        player.getInventory().setItem(player.getInventory().getSelectedSlot(), ItemStack.EMPTY);
        double angle = player.getRandom().nextDouble() * Math.PI * 2;
        // Launch speed chosen so the item lands roughly five blocks away.
        Vec3 velocity = new Vec3(Math.cos(angle) * 0.45, 0.3, Math.sin(angle) * 0.45);
        ItemEntity item = new ItemEntity(player.level(), player.getX(), player.getEyeY(), player.getZ(), held, velocity.x, velocity.y, velocity.z);
        item.setDefaultPickUpDelay();
        player.level().addFreshEntity(item);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ITEM_PICKUP, SoundSource.HOSTILE, 1.0F, 0.5F);
    }

    private static boolean dark(ServerPlayer player) {
        return player.level().getMaxLocalRawBrightness(player.blockPosition()) < 8;
    }

    /** A heartbeat first, then the air drains until the player finds light; out of water nothing else would hurt them. */
    private static void suffocate(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.0F, 1.0F);
        Scheduler.repeat(player, 40, 2, (p, n) -> {
            if (!dark(p) || p.isInWater() || p.getHealth() <= SAFE_HEALTH || n >= 150) {
                return false;
            }
            p.setAirSupply(Math.max(0, p.getAirSupply() - 12));
            if (n % 10 == 0) {
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 0.8F, 1.0F);
                if (p.getAirSupply() == 0) {
                    p.hurtServer(p.level(), p.damageSources().drown(), 1.0F);
                }
            }
            return true;
        });
    }

    /** Three knocks, a bolt that misses, and a bolt for whoever has not moved since. */
    private static void thunderKnocks(ServerPlayer player) {
        Sounds.knocks(player, Sounds.knockSpot(player));
        Scheduler.later(player, 60, p -> {
            Vec3 stood = p.position();
            strike(p.level(), BlockPos.containing(Nearby.randomAround(p, 6.0)));
            Scheduler.later(p, 40, q -> {
                if (q.position().distanceToSqr(stood) < 4.0 && q.getHealth() > SAFE_HEALTH) {
                    strike(q.level(), q.blockPosition());
                    q.hurtServer(q.level(), q.damageSources().lightningBolt(), Math.min(6.0F, q.getHealth() - 1.0F));
                }
            });
        });
    }

    /** Visual only: it lights and sounds like lightning but sets nothing on fire. */
    private static void strike(ServerLevel level, BlockPos pos) {
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
        if (bolt != null) {
            bolt.setPos(Vec3.atBottomCenterOf(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos)));
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }

    /** A hit a second, a little harder each time, until the player is 10 blocks from where it began. */
    private static void embrace(ServerPlayer player) {
        Vec3 origin = player.position();
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_NEARBY_CLOSE, SoundSource.HOSTILE, 1.0F, 0.7F);
        Scheduler.repeat(player, 30, 20, (p, n) -> {
            if (p.position().distanceToSqr(origin) >= 100.0 || p.getHealth() <= SAFE_HEALTH || n >= 20) {
                return false;
            }
            p.level().sendParticles(ParticleTypes.SOUL, p.getX(), p.getY() + 1.0, p.getZ(), 8, 0.4, 0.6, 0.4, 0.02);
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE, 1.0F, 0.6F);
            p.hurtServer(p.level(), p.damageSources().magic(), Math.min(0.5F * (n + 1), p.getHealth() - SAFE_HEALTH));
            return true;
        });
    }
}
