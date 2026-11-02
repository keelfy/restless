package dev.keelfy.restless.haunt;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/**
 * Every event that ran lately and the spirit behind it, newest first, for /haunt history. In memory only, the last
 * {@value #LIMIT} events of all players; server-thread only.
 */
public final class History {
    private static final int LIMIT = 100;
    private static final Deque<Entry> ENTRIES = new ArrayDeque<>();

    private History() {}

    /**
     * @param at game time the event ran
     * @param spirit spirit type and story of whoever caused it; null when no spirit did (a command, the debug menu)
     */
    public record Entry(
            long at, String player, String event, @Nullable String spirit) {}

    public static void record(long at, String player, String event, @Nullable String spirit) {
        ENTRIES.addFirst(new Entry(at, player, event, spirit));
        while (ENTRIES.size() > LIMIT) {
            ENTRIES.removeLast();
        }
    }

    /** Up to {@code count} newest entries of the player called {@code player}, or of everyone when it is null. */
    public static List<Entry> last(@Nullable String player, int count) {
        Predicate<Entry> mine = e -> player == null || Objects.equals(e.player(), player);
        return ENTRIES.stream().filter(mine).limit(count).toList();
    }

    static void reset() {
        ENTRIES.clear();
    }
}
