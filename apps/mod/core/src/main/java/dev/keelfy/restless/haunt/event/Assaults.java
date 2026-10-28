package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
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
    private Assaults() {}

    static List<HauntEvent> all() {
        return List.of(
            // 53: freeze the legs for a moment
            SimpleEvent.harmful("trip", EventTag.ASSAULT, player -> !player.hasEffect(MobEffects.SLOWNESS), player -> {
                Config.Section config = Config.event("trip");
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, config.integer("ticks"), config.integer("amplifier")), null);
                player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
                Config.Tone tone = config.tone("sound");
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_SMALL_FALL, SoundSource.HOSTILE, tone.volume(), tone.pitch());
            }),
            // 54
            SimpleEvent.harmful("scorch", EventTag.ASSAULT,
                player -> player.getHealth() > Config.event("scorch").num("min_health") && !player.isInWater() && !player.isOnFire() && !player.fireImmune(),
                player -> player.igniteForSeconds((float) Config.event("scorch").num("seconds"))),
            // 55: like cold_touch, harder
            SimpleEvent.harmful("wrath", EventTag.ASSAULT, player -> player.getHealth() > Config.event("wrath").num("min_health"), player -> {
                Config.Section config = Config.event("wrath");
                float damage = Math.min(config.integer("damage_min") + player.getRandom().nextInt(config.integer("damage_spread")),
                    player.getHealth() - (float) config.num("keep_health"));
                Config.Tone tone = config.tone("sound");
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WARDEN_NEARBY_CLOSE, SoundSource.HOSTILE, tone.volume(), tone.pitch());
                player.hurtServer(player.level(), player.damageSources().magic(), damage);
            }),
            // 56
            SimpleEvent.harmful("snatch", EventTag.ASSAULT, player -> !player.getMainHandItem().isEmpty(), Assaults::snatch),
            // 57
            SimpleEvent.harmful("suffocation", EventTag.ASSAULT,
                player -> healthy(player, "suffocation") && !player.isInWater() && dark(player), Assaults::suffocate),
            // 52: always toward a drop onto solid ground
            SimpleEvent.harmful("shove", EventTag.ASSAULT, player -> healthy(player, "shove") && Place.ledge(player) != null, Assaults::shove),
            // 59: straight up from solid ground
            SimpleEvent.harmful("toss", EventTag.ASSAULT,
                player -> healthy(player, "toss") && player.onGround() && !player.isInWater() && !player.isInLava(),
                Assaults::toss),
            // 58
            SimpleEvent.harmful("thunder_knocks", EventTag.ASSAULT, player -> healthy(player, "thunder_knocks"), Assaults::thunderKnocks),
            // 60
            SimpleEvent.harmful("embrace", EventTag.ASSAULT, player -> healthy(player, "embrace"), Assaults::embrace)
        );
    }

    /** Telegraphed events stop hurting once the player is down to this much health. */
    private static float safeHealth() {
        return (float) Config.section("assaults").num("safe_health");
    }

    /** Enough health above the safe floor for the event to start. */
    private static boolean healthy(ServerPlayer player, String id) {
        return player.getHealth() > safeHealth() + Config.event(id).num("margin");
    }

    private static void shove(ServerPlayer player) {
        Direction toward = Place.ledge(player);
        if (toward != null) {
            Config.Section config = Config.event("shove");
            push(player, toward.getStepX() * config.num("push"), config.num("lift"), toward.getStepZ() * config.num("push"), config.tone("sound"));
        }
    }

    private static void toss(ServerPlayer player) {
        Config.Section config = Config.event("toss");
        push(player, 0.0, config.num("lift"), 0.0, config.tone("sound"));
    }

    private static void push(ServerPlayer player, double x, double y, double z, Config.Tone tone) {
        player.setDeltaMovement(x, y, z);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.HOSTILE, tone.volume(), tone.pitch());
    }

    private static void snatch(ServerPlayer player) {
        ItemStack held = player.getMainHandItem().copy();
        player.getInventory().setItem(player.getInventory().getSelectedSlot(), ItemStack.EMPTY);
        double angle = player.getRandom().nextDouble() * Math.PI * 2;
        Config.Section config = Config.event("snatch");
        Vec3 velocity = new Vec3(Math.cos(angle) * config.num("speed"), config.num("lift"), Math.sin(angle) * config.num("speed"));
        ItemEntity item = new ItemEntity(player.level(), player.getX(), player.getEyeY(), player.getZ(), held, velocity.x, velocity.y, velocity.z);
        item.setDefaultPickUpDelay();
        player.level().addFreshEntity(item);
        Config.Tone tone = config.tone("sound");
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ITEM_PICKUP, SoundSource.HOSTILE, tone.volume(), tone.pitch());
    }

    private static boolean dark(ServerPlayer player) {
        return player.level().getMaxLocalRawBrightness(player.blockPosition()) < Config.event("suffocation").integer("dark_below");
    }

    /** A heartbeat first, then the air drains until the player finds light; out of water nothing else would hurt them. */
    private static void suffocate(ServerPlayer player) {
        Config.Section config = Config.event("suffocation");
        Config.Tone start = config.tone("sound");
        Config.Tone pulse = config.tone("pulse_sound");
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, start.volume(), start.pitch());
        Scheduler.repeat(player, config.integer("delay_ticks"), config.integer("interval_ticks"), (p, n) -> {
            if (!dark(p) || p.isInWater() || p.getHealth() <= safeHealth() || n >= config.integer("max_steps")) {
                return false;
            }
            p.setAirSupply(Math.max(0, p.getAirSupply() - config.integer("drain")));
            if (n % config.integer("pulse_every") == 0) {
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, pulse.volume(), pulse.pitch());
                if (p.getAirSupply() == 0) {
                    p.hurtServer(p.level(), p.damageSources().drown(), (float) config.num("damage"));
                }
            }
            return true;
        });
    }

    /** Three knocks, a bolt that misses, and a bolt for whoever has not moved since. */
    private static void thunderKnocks(ServerPlayer player) {
        Sounds.knocks(player, Sounds.knockSpot(player));
        Config.Section config = Config.event("thunder_knocks");
        double still = config.num("still_blocks");
        Scheduler.later(player, config.integer("strike_delay_ticks"), p -> {
            Vec3 stood = p.position();
            strike(p.level(), BlockPos.containing(Nearby.randomAround(p, config.num("miss_distance"))));
            Scheduler.later(p, config.integer("hit_delay_ticks"), q -> {
                if (q.position().distanceToSqr(stood) < still * still && q.getHealth() > safeHealth()) {
                    strike(q.level(), q.blockPosition());
                    q.hurtServer(q.level(), q.damageSources().lightningBolt(), Math.min((float) config.num("damage"), q.getHealth() - (float) config.num("keep_health")));
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

    /** A hit every so often, a little harder each time, until the player is far enough from where it began. */
    private static void embrace(ServerPlayer player) {
        Vec3 origin = player.position();
        Config.Section config = Config.event("embrace");
        Config.Tone start = config.tone("sound");
        Config.Tone hit = config.tone("hit_sound");
        Config.Burst souls = config.burst("particles");
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_NEARBY_CLOSE, SoundSource.HOSTILE, start.volume(), start.pitch());
        double escape = config.num("escape_blocks");
        Scheduler.repeat(player, config.integer("delay_ticks"), config.integer("interval_ticks"), (p, n) -> {
            if (p.position().distanceToSqr(origin) >= escape * escape || p.getHealth() <= safeHealth() || n >= config.integer("max_steps")) {
                return false;
            }
            p.level().sendParticles(ParticleTypes.SOUL, p.getX(), p.getY() + souls.offsetY(), p.getZ(), souls.count(), souls.dx(), souls.dy(), souls.dz(), souls.speed());
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE, hit.volume(), hit.pitch());
            p.hurtServer(p.level(), p.damageSources().magic(), Math.min((float) config.num("damage_step") * (n + 1), p.getHealth() - safeHealth()));
            return true;
        });
    }
}
