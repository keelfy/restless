package dev.keelfy.restless.spirit;

import dev.keelfy.restless.haunt.event.EventTag;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * A quirk that sets two spirits of one type apart.
 *
 * @param kind which of the three axes the trait belongs to; a spirit rolls each axis on its own (see
 *     {@link Traits#roll})
 * @param affinity weight multiplier per event tag; missing tags count as 1
 * @param activity how much more often the spirit acts while {@code when} holds; 2 means twice as often
 * @param noise ticks the next event comes sooner for every sound its player makes; 0 for a spirit that ignores noise
 * @param reacts player actions that can make the spirit act right away
 * @param clues the ways this trait gives the spirit away
 * @param words what the spirit says through a spirit box
 * @param planchette ids of what the planchette may spell for this trait, in the lang file as
 *     {@code planchette.restless.<id>}
 * @param lies a spirit with this trait often names a trait it does not have, through the spirit box and the ouija board
 * @param weakness the focus this habit votes for; the spirit is weak to the focus most of its habits name. A condition
 *     or leaning carries one only if it was a voting trait before the axes existed: it then breaks ties and decides for
 *     a saved spirit with no habit, never for a spirit rolled now. {@code null} for the rest
 */
public record Trait(
        String id,
        Kind kind,
        Map<EventTag, Double> affinity,
        double activity,
        Condition when,
        double noise,
        List<Trigger> reacts,
        Set<Clue> clues,
        List<String> words,
        List<String> planchette,
        boolean lies,
        @Nullable Focus weakness) {
    /** The axis of a trait. */
    public enum Kind {
        /** What the spirit does; only habits name the nature. */
        HABIT,
        /** When the spirit acts, or what it reacts to. */
        CONDITION,
        /** What the spirit fears or wants. */
        LEANING
    }

    /** What the spirit's surroundings must be like for its activity modifier to apply. */
    public enum Condition {
        ANY,
        RAIN,
        NIGHT,
        DARK
    }

    /** A player action a spirit can react to. */
    public enum Trigger {
        /** Opens a chest or barrel. */
        CHEST,
        /** Uses a bed. */
        BED,
        /** Goes below y 0. */
        DEEP,
        /** Kills a mob. */
        KILL
    }

    public double affinity(EventTag tag) {
        return affinity.getOrDefault(tag, 1.0);
    }

    public double activity(Set<Condition> now) {
        return when == Condition.ANY || now.contains(when) ? activity : 1.0;
    }
}
