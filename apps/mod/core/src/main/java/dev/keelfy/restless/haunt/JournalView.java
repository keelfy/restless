package dev.keelfy.restless.haunt;

import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.PhotoTooltip;
import dev.keelfy.restless.content.Picture;
import dev.keelfy.restless.haunt.Notebook.Kind;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

/**
 * A field notebook with bookmarks and pages fitted to the reader's font and the book's size: what it says, which page
 * is open, and how to draw and click it. The screen and the book in the reader's hands are both views onto it.
 */
@OnlyIn(Dist.CLIENT)
public final class JournalView {
    private static final int INK = 0xFF382B24;
    private static final int LINK = 0xFF713E38;
    private static final int MUTED = 0xFF77634D;
    private static final int PAPER = 0xFFE5D5AD;
    private static final int LINE = 12;
    private static final int ICON = 16;
    private static final int ENTRY_GAP = 5;
    private static final int EDGE = 0xFF5A4630;

    /** How far the bookmarks stick out past the book's right edge. */
    public static final int OVERHANG = 70;

    private static final int TAB_TOP = 24;
    private static final int TAB_HEIGHT = 24;
    private static final int TAB_STEP = 26;

    /** The tools and finds a dossier shows slots for, in order; the ones the player has used light up. */
    private static final List<Kind> CLUES =
            List.of(Kind.PHOTO, Kind.FOOTPRINTS, Kind.VOICE, Kind.PLANCHETTE, Kind.GRAVE);

    private static final Map<Kind, ItemStack> ICONS = new EnumMap<>(Kind.class);

    /** What a page holds, top to bottom. */
    private record Block(
            List<FormattedCharSequence> lines,
            @Nullable Kind icon,
            @Nullable Picture picture,
            @Nullable Set<Kind> clues,
            JournalPages.@Nullable Filing filing,
            int height) {}

    /** Where a filing entry was drawn this frame, for clicks. */
    private record Hit(int x, int y, int width, int height, JournalPages.Filing filing) {}

