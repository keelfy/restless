package dev.keelfy.restless.haunt;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.haunt.event.HauntEvent;
import dev.keelfy.restless.state.Session;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Keeps one broken haunt event from taking the server tick down with it. An event whose {@link HauntEvent#canRun} or
 * {@link HauntEvent#run} throws is logged once and left out until the server stops; a later step or undo that throws is
 * logged and dropped by {@link Scheduler} and {@link Restore}. Call events through here, never directly, outside tests.
 */
public final class Faults {
    private static final Set<Identifier> BROKEN = Session.SERVER.keep(ConcurrentHashMap.newKeySet());

    private Faults() {}

    /** {@link HauntEvent#canRun}, false for a broken event. */
    public static boolean canRun(HauntEvent event, ServerPlayer player) {
        if (BROKEN.contains(event.id())) {
            return false;
        }
        try {
            return event.canRun(player);
        } catch (RuntimeException e) {
            broke(event, "canRun", e);
            return false;
        }
    }

    /** {@link HauntEvent#run}; @return whether it ran to the end */
    public static boolean run(HauntEvent event, ServerPlayer player) {
        if (BROKEN.contains(event.id())) {
            return false;
        }
        try {
            event.run(player);
            return true;
        } catch (RuntimeException e) {
            broke(event, "run", e);
            return false;
        }
    }

    public static boolean broken(HauntEvent event) {
        return BROKEN.contains(event.id());
    }

    private static void broke(HauntEvent event, String where, RuntimeException e) {
        if (BROKEN.add(event.id())) {
            Restless.LOGGER.error(
                    "Haunt event {} failed in {} and is off until the server stops", event.id(), where, e);
        }
    }
}
