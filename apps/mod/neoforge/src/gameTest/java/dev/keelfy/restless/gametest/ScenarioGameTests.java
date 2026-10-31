package dev.keelfy.restless.gametest;

import static dev.keelfy.restless.gametest.HauntEventGameTests.check;
import static dev.keelfy.restless.gametest.HauntEventGameTests.connected;

import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntEvents;
import dev.keelfy.restless.haunt.HauntTier;
import dev.keelfy.restless.haunt.Scenario;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

final class ScenarioGameTests {
    private ScenarioGameTests() {}

    /** Records the server tick each time it runs. */
    private record Probe(
            Identifier id, HauntTier tier, Set<EventTag> tags, boolean ready, List<Integer> ran, boolean[] held)
            implements HauntEvent {
        Probe(String id, HauntTier tier, EventTag tag, boolean ready) {
            this(Identifier.withDefaultNamespace(id), tier, Set.of(tag), ready, new ArrayList<>(), new boolean[1]);
        }

        @Override
        public boolean busy(ServerPlayer player) {
            return held[0];
        }

        @Override
        public boolean canRun(ServerPlayer player) {
            return ready;
        }

        @Override
        public void run(ServerPlayer player) {
            ran.add(player.level().getServer().getTickCount());
        }
    }

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        HauntEventGameTests.TIMEOUT.put("scenario_beats", 80);
        tests.put("scenario_beats", helper -> {
            Probe knock = new Probe("probe_knock", HauntTier.LIGHT, EventTag.SOUND, true);
            Probe missing = new Probe("probe_missing", HauntTier.LIGHT, EventTag.ITEMS, false);
            Probe hit = new Probe("probe_hit", HauntTier.HARMFUL, EventTag.ASSAULT, true);
            Scenario scenario = Scenario.parse(
                    Identifier.withDefaultNamespace("probe"),
                    "probe_knock +1 probe_missing +1 probe_hit",
                    id -> List.of(knock, missing, hit).stream()
                            .filter(e -> e.id().equals(id))
                            .findFirst()
                            .orElseThrow());
            check(helper, scenario.tier() == HauntTier.HARMFUL, "Any harmful beat must make the scenario harmful");
            check(
                    helper,
                    scenario.tags().equals(Set.of(EventTag.SOUND, EventTag.ITEMS, EventTag.ASSAULT)),
                    "A scenario must carry every beat's tags");
            ServerPlayer player = connected(helper);
            int start = helper.getLevel().getServer().getTickCount();
            scenario.run(player);
            check(helper, knock.ran().equals(List.of(start)), "The first beat must run at once");
            check(helper, hit.ran().isEmpty(), "Later beats must wait");
            helper.runAfterDelay(50, () -> {
                check(helper, missing.ran().isEmpty(), "A beat with nothing to act on must be skipped");
                check(helper, hit.ran().size() == 1, "A beat after a skipped one must still run, once");
                int after = hit.ran().getFirst() - start;
                check(helper, after >= 39 && after <= 41, "Delays must add up: the last beat came after " + after);
                player.discard();
                helper.succeed();
            });
        });
        tests.put("scenario_dropped_with_player", helper -> {
            Probe knock = new Probe("probe_knock", HauntTier.LIGHT, EventTag.SOUND, true);
            Probe later = new Probe("probe_later", HauntTier.LIGHT, EventTag.SOUND, true);
            Scenario scenario = Scenario.parse(
                    Identifier.withDefaultNamespace("probe"),
                    "probe_knock +1 probe_later",
                    id -> id.equals(knock.id()) ? knock : later);
            ServerPlayer player = connected(helper);
            scenario.run(player);
            player.discard();
            helper.runAfterDelay(30, () -> {
                check(helper, later.ran().isEmpty(), "A player gone must drop the rest of the scenario");
                helper.succeed();
            });
        });
        HauntEventGameTests.TIMEOUT.put("scenario_waits", 120);
        tests.put("scenario_waits", helper -> {
            Probe knock = new Probe("probe_knock", HauntTier.LIGHT, EventTag.SOUND, true);
            Probe after = new Probe("probe_after", HauntTier.LIGHT, EventTag.SOUND, true);
            Probe last = new Probe("probe_last", HauntTier.LIGHT, EventTag.SOUND, true);
            Scenario scenario = Scenario.parse(
                    Identifier.withDefaultNamespace("probe"),
                    "probe_knock >gone +1 probe_after +1 probe_last",
                    id -> List.of(knock, after, last).stream()
                            .filter(e -> e.id().equals(id))
                            .findFirst()
                            .orElseThrow());
            check(helper, scenario.beats().get(1).until() == Scenario.Wait.GONE, "A wait must be read from the script");
            ServerPlayer player = connected(helper);
            knock.held()[0] = true;
            scenario.run(player);
            helper.runAfterDelay(40, () -> {
                check(helper, after.ran().isEmpty(), "A waiting beat must hold until the wait is met");
                knock.held()[0] = false;
            });
            helper.runAfterDelay(100, () -> {
                check(helper, after.ran().size() == 1, "A waiting beat must run once the wait is met");
                check(helper, last.ran().size() == 1, "Beats after a wait must follow it");
                player.discard();
                helper.succeed();
            });
        });
        tests.put("scenario_rules", helper -> {
            Probe hit = new Probe("probe_hit", HauntTier.HARMFUL, EventTag.ASSAULT, true);
            boolean refused;
            try {
                new Scenario(Identifier.withDefaultNamespace("probe"), List.of(new Scenario.Beat(0, hit)));
                refused = false;
            } catch (IllegalArgumentException expected) {
                refused = true;
            }
            check(helper, refused, "A scenario must not open with harm");
            Probe calm = new Probe("probe_calm", HauntTier.LIGHT, EventTag.SOUND, true);
            boolean waited;
            try {
                new Scenario(
                        Identifier.withDefaultNamespace("probe"),
                        List.of(new Scenario.Beat(0, Scenario.Wait.GONE, calm)));
                waited = false;
            } catch (IllegalArgumentException expected) {
                waited = true;
            }
            check(helper, waited, "A scenario must not open with a wait");
            Scenario lights = (Scenario) HauntEvents.byId(Identifier.withDefaultNamespace("lights_out"))
                    .orElseThrow();
            check(helper, lights.beats().size() == 4, "lights_out must have its four beats");
            check(helper, !lights.opensAt(0.69, false), "Below its gate a scenario must stay shut");
            check(helper, lights.opensAt(0.7, false), "At its gate a scenario must open");
            HauntEvent debt = HauntEvents.byId(Identifier.withDefaultNamespace("the_debt"))
                    .orElseThrow();
            check(helper, !debt.opensAt(1.0, false), "An evolved-only scenario must wait for evolution");
            check(helper, debt.opensAt(0.7, true), "An evolved spirit must open its scenario");
            check(
                    helper,
                    HauntEvents.all().stream()
                            .filter(e -> !(e instanceof Scenario))
                            .allMatch(e -> e.opensAt(0.0, false)),
                    "Plain events must have no gate");
            helper.succeed();
        });
    }
}
