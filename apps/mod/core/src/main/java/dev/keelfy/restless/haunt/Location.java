package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Where a spirit is. A personal spirit roams in a ring around its player and is forgotten on restart; a territorial one
 * stays at its anchor. Events whose source is free take {@link #source} as the point they come from.
 */
public final class Location {
    private static final Map<UUID, Vec3> ROAMING = new HashMap<>();
    /** The point of the spirit whose event is running right now; set only for the duration of {@link Director#fire}. */
    private static @Nullable Vec3 current;

    private Location() {}

    /**
     * Moves the roaming spirit a step and keeps it between the configured distances of the player; teleporting the
     * player pulls it along.
     */
    public static Vec3 roam(UUID spirit, ServerPlayer player) {
        Config.Section config = Config.section("location");
        Vec3 from = ROAMING.get(spirit);
        double angle = player.getRandom().nextDouble() * Math.PI * 2;
        double dx = from == null
                ? Math.cos(angle)
                : from.x - player.getX() + (player.getRandom().nextDouble() - 0.5) * 2 * config.num("step");
        double dz = from == null
                ? Math.sin(angle)
                : from.z - player.getZ() + (player.getRandom().nextDouble() - 0.5) * 2 * config.num("step");
        double length = Math.max(Math.sqrt(dx * dx + dz * dz), 1e-6);
        double distance = Math.clamp(
                from == null ? config.num("max_distance") : length,
                config.num("min_distance"),
                config.num("max_distance"));
        Vec3 to = new Vec3(
                player.getX() + dx / length * distance,
                player.getY() + config.num("height"),
                player.getZ() + dz / length * distance);
        ROAMING.put(spirit, to);
        return to;
    }

    public static @Nullable Vec3 roaming(UUID spirit) {
        return ROAMING.get(spirit);
    }

    public static void forget(UUID spirit) {
        ROAMING.remove(spirit);
    }

    public static void clear() {
        ROAMING.clear();
    }

    static void during(@Nullable Vec3 point, Runnable action) {
        current = point;
        try {
            action.run();
        } finally {
            current = null;
        }
    }

    /** The running event's spirit point, or {@code fallback} when the event was not started by a spirit. */
    public static Vec3 source(Vec3 fallback) {
        return current != null ? current : fallback;
    }
}
