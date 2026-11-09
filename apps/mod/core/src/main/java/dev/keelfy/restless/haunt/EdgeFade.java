package dev.keelfy.restless.haunt;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * A smooth fade along the screen's edges: a gradient band on each side, so the fall-off has no steps, and the bands
 * overlap in the corners, which come out darker, as in a real vignette, rather than showing a seam on the diagonal.
 */
final class EdgeFade {
    /** Straight gradient pieces each band's curve is drawn with. */
    private static final int PIECES = 16;

    private EdgeFade() {}

    /** Draws {@code rgb} at {@code peak} opacity on the very edge, falling off to nothing {@code reach} pixels in. */
    static void draw(GuiGraphicsExtractor graphics, int rgb, int reach, double peak) {
        if (reach <= 0 || peak <= 0) {
            return;
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        for (int k = 0; k < PIECES; k++) {
            int near = reach * k / PIECES;
            int far = reach * (k + 1) / PIECES;
            if (near == far) {
                continue;
            }
            int outer = color(rgb, near, reach, peak);
            int inner = color(rgb, far, reach, peak);
            graphics.fillGradient(0, near, width, far, outer, inner);
            graphics.fillGradient(0, height - far, width, height - near, inner, outer);
            // The side bands, turned a quarter: local (x, y) lands on screen at (-y, x).
            graphics.pose().pushMatrix();
            graphics.pose().rotate((float) (Math.PI / 2));
            graphics.fillGradient(0, -far, height, -near, inner, outer);
            graphics.fillGradient(0, near - width, height, far - width, outer, inner);
            graphics.pose().popMatrix();
        }
    }

    private static int color(int rgb, int depth, int reach, double peak) {
        double fade = 1.0 - (double) depth / reach;
        return (int) Math.round(255 * Math.min(1.0, peak) * fade * fade) << 24 | rgb;
    }
}
