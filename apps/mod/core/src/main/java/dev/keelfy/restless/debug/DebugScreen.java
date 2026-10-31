package dev.keelfy.restless.debug;

import dev.keelfy.restless.config.Lang;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntTier;
import java.util.Locale;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** {@code /haunt}: the viewer's haunt state beside every event, with a button to run the selected one. */
@OnlyIn(Dist.CLIENT)
public final class DebugScreen extends Screen {
    private static final int MARGIN = 10;
    private static final int TOP = 30;
    private static final int BOTTOM = 36;
    private static final int LINE = 11;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFF808080;
    private static final int GREEN = 0xFF55FF55;
    private static final int RED = 0xFFFF5555;

    private final DebugPayload data;
    private EventList list;
    private Button fire;

    private DebugScreen(DebugPayload data) {
        super(Component.literal("Haunt"));
        this.data = data;
    }

    public static void open(DebugPayload data) {
        Minecraft.getInstance().setScreenAndShow(new DebugScreen(data));
    }

    @Override
    protected void init() {
        int listWidth = Math.min(280, width / 2);
        list = addRenderableWidget(new EventList(listWidth, height - TOP - BOTTOM));
        list.updateSizeAndPosition(listWidth, height - TOP - BOTTOM, MARGIN, TOP);
        data.events().forEach(list::add);

        int side = MARGIN * 2 + listWidth;
        fire = addRenderableWidget(
                Button.builder(Component.literal("Fire in " + DebugMenu.FIRE_DELAY / 20 + "s"), b -> fire())
                        .bounds(side, height - BOTTOM + 8, 120, 20)
                        .build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width - MARGIN - 80, height - BOTTOM + 8, 80, 20)
                .build());
        fire.active = false;
    }

    @Override
    public void tick() {
        fire.active = list.getSelected() != null;
    }

    private void fire() {
        EventList.Row row = list.getSelected();
        if (row != null) {
            minecraft.getConnection().send(new ServerboundCustomPayloadPacket(new FirePayload(row.event.id())));
            onClose();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.text(font, title, MARGIN, MARGIN, WHITE);

        int x = MARGIN * 2 + list.getWidth();
        int y = TOP;
        graphics.text(font, data.player(), x, y, WHITE);
        y += LINE;
        y += LINE;
        graphics.text(font, "Spirits (" + data.spirits().size() + ")", x, y, WHITE);
        for (int i = 0; i < data.spirits().size(); i++) {
            y += LINE;
            DebugPayload.Spirit spirit = data.spirits().get(i);
            graphics.text(
                    font,
                    String.format(
                            Locale.ROOT,
                            "%d. %s%s  seed %x  power %.2f  next in %ds",
                            i + 1,
                            spirit.id(),
                            spirit.territorial() ? " (territorial)" : "",
                            spirit.seed(),
                            spirit.power(),
                            spirit.seconds()),
                    x,
                    y,
                    GRAY);
            y += LINE;
            graphics.text(font, "    " + String.join(", ", spirit.traits()), x, y, GRAY);
        }
        y += LINE * 2;

        EventList.Row row = list.getSelected();
        if (row == null) {
            graphics.text(font, "Select an event", x, y, GRAY);
            return;
        }
        DebugPayload.Event event = row.event;
        graphics.text(font, row.name, x, y, WHITE);
        y += LINE;
        graphics.text(font, event.id().toString(), x, y, GRAY);
        y += LINE;
        String tags = event.tags().stream()
                .map(EventTag::name)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(", "));
        graphics.text(
                font,
                event.tier().name().toLowerCase(Locale.ROOT) + "   weight " + event.weight() + "   " + tags,
                x,
                y,
                GRAY);
        y += LINE;
        graphics.text(
                font, event.canRun() ? "can run now" : "nothing to act on here", x, y, event.canRun() ? GREEN : RED);
        y += LINE;
        for (int i = 0; i < event.chances().size(); i++) {
            graphics.text(
                    font,
                    String.format(
                            Locale.ROOT,
                            "%d. %s rolls it next: %.1f%%",
                            i + 1,
                            data.spirits().get(i).id(),
                            event.chances().get(i) * 100),
                    x,
                    y,
                    GRAY);
            y += LINE;
        }
        y += LINE;
        graphics.textWithWordWrap(font, Component.literal(row.description), x, y, width - x - MARGIN, WHITE);
    }

    private final class EventList extends ObjectSelectionList<EventList.Row> {
        EventList(int width, int height) {
            super(DebugScreen.this.minecraft, width, height, TOP, LINE + 4);
        }

        void add(DebugPayload.Event event) {
            addEntry(new Row(event));
        }

        @Override
        public int getRowWidth() {
            // The bar sits a bar's width and 2 px beyond the row's right edge, and only a click inside the widget
            // reaches it.
            return width - 4 * scrollbarWidth() - 4;
        }

        private final class Row extends ObjectSelectionList.Entry<Row> {
            final DebugPayload.Event event;
            final String name;
            final String description;

            Row(DebugPayload.Event event) {
                String language = minecraft.getLanguageManager().getSelected();
                this.event = event;
                this.name = Lang.name(language, event.id().toString());
                this.description = Lang.description(language, event.id().toString());
            }

            @Override
            public Component getNarration() {
                return Component.literal(name);
            }

            @Override
            public void extractContent(
                    GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                if (hovered) {
                    graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x30FFFFFF);
                }
                graphics.text(font, name, getContentX(), getContentY(), event.canRun() ? WHITE : GRAY);
                String tier = event.tier() == HauntTier.HARMFUL ? "harmful" : "light";
                graphics.text(
                        font,
                        tier,
                        getContentRight() - font.width(tier),
                        getContentY(),
                        event.tier() == HauntTier.HARMFUL ? RED : GREEN);
            }
        }
    }
}
