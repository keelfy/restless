package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.haunt.HauntEvent;
import java.util.List;
import java.util.stream.Stream;

/** Every lambda-defined event, grouped by theme in the sibling classes. */
public final class Catalog {
    private Catalog() {}

    public static List<HauntEvent> all() {
        return Stream.of(
                        Lights.all(),
                        Mechanisms.all(),
                        Sounds.all(),
                        Afflictions.all(),
                        Assaults.all(),
                        Ambushes.all(),
                        Torments.all(),
                        Brinks.all(),
                        Lethals.all(),
                        ItemEvents.all(),
                        BlockEvents.all(),
                        MobEvents.all(),
                        Presence.all(),
                        Lies.all(),
                        Illusions.all(),
                        Trees.all(),
                        Waters.all(),
                        Household.all(),
                        Seals.all(),
                        Creatures.all(),
                        Mirages.all(),
                        Veils.all(),
                        Hallucinations.all(),
                        Apparitions.all(),
                        Nether.all(),
                        TheEnd.all(),
                        Wilds.all(),
                        Habitats.all(),
                        Strangers.all(),
                        Rites.all(),
                        Fixtures.all(),
                        Tricks.all(),
                        Cameras.all(),
                        Screens.all(),
                        Remodels.all(),
                        Shrines.all(),
                        Herds.all(),
                        Sightings.all(),
                        Followers.all(),
                        Machines.all(),
                        Relics.all(),
                        Fittings.all(),
                        Signatures.all())
                .flatMap(List::stream)
                .toList();
    }
}
