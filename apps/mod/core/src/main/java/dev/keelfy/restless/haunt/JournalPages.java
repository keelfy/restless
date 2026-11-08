package dev.keelfy.restless.haunt;

import dev.keelfy.restless.content.Picture;
import dev.keelfy.restless.haunt.Notebook.Chapter;
import dev.keelfy.restless.haunt.Notebook.Kind;
import dev.keelfy.restless.haunt.Notebook.Note;
import dev.keelfy.restless.spirit.Spirits;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Lays a {@link Notebook} out as cards under sections: the guide, a dossier per met spirit, the archive of those the
 * player banished and filed, loose notes, the bestiary and the chronicle. It only arranges what the player saw; it
 * draws no conclusions for them. How many cards fit a page is up to whoever draws them.
 */
public final class JournalPages {
    private static final String EVOLVED = Notebook.EVOLVED;

    /** What clicking an entry asks for: file banishment note {@code record} under the {@code dossier}th met spirit. */
    public record Filing(int record, int dossier) {}

    /**
     * One thing written on a card; {@code kind} tells what showed it, when a note did, and {@code filing} makes it
     * something to click.
     */
    public record Entry(Optional<Kind> kind, Component text, Optional<Filing> filing) {
        static Entry plain(Component text) {
            return new Entry(Optional.empty(), text, Optional.empty());
        }
    }

    /** A heading, maybe a pasted photograph, and what is written under them. */
    public record Card(Component heading, Optional<Picture> picture, List<Entry> entries) {}

    /** A run of cards under one tab. */
    public record Section(String key, List<Card> cards) {}

    private JournalPages() {}

    static Component title(Chapter chapter) {
        return Component.translatable("journal.restless.guide." + chapter.id() + ".title");
    }

    public static List<Section> sections(Notebook notebook) {
        List<Section> sections = new ArrayList<>();
        sections.add(new Section("guide", guide(notebook)));
        sections.add(new Section("spirits", dossiers(notebook, false)));
        sections.add(new Section("archive", dossiers(notebook, true)));
        sections.add(new Section("loose", loose(notebook)));
        sections.add(new Section("bestiary", bestiary(notebook)));
        sections.add(new Section("chronicle", chronicle(notebook)));
        sections.removeIf(s -> s.cards().isEmpty());

        return List.copyOf(sections);
    }

    private static List<Card> guide(Notebook notebook) {
        List<Card> cards = new ArrayList<>();
        for (Chapter chapter : Chapter.values()) {
            if (!notebook.has(chapter)) {
                continue;
            }
            List<Entry> paragraphs = new ArrayList<>();
            for (int i = 1; i <= chapter.pages; i++) {
                paragraphs.add(Entry.plain(Component.translatable("journal.restless.guide." + chapter.id() + "." + i)));
            }
            cards.add(new Card(title(chapter), Optional.empty(), paragraphs));
        }
        return cards;
    }

    /**
     * A dossier per met spirit, in the order they were met: what each tool showed of it, unreliable sources marked. A
     * spirit the player filed a banishment under moves to the archive under the name that note gave it; the others
     * offer the banishments they could be.
     */
    private static List<Card> dossiers(Notebook notebook, boolean archive) {
        List<Card> cards = new ArrayList<>();
        Map<String, Integer> unnamed = new HashMap<>();
        Map<UUID, Note> banished = new HashMap<>();
        for (Note note : notebook.notes()) {
            note.spirit().map(notebook.filed()::get).ifPresent(dossier -> banished.put(dossier, note));
        }
        List<UUID> met = notebook.met();
        for (int i = 0; i < met.size(); i++) {
            UUID spirit = met.get(i);
            List<Note> notes = notebook.notes().stream()
                    .filter(n -> !n.kind().chronicle()
                            && n.spirit().filter(spirit::equals).isPresent())
                    .toList();
            Component who = who(notes, unnamed);
            Optional<Picture> picture = Optional.ofNullable(notebook.pictures().get(spirit));
            List<Entry> entries = new ArrayList<>();
            Note gone = banished.get(spirit);
            if (gone != null) {
                who = Component.translatable(
                        "journal.restless.named",
                        gone.args().get(1),
                        gone.args().getFirst());
                entries.add(entry(gone));
            }
            notes.stream().map(JournalPages::entry).forEach(entries::add);
            if (gone == null) {
                for (int record = 0; record < notebook.notes().size(); record++) {
                    if (notebook.fits(record, i)) {
                        entries.add(new Entry(
                                Optional.empty(),
                                Component.translatable(
                                        "journal.restless.file",
                                        notebook.notes().get(record).args().get(1)),
                                Optional.of(new Filing(record, i))));
                    }
                }
            }
            if ((gone != null) == archive) {
                cards.add(new Card(who, picture, entries));
            }
        }
        return cards;
    }

