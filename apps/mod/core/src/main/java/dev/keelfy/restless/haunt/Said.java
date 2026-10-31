package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * Keeps a spirit from saying the same thing twice. Each text event (a title, a chat line, a sign) remembers, per
 * spirit, which of its variants were used; once all were, the event rests for that spirit for a long while, then starts
 * over. An event with no spirit behind it (a command, the debug menu) counts against the player instead.
 */
public final class Said {
    /** The spirit whose roll or event is running right now; set only for the duration of {@link #during}. */
    private static @Nullable UUID speaker;

    private Said() {}

    static void during(UUID spirit, Runnable action) {
        speaker = spirit;
        try {
            action.run();
        } finally {
            speaker = null;
        }
    }

    /** The spirit set by {@link #during}, if any; to carry it into a later step. */
    static @Nullable UUID current() {
        return speaker;
    }

    /** Whether the speaker has a variant of event {@code id} left to say. */
    public static <T> boolean left(ServerPlayer player, String id, List<T> variants, Function<T, String> key) {
        return !unsaid(player, id, variants, key).isEmpty();
    }

    public static boolean left(ServerPlayer player, String id, List<String> variants) {
        return left(player, id, variants, Function.identity());
    }

    /**
     * A variant of event {@code id} the speaker has not said since it last rested, now said; null when there is none.
     */
    public static <T> @Nullable T pick(ServerPlayer player, String id, List<T> variants, Function<T, String> key) {
        List<T> fresh = unsaid(player, id, variants, key);
        if (fresh.isEmpty()) {
            return null;
        }
        T picked = fresh.get(player.getRandom().nextInt(fresh.size()));
        HauntData data = HauntData.get(player.level().getServer());
        List<String> used = new ArrayList<>(data.spoken(speaker(player), id).used());
        used.add(key.apply(picked));
        data.putSpoken(
                speaker(player),
                id,
                fresh.size() == 1
                        ? new HauntData.Spoken(
                                List.of(), now(player) + Config.section("said").integer("rest_ticks"))
                        : new HauntData.Spoken(used, 0L));
        return picked;
    }

    public static @Nullable String pick(ServerPlayer player, String id, List<String> variants) {
        return pick(player, id, variants, Function.identity());
    }

    private static <T> List<T> unsaid(ServerPlayer player, String id, List<T> variants, Function<T, String> key) {
        HauntData.Spoken spoken = HauntData.get(player.level().getServer()).spoken(speaker(player), id);
        if (now(player) < spoken.restUntil()) {
            return List.of();
        }
        return variants.stream()
                .filter(v -> !spoken.used().contains(key.apply(v)))
                .toList();
    }

    private static UUID speaker(ServerPlayer player) {
        return speaker != null ? speaker : player.getUUID();
    }

    private static long now(ServerPlayer player) {
        return player.level().getServer().overworld().getGameTime();
    }
}
