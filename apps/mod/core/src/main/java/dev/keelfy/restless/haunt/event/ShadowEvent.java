package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntTier;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A dark figure that stands far off and walks toward the player only while the player is not looking at it. It is a
 * vanilla wither skeleton with its AI switched off and moved by hand, so a client without special rendering sees it.
 * Touching it hurts, but never below the assault safe health, and it goes away on its own after a while.
 */
public final class ShadowEvent implements HauntEvent {
    /** The shadow each player has now; one at a time. */
    private static final Map<UUID, Mob> ACTIVE = new ConcurrentHashMap<>();

    @Override
    public Identifier id() {
        return Identifier.parse("shadow");
    }

    @Override
    public HauntTier tier() {
        return HauntTier.HARMFUL;
    }

    @Override
    public Set<EventTag> tags() {
        return Set.of(EventTag.ASSAULT);
    }

    @Override
    public boolean canRun(ServerPlayer player) {
        return player.level().getDifficulty() != Difficulty.PEACEFUL
            && !ACTIVE.containsKey(player.getUUID())
            && Assaults.healthy(player, "shadow")
            && spot(player) != null;
    }

    @Override
    public void run(ServerPlayer player) {
        BlockPos spot = spot(player);
        if (spot == null) {
            return;
        }
        Config.Section config = Config.event("shadow");
        ServerLevel level = player.level();
        Mob shadow = EntityTypes.WITHER_SKELETON.spawn(level, spot, EntitySpawnReason.EVENT);
        if (shadow == null) {
            return;
        }
        shadow.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        shadow.setNoAi(true);
        shadow.setNoGravity(true);
        shadow.setSilent(true);
        shadow.setPermanentlyInvulnerable(true);
        shadow.setPersistenceRequired();
        shadow.lookAt(EntityAnchorArgument.Anchor.EYES, player.getEyePosition());
        UUID owner = player.getUUID();
        ACTIVE.put(owner, shadow);
        play(player, SoundEvents.WARDEN_NEARBY_CLOSE, config.tone("sound"));
        // The chase ends itself; this catches the cases where the scheduler drops it (the player died or left).
        Restore.after(level, config.integer("max_ticks") + config.integer("cleanup_ticks"), force -> {
            vanish(shadow, owner);
            return true;
        });
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), new Chase(shadow, owner)::step);
    }

    private static @Nullable BlockPos spot(ServerPlayer player) {
        Config.Section config = Config.event("shadow");
        return Place.spawnSpot(player, config.integer("min_distance"), config.integer("max_distance"));
    }

    private static void vanish(Mob shadow, UUID owner) {
        if (shadow.isAlive()) {
            Config.Burst smoke = Config.event("shadow").burst("particles");
            ((ServerLevel) shadow.level()).sendParticles(ParticleTypes.LARGE_SMOKE, shadow.getX(), shadow.getY() + smoke.offsetY(), shadow.getZ(),
                smoke.count(), smoke.dx(), smoke.dy(), smoke.dz(), smoke.speed());
            shadow.discard();
        }
        ACTIVE.remove(owner, shadow);
    }

    private static void play(ServerPlayer player, SoundEvent sound, Config.Tone tone) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.HOSTILE, tone.volume(), tone.pitch());
    }

    private static final class Chase {
        private final Mob shadow;
        private final UUID owner;
        private int stuck;

        Chase(Mob shadow, UUID owner) {
            this.shadow = shadow;
            this.owner = owner;
        }

        /** @return true to keep chasing */
        boolean step(ServerPlayer player, int n) {
            Config.Section config = Config.event("shadow");
            if (!shadow.isAlive() || shadow.level() != player.level() || stuck >= config.integer("stuck_steps")
                || n * config.integer("interval_ticks") >= config.integer("max_ticks")) {
                vanish(shadow, owner);
                return false;
            }
            if (n % config.integer("beat_steps") == 0) {
                play(player, SoundEvents.WARDEN_HEARTBEAT, config.tone("beat_sound"));
            }
            double touch = config.num("touch_distance");
            if (player.distanceToSqr(shadow) <= touch * touch) {
                touch(player, config);
                vanish(shadow, owner);
                return false;
            }
            if (!watched(player, config)) {
                advance(player, config);
            }
            return true;
        }

        private boolean watched(ServerPlayer player, Config.Section config) {
            Vec3 toShadow = shadow.getEyePosition().subtract(player.getEyePosition()).normalize();
            double facing = player.getLookAngle().dot(toShadow);
            return facing >= Math.cos(Math.toRadians(config.num("watch_degrees"))) && player.hasLineOfSight(shadow);
        }

        /** One stride toward the player, or no move (and a count toward giving up) if there is no standing room. */
        private void advance(ServerPlayer player, Config.Section config) {
            if (Walk.toward(player, shadow, config.num("speed"), config.integer("climb"), config.integer("drop"))) {
                shadow.lookAt(EntityAnchorArgument.Anchor.EYES, player.getEyePosition());
                stuck = 0;
            } else {
                stuck++;
            }
        }

        private void touch(ServerPlayer player, Config.Section config) {
            float room = player.getHealth() - Assaults.safeHealth();
            if (room > 0) {
                play(player, SoundEvents.WARDEN_ATTACK_IMPACT, config.tone("touch_sound"));
                player.hurtServer(player.level(), player.damageSources().magic(), Math.min((float) config.num("damage"), room));
            }
        }
    }
}
