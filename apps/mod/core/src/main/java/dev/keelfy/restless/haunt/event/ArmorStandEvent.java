package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.Hardcore;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntTier;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Rotations;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * An armor stand that turns up behind the player and follows them, a stride at a time and only while they are not
 * looking, striking a new pose each time it moves. It is a real armor stand, but breaking it drops nothing. It goes away
 * after a while; under {@linkplain Hardcore hardcore} it stays where it ended up.
 */
final class ArmorStandEvent implements HauntEvent {
    private static final String ID = "armor_stand";
    /** Arms out reaching, arms raised over the head, the head lolling on its shoulder, a lunge. */
    private static final List<ArmorStand.ArmorStandPose> POSES = List.of(
        new ArmorStand.ArmorStandPose(new Rotations(0, 0, 0), new Rotations(0, 0, 0), new Rotations(-90, 0, 0), new Rotations(-90, 0, 0), new Rotations(0, 0, 0), new Rotations(0, 0, 0)),
        new ArmorStand.ArmorStandPose(new Rotations(-15, 0, 0), new Rotations(0, 0, 0), new Rotations(-170, 0, -10), new Rotations(-170, 0, 10), new Rotations(0, 0, 0), new Rotations(0, 0, 0)),
        new ArmorStand.ArmorStandPose(new Rotations(0, 0, 40), new Rotations(0, 0, 5), new Rotations(0, 0, -5), new Rotations(0, 0, 5), new Rotations(0, 0, 0), new Rotations(0, 0, 0)),
        new ArmorStand.ArmorStandPose(new Rotations(20, 0, 0), new Rotations(10, 0, 0), new Rotations(20, 0, 0), new Rotations(-110, 0, 0), new Rotations(-20, 0, 0), new Rotations(20, 0, 0)));

    /** The stand each player has now; one at a time. */
    private static final Map<UUID, ArmorStand> ACTIVE = new ConcurrentHashMap<>();

    @Override
    public Identifier id() {
        return Identifier.parse(ID);
    }

    @Override
    public HauntTier tier() {
        return HauntTier.HARMFUL;
    }

    @Override
    public Set<EventTag> tags() {
        return Set.of(EventTag.PRESENCE);
    }

    @Override
    public boolean canRun(ServerPlayer player) {
        return !ACTIVE.containsKey(player.getUUID()) && spot(player) != null;
    }

    @Override
    public void run(ServerPlayer player) {
        BlockPos spot = spot(player);
        if (spot == null) {
            return;
        }
        Config.Section config = Config.event(ID);
        ServerLevel level = player.level();
        ArmorStand stand = new Hollow(level, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
        stand.setShowArms(true);
        stand.setArmorStandPose(POSES.get(player.getRandom().nextInt(POSES.size())));
        face(stand, player);
        level.addFreshEntity(stand);
        UUID owner = player.getUUID();
        ACTIVE.put(owner, stand);
        // The follow ends itself; this catches the cases where the scheduler drops it (the player died or left).
        Restore.revert(level, config.integer("max_ticks") + config.integer("cleanup_ticks"), force -> {
            vanish(stand);
            return true;
        });
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            if (!stand.isAlive() || stand.level() != p.level() || n * config.integer("interval_ticks") >= config.integer("max_ticks")) {
                ACTIVE.remove(owner, stand);
                if (!Hardcore.on(level)) {
                    vanish(stand);
                }
                return false;
            }
            double near = config.num("stop_distance");
            if (p.distanceToSqr(stand) > near * near && !watched(p, stand, config)
                && Walk.toward(p, stand, config.num("speed"), config.integer("climb"), config.integer("drop"))) {
                face(stand, p);
                if (p.getRandom().nextDouble() < config.num("pose_chance")) {
                    stand.setArmorStandPose(POSES.get(p.getRandom().nextInt(POSES.size())));
                }
            }
            return true;
        });
    }

    private static @Nullable BlockPos spot(ServerPlayer player) {
        Config.Section config = Config.event(ID);
        return Place.standingAhead(player, config.num("distance"), config.num("min_angle"), config.num("max_angle"));
    }

    private static boolean watched(ServerPlayer player, ArmorStand stand, Config.Section config) {
        Vec3 toStand = stand.getEyePosition().subtract(player.getEyePosition()).normalize();
        return player.getLookAngle().dot(toStand) >= Math.cos(Math.toRadians(config.num("watch_degrees"))) && player.hasLineOfSight(stand);
    }

    private static void face(ArmorStand stand, ServerPlayer player) {
        float yaw = (float) Math.toDegrees(Mth.atan2(player.getZ() - stand.getZ(), player.getX() - stand.getX())) - 90.0F;
        stand.setYRot(yaw);
        stand.setYBodyRot(yaw);
        stand.setYHeadRot(yaw);
    }

    /** Falls apart as a broken armor stand does, with nothing left behind. */
    private static void vanish(ArmorStand stand) {
        if (stand.isAlive() && stand.level() instanceof ServerLevel level) {
            level.playSound(null, stand.getX(), stand.getY(), stand.getZ(), SoundEvents.ARMOR_STAND_BREAK, stand.getSoundSource(), 1.0F, 1.0F);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.defaultBlockState()),
                stand.getX(), stand.getY(0.66), stand.getZ(), 10, stand.getBbWidth() / 4.0, stand.getBbHeight() / 4.0, stand.getBbWidth() / 4.0, 0.05);
            stand.discard();
        }
    }

    /** A vanilla armor stand that breaks at the first blow and drops nothing: not itself, and it wears nothing. */
    private static final class Hollow extends ArmorStand {
        Hollow(ServerLevel level, double x, double y, double z) {
            super(level, x, y, z);
        }

        @Override
        public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
            if (isRemoved() || isInvulnerableTo(level, source) || source.getEntity() instanceof Player player && !player.getAbilities().mayBuild) {
                return false;
            }
            vanish(this);
            return true;
        }
    }
}
