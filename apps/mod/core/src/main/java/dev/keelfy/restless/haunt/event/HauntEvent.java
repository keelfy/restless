package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.scenario.Scenario;
import dev.keelfy.restless.spirit.SpiritType;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

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

    /**
     * The one spirit type that may roll the event, from {@code events.<id>.spirit}; null when any type may. A
     * {@link Scenario} reads the same key as a boost instead, see {@link #signature}.
     */
    default @Nullable String only() {
        Config.Section config = Config.event(id().toString());
        return config.has("spirit") ? config.text("spirit") : null;
    }

    /** Whether a spirit this strong may roll the event at all; only {@link Scenario}s are gated. */
    default boolean opensAt(double power, boolean evolved) {
        return true;
    }

    /** How much likelier the event is for this spirit, over its affinity; only an evolved spirit's own scenario is. */
    default double signature(SpiritType spirit, boolean evolved) {
        return 1.0;
    }

    /** Whether something this event raised for the player is still going; only {@code >gone} waits ask. */
    default boolean busy(ServerPlayer player) {
        return false;
    }

    /** Whether {@link #canRun} is true wherever the player is. */
    default boolean always() {
        return false;
    }

    /** Cheap check whether the event has anything to act on right now. */
    boolean canRun(ServerPlayer player);

    void run(ServerPlayer player);
}
