package dev.keelfy.restless.banish.client;

import dev.keelfy.restless.config.Config;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** The closing text of the Requiem: the poem scrolls up a black screen; Esc or a click ends it. */
@OnlyIn(Dist.CLIENT)
public final class RequiemScreen extends Screen {
    /** Entries of the poem in the lang files; an empty one is the space between verses. */
    static final int LINES = 15;

    private static final int LINE = 16;
    private static final int WHITE = 0xFFE8E4DC;

    private final List<FormattedCharSequence> lines = new ArrayList<>();
    private int ticks;

    private RequiemScreen() {
        super(Component.translatable("requiem.restless.title"));
    }

    public static void open() {
        Minecraft.getInstance().setScreenAndShow(new RequiemScreen());
    }

    @Override
    protected void init() {
        lines.clear();
        for (int i = 1; i <= LINES; i++) {
            List<FormattedCharSequence> verse =
                    font.split(Component.translatable("requiem.restless.poem." + i), Math.max(40, width - 40));
            if (verse.isEmpty()) {
                lines.add(FormattedCharSequence.EMPTY);
            } else {
                lines.addAll(verse);
            }
        }
    }

    @Override
    public void tick() {
        ticks++;
        if (offset(0) > height + lines.size() * LINE) {
            onClose();
        }
    }

    private float offset(float partial) {
        return (ticks + partial) * LINE / Config.section("requiem").integer("poem_line_ticks");
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        onClose();
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        graphics.fill(0, 0, width, height, 0xFF000000);
        float scroll = offset(partial);
        for (int i = 0; i < lines.size(); i++) {
            float y = height + i * LINE - scroll;
            if (y > -LINE && y < height) {
                FormattedCharSequence line = lines.get(i);
                graphics.text(font, line, (width - font.width(line)) / 2, (int) y, WHITE, false);
            }
        }
    }
}
