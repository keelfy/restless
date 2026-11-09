package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.resources.Identifier;

/**
 * Builds every scenario from {@code scenarios.json}: each event there with a {@code script} (written as in
 * {@code docs/scenarios.md}) is one, in file order. It also needs a name in every language. A typo or a harmful opening
 * fails at startup.
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
        List<Scenario> all = Config.eventKeys().stream()
                .filter(key -> Config.event(key).has("script"))
                .map(key ->
                        Scenario.parse(Identifier.parse(key), Config.event(key).text("script"), lookup))
                .toList();
        for (Scenario scenario : all) {
            if (byId.containsKey(scenario.id())) {
                throw new IllegalStateException("A scenario takes the id of an event: " + scenario.id());
            }
        }
        return all;
    }
}
