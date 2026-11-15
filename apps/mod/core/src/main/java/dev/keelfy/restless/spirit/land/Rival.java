package dev.keelfy.restless.spirit.land;

import dev.keelfy.restless.spirit.Story;
import dev.keelfy.restless.state.HauntData;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@link Quest#RIVAL}: another spirit of the same player killed this one, and it rests only once that one is gone. The
 * rival is the first other spirit of the owner (or of the reader, for a spirit with no owner) when the stone is first
 * read; with none the killer is already gone and the next visit lays it to rest.
 */
final class Rival {
    private Rival() {}

    /** Picks the rival on the first reading. @return its name, as the stone gives it */
    static Component read(ServerPlayer player, HauntData data, HauntData.Haunting spirit, HauntData.Grave grave) {
        if (grave.stage() == 0) {
            UUID owner = data.owner(spirit.uuid()).orElse(player.getUUID());
            List<HauntData.Haunting> theirs =
                    Optional.ofNullable(data.get(owner)).orElse(List.of());
            Optional<UUID> rival = theirs.stream()
                    .map(HauntData.Haunting::uuid)
                    .filter(id -> !id.equals(spirit.uuid()))
                    .findFirst();
            grave = grave.staged(1, rival);
            data.putGrave(spirit.uuid(), grave);
        }
        return grave.mark()
                .flatMap(data::find)
                .<Component>map(
                        rival -> Component.literal(Story.of(rival.seed()).name()))
                .orElse(Component.translatable("message.restless.rival_gone"));
    }

    static boolean gone(HauntData data, HauntData.Grave grave) {
        return grave.stage() >= 1 && grave.mark().flatMap(data::find).isEmpty();
    }
}
