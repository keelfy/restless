package dev.keelfy.restless.haunt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import org.jspecify.annotations.Nullable;

/**
 * One player's journal as data: what they saw and which guide chapters they have opened. It keeps observations, never
 * conclusions; how they are laid out on paper is {@link JournalPages}'s business.
 *
 * @param notes oldest first
 * @param chapters ids of the {@link Chapter}s the player has opened
 */
public record Notebook(List<Note> notes, Set<String> chapters) {
    public static final Notebook EMPTY = new Notebook(List.of(), Set.of());

    static final Codec<Notebook> CODEC = RecordCodecBuilder.create(i -> i.group(
                    Note.CODEC.listOf().optionalFieldOf("notes", List.of()).forGetter(Notebook::notes),
                    Codec.STRING
                            .listOf()
                            .xmap(Set::copyOf, List::copyOf)
                            .optionalFieldOf("chapters", Set.of())
                            .forGetter(Notebook::chapters))
            .apply(i, Notebook::new));

    /** What kind of thing was seen; decides where it goes in the journal. */
    public enum Kind {
        /**
         * A camera photo. Args: type, hint, weakness; tag: the type id as shown ({@code shade} or
         * {@code shade_evolved}).
         */
        PHOTO(true),
        /** A print in ash. Args: the print. */
        FOOTPRINTS(true),
        /** A spirit box word. Args: the word. */
        VOICE(false),
        /** Letters the planchette spelled. Args: the hint. */
        PLANCHETTE(false),
        /** The spirit box told where the bones lie. Args: x, z. */
        GRAVE_WHISPER(true),
        /** A gravestone was read. Args: name, x, z, wanted thing. */
        GRAVE(true),
        /** A dream on waking. Args: the line. */
        DREAM(true),
        /** Args: name, type. */
        RESTED(true),
        /** Args: type, name. */
        BANISHED(true),
        /** Args: name, old type, new type. */
        EVOLVED(true),
        /** An entry from before the journal had kinds. Args: its text. */
        OLD(true);

        static final Codec<Kind> CODEC = Codec.STRING.xmap(
                s -> valueOf(s.toUpperCase(Locale.ROOT)), k -> k.name().toLowerCase(Locale.ROOT));

        /** False for the tools a lying spirit can speak through. */
        public final boolean reliable;

        Kind(boolean reliable) {
            this.reliable = reliable;
        }

        /** Things that happened, rather than things learned about a spirit. */
        public boolean chronicle() {
            return this == RESTED || this == BANISHED || this == EVOLVED || this == OLD;
        }

        /** A spirit is met, and gets its own pages, once it was photographed or its grave read. */
        public boolean meets() {
            return this == PHOTO || this == GRAVE;
        }
    }

    /**
     * @param spirit who it is about; empty when nobody knows
     * @param tag a short id some kinds carry, see {@link Kind}
     */
    public record Note(Kind kind, Optional<UUID> spirit, Optional<String> tag, List<Component> args) {
        static final Codec<Note> CODEC = RecordCodecBuilder.create(i -> i.group(
                        Kind.CODEC.fieldOf("kind").forGetter(Note::kind),
                        UUIDUtil.CODEC.optionalFieldOf("spirit").forGetter(Note::spirit),
                        Codec.STRING.optionalFieldOf("tag").forGetter(Note::tag),
                        ComponentSerialization.CODEC
                                .listOf()
                                .optionalFieldOf("args", List.of())
                                .forGetter(Note::args))
                .apply(i, Note::new));
    }

    /** Guide chapters, opened one at a time as the player runs into what each is about. */
    public enum Chapter {
        /** Given with the journal. */
        START(2),
        /** The first event a spirit runs on the player: the signs and the tools. */
        HAUNTED(3),
        /** The first clue: reading clues, and that some lie. */
        CLUES(2),
        /** The first photo, which shows a weakness: the altar and the ritual. */
        WEAKNESS(3),
        /** The first grave or word of one: unfinished business. */
        GRAVES(2),
        /** The first time the edges darken. */
        SANITY(2),
        /** The first spirit that grew up. */
        EVOLUTION(2),
        /** The first spirit gone for good: essences. */
        ESSENCE(2);

        /** How many book pages its text takes. */
        public final int pages;

        Chapter(int pages) {
            this.pages = pages;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** @return null when the notebook already holds this exact note */
    public @Nullable Notebook with(Note note) {
        if (notes.contains(note)) {
            return null;
        }
        List<Note> grown = new ArrayList<>(notes);
        grown.add(note);
        return new Notebook(List.copyOf(grown), chapters);
    }

    /** @return null when the chapter is already open */
    public @Nullable Notebook opened(Chapter chapter) {
        if (chapters.contains(chapter.id())) {
            return null;
        }
        Set<String> grown = new HashSet<>(chapters);
        grown.add(chapter.id());
        return new Notebook(notes, Set.copyOf(grown));
    }

    public boolean has(Chapter chapter) {
        return chapters.contains(chapter.id());
    }

    /** Spirits the player has met, in the order they met them. */
    public List<UUID> met() {
        return notes.stream()
                .filter(n -> n.kind().meets())
                .flatMap(n -> n.spirit().stream())
                .distinct()
                .toList();
    }
}
