package dev.keelfy.restless.haunt;

import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

public interface HauntEvent {
    String id();

    HauntTier tier();

    Set<EventTag> tags();

    default double baseWeight() {
        return 1.0;
    }

    /** Cheap check whether the event has anything to act on right now. */
    boolean canRun(ServerPlayer player);

    void run(ServerPlayer player);
}
