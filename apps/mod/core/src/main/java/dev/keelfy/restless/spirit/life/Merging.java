package dev.keelfy.restless.spirit.life;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Location;
import dev.keelfy.restless.haunt.Ticks;
import dev.keelfy.restless.journal.Journal;
import dev.keelfy.restless.journal.Notebook;
import dev.keelfy.restless.spirit.Focus;
import dev.keelfy.restless.spirit.Story;
import dev.keelfy.restless.spirit.Trait;
import dev.keelfy.restless.spirit.Traits;
import dev.keelfy.restless.state.HauntData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Two personal spirits of one player, both older than {@code merging.days}, merge: the elder swallows the younger,
 * takes its habits and keeps its nature as a second one ({@link dev.keelfy.restless.state.Marks#merged}), which its
 * ritual asks an offering for. The elder's grave stays, or the younger's when the elder has none. An echo neither
 * merges nor is swallowed.
 */
public final class Merging {
    private Merging() {}

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % Config.section("merging").integer("check_ticks") != 0) {
            return;
        }
        HauntData data = HauntData.get(server);
        long now = server.overworld().getGameTime();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            List<HauntData.Haunting> old = ripe(data, player, now);
            if (old.size() >= 2) {
                merge(player, data, old.get(0), old.get(1));
            }
        }
    }

    /** The player's spirits old enough to merge, eldest first; echoes left out. */
    public static List<HauntData.Haunting> ripe(HauntData data, ServerPlayer player, long now) {
        List<HauntData.Haunting> own = data.get(player.getUUID());
        double days = Config.section("merging").num("days");
        return (own == null ? List.<HauntData.Haunting>of() : own)
                .stream()
                        .filter(h -> !Echoes.is(data, h) && now - h.bornAt() >= days * Ticks.DAY)
                        .sorted(Comparator.comparingLong(HauntData.Haunting::bornAt))
                        .toList();
    }

    /** The elder swallows the younger; @return the merged spirit, under the elder's id */
    public static HauntData.Haunting merge(
            ServerPlayer player, HauntData data, HauntData.Haunting elder, HauntData.Haunting younger) {
        List<String> traits = new ArrayList<>(elder.traits());
        for (Trait habit : Traits.of(younger.traits())) {
            if (habit.kind() == Trait.Kind.HABIT && !traits.contains(habit.id())) {
                traits.add(habit.id());
            }
        }
        HauntData.Haunting merged = new HauntData.Haunting(
                elder.uuid(),
                elder.spirit(),
                elder.seed(),
                List.copyOf(traits),
                elder.bornAt(),
                Math.max(elder.anger(), younger.anger()),
                elder.nextEventAt(),
                elder.evolved() || younger.evolved(),
                elder.notice(),
                Math.max(elder.waryUntil(), younger.waryUntil()));
        Focus nature = Traits.weakness(Traits.of(merged.traits()));
        Focus mine = Traits.weakness(Traits.of(elder.traits()));
        Focus other = mine == nature ? Traits.weakness(Traits.of(younger.traits())) : mine;
        List<HauntData.Haunting> own = new ArrayList<>(data.get(player.getUUID()));
        own.removeIf(h -> h.uuid().equals(younger.uuid()));
        own.replaceAll(h -> h.uuid().equals(elder.uuid()) ? merged : h);
        data.put(player.getUUID(), own);
        if (other != nature) {
            data.putMarks(elder.uuid(), data.marks(elder.uuid()).mergedWith(other));
        }
        data.putMarks(younger.uuid(), dev.keelfy.restless.state.Marks.NONE);
        HauntData.Grave kept = data.graves().get(elder.uuid());
        HauntData.Grave theirs = data.graves().get(younger.uuid());
        if (kept == null && theirs != null) {
            data.putGrave(elder.uuid(), theirs);
        }
        data.putGrave(younger.uuid(), null);
        Location.forget(younger.uuid());
        player.level()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.SCULK_SHRIEKER_SHRIEK,
                        SoundSource.HOSTILE,
                        0.8f,
                        0.6f);
        player.sendSystemMessage(Component.translatable("message.restless.merged"), true);
        Journal.note(
                player,
                Notebook.Kind.MERGED,
                merged,
                Story.of(elder.seed()).name(),
                Story.of(younger.seed()).name());
        return merged;
    }
}
