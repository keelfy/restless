package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntTier;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Event defined by two lambdas; for effects too small to deserve a class. */
record SimpleEvent(
        Identifier id,
        HauntTier tier,
        Set<EventTag> tags,
        Predicate<ServerPlayer> condition,
        Consumer<ServerPlayer> action)
        implements HauntEvent {
    static SimpleEvent light(
            String id, EventTag tag, Predicate<ServerPlayer> condition, Consumer<ServerPlayer> action) {
        return new SimpleEvent(Identifier.parse(id), HauntTier.LIGHT, Set.of(tag), condition, action);
    }

    static SimpleEvent harmful(
            String id, EventTag tag, Predicate<ServerPlayer> condition, Consumer<ServerPlayer> action) {
        return new SimpleEvent(Identifier.parse(id), HauntTier.HARMFUL, Set.of(tag), condition, action);
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
