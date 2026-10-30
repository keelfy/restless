package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.haunt.HauntEvent;
import java.util.List;
import java.util.stream.Stream;

/** Every lambda-defined event, grouped by theme in the sibling classes. */
public final class Catalog {
    private Catalog() {}

    public static List<HauntEvent> all() {
        return Stream.of(
                Lights.all(), Mechanisms.all(), Sounds.all(), Afflictions.all(), Assaults.all(),
                ItemEvents.all(), BlockEvents.all(), MobEvents.all(), Presence.all(), Lies.all(), Illusions.all(), Trees.all(), Waters.all(), Household.all())
            .flatMap(List::stream)
            .toList();
    }
}
