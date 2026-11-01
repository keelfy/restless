package dev.keelfy.restless.haunt;

import com.mojang.blaze3d.platform.InputConstants;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.PhotoTooltip;
import dev.keelfy.restless.content.Picture;
import dev.keelfy.restless.haunt.Notebook.Kind;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

/** A field notebook with bookmarks and pages fitted to the reader's font and window. */
@OnlyIn(Dist.CLIENT)
public final class JournalScreen extends Screen {
    private static final int INK = 0xFF382B24;
    private static final int LINK = 0xFF713E38;
    private static final int MUTED = 0xFF77634D;
    private static final int PAPER = 0xFFE5D5AD;
    private static final int LINE = 12;
    private final JournalPayload data;
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

    private final List<Hit> hits = new ArrayList<>();

    /** The tools and finds a dossier shows slots for, in order; the ones the player has used light up. */
    private static final List<Kind> CLUES =
            List.of(Kind.PHOTO, Kind.FOOTPRINTS, Kind.VOICE, Kind.PLANCHETTE, Kind.GRAVE);

    private static final int ICON = 16;
    private static final int ENTRY_GAP = 5;
    private final List<List<Block>> pages = new ArrayList<>();
    private List<Block> laid = new ArrayList<>();
    private int room;
    private int used;
    private int section;
    private int page;
    private int left;
    private int top;
    private int bookWidth;
    private int bookHeight;
    private int sidebar;
    private int textX;
    private int textWidth;
    private Button previous;
    private Button next;

    private JournalScreen(JournalPayload data, @Nullable String key) {
        super(Component.translatable("item.restless.journal"));
        this.data = data;
        for (int i = 0; i < data.sections().size(); i++) {
            if (data.sections().get(i).key().equals(key)) {
                section = i;
            }
        }
    }

    /** Opens the journal, or when it is already open (a filing changed it) puts the new pages in the same tab. */
    public static void open(JournalPayload data) {
        Minecraft minecraft = Minecraft.getInstance();
        String key = minecraft.gui.screen() instanceof JournalScreen open
                        && open.section < open.data.sections().size()
                ? open.data.sections().get(open.section).key()
                : null;
        minecraft.setScreenAndShow(new JournalScreen(data, key));
    }

    @Override
    protected void init() {
        bookWidth = Math.min(500, width - 16);
        bookHeight = Math.min(330, height - 20);
        left = (width - bookWidth) / 2;
        top = (height - bookHeight) / 2;
        sidebar = Math.min(138, bookWidth / 3);
        textX = left + sidebar + 22;
        textWidth = bookWidth - sidebar - 40;
        for (int i = 0; i < data.sections().size(); i++) {
            int index = i;
            addRenderableWidget(new PaperButton(
                    left + 12,
                    top + 42 + i * 28,
                    sidebar - 14,
                    26,
                    Component.translatable(
                            "journal.restless.tab." + data.sections().get(i).key()),
                    b -> select(index),
                    i));
        }
        previous = addRenderableWidget(
                new PaperButton(textX, top + bookHeight - 32, 28, 20, Component.literal("<"), b -> turn(-1), -1));
        next = addRenderableWidget(new PaperButton(
                left + bookWidth - 46, top + bookHeight - 32, 28, 20, Component.literal(">"), b -> turn(1), -1));
        addRenderableWidget(new PaperButton(
                left + 12, top + bookHeight - 32, sidebar - 14, 20, CommonComponents.GUI_DONE, b -> onClose(), -1));
        paginate();
    }

    private Component sectionTitle(int index) {
        return Component.translatable(
                "journal.restless.section." + data.sections().get(index).key());
    }

    private void select(int index) {
        if (section != index) {
            section = index;
            page = 0;
            paginate();
            scheduleNarration();
        }
    }

