package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The sense that something is there. */
final class Presence {
    private Presence() {}

    static List<HauntEvent> all() {
        return List.of(
            // 94
            PresenceFigure.edge(),
            // 95
            PresenceFigure.fog(),
            // 123
            PresenceFigure.distantPlayer(),
            // 120
            PresenceFigure.distantWalker(),
            // 97: a puff of breath at the face where nobody should be breathing out
            SimpleEvent.light("cold_breath", EventTag.PRESENCE, Presence::warm, player -> {
                Config.Section config = Config.event("cold_breath");
                Config.Burst puff = config.burst("particles");
                Vec3 look = player.getLookAngle();
                Vec3 face = player.getEyePosition().add(look.x * config.num("forward"), puff.offsetY(), look.z * config.num("forward"));
                player.level().sendParticles(ParticleTypes.CLOUD, face.x, face.y, face.z, puff.count(), puff.dx(), puff.dy(), puff.dz(), puff.speed());
            }),
            // 96
            SimpleEvent.light("footprints", EventTag.PRESENCE, player -> soft(player) != null, Presence::footprints),
            // 98
            SimpleEvent.light("local_rain", EventTag.PRESENCE,
                player -> player.level().canSeeSky(player.blockPosition()) && !player.level().isRainingAt(player.blockPosition()),
                Presence::localRain),
            // 99
            SimpleEvent.harmful("lost_time", EventTag.PRESENCE, player -> player.level().isDarkOutside(), player -> {
                var server = player.level().getServer();
                server.clockManager().addTicks(server.registryAccess().getOrThrow(WorldClocks.OVERWORLD), Config.event("lost_time").integer("skip_ticks"));
            })
        );
    }

    private static boolean warm(ServerPlayer player) {
        ServerLevel level = player.level();
        return level.getBiome(player.blockPosition()).value().warmEnoughToRain(player.blockPosition(), level.getSeaLevel());
    }

    /** Snow or sand the player is standing on or in; its sound and particles are what the prints are made of. */
    private static @Nullable BlockState soft(ServerPlayer player) {
        BlockPos feet = player.blockPosition();
        for (BlockPos pos : new BlockPos[] {feet, feet.below()}) {
            BlockState state = player.level().getBlockState(pos);
            if (state.is(BlockTags.SAND) || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) {
                return state;
            }
        }
        return null;
    }

    /** Something walks in the player's tracks, one step behind: puffs and crunching where they stood a moment ago. */
    private static void footprints(ServerPlayer player) {
        Config.Section config = Config.event("footprints");
        double minMove = config.num("min_move");
        Config.Burst dust = config.burst("particles");
        Config.Tone step = config.tone("sound");
        Vec3[] last = {player.position()};
        Scheduler.repeat(player, config.integer("delay_ticks"), config.integer("interval_ticks"), (p, n) -> {
            Vec3 spot = last[0];
            last[0] = p.position();
            BlockState ground = soft(p);
            if (ground != null && spot.distanceToSqr(p.position()) > minMove * minMove) {
                p.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), spot.x, spot.y + dust.offsetY(), spot.z,
                    dust.count(), dust.dx(), dust.dy(), dust.dz(), dust.speed());
                p.level().playSound(null, spot.x, spot.y, spot.z, ground.getSoundType().getStepSound(), SoundSource.HOSTILE, step.volume(), step.pitch());
            }
            return n + 1 < config.integer("steps");
        });
    }

    /** Rain falling on nothing but the player. */
    private static void localRain(ServerPlayer player) {
        Config.Section config = Config.event("local_rain");
        Config.Burst drops = config.burst("particles");
        Config.Tone patter = config.tone("sound");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            p.level().sendParticles(ParticleTypes.RAIN, p.getX(), p.getY() + drops.offsetY(), p.getZ(), drops.count(), drops.dx(), drops.dy(), drops.dz(), drops.speed());
            if (n % config.integer("sound_every_steps") == 0) {
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, patter.volume(), patter.pitch());
            }
            return n + 1 < config.integer("steps");
        });
    }
}
