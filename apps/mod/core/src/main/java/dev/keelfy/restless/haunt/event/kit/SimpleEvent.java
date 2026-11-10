package dev.keelfy.restless.haunt.event.kit;

import dev.keelfy.restless.haunt.event.EventTag;
import dev.keelfy.restless.haunt.event.HauntEvent;
import dev.keelfy.restless.haunt.event.HauntTier;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Event defined by two lambdas; for effects too small to deserve a class. */
public record SimpleEvent(
        Identifier id,
        HauntTier tier,
        Set<EventTag> tags,
        Predicate<ServerPlayer> condition,
        Consumer<ServerPlayer> action,
        boolean waits)
        implements HauntEvent {
    public SimpleEvent(
            Identifier id,
            HauntTier tier,
            Set<EventTag> tags,
            Predicate<ServerPlayer> condition,
            Consumer<ServerPlayer> action) {
        this(id, tier, tags, condition, action, false);
    }

    /** The condition of an event that has something to act on wherever the player is. */
    public static final Predicate<ServerPlayer> ALWAYS = player -> true;

    public static SimpleEvent light(
            String id, EventTag tag, Predicate<ServerPlayer> condition, Consumer<ServerPlayer> action) {
        return new SimpleEvent(Identifier.parse(id), HauntTier.LIGHT, Set.of(tag), condition, action);
    }

    /** A light event whose scare comes later; its action calls {@code Director.landed} when it does. */
    public static SimpleEvent waiting(
            String id, EventTag tag, Predicate<ServerPlayer> condition, Consumer<ServerPlayer> action) {
        return new SimpleEvent(Identifier.parse(id), HauntTier.LIGHT, Set.of(tag), condition, action, true);
    }

    /** A harmful event whose scare comes later; its action calls {@code Director.landed} when it does. */
    public static SimpleEvent waitingHarmful(
            String id, EventTag tag, Predicate<ServerPlayer> condition, Consumer<ServerPlayer> action) {
        return new SimpleEvent(Identifier.parse(id), HauntTier.HARMFUL, Set.of(tag), condition, action, true);
    }

    public static SimpleEvent harmful(
            String id, EventTag tag, Predicate<ServerPlayer> condition, Consumer<ServerPlayer> action) {
        return new SimpleEvent(Identifier.parse(id), HauntTier.HARMFUL, Set.of(tag), condition, action);
    }

    @Override
    public boolean waits() {
        return waits;
    }

    @Override
    public boolean always() {
        return condition == ALWAYS;
    }

    @Override
    public boolean canRun(ServerPlayer player) {
        return condition.test(player);
    }

    @Override
    public void run(ServerPlayer player) {
        action.accept(player);
    }
}
