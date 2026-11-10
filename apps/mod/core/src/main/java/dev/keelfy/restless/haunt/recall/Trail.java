package dev.keelfy.restless.haunt.recall;

import dev.keelfy.restless.state.Session;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Where each player was over the last thirty seconds, one sample a second: the path a double can walk. */
public final class Trail {
    private static final int EVERY_TICKS = 20;
    private static final int SAMPLES = 30;
    private static final Map<UUID, ArrayDeque<Vec3>> PATHS = Session.SERVER.keep(new HashMap<>());

    private Trail() {}

    /** Call once per server tick. */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % EVERY_TICKS != 0) {
            return;
        }
        server.getPlayerList().getPlayers().forEach(Trail::sample);
    }

    /** One sample of where the player is now. */
    public static void sample(ServerPlayer player) {
        ArrayDeque<Vec3> path = PATHS.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
        path.addLast(player.position());
        while (path.size() > SAMPLES) {
            path.removeFirst();
        }
    }

    /** The samples, oldest first. */
    public static List<Vec3> of(ServerPlayer player) {
        ArrayDeque<Vec3> path = PATHS.get(player.getUUID());
        return path == null ? List.of() : List.copyOf(path);
    }
}
