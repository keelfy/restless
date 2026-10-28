package dev.keelfy.restless.haunt.event;

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
            // 97: a puff of breath at the face where nobody should be breathing out
            SimpleEvent.light("cold_breath", EventTag.PRESENCE, 0.6, Presence::warm, player -> {
                Vec3 look = player.getLookAngle();
                Vec3 face = player.getEyePosition().add(look.x * 0.4, -0.1, look.z * 0.4);
                player.level().sendParticles(ParticleTypes.CLOUD, face.x, face.y, face.z, 6, 0.05, 0.05, 0.05, 0.01);
            }),
            // 96
            SimpleEvent.light("footprints", EventTag.PRESENCE, 0.6, player -> soft(player) != null, Presence::footprints),
            // 98
            SimpleEvent.light("local_rain", EventTag.PRESENCE, 0.6,
                player -> player.level().canSeeSky(player.blockPosition()) && !player.level().isRainingAt(player.blockPosition()),
                Presence::localRain),
            // 99
            SimpleEvent.harmful("lost_time", EventTag.PRESENCE, 0.5, player -> player.level().isDarkOutside(), player -> {
                var server = player.level().getServer();
                server.clockManager().addTicks(server.registryAccess().getOrThrow(WorldClocks.OVERWORLD), 1000);
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
        Vec3[] last = {player.position()};
        Scheduler.repeat(player, 10, 10, (p, n) -> {
            Vec3 spot = last[0];
            last[0] = p.position();
            BlockState ground = soft(p);
            if (ground != null && spot.distanceToSqr(p.position()) > 0.25) {
                p.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), spot.x, spot.y + 0.05, spot.z, 8, 0.15, 0.0, 0.15, 0.02);
                p.level().playSound(null, spot.x, spot.y, spot.z, ground.getSoundType().getStepSound(), SoundSource.HOSTILE, 0.8F, 0.8F);
            }
            return n < 11;
        });
    }

    /** Six seconds of rain falling on nothing but the player. */
    private static void localRain(ServerPlayer player) {
        Scheduler.repeat(player, 0, 2, (p, n) -> {
            p.level().sendParticles(ParticleTypes.RAIN, p.getX(), p.getY() + 3.0, p.getZ(), 12, 1.5, 0.2, 1.5, 0.0);
            if (n % 10 == 0) {
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 0.6F, 1.0F);
            }
            return n < 60;
        });
    }
}
