package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.event.HauntEvent;
import dev.keelfy.restless.state.Session;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Pacing of the loud scares. An event with {@code startle: true} is a jump scare; after one has run for a player, none
 * may roll for them again until {@code director.startle_gap_ticks} have passed, so they stay rare however the weights
 * add up. In memory only.
 */
public final class Startle {
    private static final Map<UUID, Long> LAST = Session.SERVER.keep(new ConcurrentHashMap<>());
    private static final Map<String, Boolean> STARTLING = Session.SERVER.keep(new ConcurrentHashMap<>());

    private Startle() {}

    /** Notes that the event ran for the player, if it is a jump scare. */
    static void record(ServerPlayer player, HauntEvent event) {
        if (startling(event)) {
            LAST.put(player.getUUID(), now(player));
        }
    }

    /** True for a jump scare that ran for the player less than the gap ago. */
    static boolean blocks(ServerPlayer player, HauntEvent event) {
        return startling(event)
                && held(
                        LAST.get(player.getUUID()),
                        now(player),
                        Config.section("director").integer("startle_gap_ticks"));
    }

    /** Whether a jump scare that ran at {@code last} (null: never) still holds the next one back at {@code now}. */
    static boolean held(Long last, long now, int gap) {
        return last != null && now - last < gap;
    }

    /** Whether the event's config marks it a jump scare; an event with no config, such as a test's, is not one. */
    private static boolean startling(HauntEvent event) {
        return STARTLING.computeIfAbsent(event.id().toString(), id -> {
            if (!Config.eventKeys().contains(id)) {
                return false;
            }
            Config.Section config = Config.event(id);
            return config.has("startle") && config.flag("startle");
        });
    }

    private static long now(ServerPlayer player) {
        return player.level().getServer().overworld().getGameTime();
    }
}
