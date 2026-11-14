package dev.keelfy.restless.gametest;

import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.event.HauntEvent;
import dev.keelfy.restless.haunt.event.HauntEvents;
import dev.keelfy.restless.haunt.recall.Said;
import dev.keelfy.restless.journal.Notebook;
import dev.keelfy.restless.spirit.Spirits;
import dev.keelfy.restless.spirit.Trait;
import dev.keelfy.restless.spirit.Traits;
import dev.keelfy.restless.state.HauntData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;

/**
 * Every habit has a tell: only a spirit with that habit rolls it, it runs, and the journal notes it. A tell that needs
 * something near the player to act on sets it up in {@link #SETUPS}.
 */
final class TellGameTests {
    /** What a tell needs around the player before it can run, by habit. */
    private static final Map<String, Consumer<ServerPlayer>> SETUPS = new HashMap<>();

    private TellGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("tell_every_habit_has_one", helper -> {
            for (Trait habit : Traits.all()) {
                if (habit.kind() == Trait.Kind.HABIT) {
                    HauntEventGameTests.check(
                            helper, tell(habit.id()) != null, "the habit " + habit.id() + " has no tell");
                }
            }
            helper.succeed();
        });
        for (Trait habit : Traits.all()) {
            if (habit.kind() != Trait.Kind.HABIT) {
                continue;
            }
            String id = habit.id();
            tests.put("tell_" + id, helper -> {
                HauntEvent tell = tell(id);
                if (tell == null) {
                    helper.fail("no tell for " + id);
                    return;
                }
                ServerPlayer player = HauntEventGameTests.connected(helper);
                SETUPS.getOrDefault(id, p -> {}).accept(player);
                var type = Spirits.byId("shade").orElseThrow();
                List<Trait> without = Traits.all().stream()
                        .filter(t -> t.kind() == Trait.Kind.HABIT && !t.id().equals(id))
                        .limit(3)
                        .toList();
                List<Trait> with = new ArrayList<>(without);
                with.add(habit);
                HauntEventGameTests.check(
                        helper,
                        Director.weight(tell, player, type, without, 1.0, false) == 0.0,
                        "a spirit without " + id + " must never roll its tell");
                HauntEventGameTests.check(
                        helper,
                        Director.weight(tell, player, type, with, 1.0, false) > 0.0,
                        "a spirit with " + id + " must roll its tell here");
                UUID spirit = spirit(player, id);
                Said.during(spirit, () -> tell.run(player));
                boolean noted = HauntData.get(player.level().getServer()).notebook(player.getUUID()).notes().stream()
                        .anyMatch(n -> n.kind() == Notebook.Kind.TELL
                                && n.spirit().filter(spirit::equals).isPresent()
                                && n.args().stream()
                                        .anyMatch(a -> a.getContents() instanceof TranslatableContents t
                                                && t.getKey().equals("journal.restless.tell." + id)));
                HauntEventGameTests.check(helper, noted, "the tell of " + id + " must be noted in the journal");
                helper.succeed();
            });
        }
    }

    private static HauntEvent tell(String habit) {
        return HauntEvents.all().stream()
                .filter(e -> habit.equals(e.habit()))
                .findFirst()
                .orElse(null);
    }

    private static UUID spirit(ServerPlayer player, String habit) {
        HauntData.Haunting born = new HauntData.Haunting(
                UUID.randomUUID(), "shade", 1L, List.of(habit), 0L, 0.0, Long.MAX_VALUE, false, 0, 0L);
        HauntData data = HauntData.get(player.level().getServer());
        List<HauntData.Haunting> all =
                new ArrayList<>(data.get(player.getUUID()) == null ? List.of() : data.get(player.getUUID()));
        all.add(born);
        data.put(player.getUUID(), all);
        return born.uuid();
    }
}
