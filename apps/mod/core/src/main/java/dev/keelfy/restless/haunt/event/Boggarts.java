package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityTypes;

/** The boggart's disgust: swarms of tiny creatures that only the player sees, see {@link Swarm}. */
final class Boggarts {
    private Boggarts() {}

    static List<HauntEvent> all() {
        return List.of(
                // boggart: dozens of tiny spiders run at the player from all round, and circle at their feet
                SimpleEvent.light(
                        "boggart_spiders",
                        EventTag.MOBS,
                        Swarm::canRun,
                        player -> Swarm.run(
                                player,
                                Swarm.Spec.of("boggart_spiders", EntityTypes.SPIDER, false, SoundEvents.SPIDER_STEP))),
                // boggart: tiny spiders swarm up the player's legs and round the body to the face
                SimpleEvent.light(
                        "boggart_crawl",
                        EventTag.PRESENCE,
                        Swarm::canRun,
                        player -> Swarm.run(
                                player,
                                Swarm.Spec.of("boggart_crawl", EntityTypes.SPIDER, true, SoundEvents.SPIDER_STEP))),
                // boggart: silverfish pour across the floor round the player's feet
                SimpleEvent.light(
                        "boggart_silverfish",
                        EventTag.MOBS,
                        Swarm::canRun,
                        player -> Swarm.run(
                                player,
                                Swarm.Spec.of(
                                        "boggart_silverfish",
                                        EntityTypes.SILVERFISH,
                                        false,
                                        SoundEvents.SILVERFISH_STEP))));
    }
}
