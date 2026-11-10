package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.haunt.scenario.Scenarios;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;

public final class HauntEvents {
    private static final List<HauntEvent> EVENTS = Stream.concat(
                    Stream.of(
                            new ExtinguishLightEvent(),
                            new DoorEvent(),
                            new BlindnessEvent(),
                            new ColdTouchEvent(),
                            new ShadowEvent(),
                            new DoubleEvent()),
                    Catalog.all().stream())
            .toList();
    /** The plain events, then the scenarios made of them. */
    private static final List<HauntEvent> ALL =
            Stream.concat(EVENTS.stream(), Scenarios.all(EVENTS).stream()).toList();

    private HauntEvents() {}

    public static List<HauntEvent> all() {
        return ALL;
    }

    public static Optional<HauntEvent> byId(Identifier id) {
        return ALL.stream().filter(e -> e.id().equals(id)).findFirst();
    }
}
