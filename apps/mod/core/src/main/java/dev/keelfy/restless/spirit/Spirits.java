package dev.keelfy.restless.spirit;

import dev.keelfy.restless.haunt.EventTag;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Spirits {
    public static final SpiritType POLTERGEIST = new SpiritType("poltergeist", Map.of(
        EventTag.DOORS, 4.0,
        EventTag.LIGHTS, 1.0,
        EventTag.AFFLICTION, 0.5,
        EventTag.ASSAULT, 0.5,
        EventTag.SOUND, 0.5,
        EventTag.ITEMS, 3.0,
        EventTag.BLOCKS, 1.5,
        EventTag.PRESENCE, 0.5
    ), 1.3);

    public static final SpiritType SHADE = new SpiritType("shade", Map.of(
        EventTag.DOORS, 0.3,
        EventTag.LIGHTS, 3.0,
        EventTag.AFFLICTION, 2.0,
        EventTag.ASSAULT, 1.0,
        EventTag.SOUND, 2.5,
        EventTag.ITEMS, 0.5,
        EventTag.PRESENCE, 2.0
    ), 0.8);

    private static final List<SpiritType> ALL = List.of(POLTERGEIST, SHADE);

    private Spirits() {}

    public static List<SpiritType> all() {
        return ALL;
    }

    public static Optional<SpiritType> byId(String id) {
        return ALL.stream().filter(s -> s.id().equals(id)).findFirst();
    }
}
