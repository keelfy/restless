package dev.keelfy.restless.gametest;

import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.Tally;
import dev.keelfy.restless.haunt.event.HauntEvent;
import dev.keelfy.restless.haunt.event.HauntEvents;
import dev.keelfy.restless.state.HauntData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;

/** Event counts survive a save and a load. */
final class TelemetryGameTests {
    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("event_counts_survive_save_and_load", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            HauntData data = HauntData.get(helper.getLevel().getServer());
            List<String> forced = new ArrayList<>();
            for (HauntEvent event : HauntEvents.all()) {
                if (forced.size() < 3 && event.canRun(player) && Director.fire(player, event)) {
                    forced.add(event.id().toString());
                }
            }
            HauntEventGameTests.check(helper, forced.size() == 3, "three events must run on the plot");
            var saved = HauntData.codec().encodeStart(NbtOps.INSTANCE, data).getOrThrow();
            HauntData loaded = HauntData.codec().parse(NbtOps.INSTANCE, saved).getOrThrow();
            for (String id : forced) {
                HauntEventGameTests.check(helper, data.tally().of(id).ran() >= 1, id + " must count a run");
                HauntEventGameTests.check(
                        helper,
                        loaded.tally().of(id).equals(data.tally().of(id)),
                        id + " must keep its counts through a save and load");
            }
            HauntEventGameTests.check(
                    helper, loaded.tally().of("restless:never_seen").equals(Tally.Counts.NONE), "unknown ids are zero");
            player.discard();
            helper.succeed();
        });
    }
}
