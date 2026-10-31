package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Notebook.Chapter;
import dev.keelfy.restless.haunt.Notebook.Kind;
import dev.keelfy.restless.haunt.Notebook.Note;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Lays a {@link Notebook} out as book pages: contents, the guide, a few pages per met spirit, loose notes, the
 * bestiary and the chronicle. It only arranges what the player saw; it draws no conclusions for them.
 */
public final class JournalPages {
    private static final String EVOLVED = "_evolved";

    /** A run of pages under one heading in the contents. */
    private record Section(String key, List<Component> pages) {}

    private JournalPages() {}

    static Component title(Chapter chapter) {
        return Component.translatable("journal.restless.guide." + chapter.id() + ".title");
    }

    public static List<Component> of(Notebook notebook) {
        List<Section> sections = new ArrayList<>();
        sections.add(new Section("guide", guide(notebook)));
        sections.add(new Section("spirits", spirits(notebook)));
        sections.add(new Section("loose", loose(notebook)));
        sections.add(new Section("bestiary", bestiary(notebook)));
        sections.add(new Section("chronicle", chronicle(notebook)));
        sections.removeIf(s -> s.pages().isEmpty());

        MutableComponent contents = heading(Component.translatable("journal.restless.contents"));
        List<Component> pages = new ArrayList<>();
        pages.add(contents);
        for (Section section : sections) {
            int page = pages.size() + 1;
            contents.append("\n\n").append(Component.translatable("journal.restless.contents.line",
                    Component.translatable("journal.restless.section." + section.key()), page)
                .withStyle(s -> s.withColor(ChatFormatting.DARK_BLUE).withClickEvent(new ClickEvent.ChangePage(page))));
            pages.addAll(section.pages());
        }
        return pages;
    }

    private static List<Component> guide(Notebook notebook) {
        List<Component> pages = new ArrayList<>();
        for (Chapter chapter : Chapter.values()) {
            if (!notebook.has(chapter)) {
                continue;
            }
            for (int i = 1; i <= chapter.pages; i++) {
                Component text = Component.translatable("journal.restless.guide." + chapter.id() + "." + i);
                pages.add(i == 1 ? heading(title(chapter)).append("\n\n").append(text) : text);
            }
        }
        return pages;
    }

    /** A dossier per met spirit, in the order they were met: what each tool showed of it, unreliable sources marked. */
    private static List<Component> spirits(Notebook notebook) {
        List<Component> pages = new ArrayList<>();
        Map<String, Integer> unnamed = new HashMap<>();
        for (UUID spirit : notebook.met()) {
            List<Note> notes = notebook.notes().stream()
                .filter(n -> !n.kind().chronicle() && n.spirit().filter(spirit::equals).isPresent()).toList();
            pages.addAll(paged(heading(who(notes, unnamed)), notes.stream().map(JournalPages::line).toList(), perPage()));
        }
        return pages;
    }

    /** Its name once its grave was read; until then its kind, numbered when the player has met several of one kind unnamed. */
    private static Component who(List<Note> notes, Map<String, Integer> unnamed) {
        Optional<Note> photo = last(notes, Kind.PHOTO);
        Component kind = photo.map(n -> n.args().getFirst()).orElse(Component.translatable("journal.restless.unknown_kind"));
        Optional<Component> name = last(notes, Kind.GRAVE).map(n -> n.args().getFirst());
        if (name.isPresent()) {
            return Component.translatable("journal.restless.named", name.get(), kind);
        }
        int number = unnamed.merge(photo.flatMap(Note::tag).orElse(""), 1, Integer::sum);
        return number == 1 ? Component.translatable("journal.restless.unnamed", kind)
            : Component.translatable("journal.restless.unnamed_numbered", kind, number);
    }

    /** What was seen of spirits the player has not met yet, or of nobody in particular: it stays theirs to sort out. */
    private static List<Component> loose(Notebook notebook) {
        Set<UUID> met = Set.copyOf(notebook.met());
        List<Component> lines = notebook.notes().stream()
            .filter(n -> !n.kind().chronicle() && n.spirit().filter(met::contains).isEmpty())
            .map(JournalPages::line).toList();
        return paged(heading(Component.translatable("journal.restless.section.loose")).append("\n\n")
            .append(Component.translatable("journal.restless.loose.intro").withStyle(ChatFormatting.ITALIC)), lines, perPage());
    }

    /** A page per kind of spirit the player has photographed, and every trait a photo named with what it shrinks from. */
    private static List<Component> bestiary(Notebook notebook) {
        Map<String, Boolean> kinds = new LinkedHashMap<>();
        Set<Component> traits = new LinkedHashSet<>();
        for (Note note : notebook.notes()) {
            if (note.kind() != Kind.PHOTO || note.tag().isEmpty()) {
                continue;
            }
            String tag = note.tag().get();
            boolean grown = tag.endsWith(EVOLVED);
            kinds.merge(grown ? tag.substring(0, tag.length() - EVOLVED.length()) : tag, grown, Boolean::logicalOr);
            traits.add(Component.translatable("journal.restless.bestiary.trait", note.args().get(1), note.args().get(2)));
        }
        List<Component> pages = new ArrayList<>();
        kinds.forEach((kind, grown) -> {
            MutableComponent page = Component.translatable("journal.restless.bestiary." + kind);
            if (grown) {
                page.append("\n\n").append(Component.translatable("journal.restless.bestiary.grown",
                    Component.translatable("spirit.restless." + kind + EVOLVED)));
            }
            pages.add(page);
        });
        pages.addAll(paged(heading(Component.translatable("journal.restless.bestiary.traits")), List.copyOf(traits),
            Config.section("journal").integer("traits_per_page")));
        return pages;
    }

    private static List<Component> chronicle(Notebook notebook) {
        List<Component> lines = notebook.notes().stream().filter(n -> n.kind().chronicle()).map(JournalPages::line).toList();
        return paged(heading(Component.translatable("journal.restless.section.chronicle")), lines, perPage());
    }

    private static Component line(Note note) {
        if (note.kind() == Kind.OLD) {
            return note.args().getFirst();
        }
        MutableComponent line = Component.translatable("journal.restless.note." + note.kind().name().toLowerCase(Locale.ROOT),
            note.args().toArray());
        return note.kind().reliable ? line
            : line.append(" ").append(Component.translatable("journal.restless.may_lie").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    /** {@code lines}, {@code perPage} to a page and a blank line apart, under {@code heading} on the first; no pages when there are no lines. */
    private static List<Component> paged(Component heading, List<Component> lines, int perPage) {
        List<Component> pages = new ArrayList<>();
        for (int i = 0; i < lines.size(); i += perPage) {
            MutableComponent page = i == 0 ? heading.copy() : Component.empty();
            for (int j = i; j < Math.min(i + perPage, lines.size()); j++) {
                if (i == 0 || j > i) {
                    page.append("\n\n");
                }
                page.append(lines.get(j));
            }
            pages.add(page);
        }
        return pages;
    }

    private static Optional<Note> last(List<Note> notes, Kind kind) {
        return notes.stream().filter(n -> n.kind() == kind).reduce((a, b) -> b);
    }

    private static int perPage() {
        return Config.section("journal").integer("entries_per_page");
    }

    private static MutableComponent heading(Component text) {
        return Component.empty().append(text.copy().withStyle(ChatFormatting.BOLD));
    }
}
