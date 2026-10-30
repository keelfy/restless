package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Location;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Search and effect helpers shared by the events. */
final class Nearby {
    static final double FULL_TURN = 360.0;

    private Nearby() {}

    /** Search radius of an event, from {@code events.<id>.radius}. */
    static int radius(String event) {
        return Config.event(event).integer("radius");
    }

    static @Nullable BlockPos random(ServerPlayer player, String event, Predicate<BlockState> match) {
        return random(player, radius(event), match);
    }

    static List<BlockPos> all(ServerPlayer player, String event, Predicate<BlockState> match) {
        return all(player, radius(event), match);
    }

    static boolean any(ServerPlayer player, String event, Predicate<BlockState> match) {
        return any(player, radius(event), match);
    }

    /** Random loaded block within a cube of the given radius around the player that matches. */
    static @Nullable BlockPos random(ServerPlayer player, int radius, Predicate<BlockState> match) {
        List<BlockPos> hits = all(player, radius, match);
        return hits.isEmpty() ? null : hits.get(player.getRandom().nextInt(hits.size()));
    }

    /** Every loaded block within a cube of the given radius around the player that matches. */
    static List<BlockPos> all(ServerPlayer player, int radius, Predicate<BlockState> match) {
        return all(player.level(), player.blockPosition(), radius, match);
    }

    /** Every loaded block within a cube of the given radius around {@code center} that matches. */
    static List<BlockPos> all(ServerLevel level, BlockPos center, int radius, Predicate<BlockState> match) {
        List<BlockPos> hits = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (level.isLoaded(pos) && match.test(level.getBlockState(pos))) {
                hits.add(pos.immutable());
            }
        }
        return hits;
    }

    static boolean any(ServerPlayer player, int radius, Predicate<BlockState> match) {
        return random(player, radius, match) != null;
    }

    static void sound(ServerPlayer player, SoundEvent sound, Vec3 at, Config.Tone tone) {
        player.level().playSound(null, at.x, at.y, at.z, sound, SoundSource.HOSTILE, tone.volume(), tone.pitch());
    }

    /** Sound at a point {@code distance} blocks from the player; {@code yawOffset} is relative to where they look (180 = behind). */
    static Vec3 around(ServerPlayer player, double distance, double yawOffset) {
        double yaw = Math.toRadians(player.getYRot() + yawOffset);
        return player.position().add(-Math.sin(yaw) * distance, Config.section("place").num("sound_height"), Math.cos(yaw) * distance);
    }

    static Vec3 randomAround(ServerPlayer player, double distance) {
        return around(player, distance, player.getRandom().nextDouble() * FULL_TURN);
    }

    /** Where a source-free sound comes from: the spirit that sent it, or {@code distance} blocks away at a random bearing when none did. */
    static Vec3 source(ServerPlayer player, double distance) {
        return Location.source(randomAround(player, distance));
    }
}
