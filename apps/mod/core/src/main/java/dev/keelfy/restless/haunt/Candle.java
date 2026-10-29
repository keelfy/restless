package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

/**
 * The spirit candle in a player's hand. Its flame leans toward the spirit that acts next, and burns blue for the last
 * seconds before that spirit's event, so the warning is exact: it reads the same timer the {@link Director} runs.
 */
public final class Candle {
    private Candle() {}

    private record Next(long dueAt, Vec3 at) {}

    public static void tick(MinecraftServer server) {
        Config.Section config = Config.section("candle");
        if (server.getTickCount() % config.integer("step_ticks") != 0) {
            return;
        }
        long now = server.overworld().getGameTime();
        HauntData data = null;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!holds(player)) {
                continue;
            }
            data = data == null ? HauntData.get(server) : data;
            Vec3 hand = player.getEyePosition().add(player.getLookAngle().scale(0.6)).add(0, -0.4, 0);
            Next next = next(player, data);
            boolean warning = next != null && next.dueAt() - now <= config.integer("warning_ticks");
            Vec3 lean = next == null || next.at() == null ? Vec3.ZERO : next.at().subtract(hand).multiply(1, 0, 1).normalize();
            player.level().sendParticles(warning ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME,
                hand.x, hand.y, hand.z, 0, lean.x * config.num("lean"), 1.0, lean.z * config.num("lean"), 0.03);
        }
    }

    private static boolean holds(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).is(Content.SPIRIT_CANDLE.get())) {
                return true;
            }
        }
        return false;
    }

    /** The spirit of this player, personal or territorial and in reach, whose event comes first. */
    private static Next next(ServerPlayer player, HauntData data) {
        List<Next> all = new ArrayList<>();
        List<HauntData.Haunting> personal = data.get(player.getUUID());
        if (personal != null) {
            for (HauntData.Haunting haunting : personal) {
                UUID id = haunting.uuid();
                all.add(new Next(haunting.nextEventAt(), Location.roaming(id)));
            }
        }
        ServerLevel level = player.level();
        for (HauntData.Territorial territorial : data.territorial()) {
            if (territorial.dimension().equals(level.dimension()) && territorial.reaches(player)) {
                all.add(new Next(territorial.haunting().nextEventAt(), Vec3.atCenterOf(territorial.anchor())));
            }
        }
        return all.stream().min(Comparator.comparingLong(Next::dueAt)).orElse(null);
    }
}
