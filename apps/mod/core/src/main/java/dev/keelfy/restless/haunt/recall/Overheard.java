package dev.keelfy.restless.haunt.recall;

import dev.keelfy.restless.state.Session;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/** The last few things each player said in chat, with when, for the mimic to give back. In memory only. */
public final class Overheard {
    /** One chat line and the game time it was said. */
    public record Line(String text, long at) {}

    private static final int KEPT = 5;
    private static final Map<UUID, ArrayDeque<Line>> LINES = Session.SERVER.keep(new HashMap<>());

    private Overheard() {}

    /** Call when a player says something in chat. */
    public static void heard(ServerPlayer player, String line) {
        ArrayDeque<Line> lines = LINES.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
        lines.addFirst(new Line(line, player.level().getGameTime()));
        while (lines.size() > KEPT) {
            lines.removeLast();
        }
    }

    /** The player's lines said within {@code maxAge} ticks, newest first. */
    public static List<Line> recent(ServerPlayer player, long maxAge) {
        ArrayDeque<Line> lines = LINES.get(player.getUUID());
        long now = player.level().getGameTime();
        return lines == null
                ? List.of()
                : lines.stream().filter(line -> now - line.at() <= maxAge).toList();
    }
}
