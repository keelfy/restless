package dev.keelfy.restless.haunt;

import com.mojang.blaze3d.platform.InputConstants;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.PhotoTooltip;
import dev.keelfy.restless.content.Picture;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

/** The journal as a screen: a {@link JournalView} fitted to the window, with buttons for its bookmarks and arrows. */
@OnlyIn(Dist.CLIENT)
public final class JournalScreen extends Screen {
    /** The latest pages the server sent, for the book in the reader's hands. */
    private static @Nullable JournalPayload latest;

    private final JournalView view;
    private final List<PaperButton> buttons = new ArrayList<>();
    private int left;
    private int top;
    private int bookWidth;
    private int bookHeight;

    private JournalScreen(JournalPayload data, @Nullable String key) {
        super(Component.translatable("item.restless.journal"));
        view = new JournalView(Minecraft.getInstance().font, data, key);
    }

    /** The latest pages the server sent, or none yet. */
    public static @Nullable JournalPayload latest() {
        return latest;
    }

    /** Drops the pages of a world the reader has left. */
    public static void forget() {
        latest = null;
    }

    /**
     * Pages from the server: kept for the book in hand, and shown in the screen when the reader asked to open it or
     * already has it open (a filing changed it), in the same tab.
     */
    public static void receive(JournalPayload payload) {
        latest = payload;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() instanceof JournalScreen open) {
            open.view.update(payload);
            open.rebuildWidgets();
        } else if (payload.open()) {
            minecraft.setScreenAndShow(new JournalScreen(payload, null));
        }
    }

    @Override
    protected void init() {
        bookWidth = Math.min(500, width - 16 - JournalView.OVERHANG);
        bookHeight = Math.min(330, height - 20);
        left = (width - bookWidth - JournalView.OVERHANG) / 2;
        top = (height - bookHeight) / 2;
        view.layout(bookWidth, bookHeight);
        buttons.clear();
        for (JournalView.Control control : view.controls()) {
            buttons.add(addRenderableWidget(new PaperButton(control)));
        }
        addRenderableWidget(new PaperButton(
                left + 12, top + bookHeight - 32, view.sidebar() - 14, 20, CommonComponents.GUI_DONE, b -> onClose()));
        updateButtons();
    }

    private void updateButtons() {
        buttons.forEach(b -> b.active = b.control == null || b.control.active().getAsBoolean());
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

    private void turn(int delta) {
        view.turn(delta);
        updateButtons();
        scheduleNarration();
    }

    @Override
    public Component getNarrationMessage() {
        return title.copy().append(". ").append(view.narration());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Leather cover, stacked page edges, and a stitched spine, all in GUI pixels.
        graphics.pose().pushMatrix();
        graphics.pose().translate(left, top);
        graphics.pose().scale(bookWidth / 16.0f, bookHeight / 16.0f);
        graphics.fakeItem(new ItemStack(Content.JOURNAL.get()), 0, 0);
        graphics.pose().popMatrix();
        view.draw(new GuiCanvas(graphics, left, top));
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Keep the world and the reader's hands visible behind the open pages.
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return view.file(event.x() - left, event.y() - top) || super.mouseClicked(event, doubleClick);
    }

    /** The screen's pixels, with the book's corner at ({@code left}, {@code top}). */
    private final class GuiCanvas implements PageCanvas {
        private final GuiGraphicsExtractor graphics;
        private final int dx;
        private final int dy;

        GuiCanvas(GuiGraphicsExtractor graphics, int dx, int dy) {
            this.graphics = graphics;
            this.dx = dx;
            this.dy = dy;
        }

        @Override
        public void fill(int x0, int y0, int x1, int y1, int color) {
            graphics.fill(dx + x0, dy + y0, dx + x1, dy + y1, color);
        }

        @Override
        public void text(FormattedCharSequence text, int x, int y, int color) {
            graphics.text(font, text, dx + x, dy + y, color, false);
        }

        @Override
        public void item(ItemStack stack, int x, int y) {
            graphics.fakeItem(stack, dx + x, dy + y);
        }

        @Override
        public void picture(Picture picture, int x, int y) {
            PhotoTooltip.draw(graphics, picture, dx + x, dy + y);
        }
    }

    private final class PaperButton extends Button {
        private final JournalView.@Nullable Control control;

        /** A bookmark or arrow of the view, sitting where the view puts it on the screen. */
        PaperButton(JournalView.Control control) {
            super(
                    left + control.x(),
                    top + control.y(),
                    control.width(),
                    control.height(),
                    control.label(),
                    b -> {
                        control.press().run();
                        updateButtons();
                        scheduleNarration();
                    },
                    DEFAULT_NARRATION);
            this.control = control;
        }

        PaperButton(int x, int y, int width, int height, Component text, OnPress action) {
            super(x, y, width, height, text, action, DEFAULT_NARRATION);
            this.control = null;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            view.button(
                    new GuiCanvas(graphics, 0, 0),
                    getX(),
                    getY(),
                    getWidth(),
                    getHeight(),
                    getMessage(),
                    control != null && view.selected(control),
                    isHoveredOrFocused(),
                    active,
                    isFocused());
        }
    }
}
