package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public interface HauntEvent {
    /**
     * The namespace names the mod whose content the event uses: {@code minecraft} for vanilla, another mod's id for an
     * integration.
     */
    Identifier id();

    HauntTier tier();

    Set<EventTag> tags();

    /** Relative likelihood against other events, from {@code events.<id>.weight}. */
    default double baseWeight() {
        return Config.event(id().toString()).num("weight");
    }

    /** Whether a spirit this strong may roll the event at all; only {@link Scenario}s are gated. */
    default boolean opensAt(double power, boolean evolved) {
        return true;
    }

    /** Whether something this event raised for the player is still going; only {@code >gone} waits ask. */
    default boolean busy(ServerPlayer player) {
        return false;
    }

    /** Cheap check whether the event has anything to act on right now. */
    boolean canRun(ServerPlayer player);

    void run(ServerPlayer player);
}
