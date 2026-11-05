package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

/**
 * The spirit candle in a player's hand. Its flame leans toward the spirit that acts next, and burns blue for the last
 * seconds before that spirit's event, so the warning is exact: it reads the same timer the {@link Director} runs.
 */
public final class Candle {
    /** Until when each player's candle is out, in overworld game time. */
    private static final Map<UUID, Long> OUT = Session.SERVER.keep(new ConcurrentHashMap<>());

    private Candle() {}

    /** The candle in this player's hand goes out for {@code ticks}: no flame, no lean, no warning. */
    public static void snuff(ServerPlayer player, int ticks) {
        OUT.put(player.getUUID(), player.level().getServer().overworld().getGameTime() + ticks);
    }

    public static boolean isOut(ServerPlayer player) {
        Long until = OUT.get(player.getUUID());
        return until != null && until > player.level().getServer().overworld().getGameTime();
    }

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
            if (OUT.getOrDefault(player.getUUID(), 0L) > now) {
                continue;
            }
            data = data == null ? HauntData.get(server) : data;
            Vec3 hand = player.getEyePosition()
                    .add(player.getLookAngle().scale(0.6))
                    .add(0, -0.4, 0);
            Optional<Sense.Spirit> next = Sense.of(player, data).stream()
                    .min(Comparator.comparingLong(s -> s.haunting().nextEventAt()));
            boolean warning = next.filter(s -> s.haunting().nextEventAt() - now <= config.integer("warning_ticks"))
                    .isPresent();
            Vec3 lean = next.map(Sense.Spirit::at)
                    .map(at -> at.subtract(hand).multiply(1, 0, 1).normalize())
                    .orElse(Vec3.ZERO);
            player.level()
                    .sendParticles(
                            warning ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME,
                            hand.x,
                            hand.y,
                            hand.z,
                            0,
                            lean.x * config.num("lean"),
                            1.0,
                            lean.z * config.num("lean"),
                            0.03);
        }
    }

    public static boolean holds(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).is(Content.SPIRIT_CANDLE.get())) {
                return true;
            }
        }
        return false;
    }
}