    /** A bookmark or a page arrow: where it sits on the book, what it says and what it does. */
    public record Control(
            int x,
            int y,
            int width,
            int height,
            Component label,
            int bookmark,
            BooleanSupplier active,
            Runnable press) {
        public boolean contains(double px, double py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }

    private final Font font;
    private final List<List<Block>> pages = new ArrayList<>();
    private final List<Hit> hits = new ArrayList<>();
    private JournalPayload data;
    private List<Block> laid = new ArrayList<>();
    private List<Control> controls = List.of();
    private int room;
    private int used;
    private int section;
    private int page;
    private int width;
    private int height;
    private int sidebar;
    private int textX;
    private int textWidth;

    public JournalView(Font font, JournalPayload data, @Nullable String key) {
        this.font = font;
        this.data = data;
        section = indexOf(key);
    }

    private int indexOf(@Nullable String key) {
        for (int i = 0; i < data.sections().size(); i++) {
            if (data.sections().get(i).key().equals(key)) {
                return i;
            }
        }
        return 0;
    }

    /** The key of the open section, so that a book with new pages stays open at the same tab. */
    public @Nullable String sectionKey() {
        return section < data.sections().size() ? data.sections().get(section).key() : null;
    }

    public JournalPayload data() {
        return data;
    }

    /** Puts new pages in the book, in the same tab. */
    public void update(JournalPayload fresh) {
        String key = sectionKey();
        data = fresh;
        section = indexOf(key);
        if (width > 0) {
            layout(width, height);
        }
    }

    /** Fits the pages to a book of this many pixels. */
    public void layout(int width, int height) {
        this.width = width;
        this.height = height;
        sidebar = width / 2 - 10;
        textX = sidebar + 22;
        textWidth = width - sidebar - 40;
        List<Control> list = new ArrayList<>();
        for (int i = 0; i < data.sections().size(); i++) {
            int index = i;
            // A bookmark is tucked a little way into the pages and the rest of it sticks out of the edge.
            list.add(new Control(
                    width - 3,
                    TAB_TOP + i * TAB_STEP,
                    OVERHANG + 3,
                    TAB_HEIGHT,
                    Component.translatable(
                            "journal.restless.tab." + data.sections().get(i).key()),
                    i,
                    () -> true,
                    () -> select(index)));
        }
        list.add(new Control(textX, height - 32, 28, 20, Component.literal("<"), -1, () -> page > 0, () -> turn(-1)));
        list.add(new Control(
                width - 46,
                height - 32,
                28,
                20,
                Component.literal(">"),
                -1,
                () -> page + 1 < pages.size(),
                () -> turn(1)));
        controls = List.copyOf(list);
        paginate();
    }

    public List<Control> controls() {
        return controls;
    }

    public boolean selected(Control control) {
        return control.bookmark() >= 0 && section == control.bookmark();
    }

    /** Where the pages start, for whoever places the buttons that are not the view's own. */
    public int sidebar() {
        return sidebar;
    }

    public void select(int index) {
        if (section != index) {
            section = index;
            page = 0;
            paginate();
        }
    }

    public void turn(int delta) {
        page = Math.clamp(page + delta, 0, pages.size() - 1);
    }

    private Component sectionTitle() {
        return data.sections().isEmpty()
                ? Component.translatable("item.restless.journal")
                : Component.translatable("journal.restless.section."
                        + data.sections().get(section).key());
    }

    private Component pageNumber() {
        return Component.translatable("journal.restless.page", page + 1, pages.size());
    }

    /** What a screen reader says: the open section and page, and the page's text. */
    public Component narration() {
        var message = sectionTitle().copy();
        if (!pages.isEmpty()) {
            message.append(". ").append(pageNumber());
            for (Block block : pages.get(page)) {
                for (FormattedCharSequence line : block.lines()) {
                    StringBuilder text = new StringBuilder();
                    line.accept((index, style, codePoint) -> {
                        text.appendCodePoint(codePoint);
                        return true;
                    });
                    message.append(" ").append(text.toString());
                }
            }
        }
        return message;
    }

    private void paginate() {
        pages.clear();
        room = Math.max(LINE, height - 90);
        laid = new ArrayList<>();
        used = 0;
        if (!data.sections().isEmpty()) {
            JournalPages.Section current = data.sections().get(section);
            for (JournalPages.Card card : current.cards()) {
                // Each card starts on a page of its own.
                if (!laid.isEmpty()) {
                    flush();
                }
                blocks(current.key(), card).forEach(this::place);
            }
        }
        if (!laid.isEmpty() || pages.isEmpty()) {
            flush();
        }
        page = Math.min(page, pages.size() - 1);
    }

    private void flush() {
        pages.add(laid);
        laid = new ArrayList<>();
        used = 0;
    }

    /**
     * Puts a block on the page; text that does not fit runs over onto the next page line by line, a picture moves
     * whole.
     */
    private void place(Block block) {
        if (block.height() > room - used) {
            List<FormattedCharSequence> lines = block.lines();
            int fit = block.picture() == null && block.clues() == null && block.filing() == null
                    ? (room - used) / LINE
                    : 0;
            if (fit >= 1 && fit < lines.size()) {
                laid.add(new Block(lines.subList(0, fit), block.icon(), null, null, null, fit * LINE));
                flush();
                place(new Block(
                        lines.subList(fit, lines.size()),
                        null,
                        null,
                        null,
                        null,
                        (lines.size() - fit) * LINE + ENTRY_GAP));
                return;
            }
            if (!laid.isEmpty()) {
                flush();
            }
        }
        laid.add(block);
        used += block.height();
    }

    /** A card laid out: its heading, its photograph and the slots beside it, then each entry under its tool's icon. */
    private List<Block> blocks(String key, JournalPages.Card card) {
        List<Block> blocks = new ArrayList<>();
        if (!card.heading().getString().isEmpty()) {
            List<FormattedCharSequence> lines =
                    font.split(card.heading().copy().withStyle(ChatFormatting.BOLD), Math.max(1, textWidth));
            blocks.add(new Block(lines, null, null, null, null, lines.size() * LINE + 4));
        }
        if (key.equals("spirits") || key.equals("archive")) {
            Set<Kind> found =
                    card.entries().stream().flatMap(e -> e.kind().stream()).collect(Collectors.toSet());
            int height = card.picture().isPresent()
                    ? Math.max(Picture.HEIGHT + PhotoTooltip.BORDER * 2, CLUES.size() * (ICON + 4))
                    : ICON;
            blocks.add(new Block(List.of(), null, card.picture().orElse(null), found, null, height + 8));
        }
        for (JournalPages.Entry entry : card.entries()) {
            Kind icon = entry.kind().filter(k -> icon(k) != null).orElse(null);
            int lineWidth = Math.max(1, textWidth - (icon == null ? 0 : ICON + 6));
            List<FormattedCharSequence> lines = font.split(entry.text(), lineWidth);
            blocks.add(new Block(
                    lines,
                    icon,
                    null,
                    null,
                    entry.filing().orElse(null),
                    Math.max(lines.size() * LINE, icon == null ? 0 : ICON) + ENTRY_GAP));
        }
        return blocks;
    }

    private static @Nullable ItemStack icon(Kind kind) {
        Item item = switch (kind) {
            case PHOTO -> Content.PHOTO.get();
            case FOOTPRINTS -> Content.ASH.get();
            case VOICE, GRAVE_WHISPER -> Content.SPIRIT_BOX_ITEM.get();
            case PLANCHETTE -> Content.OUIJA_BOARD_ITEM.get();
            case GRAVE -> Content.GRAVESTONE_ITEM.get();
            case DREAM -> Content.DREAMCATCHER_ITEM.get();
            default -> null;
        };
        return item == null ? null : ICONS.computeIfAbsent(kind, k -> new ItemStack(item));
    }

    /** Draws the titles, the open page and its number; the bookmarks and arrows are the buttons' business. */
    public void draw(PageCanvas canvas) {
        canvas.text(Component.translatable("item.restless.journal").getVisualOrderText(), 15, 17, INK);
        canvas.text(sectionTitle().getVisualOrderText(), textX, 17, MUTED);
        canvas.fill(textX, 32, width - 18, 33, 0xFFBEA47C);
        int y = 42;
        hits.clear();
        if (!pages.isEmpty()) {
            for (Block block : pages.get(page)) {
                drawBlock(canvas, block, y);
                y += block.height();
            }
        }
        Component number = pageNumber();
        canvas.text(number.getVisualOrderText(), textX + (textWidth - font.width(number)) / 2, height - 26, MUTED);
    }

    /** A bookmark or an arrow, or any paper button, with its label. */
    public void button(
            PageCanvas canvas,
            int x,
            int y,
            int w,
            int h,
            Component label,
            boolean selected,
            boolean hovered,
            boolean active,
            boolean focused) {
        int background = selected ? 0xFF713E38 : hovered && active ? 0xFFEEDFBA : 0xFFC9B28A;
        canvas.fill(x, y, x + w, y + h, EDGE);
        canvas.fill(x + 1, y + 1, x + w - 1, y + h - 1, background);
        if (focused) {
            canvas.fill(x + 1, y + 1, x + 3, y + h - 1, INK);
        }
        List<FormattedCharSequence> lines = font.split(label, w - 10);
        int ty = y + (h - lines.size() * font.lineHeight) / 2;
        for (FormattedCharSequence line : lines) {
            canvas.text(line, x + 5, ty, selected ? PAPER : active ? INK : MUTED);
            ty += font.lineHeight;
        }
    }

    /** Draws every control for a pointer at ({@code px}, {@code py}), for a book that has no widgets of its own. */
    public void drawControls(PageCanvas canvas, double px, double py) {
        for (Control control : controls) {
            button(
                    canvas,
                    control.x(),
                    control.y(),
                    control.width(),
                    control.height(),
                    control.label(),
                    selected(control),
                    control.contains(px, py),
                    control.active().getAsBoolean(),
                    false);
        }
    }

    private void drawBlock(PageCanvas canvas, Block block, int y) {
        if (block.clues() != null) {
            drawEvidence(canvas, block, y);
            return;
        }
        int x = textX;
        if (block.icon() != null) {
            canvas.item(icon(block.icon()), textX, y - 1);
            x += ICON + 6;
        }
        if (block.filing() != null) {
            hits.add(new Hit(x, y, textWidth, block.lines().size() * LINE, block.filing()));
        }
        for (FormattedCharSequence line : block.lines()) {
            canvas.text(line, x, y, block.filing() != null ? LINK : INK);
            y += LINE;
        }
    }

    /** The pasted photograph, taped on at an angle of its own, and one slot per tool: dim until the player used it. */
    private void drawEvidence(PageCanvas canvas, Block block, int y) {
        int x = textX;
        Picture picture = block.picture();
        boolean pasted = picture != null;
        if (pasted) {
            int w = Picture.WIDTH + PhotoTooltip.BORDER * 2;
            int h = Picture.HEIGHT + PhotoTooltip.BORDER * 2;
            canvas.fill(x + 2, y + 2, x + w + 2, y + h + 2, 0x55000000);
            canvas.picture(picture, x, y);
            canvas.fill(x - 4, y - 3, x + 10, y + 3, 0xB0E8DCA8);
            canvas.fill(x + w - 10, y - 3, x + w + 4, y + 3, 0xB0E8DCA8);
            x += w + 14;
        }
        int step = ICON + 4;
        for (int i = 0; i < CLUES.size(); i++) {
            int ix = pasted ? x : x + i * step;
            int iy = pasted ? y + i * step : y;
            canvas.item(icon(CLUES.get(i)), ix, iy);
            if (!block.clues().contains(CLUES.get(i))) {
                canvas.fill(ix, iy, ix + ICON, iy + ICON, 0xC0E5D5AD);
            }
        }
    }

    /** Files the banishment under the entry at ({@code x}, {@code y}) in its dossier, if there is one. */
    public boolean file(double x, double y) {
        for (Hit hit : hits) {
            if (x >= hit.x() && x < hit.x() + hit.width() && y >= hit.y() && y < hit.y() + hit.height()) {
                Minecraft.getInstance()
                        .getConnection()
                        .send(new ServerboundCustomPayloadPacket(new FilePayload(
                                hit.filing().record(), hit.filing().dossier())));
                return true;
            }
        }
        return false;
    }

    /** A click on the book at ({@code x}, {@code y}): a bookmark or an arrow, or an entry that can be filed. */
    public boolean click(double x, double y) {
        for (Control control : controls) {
            if (control.contains(x, y)) {
                if (control.active().getAsBoolean()) {
                    control.press().run();
                }
                return true;
            }
        }
        return file(x, y);
    }
}
