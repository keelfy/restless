package dev.keelfy.restless.haunt;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The last events each player went through, newest first, so the director does not roll the same one again until
 * {@code gap} others have happened. In memory only; server-thread only.
 */
public final class Recent {
    private static final Map<UUID, Deque<String>> HISTORY = new HashMap<>();

    private Recent() {}

    /** @param gap how many other events must happen before this one may repeat */
    public static void record(UUID player, String id, int gap) {
        Deque<String> history = HISTORY.computeIfAbsent(player, p -> new ArrayDeque<>());
        history.addFirst(id);
        while (history.size() > gap) {
            history.removeLast();
        }
    }

    /** True when the event is among the player's last {@code gap} events, as last recorded. */
    public static boolean blocks(UUID player, String id) {
        Deque<String> history = HISTORY.get(player);
        return history != null && history.contains(id);
    }

    static void reset() {
        HISTORY.clear();
    }
}
