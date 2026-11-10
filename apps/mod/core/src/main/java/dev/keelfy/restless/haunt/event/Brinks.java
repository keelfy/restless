package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.harm.Band;
import dev.keelfy.restless.state.HauntData;
import dev.keelfy.restless.ward.Wards;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Assaults that can take the player down to {@code assaults.brink_health}. Each warns first and has a way out; none can
 * kill. They run only for a strong spirit (see {@code Bands}). Every hit, every loop that keeps hurting, and every mob
 * or arrow they make is held to {@link Band#BRINK}.
 */
final class Brinks {
    private static final String HANGING = "restless:the_hanging";
    private static final String HELD_UNDER = "restless:held_under";
    private static final String PYRE = "restless:pyre";
    private static final String WITHERING = "restless:withering";
    private static final String COUNT = "restless:count";
    private static final String HOLLOW = "restless:the_hollow";
    private static final String LEDGE_PUSH = "restless:ledge_push";
    private static final String KNIVES = "restless:circle_of_knives";
    private static final String SMOTHER = "restless:smother_brink";
    private static final String EMBRACE = "restless:embrace_brink";
    private static final String RED_ROOM = "restless:red_room";
    private static final String DRAIN = "restless:drain";
    private static final String BOUND = "restless:bound";

    private Brinks() {}

    static List<HauntEvent> all() {
        return List.of(
                SimpleEvent.harmful(
                        "restless:final_blow",
                        EventTag.ASSAULT,
                        player -> player.getHealth()
                                > Config.event("restless:final_blow").num("min_health"),
                        Brinks::finalBlow),
                SimpleEvent.harmful(
                        HANGING,
                        EventTag.ASSAULT,
                        healthy(HANGING)
                                .and(player -> player.onGround()
                                        && !player.isInWater()
                                        && !player.hasEffect(MobEffects.SLOW_FALLING)),
                        Brinks::hang),
                SimpleEvent.harmful(
                        HELD_UNDER,
                        EventTag.ASSAULT,
                        healthy(HELD_UNDER)
                                .and(player -> player.isUnderWater() && !player.hasEffect(MobEffects.WATER_BREATHING)),
                        Brinks::heldUnder),
                SimpleEvent.harmful(
                        PYRE,
                        EventTag.ASSAULT,
                        healthy(PYRE).and(player -> !player.isInWater() && !player.fireImmune()),
                        Brinks::pyre),
                SimpleEvent.harmful(
                        WITHERING,
                        EventTag.ASSAULT,
                        healthy(WITHERING).and(player -> !player.hasEffect(MobEffects.WITHER)),
                        Brinks::wither),
                SimpleEvent.harmful(
                        COUNT, EventTag.ASSAULT, healthy(COUNT).and(player -> !lit(player)), Brinks::countdown),
                SimpleEvent.harmful(
                        HOLLOW,
                        EventTag.ASSAULT,
                        healthy(HOLLOW).and(player -> hostileSpot(player, HOLLOW) != null),
                        Brinks::hollow),
                SimpleEvent.harmful(
                        LEDGE_PUSH,
                        EventTag.ASSAULT,
                        healthy(LEDGE_PUSH).and(player -> Place.ledge(player) != null),
                        Brinks::ledgePush),
                SimpleEvent.harmful(KNIVES, EventTag.ASSAULT, healthy(KNIVES), Brinks::knives),
                SimpleEvent.harmful(
                        SMOTHER,
                        EventTag.ASSAULT,
                        healthy(SMOTHER).and(player -> !player.isInWater() && Assaults.dark(player, SMOTHER)),
                        player -> Assaults.suffocate(player, SMOTHER, Band.BRINK)),
                SimpleEvent.harmful(
                        EMBRACE,
                        EventTag.ASSAULT,
                        healthy(EMBRACE),
                        player -> Assaults.embrace(player, EMBRACE, Band.BRINK)),
                SimpleEvent.harmful(
                        RED_ROOM,
                        EventTag.ASSAULT,
                        healthy(RED_ROOM).and(player -> figureSpot(player) != null),
                        Brinks::redRoom),
                SimpleEvent.harmful(DRAIN, EventTag.ASSAULT, healthy(DRAIN), Brinks::drain),
                SimpleEvent.harmful(
                        BOUND,
                        EventTag.ASSAULT,
                        healthy(BOUND).and(player -> hostileSpot(player, BOUND) != null),
                        Brinks::bound));
    }

    /** Enough health above the brink for the event to be worth starting. */
    private static Predicate<ServerPlayer> healthy(String id) {
        return player -> player.getHealth() > Config.event(id).num("min_health");
    }

    private static void play(ServerPlayer player, SoundEvent sound, Config.Tone tone) {
        player.level()
                .playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        sound,
                        SoundSource.HOSTILE,
                        tone.volume(),
                        tone.pitch());
    }

    private static void burst(ServerPlayer player, ParticleOptions type, Config.Burst b) {
        player.level()
                .sendParticles(
                        type,
                        player.getX(),
                        player.getY() + b.offsetY(),
                        player.getZ(),
                        b.count(),
                        b.dx(),
                        b.dy(),
                        b.dz(),
                        b.speed());
    }

    /** Hurts by at most what the brink still allows; nothing if the player is already there. */
    private static void hurt(ServerPlayer player, DamageSource source, double wanted) {
        float amount = Math.min((float) wanted, Assaults.room(player, Band.BRINK));
        if (amount > 0) {
            player.hurtServer(player.level(), source, amount);
        }
    }

    private static void hurt(ServerPlayer player, double wanted) {
        hurt(player, Taken.by(player.level(), Taken.TAKEN), wanted);
    }

    private static void send(ServerPlayer player, Vec3 motion) {
        player.setDeltaMovement(motion);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    /** Fall distance past the safe one that the brink still allows. */
    private static double fallRoom(ServerPlayer player) {
        return Math.floor(
                Assaults.room(player, Band.BRINK) / player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER));
    }

    /**
     * Follows a fall the spirit began. The fall distance is held so the landing cannot go under the brink; a player who
     * really lands after a drop is taken down to it. Water, climbing, slow falling or flight end it without harm.
     */
    private static void settle(ServerPlayer player, Config.Section config) {
        double[] apex = {player.getY()};
        Scheduler.repeat(player, 0, 1, (p, n) -> {
            apex[0] = Math.max(apex[0], p.getY());
            double safe = p.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
            if (p.isInWater()
                    || p.isInLava()
                    || p.onClimbable()
                    || p.hasEffect(MobEffects.SLOW_FALLING)
                    || p.getAbilities().flying
                    || n >= config.integer("max_ticks")) {
                return false;
            }
            if (n >= config.integer("grace_ticks") && p.onGround()) {
                if (apex[0] - p.getY() > safe) {
                    play(p, SoundEvents.PLAYER_BIG_FALL, config.tone("hit_sound"));
                    hurt(p, Float.MAX_VALUE);
                }
                return false;
            }
            double limit = Math.max(0.0, safe + fallRoom(p) - config.num("fall_step"));
            if (p.fallDistance > limit) {
                p.fallDistance = limit;
            }
            return true;
        });
    }

    /** Lifted off the ground until a fall from there would leave half a heart, then let go. */
    private static void hang(ServerPlayer player) {
        Config.Section config = Config.event(HANGING);
        play(player, SoundEvents.WARDEN_NEARBY_CLOSER, config.tone("sound"));
        double from = player.getY();
        int warn = config.integer("warn_ticks");
        Scheduler.repeat(player, 0, 1, (p, n) -> {
            if (n < warn) {
                burst(p, ParticleTypes.SOUL, config.burst("particles"));
                return true;
            }
            double height = Math.min(
                    config.num("max_height"), p.getAttributeValue(Attributes.SAFE_FALL_DISTANCE) + fallRoom(p));
            if (p.getY() - from >= height || n - warn >= config.integer("rise_ticks") || p.isInWater()) {
                p.removeEffect(MobEffects.LEVITATION);
                settle(p, config);
                return false;
            }
            p.addEffect(
                    new MobEffectInstance(
                            MobEffects.LEVITATION, config.integer("effect_ticks"), config.integer("amplifier")),
                    null);
            return true;
        });
    }

    /** A shove at an edge, unless the player crouches; whoever goes over is taken down to half a heart by the fall. */
    private static void ledgePush(ServerPlayer player) {
        Config.Section config = Config.event(LEDGE_PUSH);
        play(player, SoundEvents.WARDEN_NEARBY_CLOSE, config.tone("sound"));
        int interval = config.integer("interval_ticks");
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            if (n * interval < config.integer("warn_ticks")) {
                burst(p, ParticleTypes.SOUL, config.burst("particles"));
                return true;
            }
            Direction toward = Place.ledge(p);
            if (toward != null && !p.isShiftKeyDown()) {
                double push = config.num("push");
                send(p, new Vec3(toward.getStepX() * push, config.num("lift"), toward.getStepZ() * push));
                play(p, SoundEvents.PLAYER_ATTACK_KNOCKBACK, config.tone("push_sound"));
                settle(p, config);
            }
            return false;
        });
    }

    /**
     * Bubbles and a heartbeat, then the water pulls down and the air runs short; sideways out of the column is free.
     */
    private static void heldUnder(ServerPlayer player) {
        Config.Section config = Config.event(HELD_UNDER);
        Vec3 origin = player.position();
        double escape = config.num("escape_blocks");
        int interval = config.integer("interval_ticks");
        int begin = config.integer("warn_ticks") / interval;
        play(player, SoundEvents.WARDEN_HEARTBEAT, config.tone("sound"));
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            double dx = p.getX() - origin.x;
            double dz = p.getZ() - origin.z;
            if (!p.isUnderWater()
                    || dx * dx + dz * dz >= escape * escape
                    || p.hasEffect(MobEffects.WATER_BREATHING)
                    || n >= begin + config.integer("max_ticks") / interval) {
                return false;
            }
            if (n < begin) {
                burst(p, ParticleTypes.BUBBLE, config.burst("particles"));
                return true;
            }
            if (n == begin) {
                p.setAirSupply(Math.min(p.getAirSupply(), config.integer("start_air")));
            }
            Vec3 motion = p.getDeltaMovement();
            send(p, new Vec3(motion.x, Math.min(motion.y, -config.num("pull")), motion.z));
            // Drowning hurts on its own; once the next bout would pass the brink, the air is held at empty instead.
            if (p.getAirSupply() <= 0 && Assaults.room(p, Band.BRINK) < config.num("drown_damage")) {
                p.setAirSupply(0);
            }
            return true;
        });
    }

    /** Flames that water alone puts out; they are put out here before they would go under the brink. */
    private static void pyre(ServerPlayer player) {
        Config.Section config = Config.event(PYRE);
        int interval = config.integer("interval_ticks");
        int begin = config.integer("warn_ticks") / interval;
        play(player, SoundEvents.BLAZE_AMBIENT, config.tone("sound"));
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            if (n < begin) {
                burst(p, ParticleTypes.FLAME, config.burst("particles"));
                return true;
            }
            if (Assaults.room(p, Band.BRINK) < config.num("fire_damage")) {
                p.clearFire();
                return false;
            }
            if (n == begin ? p.isInWater() : !p.isOnFire()) {
                return false;
            }
            if (n - begin < config.integer("burn_ticks") / interval) {
                p.removeEffect(MobEffects.FIRE_RESISTANCE);
                p.setRemainingFireTicks(Math.max(p.getRemainingFireTicks(), config.integer("keep_ticks")));
            }
            return true;
        });
    }

    /** A wither that goes on until the player drinks milk, or until the next tick of it would pass the brink. */
    private static void wither(ServerPlayer player) {
        Config.Section config = Config.event(WITHERING);
        int interval = config.integer("interval_ticks");
        int begin = config.integer("warn_ticks") / interval;
        play(player, SoundEvents.WITHER_AMBIENT, config.tone("sound"));
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            if (n < begin) {
                burst(p, ParticleTypes.SMOKE, config.burst("particles"));
                return true;
            }
            if (n == begin) {
                p.addEffect(
                        new MobEffectInstance(
                                MobEffects.WITHER, config.integer("duration_ticks"), config.integer("amplifier")),
                        null);
                return true;
            }
            if (Assaults.room(p, Band.BRINK) < config.num("wither_damage")) {
                p.removeEffect(MobEffects.WITHER);
                return false;
            }
            return p.hasEffect(MobEffects.WITHER);
        });
    }

    /** Light, or a ring of salt, is all that stops the count. */
    private static boolean lit(ServerPlayer player) {
        return player.level().getMaxLocalRawBrightness(player.blockPosition())
                        >= Config.event(COUNT).integer("light_safe")
                || Wards.shelters(player);
    }

    /** "3", "2", "1" on the screen with a bell; whoever is still in the dark then is struck. */
    private static void countdown(ServerPlayer player) {
        Config.Section config = Config.event(COUNT);
        Scheduler.repeat(player, 0, config.integer("step_ticks"), (p, n) -> {
            int from = config.integer("count_from");
            if (n < from) {
                p.sendSystemMessage(Component.literal((from - n) + "\u2026"), true);
                play(p, SoundEvents.BELL_BLOCK, config.tone("sound"));
                return true;
            }
            if (!lit(p)) {
                play(p, SoundEvents.WARDEN_ATTACK_IMPACT, config.tone("hit_sound"));
                hurt(p, config.num("damage"));
            }
            return false;
        });
    }

    private static @Nullable BlockPos hostileSpot(ServerPlayer player, String id) {
        Config.Section config = Config.event(id);
        return player.level().getDifficulty() == Difficulty.PEACEFUL
                ? null
                : Place.spawnSpot(player, config.integer("min_distance"), config.integer("max_distance"));
    }

    /** A hostile of the event's {@code mobs}, bound to the spirit: its blows are held to the brink. */
    private static @Nullable Mob spawnHostile(ServerPlayer player, String id) {
        Config.Section config = Config.event(id);
        BlockPos spot = hostileSpot(player, id);
        List<String> mobs = config.texts("mobs");
        if (spot == null || mobs.isEmpty()) {
            return null;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE
                .getOptional(Identifier.parse(mobs.get(player.getRandom().nextInt(mobs.size()))))
                .orElse(EntityTypes.ZOMBIE);
        if (!(type.spawn(player.level(), spot, EntitySpawnReason.EVENT) instanceof Mob mob)) {
            return null;
        }
        Band.BRINK.mark(mob);
        mob.setPersistenceRequired();
        mob.setTarget(player);
        // The loops that watch the mob end on their own; this catches a player who left, and a clean shutdown.
        Restore.after(player.level(), config.integer("max_ticks") + config.integer("cleanup_ticks"), force -> {
            vanish(mob, id);
            return true;
        });
        return mob;
    }

    private static void vanish(Mob mob, String id) {
        if (mob.isAlive()) {
            Config.Burst smoke = Config.event(id).burst("particles");
            ((ServerLevel) mob.level())
                    .sendParticles(
                            ParticleTypes.LARGE_SMOKE,
                            mob.getX(),
                            mob.getY() + smoke.offsetY(),
                            mob.getZ(),
                            smoke.count(),
                            smoke.dx(),
                            smoke.dy(),
                            smoke.dz(),
                            smoke.speed());
            mob.discard();
        }
    }

    /** A named creature of the spirit's hunts the player; killing it eases the spirit's anger. */
    private static void hollow(ServerPlayer player) {
        Config.Section config = Config.event(HOLLOW);
        play(player, SoundEvents.WARDEN_NEARBY_CLOSER, config.tone("sound"));
        Scheduler.later(player, config.integer("warn_ticks"), p -> {
            Mob hollow = spawnHostile(p, HOLLOW);
            if (hollow == null) {
                return;
            }
            hollow.setCustomName(Component.literal(config.text("name")));
            hollow.setCustomNameVisible(true);
            int interval = config.integer("interval_ticks");
            Scheduler.repeat(p, 0, interval, (q, n) -> {
                if (hollow.isDeadOrDying()) {
                    if (hollow.getKillCredit() == q) {
                        soothe(q, config.num("relief"));
                    }
                    return false;
                }
                if (!hollow.isAlive() || hollow.level() != q.level() || n * interval >= config.integer("max_ticks")) {
                    vanish(hollow, HOLLOW);
                    return false;
                }
                hollow.setTarget(q);
                return true;
            });
        });
    }

    /** Lowers the anger of the player's spirits, never below none. */
    private static void soothe(ServerPlayer player, double by) {
        ServerLevel level = player.level();
        HauntData data = HauntData.get(level.getServer());
        List<HauntData.Haunting> own = new ArrayList<>(
                Director.hauntings(player, data, level.getServer().overworld().getGameTime()));
        own.replaceAll(h -> h.angered(-Math.min(by, h.anger())));
        data.put(player.getUUID(), own);
    }

    private static Vec3 ring(Vec3 origin, double radius, double height, int i, int points) {
        double angle = i * Math.PI * 2 / points;
        return origin.add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
    }

    /** A ring of arrows shows in the air, then closes in on where the player stood. */
    private static void knives(ServerPlayer player) {
        Config.Section config = Config.event(KNIVES);
        Vec3 origin = player.position();
        double radius = config.num("ring_radius");
        int points = config.integer("ring_points");
        int interval = config.integer("interval_ticks");
        play(player, SoundEvents.CROSSBOW_LOADING_END.value(), config.tone("sound"));
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            if (n * interval < config.integer("warn_ticks")) {
                for (int i = 0; i < points; i++) {
                    Vec3 at = ring(origin, radius, config.num("ring_height"), i, points);
                    p.level().sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
                }
                return true;
            }
            ServerLevel level = p.level();
            Vec3 target = origin.add(0.0, config.num("aim_height"), 0.0);
            List<Arrow> arrows = new ArrayList<>();
            for (int i = 0; i < points; i++) {
                Vec3 from = ring(origin, radius, config.num("ring_height"), i, points);
                Vec3 aim = target.subtract(from);
                ItemStack ammo = new ItemStack(Items.ARROW);
                Arrow arrow = Projectile.spawnProjectileUsingShoot(
                        new Arrow(level, from.x, from.y, from.z, ammo, null),
                        level,
                        ammo,
                        aim.x,
                        aim.y,
                        aim.z,
                        (float) config.num("speed"),
                        0.0F);
                arrow.setNoGravity(true);
                arrow.setBaseDamage(config.num("base_damage"));
                arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                Band.BRINK.mark(arrow);
                arrows.add(arrow);
            }
            Restore.after(level, config.integer("life_ticks"), force -> {
                arrows.forEach(Entity::discard);
                return true;
            });
            return false;
        });
    }

    private static @Nullable BlockPos figureSpot(ServerPlayer player) {
        Config.Section config = Config.event(RED_ROOM);
        return Place.standingAhead(player, config.num("distance"), config.num("min_angle"), config.num("max_angle"));
    }

    /** Darkness and a heartbeat with a figure in sight; each second it is looked at, it strikes. */
    private static void redRoom(ServerPlayer player) {
        Config.Section config = Config.event(RED_ROOM);
        BlockPos spot = figureSpot(player);
        if (spot == null) {
            return;
        }
        Vec3 at = Vec3.atBottomCenterOf(spot);
        PacketEntity figure = PacketEntity.show(
                player, EntityTypes.WITHER_SKELETON, at, PacketEntity.yawToward(at, player.position()));
        int warn = config.integer("warn_ticks");
        int interval = config.integer("interval_ticks");
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, warn + config.integer("duration_ticks"), 0), null);
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            int elapsed = n * interval;
            if (elapsed >= warn + config.integer("duration_ticks")) {
                figure.remove(p);
                return false;
            }
            if (elapsed % config.integer("beat_ticks") == 0) {
                play(p, SoundEvents.WARDEN_HEARTBEAT, config.tone("sound"));
            }
            Vec3 toFigure = at.add(0.0, p.getEyeHeight(), 0.0)
                    .subtract(p.getEyePosition())
                    .normalize();
            if (elapsed >= warn
                    && elapsed % config.integer("hit_ticks") == 0
                    && p.getLookAngle().dot(toFigure) >= Math.cos(Math.toRadians(config.num("watch_degrees")))) {
                play(p, SoundEvents.WARDEN_ATTACK_IMPACT, config.tone("hit_sound"));
                hurt(p, config.num("damage"));
            }
            return true;
        });
    }

    /** Health leaves a heart a second until the player eats. */
    private static void drain(ServerPlayer player) {
        Config.Section config = Config.event(DRAIN);
        int interval = config.integer("interval_ticks");
        int begin = config.integer("warn_ticks") / interval;
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            if (n < begin) {
                play(p, SoundEvents.WARDEN_HEARTBEAT, config.tone("sound"));
                return true;
            }
            if (n == begin) {
                p.getFoodData().setFoodLevel(Math.min(p.getFoodData().getFoodLevel(), config.integer("low_food")));
                p.getFoodData().setSaturation(0.0F);
                return true;
            }
            if (p.getFoodData().getFoodLevel() > config.integer("low_food")
                    || n - begin > config.integer("max_steps")
                    || Assaults.room(p, Band.BRINK) <= 0) {
                return false;
            }
            burst(p, ParticleTypes.DAMAGE_INDICATOR, config.burst("particles"));
            play(p, SoundEvents.PLAYER_HURT_FREEZE, config.tone("hit_sound"));
            hurt(p, config.num("damage"));
            return true;
        });
    }

    /** Chains ring and the player is held in place while a hostile is dragged to them. */
    private static void bound(ServerPlayer player) {
        Config.Section config = Config.event(BOUND);
        int interval = config.integer("interval_ticks");
        int begin = config.integer("warn_ticks") / interval;
        play(player, SoundEvents.CHAIN_BREAK, config.tone("sound"));
        Mob[] hostile = new Mob[1];
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            if (n < begin) {
                burst(p, ParticleTypes.SOUL, config.burst("particles"));
                return true;
            }
            if (n == begin) {
                hostile[0] = spawnHostile(p, BOUND);
            }
            Mob mob = hostile[0];
            if (mob == null
                    || !mob.isAlive()
                    || mob.level() != p.level()
                    || (n - begin) * interval >= config.integer("max_ticks")) {
                if (mob != null) {
                    vanish(mob, BOUND);
                }
                p.removeEffect(MobEffects.SLOWNESS);
                return false;
            }
            p.addEffect(
                    new MobEffectInstance(
                            MobEffects.SLOWNESS, config.integer("effect_ticks"), config.integer("amplifier")),
                    null);
            mob.setTarget(p);
            Vec3 gap = p.position().subtract(mob.position());
            if ((n - begin) < config.integer("pull_steps") && gap.length() > config.num("arrive_distance")) {
                mob.setDeltaMovement(gap.normalize().scale(config.num("pull")));
            }
            return true;
        });
    }

    /** A roar and a ring of souls closing over the ground; whoever is still inside it when it ends is struck. */
    private static void finalBlow(ServerPlayer player) {
        Config.Section config = Config.event("restless:final_blow");
        Vec3 origin = player.position();
        double radius = config.num("ring_radius");
        Config.Tone roar = config.tone("sound");
        player.level()
                .playSound(
                        null,
                        origin.x,
                        origin.y,
                        origin.z,
                        SoundEvents.WARDEN_ROAR,
                        SoundSource.HOSTILE,
                        roar.volume(),
                        roar.pitch());
        int interval = config.integer("interval_ticks");
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            if (n * interval >= config.integer("warn_ticks")) {
                if (p.position().distanceToSqr(origin) <= radius * radius) {
                    Config.Tone hit = config.tone("hit_sound");
                    p.level()
                            .playSound(
                                    null,
                                    p.getX(),
                                    p.getY(),
                                    p.getZ(),
                                    SoundEvents.WARDEN_ATTACK_IMPACT,
                                    SoundSource.HOSTILE,
                                    hit.volume(),
                                    hit.pitch());
                    p.hurtServer(
                            p.level(),
                            Taken.by(p.level(), Taken.TAKEN),
                            Math.min((float) config.num("damage"), Assaults.room(p, Band.BRINK)));
                }
                return false;
            }
            int points = config.integer("ring_points");
            for (int i = 0; i < points; i++) {
                double angle = i * Math.PI * 2 / points;
                p.level()
                        .sendParticles(
                                ParticleTypes.SOUL,
                                origin.x + Math.cos(angle) * radius,
                                origin.y + 0.2,
                                origin.z + Math.sin(angle) * radius,
                                1,
                                0.0,
                                0.1,
                                0.0,
                                0.01);
            }
            return true;
        });
    }
}
