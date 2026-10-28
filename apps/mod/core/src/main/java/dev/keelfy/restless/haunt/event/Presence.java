package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.phys.Vec3;

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
}
