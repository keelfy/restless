package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Anger;
import dev.keelfy.restless.haunt.Band;
import dev.keelfy.restless.haunt.Bands;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Journal;
import dev.keelfy.restless.haunt.Notebook;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.Ticks;
import dev.keelfy.restless.haunt.Wards;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Assaults that can kill, from the cruelest evolved spirits only (see {@code Bands}). Each warns, and each can be
 * escaped by what it says. Death comes through normal damage, so a totem of undying works.
 */
final class Lethals {
    private static final String REAPING = "restless:reaping";
    private static final String LAST_CALL = "restless:last_call";
    private static final String DROWNER = "restless:drowner";
    private static final String AVENGER = "restless:the_avenger";
    private static final String HELLRING = "restless:hellring";
    private static final String DOUBLE = "restless:the_double";
    private static final String FORETOLD = "restless:foretold";

    /** The player's mob kills when an avenger last came, so a new one needs a fresh killing spree. */
    private static final Map<UUID, Integer> AVENGED = new ConcurrentHashMap<>();

    private static final HauntEvent FORETELLING = SimpleEvent.harmful(
            FORETOLD,
            EventTag.ASSAULT,
            player -> player.level().dimension() == Level.OVERWORLD
                    && player.level().getOverworldClockTime() % Ticks.DAY
                            < Config.event(FORETOLD).integer("latest_time"),
            Lethals::foretold);

    private Lethals() {}

    static List<HauntEvent> all() {
        return List.of(
                SimpleEvent.harmful("restless:judgement", EventTag.ASSAULT, SimpleEvent.ALWAYS, Lethals::judgement),
                // The shadow of the light-eater: it freezes while watched, and its touch kills.
                new ShadowEvent(Identifier.parse(REAPING)),
                SimpleEvent.harmful(
                        "restless:last_call",
                        EventTag.ASSAULT,
                        player -> Assaults.night(player) && Assaults.callTarget(player, LAST_CALL) != null,
                        player -> Assaults.call(player, LAST_CALL)),
                SimpleEvent.harmful("restless:drowner", EventTag.ASSAULT, ServerPlayer::isInWater, Lethals::drowner),
                SimpleEvent.harmful(
                        "restless:the_avenger",
                        EventTag.MOBS,
                        player -> sprees(player) && avengerSpot(player) != null,
                        Lethals::avenger),
                SimpleEvent.harmful("restless:hellring", EventTag.ASSAULT, SimpleEvent.ALWAYS, Lethals::hellring),
                new DoubleEvent(Identifier.parse(DOUBLE)),
                FORETELLING);
    }

    /** Three knocks, a bolt that misses, and a bolt that kills whoever has not moved since. */
    private static void judgement(ServerPlayer player) {
        Sounds.knocks(player, Sounds.knockSpot(player));
        Config.Section config = Config.event("restless:judgement");
        double still = config.num("still_blocks");
        Scheduler.later(player, config.integer("strike_delay_ticks"), p -> {
            Vec3 stood = p.position();
            Assaults.strike(p.level(), BlockPos.containing(Nearby.randomAround(p, config.num("miss_distance"))));
            Scheduler.later(p, config.integer("hit_delay_ticks"), q -> {
                if (q.position().distanceToSqr(stood) < still * still) {
                    Assaults.strike(q.level(), q.blockPosition());
                    q.hurtServer(q.level(), q.damageSources().lightningBolt(), (float) config.num("damage"));
                }
            });
        });
    }

