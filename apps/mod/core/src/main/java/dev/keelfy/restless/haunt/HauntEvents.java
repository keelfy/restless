package dev.keelfy.restless.haunt;

import dev.keelfy.restless.haunt.event.BlindnessEvent;
import dev.keelfy.restless.haunt.event.Catalog;
import dev.keelfy.restless.haunt.event.ColdTouchEvent;
import dev.keelfy.restless.haunt.event.DoorEvent;
import dev.keelfy.restless.haunt.event.ExtinguishLightEvent;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;

public final class HauntEvents {
    private static final List<HauntEvent> ALL = Stream.concat(
            Stream.of(new ExtinguishLightEvent(), new DoorEvent(), new BlindnessEvent(), new ColdTouchEvent()),
            Catalog.all().stream())
        .toList();

    private HauntEvents() {}

    public static List<HauntEvent> all() {
        return ALL;
    }

    public static Optional<HauntEvent> byId(Identifier id) {
        return ALL.stream().filter(e -> e.id().equals(id)).findFirst();
    }
}