    private void paginate() {
        pages.clear();
        room = Math.max(LINE, bookHeight - 90);
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
        this.page = Math.min(this.page, pages.size() - 1);
        updateButtons();
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
            int width = Math.max(1, textWidth - (icon == null ? 0 : ICON + 6));
            List<FormattedCharSequence> lines = font.split(entry.text(), width);
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

    private static final Map<Kind, ItemStack> ICONS = new EnumMap<>(Kind.class);

    private void turn(int delta) {
        page = Math.clamp(page + delta, 0, pages.size() - 1);
        updateButtons();
        scheduleNarration();
    }

    private void updateButtons() {
        previous.active = page > 0;
        next.active = page + 1 < pages.size();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.shortcutKey() == InputConstants.KEYCODE_PAGEDOWN
                || event.shortcutKey() == InputConstants.KEYCODE_PAGEUP) {
            turn(event.shortcutKey() == InputConstants.KEYCODE_PAGEDOWN ? 1 : -1);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (vertical != 0) {
            turn(vertical < 0 ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override
    public Component getNarrationMessage() {
        var message = title.copy();
        if (!data.sections().isEmpty()) message.append(". ").append(sectionTitle(section));
        if (!pages.isEmpty()) {
            message.append(". ").append(Component.translatable("journal.restless.page", page + 1, pages.size()));
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

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Leather cover, stacked page edges, and a stitched spine, all in GUI pixels.
        graphics.fill(left + 4, top + 5, left + bookWidth + 4, top + bookHeight + 5, 0x80000000);
        graphics.fill(left, top, left + bookWidth, top + bookHeight, 0xFF493329);
        graphics.fill(left + 5, top + 5, left + bookWidth - 5, top + bookHeight - 5, 0xFFAD9470);
        graphics.fill(left + 8, top + 7, left + bookWidth - 8, top + bookHeight - 8, PAPER);
        graphics.fill(left + 8, top + 7, left + sidebar + 5, top + bookHeight - 8, 0xFFD2BD94);
        graphics.fill(left + sidebar + 5, top + 8, left + sidebar + 10, top + bookHeight - 8, 0xFFB49A74);
        for (int y = top + 17; y < top + bookHeight - 12; y += 12) {
            graphics.fill(left + sidebar + 6, y, left + sidebar + 8, y + 4, 0xFF765B44);
        }
        graphics.text(font, title, left + 15, top + 17, INK, false);
        graphics.text(font, data.sections().isEmpty() ? title : sectionTitle(section), textX, top + 17, MUTED, false);
        graphics.fill(textX, top + 32, left + bookWidth - 18, top + 33, 0xFFBEA47C);
        int y = top + 42;
        hits.clear();
        if (!pages.isEmpty()) {
            for (Block block : pages.get(page)) {
                drawBlock(graphics, block, y);
                y += block.height();
            }
        }
        Component number = Component.translatable("journal.restless.page", page + 1, pages.size());
        graphics.text(font, number, textX + (textWidth - font.width(number)) / 2, top + bookHeight - 26, MUTED, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawBlock(GuiGraphicsExtractor graphics, Block block, int y) {
        if (block.clues() != null) {
            drawEvidence(graphics, block, y);
            return;
        }
        int x = textX;
        if (block.icon() != null) {
            graphics.fakeItem(icon(block.icon()), textX, y - 1);
            x += ICON + 6;
        }
        if (block.filing() != null) {
            hits.add(new Hit(x, y, textWidth, block.lines().size() * LINE, block.filing()));
        }
        for (FormattedCharSequence line : block.lines()) {
            graphics.text(font, line, x, y, block.filing() != null ? LINK : INK, false);
            y += LINE;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (Hit hit : hits) {
            if (event.x() >= hit.x()
                    && event.x() < hit.x() + hit.width()
                    && event.y() >= hit.y()
                    && event.y() < hit.y() + hit.height()) {
                Minecraft.getInstance()
                        .getConnection()
                        .send(new ServerboundCustomPayloadPacket(new FilePayload(
                                hit.filing().record(), hit.filing().dossier())));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** The pasted photograph, taped on at an angle of its own, and one slot per tool: dim until the player used it. */
    private void drawEvidence(GuiGraphicsExtractor graphics, Block block, int y) {
        int x = textX;
        Picture picture = block.picture();
        boolean pasted = picture != null;
        if (pasted) {
            int w = Picture.WIDTH + PhotoTooltip.BORDER * 2;
            int h = Picture.HEIGHT + PhotoTooltip.BORDER * 2;
            graphics.fill(x + 2, y + 2, x + w + 2, y + h + 2, 0x55000000);
            PhotoTooltip.draw(graphics, picture, x, y);
            graphics.fill(x - 4, y - 3, x + 10, y + 3, 0xB0E8DCA8);
            graphics.fill(x + w - 10, y - 3, x + w + 4, y + 3, 0xB0E8DCA8);
            x += w + 14;
        }
        int step = ICON + 4;
        for (int i = 0; i < CLUES.size(); i++) {
            int ix = pasted ? x : x + i * step;
            int iy = pasted ? y + i * step : y;
            graphics.fakeItem(icon(CLUES.get(i)), ix, iy);
            if (!block.clues().contains(CLUES.get(i))) {
                graphics.fill(ix, iy, ix + ICON, iy + ICON, 0xC0E5D5AD);
            }
        }
    }

    private final class PaperButton extends Button {
        private final int bookmark;

        PaperButton(int x, int y, int width, int height, Component text, OnPress action, int bookmark) {
            super(x, y, width, height, text, action, DEFAULT_NARRATION);
            this.bookmark = bookmark;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            boolean selected = bookmark >= 0 && section == bookmark;
            int background = selected ? 0xFF713E38 : isHoveredOrFocused() && active ? 0xFFEEDFBA : 0xFFC9B28A;
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), background);
            if (isFocused()) graphics.fill(getX(), getY(), getX() + 2, getY() + getHeight(), INK);
            List<FormattedCharSequence> lines = font.split(getMessage(), getWidth() - 8);
            int y = getY() + (getHeight() - lines.size() * font.lineHeight) / 2;
            for (FormattedCharSequence line : lines) {
                graphics.text(font, line, getX() + 4, y, selected ? PAPER : active ? INK : MUTED, false);
                y += font.lineHeight;
            }
        }
    }
}
