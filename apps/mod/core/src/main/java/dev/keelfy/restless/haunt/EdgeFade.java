package dev.keelfy.restless.haunt;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** A smooth fade along the screen's edges: one-pixel frames, each fainter than the last, never stacked. */
final class EdgeFade {
    private EdgeFade() {}

    /** Draws {@code rgb} at {@code peak} opacity on the very edge, falling off to nothing {@code reach} pixels in. */
    static void draw(GuiGraphicsExtractor graphics, int rgb, int reach, double peak) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        for (int i = 0; i < reach; i++) {
            double fade = 1.0 - (double) i / reach;
            int alpha = (int) Math.round(255 * Math.min(1.0, peak) * fade * fade);
            if (alpha == 0) {
                break;
            }
            int color = alpha << 24 | rgb;
            graphics.fill(i, i, width - i, i + 1, color);
            graphics.fill(i, height - i - 1, width - i, height - i, color);
            graphics.fill(i, i + 1, i + 1, height - i - 1, color);
            graphics.fill(width - i - 1, i + 1, width - i, height - i - 1, color);
        }
    }
}