    /**
     * Its name once its grave was read; until then its kind, numbered when the player has met several of one kind
     * unnamed.
     */
    private static Component who(List<Note> notes, Map<String, Integer> unnamed) {
        Optional<Note> photo = last(notes, Kind.PHOTO);
        Component kind =
                photo.map(n -> n.args().getFirst()).orElse(Component.translatable("journal.restless.unknown_kind"));
        Optional<Component> name = last(notes, Kind.GRAVE).map(n -> n.args().getFirst());
        if (name.isPresent()) {
            return Component.translatable("journal.restless.named", name.get(), kind);
        }
        int number = unnamed.merge(photo.flatMap(Note::tag).orElse(""), 1, Integer::sum);
        return number == 1
                ? Component.translatable("journal.restless.unnamed", kind)
                : Component.translatable("journal.restless.unnamed_numbered", kind, number);
    }

    /** What was seen of spirits the player has not met yet, or of nobody in particular: it stays theirs to sort out. */
    private static List<Card> loose(Notebook notebook) {
        Set<UUID> met = Set.copyOf(notebook.met());
        List<Entry> entries = notebook.notes().stream()
                .filter(n -> !n.kind().chronicle()
                        && n.spirit().filter(met::contains).isEmpty())
                .map(JournalPages::entry)
                .toList();
        if (entries.isEmpty()) {
            return List.of();
        }
        List<Entry> all = new ArrayList<>();
        all.add(Entry.plain(
                Component.translatable("journal.restless.loose.intro").withStyle(ChatFormatting.ITALIC)));
        all.addAll(entries);
        return List.of(new Card(Component.translatable("journal.restless.section.loose"), Optional.empty(), all));
    }

    /** A card per kind of spirit the player has photographed. */
    private static List<Card> bestiary(Notebook notebook) {
        Map<String, Boolean> kinds = new LinkedHashMap<>();
        for (Note note : notebook.notes()) {
            if (note.kind() != Kind.PHOTO || note.tag().isEmpty()) {
                continue;
            }
            String tag = note.tag().get();
            boolean grown = tag.endsWith(EVOLVED);
            kinds.merge(grown ? tag.substring(0, tag.length() - EVOLVED.length()) : tag, grown, Boolean::logicalOr);
        }
        List<Card> cards = new ArrayList<>();
        kinds.forEach((kind, grown) -> {
            List<Entry> entries = new ArrayList<>();
            entries.add(Entry.plain(Component.translatable("journal.restless.bestiary." + kind)));
            Spirits.byId(kind)
                    .ifPresent(type -> entries.add(Entry.plain(Component.translatable(
                            "journal.restless.form." + type.form().name().toLowerCase(Locale.ROOT)))));
            entries.add(Entry.plain(Component.translatable("journal.restless.met." + kind)));
            if (grown) {
                entries.add(Entry.plain(Component.translatable(
                        "journal.restless.bestiary.grown",
                        Component.translatable("spirit.restless." + kind + EVOLVED))));
            }
            cards.add(new Card(
                    Component.translatable("journal.restless.bestiary." + kind + ".name"), Optional.empty(), entries));
        });
        return cards;
    }

    private static List<Card> chronicle(Notebook notebook) {
        List<Entry> entries = notebook.notes().stream()
                .filter(n -> n.kind().chronicle())
                .map(JournalPages::entry)
                .toList();
        return entries.isEmpty()
                ? List.of()
                : List.of(new Card(
                        Component.translatable("journal.restless.section.chronicle"), Optional.empty(), entries));
    }

    private static Entry entry(Note note) {
        return new Entry(Optional.of(note.kind()), line(note), Optional.empty());
    }

    private static Component line(Note note) {
        if (note.kind() == Kind.OLD) {
            return note.args().getFirst();
        }
        MutableComponent line = Component.translatable(
                "journal.restless.note." + note.kind().name().toLowerCase(Locale.ROOT),
                note.args().toArray());
        return note.kind().reliable
                ? line
                : line.append(" ")
                        .append(Component.translatable("journal.restless.may_lie")
                                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    private static Optional<Note> last(List<Note> notes, Kind kind) {
        return notes.stream().filter(n -> n.kind() == kind).reduce((a, b) -> b);
    }
}
