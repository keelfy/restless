package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.event.kit.Figure;
import dev.keelfy.restless.haunt.event.kit.Gait;
import dev.keelfy.restless.haunt.event.kit.Place;
import dev.keelfy.restless.haunt.harm.Band;
import dev.keelfy.restless.state.Session;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.SwingAnimation;
import org.jspecify.annotations.Nullable;

/**
 * A mannequin wearing the player's skin and gear walks up to the player and hits hard. It is the vanilla mannequin
 * entity moved by hand, so nothing is registered and no client code is needed. It hits only so far as its band allows
 * (the plain {@code double} is a threat; a lethal variant kills) and leaves on its own after a while. It cannot be
 * hurt, unless the event's config sets {@code banish_hits}: then that many blows from the player send it away.
 */
public final class DoubleEvent implements HauntEvent {
    private static final EquipmentSlot[] WORN = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND
    };

    private static final double DECOY_HEALTH = 200.0;

    /** Marks a ghost made by {@link #decoy}. */
    private static final String GHOST = "restless_ghost";

    /** The double each player has now; one at a time. */
    private static final Map<UUID, Mannequin> ACTIVE = Session.SERVER.keep(new ConcurrentHashMap<>());

    private final Identifier id;
    private final Band band;

    public DoubleEvent() {
        this(Identifier.parse("double"));
    }

    /** A double whose behaviour and band come from the config of event {@code id}. */
    DoubleEvent(Identifier id) {
        this.id = id;
        this.band = Band.of(id.toString());
    }

    @Override
    public Identifier id() {
        return id;
    }

    @Override
    public HauntTier tier() {
        return HauntTier.HARMFUL;
    }

    @Override
    public Set<EventTag> tags() {
        return Set.of(EventTag.MOBS);
    }

    @Override
    public boolean canRun(ServerPlayer player) {
        return player.level().getDifficulty() != Difficulty.PEACEFUL
                && !ACTIVE.containsKey(player.getUUID())
                && Assaults.healthy(player, id.toString())
                && spot(player) != null;
    }

    @Override
    public void run(ServerPlayer player) {
        BlockPos spot = spot(player);
        if (spot == null) {
            return;
        }
        Config.Section config = Config.event(id.toString());
        ServerLevel level = player.level();
        Mannequin twin = Figure.spawn(player, spot, ResolvableProfile.createResolved(player.getGameProfile()));
        if (twin == null) {
            return;
        }
        // What the death screen names it: nobody the player knows.
        twin.setCustomName(Component.literal("???"));
        for (EquipmentSlot slot : WORN) {
            twin.setItemSlot(slot, player.getItemBySlot(slot).copy());
        }
        if (config.has("banish_hits")) {
            twin.setPermanentlyInvulnerable(false);
            twin.getAttribute(Attributes.MAX_HEALTH).setBaseValue(config.num("health"));
            twin.setHealth(twin.getMaxHealth());
        }
        UUID owner = player.getUUID();
        ACTIVE.put(owner, twin);
        play(player, SoundEvents.ENDERMAN_STARE, config.tone("sound"));
        // The chase ends itself; this catches the cases where the scheduler drops it (the player died or left).
        Restore.after(level, config.integer("max_ticks") + config.integer("cleanup_ticks"), force -> {
            vanish(twin, owner, config);
            return true;
        });
        Scheduler.repeat(
                player, 0, config.integer("interval_ticks"), new Chase(twin, owner, id.toString(), band)::step);
    }

    /**
     * A harmless ghost of the player in their skin, standing where they do, gone after {@code ticks}. It is the mimic
     * charm's, not a spirit's: it does not walk or hit.
     */
    public static @Nullable Mannequin decoy(ServerPlayer player, int ticks) {
        return decoy(player, ticks, player.blockPosition(), false);
    }

    /**
     * The same ghost standing at {@code at}, holding the player's main-hand item if {@code armed}. It is invisible to a
     * client that does not know the mod and drawn translucent by one that does, wears no armor (armor would stay solid
     * on a ghost), has no body to push or be pushed, and only mobs can hurt it.
     */
    public static @Nullable Mannequin decoy(ServerPlayer player, int ticks, BlockPos at, boolean armed) {
        ServerLevel level = player.level();
        Mannequin twin = Figure.spawn(player, at, ResolvableProfile.createResolved(player.getGameProfile()));
        if (twin == null) {
            return null;
        }
        if (armed) {
            twin.setItemSlot(EquipmentSlot.MAINHAND, player.getMainHandItem().copy());
        }
        twin.addTag(GHOST);
        twin.setInvisible(true);
        twin.noPhysics = true;
        // A hostile takes only what it can hurt as a target, so the copy bleeds, but far longer than it stays.
        twin.setPermanentlyInvulnerable(false);
        twin.getAttribute(Attributes.MAX_HEALTH).setBaseValue(DECOY_HEALTH);
        twin.setHealth(twin.getMaxHealth());
        mist(level, twin, ParticleTypes.SCULK_SOUL, 16);
        Scheduler.repeat(player, 0, 3, (p, n) -> {
            if (!twin.isAlive()) {
                return false;
            }
            mist(level, twin, ParticleTypes.SOUL, 2);
            if (n % 3 == 0) {
                mist(level, twin, ParticleTypes.WHITE_ASH, 3);
            }
            return true;
        });
        Restore.after(level, ticks, force -> {
            if (twin.isAlive()) {
                mist(level, twin, ParticleTypes.SCULK_SOUL, 24);
                twin.discard();
            }
            return true;
        });
        return twin;
    }

    /** Whether {@code victim} is a ghost the player cannot strike. */
    public static boolean spared(LivingEntity victim, DamageSource source) {
        return victim instanceof Mannequin
                && victim.entityTags().contains(GHOST)
                && source.getEntity() instanceof Player;
    }

    private static void mist(ServerLevel level, Mannequin twin, ParticleOptions particle, int count) {
        level.sendParticles(particle, twin.getX(), twin.getY() + 1.0, twin.getZ(), count, 0.25, 0.6, 0.25, 0.01);
    }

    /** Sends away the player's double, if there is one: the player died, so there is nothing left to chase. */
    public static void dismiss(ServerPlayer player) {
        Mannequin twin = ACTIVE.get(player.getUUID());
        if (twin != null) {
            vanish(twin, player.getUUID(), null);
        }
    }

    private @Nullable BlockPos spot(ServerPlayer player) {
        Config.Section config = Config.event(id.toString());
        return Place.spawnSpot(player, config.integer("min_distance"), config.integer("max_distance"));
    }

    private static void vanish(Mannequin twin, UUID owner, Config.@Nullable Section config) {
        if (twin.isAlive()) {
            if (config != null) {
                smoke(twin, config.burst("particles"));
            }
            twin.discard();
        }
        ACTIVE.remove(owner, twin);
    }

    private static void smoke(Mannequin twin, Config.Burst smoke) {
        ((ServerLevel) twin.level())
                .sendParticles(
                        ParticleTypes.LARGE_SMOKE,
                        twin.getX(),
                        twin.getY() + smoke.offsetY(),
                        twin.getZ(),
                        smoke.count(),
                        smoke.dx(),
                        smoke.dy(),
                        smoke.dz(),
                        smoke.speed());
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

    private static final class Chase {
        private final Mannequin twin;
        private final UUID owner;
        private int stuck;
        private int lastHit = Integer.MIN_VALUE / 2;
        private int hits;
        private int struck;
        private final String key;
        private final Band band;
        private final Gait gait;

        Chase(Mannequin twin, UUID owner, String key, Band band) {
            this.twin = twin;
            this.owner = owner;
            this.key = key;
            this.band = band;
            Config.Section config = Config.event(key);
            this.gait = new Gait(config.integer("climb"), config.integer("drop"));
        }

        /** @return true to keep chasing */
        boolean step(ServerPlayer player, int n) {
            Config.Section config = Config.event(key);
            if (!twin.isAlive()
                    || twin.level() != player.level()
                    || stuck >= config.integer("stuck_steps")
                    || hits >= config.integer("max_hits")
                    || Assaults.room(player, band) <= 0
                    || banished(config)
                    || n * config.integer("interval_ticks") >= config.integer("max_ticks")) {
                vanish(twin, owner, config);
                return false;
            }
            double reach = config.num("reach");
            // Within reach it stops and hits, but lands first.
            if (player.distanceToSqr(twin) <= reach * reach && !gait.airborne()) {
                if (n - lastHit >= config.integer("attack_steps")) {
                    lastHit = n;
                    hits++;
                    strike(player, config);
                }
            } else if (walk(player, config)) {
                stuck = 0;
            } else {
                stuck++;
            }
            Figure.face(twin, player);
            return true;
        }

        /** Whether the player has struck it often enough to send it away; each blow is told by its health dropping. */
        private boolean banished(Config.Section config) {
            if (!config.has("banish_hits") || twin.getHealth() >= twin.getMaxHealth()) {
                return false;
            }
            twin.setHealth(twin.getMaxHealth());
            return ++struck >= config.integer("banish_hits");
        }

        /** A step's worth of ticks toward the player, jumping up and round what is in the way. */
        private boolean walk(ServerPlayer player, Config.Section config) {
            int ticks = config.integer("interval_ticks");
            for (int t = 0; t < ticks; t++) {
                if (!gait.toward(player.level(), twin, player.position(), config.num("speed") / ticks)) {
                    return false;
                }
            }
            return true;
        }

        private void strike(ServerPlayer player, Config.Section config) {
            twin.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
            play(player, SoundEvents.PLAYER_ATTACK_STRONG, config.tone("hit_sound"));
            float room = Assaults.room(player, band);
            player.hurtServer(
                    player.level(),
                    player.damageSources().mobAttack(twin),
                    Math.min((float) config.num("damage"), room));
        }
    }
}
