package dev.keelfy.restless.gametest;

import dev.keelfy.restless.divine.Divination;
import dev.keelfy.restless.sanity.Sanity;
import dev.keelfy.restless.spirit.Clue;
import dev.keelfy.restless.spirit.Trait;
import dev.keelfy.restless.spirit.Traits;
import dev.keelfy.restless.spirit.life.Evolution;
import dev.keelfy.restless.state.HauntData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;

/** One habit stays hidden from every tool until the player is shaken or the spirit evolves; evolving adds a habit. */
final class HiddenGameTests {
    private static final List<String> HABITS = List.of("light_eater", "cold_touch", "blinder", "door_slammer");

    private HiddenGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("hidden_habit_shows_only_to_the_shaken", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            HauntData.Haunting spirit = spirit(player, false);
            Trait hidden = Traits.hidden(spirit.seed(), Traits.of(spirit.traits()));
            check(helper, hidden != null, "a spirit with four habits must hide one");
            Sanity.set(player, 1.0);
            check(helper, !seen(player, hidden), "at full sanity no tool may ever name the hidden habit");
            Sanity.set(player, 0.0);
            check(helper, seen(player, hidden), "a shaken player must come to see the hidden habit");
            helper.succeed();
        });
        tests.put("hidden_habit_shows_once_evolved", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            HauntData.Haunting spirit = spirit(player, true);
            Trait hidden = Traits.hidden(spirit.seed(), Traits.of(spirit.traits()));
            Sanity.set(player, 1.0);
            check(helper, seen(player, hidden), "an evolved spirit hides nothing");
            helper.succeed();
        });
        tests.put("evolution_adds_a_habit_that_survives_a_save", helper -> {
            HauntData.Haunting young = new HauntData.Haunting(
                    UUID.randomUUID(), "shade", 5L, List.of("light_eater", "cold_touch"), 0L, 0.0, 0L, false, 0, 0L);
            HauntData.Haunting grown = Evolution.grow(young);
            check(helper, grown.evolved(), "the grown spirit must be evolved");
            check(helper, grown.traits().size() == 3, "evolving must add one habit");
            check(
                    helper,
                    Traits.weakness(Traits.of(grown.traits())) == Traits.weakness(Traits.of(young.traits())),
                    "the added habit must keep the nature");
            var saved =
                    HauntData.Haunting.CODEC.encodeStart(NbtOps.INSTANCE, grown).getOrThrow();
            HauntData.Haunting loaded =
                    HauntData.Haunting.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
            check(helper, loaded.traits().equals(grown.traits()), "the added habit must survive a save and load");
            check(helper, loaded.evolved(), "the spirit must stay evolved after a load");
            helper.succeed();
        });
    }

    private static HauntData.Haunting spirit(ServerPlayer player, boolean evolved) {
        HauntData.Haunting born = new HauntData.Haunting(
                UUID.randomUUID(), "shade", 11L, HABITS, 0L, 0.0, Long.MAX_VALUE, evolved, 0, 0L);
        HauntData.get(player.level().getServer()).put(player.getUUID(), new ArrayList<>(List.of(born)));
        return born;
    }

    /** Whether any of many readings by every tool names {@code trait}. */
    private static boolean seen(ServerPlayer player, Trait trait) {
        for (int i = 0; i < 300; i++) {
            for (Clue clue : List.of(Clue.SILHOUETTE, Clue.VOICE, Clue.PLANCHETTE)) {
                if (Divination.read(player, clue)
                        .filter(r -> r.trait().equals(trait))
                        .isPresent()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }
}
