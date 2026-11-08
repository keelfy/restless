package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.Session;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The {@code phantom_break} event: it marks the player, and the next block they break sounds twice, the second time
 * from a little way off. Nothing in the world changes. The mark is in memory only and lapses on its own.
 */
public final class Breaks {
    /** The game time at which each marked player's mark lapses. */
    private static final Map<UUID, Long> MARKED = Session.SERVER.keep(new ConcurrentHashMap<>());

    private Breaks() {}

    static boolean canMark(ServerPlayer player) {
        return !MARKED.containsKey(player.getUUID());
    }

    static void mark(ServerPlayer player) {
        long lapse =
                player.level().getGameTime() + Config.event("phantom_break").integer("max_age_ticks");
        MARKED.put(player.getUUID(), lapse);
    }

    /** Call when a player breaks a block, before it is gone; {@code state} is what stood there. */
    public static void broke(ServerPlayer player, BlockState state) {
        Long lapse = MARKED.remove(player.getUUID());
        if (lapse == null || player.level().getGameTime() > lapse) {
            return;
        }
        Config.Section config = Config.event("phantom_break");
        Scheduler.later(
                player,
                config.integer("delay_ticks"),
                p -> Nearby.sound(
                        p,
                        state.getSoundType().getBreakSound(),
                        Nearby.source(p, config.num("distance")),
                        config.tone("sound")));
    }
}
