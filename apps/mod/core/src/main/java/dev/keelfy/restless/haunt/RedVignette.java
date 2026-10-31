package dev.keelfy.restless.haunt;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The client side of the {@code red_vignette} event: the screen's edges go red, as near the world border, and back.
 * Drawn here rather than through the border warning, which shows only with the vignette setting on and is faint in
 * daylight.
 */
public final class RedVignette {
    private static final int RED = 0x9A0000;
    /** Shares of the duration spent coming in and going out. */
    private static final double IN = 0.15;

    private static final double OUT = 0.35;

    private static long start;
    private static int total;
    private static float strength;

    private RedVignette() {}

    public static void start(float peak, int ticks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && ticks > 0) {
            start = minecraft.level.getGameTime();
            total = ticks;
            strength = peak;
        }
    }

    /**
     * Draws as a HUD layer: one-pixel frames from the edge inward, each fainter than the last, so the fall-off is
     * smooth.
     */
    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (total == 0 || minecraft.level == null) {
            return;
        }
        double progress = (minecraft.level.getGameTime() - start + delta.getGameTimeDeltaPartialTick(false)) / total;
        if (progress < 0 || progress >= 1) {
            total = progress < 0 ? total : 0;
            return;
        }
        double depth = Math.min(1.0, Math.min(progress / IN, (1 - progress) / OUT));
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        EdgeFade.draw(graphics, RED, Math.max(1, Math.min(width, height) / 3), strength * depth);
    }
}