    /** Bubbles and a gurgle, then the player is held down while under water; the air does the rest. */
    private static void drowner(ServerPlayer player) {
        Config.Section config = Config.event(DROWNER);
        Config.Burst bubbles = config.burst("particles");
        Nearby.sound(player, SoundEvents.DROWNED_AMBIENT_WATER, player.position(), config.tone("sound"));
        int interval = config.integer("interval_ticks");
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            int elapsed = n * interval;
            if (!p.isInWater() || elapsed >= config.integer("warn_ticks") + config.integer("max_ticks")) {
                return false;
            }
            burst(p.level(), ParticleTypes.BUBBLE, p.position(), bubbles);
            if (elapsed >= config.integer("warn_ticks") && p.isUnderWater()) {
                Vec3 moving = p.getDeltaMovement();
                p.setDeltaMovement(moving.x, -config.num("pull"), moving.z);
                p.connection.send(new ClientboundSetEntityMotionPacket(p));
            }
            if (n % config.integer("pulse_every") == 0) {
                Nearby.sound(p, SoundEvents.DROWNED_AMBIENT_WATER, p.position(), config.tone("pulse_sound"));
            }
            return true;
        });
    }

    /** A killing spree since the last avenger came: enough fresh mob kills. */
    private static boolean sprees(ServerPlayer player) {
        return kills(player) - AVENGED.getOrDefault(player.getUUID(), 0)
                >= Config.event(AVENGER).integer("min_kills");
    }

    private static int kills(ServerPlayer player) {
        return player.getStats().getValue(Stats.CUSTOM.get(Stats.MOB_KILLS));
    }

    private static BlockPos avengerSpot(ServerPlayer player) {
        Config.Section config = Config.event(AVENGER);
        return Place.spawnSpot(player, config.integer("min_distance"), config.integer("max_distance"));
    }

    /**
     * A named vindicator rises in the distance, still for a few seconds, then hunts the player to the death. Killing it
     * quiets the spirit's anger.
     */
    private static void avenger(ServerPlayer player) {
        BlockPos spot = avengerSpot(player);
        Config.Section config = Config.event(AVENGER);
        ServerLevel level = player.level();
        Mob avenger = spot == null ? null : EntityTypes.VINDICATOR.spawn(level, spot, EntitySpawnReason.EVENT);
        if (avenger == null) {
            return;
        }
        AVENGED.put(player.getUUID(), kills(player));
        avenger.setCustomName(Component.literal(Figure.nickname(player)));
        avenger.setCustomNameVisible(true);
        avenger.getAttribute(Attributes.MAX_HEALTH).setBaseValue(config.num("health"));
        avenger.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(config.num("damage"));
        avenger.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(config.num("speed"));
        avenger.setHealth(avenger.getMaxHealth());
        avenger.setPersistenceRequired();
        avenger.setNoAi(true);
        Band.LETHAL.mark(avenger);
        Nearby.sound(player, SoundEvents.VINDICATOR_CELEBRATE, avenger.position(), config.tone("sound"));
        burst(level, ParticleTypes.SOUL, avenger.position(), config.burst("particles"));
        int warn = config.integer("warn_ticks");
        int interval = config.integer("interval_ticks");
        Restore.after(level, warn + config.integer("max_ticks") + config.integer("cleanup_ticks"), force -> {
            avenger.discard();
            return true;
        });
        Scheduler.repeat(player, warn, interval, (p, n) -> {
            if (avenger.isDeadOrDying()) {
                Anger.soothe(p, config.num("relief"));
                Nearby.sound(p, SoundEvents.SOUL_ESCAPE.value(), p.position(), config.tone("won_sound"));
                return false;
            }
            if (avenger.isRemoved() || n * interval >= config.integer("max_ticks")) {
                avenger.discard();
                return false;
            }
            avenger.setNoAi(false);
            avenger.setTarget(p);
            return true;
        });
    }

    /** A roar and a ring of fire that closes in; whoever is still inside it when it shuts is burned down. */
    private static void hellring(ServerPlayer player) {
        Config.Section config = Config.event(HELLRING);
        Vec3 origin = player.position();
        double radius = config.num("ring_radius");
        Nearby.sound(player, SoundEvents.GHAST_WARN, origin, config.tone("sound"));
        int interval = config.integer("interval_ticks");
        int points = config.integer("ring_points");
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            int elapsed = n * interval;
            int total = config.integer("ring_ticks");
            if (elapsed >= total) {
                if (p.position().distanceToSqr(origin) <= radius * radius) {
                    p.igniteForSeconds((float) config.num("burn_seconds"));
                    p.hurtServer(p.level(), Taken.by(p.level(), Taken.TAKEN), (float) config.num("damage"));
                }
                return false;
            }
            double closing = radius * (1.0 - (double) elapsed / total);
            for (int i = 0; i < points; i++) {
                double angle = i * Math.PI * 2 / points;
                for (double r : new double[] {radius, closing}) {
                    p.level()
                            .sendParticles(
                                    ParticleTypes.FLAME,
                                    origin.x + Math.cos(angle) * r,
                                    origin.y + config.num("flame_height"),
                                    origin.z + Math.sin(angle) * r,
                                    1,
                                    0.0,
                                    0.2,
                                    0.0,
                                    0.01);
                }
            }
            if (n % config.integer("pulse_every") == 0) {
                Nearby.sound(p, SoundEvents.FIRE_AMBIENT, p.position(), config.tone("pulse_sound"));
            }
            return true;
        });
    }

    /**
     * A bell and an omen in the journal: before the night is a few hours old the spirit will take the player, unless
     * they are in a bed or inside a ward by then. The bell tolls as the hour nears.
     */
    private static void foretold(ServerPlayer player) {
        Config.Section config = Config.event(FORETOLD);
        List<String> omens = config.texts("omens");
        String omen = omens.get(player.getRandom().nextInt(omens.size()));
        Nearby.sound(player, SoundEvents.BELL_BLOCK, player.position(), config.tone("sound"));
        player.sendSystemMessage(Component.literal(omen), true);
        Journal.note(player, Notebook.Kind.DREAM, null, omen);
        int check = config.integer("check_ticks");
        Scheduler.repeat(player, check, check, (p, n) -> {
            if (p.isSleeping() || n >= config.integer("max_steps")) {
                return false;
            }
            long now = p.level().getOverworldClockTime() % Ticks.DAY;
            long due = config.integer("death_time");
            if (now < due - config.integer("toll_lead_ticks")) {
                return true;
            }
            if (now < due) {
                if (n % config.integer("toll_every") == 0) {
                    Nearby.sound(p, SoundEvents.BELL_BLOCK, p.position(), config.tone("toll_sound"));
                }
                return true;
            }
            if (Wards.stops(p, FORETELLING)) {
                Wards.blocked(p);
            } else {
                Nearby.sound(p, SoundEvents.BELL_RESONATE, p.position(), config.tone("toll_sound"));
                Bands.struck(p);
                p.hurtServer(p.level(), Taken.by(p.level(), Taken.TAKEN), (float) config.num("damage"));
            }
            return false;
        });
    }

    private static void burst(ServerLevel level, ParticleOptions particle, Vec3 at, Config.Burst burst) {
        level.sendParticles(
                particle,
                at.x,
                at.y + burst.offsetY(),
                at.z,
                burst.count(),
                burst.dx(),
                burst.dy(),
                burst.dz(),
                burst.speed());
    }
}
