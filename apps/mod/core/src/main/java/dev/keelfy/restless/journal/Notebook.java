package dev.keelfy.restless.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.keelfy.restless.photo.Picture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
 * @param pictures the latest developed photograph of each spirit, pasted in
 * @param filed for a banished spirit, the dossier the player filed it under: their own conclusion, right or wrong
 */
public record Notebook(List<Note> notes, Set<String> chapters, Map<UUID, Picture> pictures, Map<UUID, UUID> filed) {
    /** Ends a type tag of a grown spirit. */
    static final String EVOLVED = "_evolved";

    public static final Notebook EMPTY = new Notebook(List.of(), Set.of(), Map.of(), Map.of());

    public static final Codec<Notebook> CODEC = RecordCodecBuilder.create(i -> i.group(
                    Note.CODEC.listOf().optionalFieldOf("notes", List.of()).forGetter(Notebook::notes),
                    Codec.STRING
                            .listOf()
                            .xmap(Set::copyOf, List::copyOf)
                            .optionalFieldOf("chapters", Set.of())
                            .forGetter(Notebook::chapters),
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, Picture.CODEC)
                            .optionalFieldOf("pictures", Map.of())
                            .forGetter(Notebook::pictures),
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, UUIDUtil.STRING_CODEC)
                            .optionalFieldOf("filed", Map.of())
                            .forGetter(Notebook::filed))
            .apply(i, Notebook::new));

    /** What kind of thing was seen; decides where it goes in the journal. */
    public enum Kind {
        /**
         * A camera photo. Args: type; tag: the type id as shown ({@code shade} or {@code shade_evolved}). Older notes
         * also carry a hint and a weakness, which are no longer shown.
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
        /** Args: type, name; tag: the type id as a photo has it. */
        BANISHED(true),
        /** In the spirit's dossier. Args: name, old type, new type. */
        EVOLVED(true),
        /** A spirit laid to rest at the Requiem: the last line of its dossier. Args: name, type. */
        LAID(true),
        /** An entry from before the journal had kinds. Args: its text. */
        OLD(true);

        public static final Codec<Kind> CODEC = Codec.STRING.xmap(
                s -> valueOf(s.toUpperCase(Locale.ROOT)), k -> k.name().toLowerCase(Locale.ROOT));

        /** False for the tools a lying spirit can speak through. */
        public final boolean reliable;

        Kind(boolean reliable) {
            this.reliable = reliable;
        }

        /** Things that happened, rather than things learned about a spirit. */
        public boolean chronicle() {
            return this == RESTED || this == BANISHED || this == OLD;
        }
    }

    /**
     * @param spirit who it is about; empty when nobody knows
     * @param tag a short id some kinds carry, see {@link Kind}
     */
    public record Note(Kind kind, Optional<UUID> spirit, Optional<String> tag, List<Component> args) {
        public static final Codec<Note> CODEC = RecordCodecBuilder.create(i -> i.group(
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
        CLUES(3),
        /** The first photo, which names a type: natures, forms, the altar and the ritual. */
        WEAKNESS(5),
        /** The first grave or word of one: unfinished business. */
        GRAVES(2),
        /** The first time the edges darken. */
        SANITY(2),
        /** The first spirit that grew up. */
        EVOLUTION(2),
        /** The first spirit gone for good: essences. */
        ESSENCE(2),
        /** The Requiem was sung: the closing chapter. */
        REQUIEM(2);

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
        return new Notebook(List.copyOf(grown), chapters, pictures, filed);
    }

    /** @return null when the chapter is already open */
    public @Nullable Notebook opened(Chapter chapter) {
        if (chapters.contains(chapter.id())) {
            return null;
        }
        Set<String> grown = new HashSet<>(chapters);
        grown.add(chapter.id());
        return new Notebook(notes, Set.copyOf(grown), pictures, filed);
    }

    /** The spirit's photograph, replacing an older one: a notebook keeps one picture per spirit. */
    public Notebook pasted(UUID spirit, Picture picture) {
        Map<UUID, Picture> grown = new HashMap<>(pictures);
        grown.put(spirit, picture);
        return new Notebook(notes, chapters, Map.copyOf(grown), filed);
    }

    /** The notes replaced, everything else kept. */
    public Notebook withNotes(List<Note> replaced) {
        return new Notebook(List.copyOf(replaced), chapters, pictures, filed);
    }

    /**
     * Whether the banishment written at note {@code record} may be filed under the {@code dossier}th met spirit: both
     * are still unclaimed and, where both say what type they were, the types agree. It cannot tell whether they are the
     * same spirit; that is the player's to judge.
     */
    public boolean fits(int record, int dossier) {
        List<UUID> met = met();
        if (record < 0 || record >= notes.size() || dossier < 0 || dossier >= met.size()) {
            return false;
        }
        Note banished = notes.get(record);
        if (banished.kind() != Kind.BANISHED
                || banished.spirit().isEmpty()
                || filed.containsKey(banished.spirit().get())
                || filed.containsValue(met.get(dossier))) {
            return false;
        }
        Optional<String> seen = notes.stream()
                .filter(n -> n.kind() == Kind.PHOTO
                        && n.spirit().filter(met.get(dossier)::equals).isPresent())
                .flatMap(n -> n.tag().stream())
                .reduce((a, b) -> b);
        return banished.tag().isEmpty()
                || seen.isEmpty()
                || base(banished.tag().get()).equals(base(seen.get()));
    }

    /** @return null when {@link #fits} says no */
    public @Nullable Notebook filed(int record, int dossier) {
        if (!fits(record, dossier)) {
            return null;
        }
        Map<UUID, UUID> grown = new HashMap<>(filed);
        grown.put(notes.get(record).spirit().get(), met().get(dossier));
        return new Notebook(notes, chapters, pictures, Map.copyOf(grown));
    }

    /** Whether a photograph named a spirit of this type: it has been met, and the journal can name it. */
    public boolean identified(String type) {
        return notes.stream()
                .anyMatch(n -> n.kind() == Kind.PHOTO
                        && n.tag().filter(tag -> base(tag).equals(type)).isPresent());
    }

    /**
     * Whether the journal knows a spirit of this type, from a photograph of one or from banishing one: what the player
     * can then read about the essence it leaves.
     */
    public boolean knows(String type) {
        return identified(type)
                || notes.stream()
                        .anyMatch(n -> n.kind() == Kind.BANISHED
                                && n.tag().filter(tag -> base(tag).equals(type)).isPresent());
    }

    /** Whether the player photographed this spirit, so that they know its type. */
    public boolean photographed(UUID spirit) {
        return notes.stream()
                .anyMatch(n -> n.kind() == Kind.PHOTO
                        && n.spirit().filter(spirit::equals).isPresent());
    }

    static String base(String tag) {
        return tag.endsWith(EVOLVED) ? tag.substring(0, tag.length() - EVOLVED.length()) : tag;
    }

    public boolean has(Chapter chapter) {
        return chapters.contains(chapter.id());
    }

    /**
     * Spirits the player has met, in the order they met them: any clue that came from a spirit gives it its own pages,
     * named by kind only once it is photographed.
     */
    public List<UUID> met() {
        return notes.stream()
                .filter(n -> !n.kind().chronicle())
                .flatMap(n -> n.spirit().stream())
                .distinct()
                .toList();
    }
}
