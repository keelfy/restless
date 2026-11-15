package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.recall.Said;
import dev.keelfy.restless.state.HauntData;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Anger you can see: a tell whose config names a {@code harsh} event runs that event instead once its spirit's anger is
 * above {@code anger.harsh_above}, where the harsh one has something to act on; the door slammer locks the door instead
 * of slamming it.
 */
final class Harsh {
    private Harsh() {}

    /** The harsh form of {@code tell} for the running spirit, if it is angry enough and the form can run here. */
    static Optional<HauntEvent> of(ServerPlayer player, String tell) {
        Config.Section config = Config.event(tell);
        UUID spirit = Said.current();
        if (!config.has("harsh") || spirit == null) {
            return Optional.empty();
        }
        double anger = HauntData.get(player.level().getServer())
                .find(spirit)
                .map(HauntData.Haunting::anger)
                .orElse(0.0);
        if (anger <= Config.section("anger").num("harsh_above")) {
            return Optional.empty();
        }
        return HauntEvents.byId(Identifier.parse(config.text("harsh"))).filter(e -> e.canRun(player));
    }
}
