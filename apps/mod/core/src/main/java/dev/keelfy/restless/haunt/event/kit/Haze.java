package dev.keelfy.restless.haunt.event.kit;

import dev.keelfy.restless.content.Presence;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.function.DoubleFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Where an event puts a spirit that has no body to show: a haze, see {@link Presence}. It is only a look, so nothing
 * here touches the world, and every call is dropped with the player's other scheduled steps.
 */
public final class Haze {
    /** Ticks between puffs of a spirit standing still; a puff outlasts this, so the haze never thins out. */
    private static final int HOLD_EVERY = 10;
    /** Ticks between puffs of a spirit on the move; close enough that the haze trails it without gaps. */
    private static final int MOVE_EVERY = 3;

    private Haze() {}

    /** A puff where a spirit stands. */
    public static void at(ServerLevel level, Vec3 feet) {
        Presence.emit(level, feet);
    }

    /** A puff of {@code count} motes. */
    public static void at(ServerLevel level, Vec3 feet, int count) {
        Presence.emit(level, feet, count);
    }

    /** A spirit standing at {@code feet} for {@code ticks}. */
    public static void linger(ServerPlayer player, Vec3 feet, int ticks) {
        Scheduler.repeat(player, 0, HOLD_EVERY, (p, n) -> {
            Presence.emit(p.level(), feet);
            return (n + 1) * HOLD_EVERY < ticks;
        });
    }

    /**
     * A spirit moving along {@code path} for {@code ticks}: {@code path} gives where it is at 0 (the start) up to 1
     * (the end), and is read afresh for each puff, so a path that follows the player keeps following them.
     */
    public static void glide(ServerPlayer player, int ticks, DoubleFunction<Vec3> path) {
        Scheduler.repeat(player, 0, MOVE_EVERY, (p, n) -> {
            double at = Math.min(1.0, (double) n * MOVE_EVERY / Math.max(1, ticks));
            Presence.emit(p.level(), path.apply(at), 1);
            return (n + 1) * MOVE_EVERY <= ticks;
        });
    }
}
