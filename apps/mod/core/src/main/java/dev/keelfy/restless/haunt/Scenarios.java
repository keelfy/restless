package dev.keelfy.restless.haunt;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.resources.Identifier;

/**
 * Every scenario, written as in {@code docs/scenarios.md} (number there in the comment). Each needs an entry in
 * {@code scenarios.json} (weight and gate) and a name in every language. A typo or a harmful opening fails at startup.
 */
final class Scenarios {
    private Scenarios() {}

    /** @param events the plain events, which are all a beat may name */
    static List<Scenario> all(List<HauntEvent> events) {
        Map<Identifier, HauntEvent> byId = events.stream().collect(Collectors.toMap(HauntEvent::id, e -> e));
        Function<Identifier, HauntEvent> lookup = id -> {
            HauntEvent event = byId.get(id);
            if (event == null) {
                throw new IllegalStateException("A scenario names no such event: " + id);
            }
            return event;
        };
        List<Scenario> all = List.of(
                // 1
                scenario("lights_out", "lantern_flicker +2 torch_chain +3 breathing +3 blackout", lookup),
                // 2
                scenario("playing_with_candles", "candles_out +4 candles_light +4 candles_out +2 ghost_laugh", lookup),
                // 4
                scenario("hearth_dies", "furnace_dies +0 campfire_out +3 cold_breath +3 frostbite", lookup),
                // 6
                scenario("lamp_code", "lamp_off +2 lamp_off +2 lamp_off +3 three_knocks +2 lever", lookup),
                // 99
                scenario("the_debt", "red_vignette +2 blackout +3 wrath +3 the_call", lookup));
        for (Scenario scenario : all) {
            if (byId.containsKey(scenario.id())) {
                throw new IllegalStateException("A scenario takes the id of an event: " + scenario.id());
            }
        }
        return all;
    }

    private static Scenario scenario(String id, String script, Function<Identifier, HauntEvent> lookup) {
        return Scenario.parse(Identifier.parse(id), script, lookup);
    }
}
