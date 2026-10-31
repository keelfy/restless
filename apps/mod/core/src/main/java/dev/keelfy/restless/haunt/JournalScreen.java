package dev.keelfy.restless.haunt;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** A field notebook with bookmarks and pages fitted to the reader's font and window. */
@OnlyIn(Dist.CLIENT)
public final class JournalScreen extends Screen {
    private static final int INK = 0xFF382B24;
    private static final int MUTED = 0xFF77634D;
    private static final int PAPER = 0xFFE5D5AD;
    private static final int LINE = 12;
    private final JournalPayload data;
    private final List<List<FormattedCharSequence>> pages = new ArrayList<>();
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

    private JournalScreen(JournalPayload data) {
        super(Component.translatable("item.restless.journal"));
        this.data = data;
    }

    public static void open(JournalPayload data) {
        Minecraft.getInstance().setScreenAndShow(new JournalScreen(data));
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
        int linesPerPage = Math.max(1, (bookHeight - 90) / LINE);
        if (!data.sections().isEmpty()) {
            for (Component entry : data.sections().get(section).pages()) {
                List<FormattedCharSequence> lines = font.split(entry, Math.max(1, textWidth));
                for (int start = 0; start < lines.size(); start += linesPerPage) {
                    pages.add(List.copyOf(lines.subList(start, Math.min(start + linesPerPage, lines.size()))));
                }
            }
        }
        if (pages.isEmpty()) pages.add(List.of());
        page = Math.min(page, pages.size() - 1);
        updateButtons();
    }

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
            for (FormattedCharSequence line : pages.get(page)) {
                StringBuilder text = new StringBuilder();
                line.accept((index, style, codePoint) -> {
                    text.appendCodePoint(codePoint);
                    return true;
                });
                message.append(" ").append(text.toString());
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
        graphics.text(
                font,
                data.sections().isEmpty()
                        ? title
                        : Component.translatable("journal.restless.tab."
                                + data.sections().get(section).key()),
                textX,
                top + 17,
                MUTED,
                false);
        graphics.fill(textX, top + 32, left + bookWidth - 18, top + 33, 0xFFBEA47C);
        int y = top + 42;
        if (!pages.isEmpty()) {
            for (FormattedCharSequence line : pages.get(page)) {
                graphics.text(font, line, textX, y, INK, false);
                y += LINE;
            }
        }
        Component number = Component.translatable("journal.restless.page", page + 1, pages.size());
        graphics.text(font, number, textX + (textWidth - font.width(number)) / 2, top + bookHeight - 26, MUTED, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
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
