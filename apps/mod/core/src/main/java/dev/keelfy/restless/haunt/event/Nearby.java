package dev.keelfy.restless.haunt.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Search and effect helpers shared by the events. */
final class Nearby {
    private Nearby() {}

    /** Random loaded block within a cube of the given radius around the player that matches. */
    static @Nullable BlockPos random(ServerPlayer player, int radius, Predicate<BlockState> match) {
        List<BlockPos> hits = all(player, radius, match);
        return hits.isEmpty() ? null : hits.get(player.getRandom().nextInt(hits.size()));
    }

    /** Every loaded block within a cube of the given radius around the player that matches. */
    static List<BlockPos> all(ServerPlayer player, int radius, Predicate<BlockState> match) {
        BlockPos center = player.blockPosition();
        List<BlockPos> hits = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (player.level().isLoaded(pos) && match.test(player.level().getBlockState(pos))) {
                hits.add(pos.immutable());
            }
        }
        return hits;
    }

    static boolean any(ServerPlayer player, int radius, Predicate<BlockState> match) {
        return random(player, radius, match) != null;
    }

    static void sound(ServerPlayer player, SoundEvent sound, Vec3 at, float volume, float pitch) {
        player.level().playSound(null, at.x, at.y, at.z, sound, SoundSource.HOSTILE, volume, pitch);
    }

    /** Sound at a point {@code distance} blocks from the player; {@code yawOffset} is relative to where they look (180 = behind). */
    static Vec3 around(ServerPlayer player, double distance, double yawOffset) {
        double yaw = Math.toRadians(player.getYRot() + yawOffset);
        return player.position().add(-Math.sin(yaw) * distance, 0.5, Math.cos(yaw) * distance);
    }

    static Vec3 randomAround(ServerPlayer player, double distance) {
        return around(player, distance, player.getRandom().nextDouble() * 360.0);
    }
}
