package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The client side of {@link Sanity}: below a threshold, darkness creeps in from the screen's edges, deeper as sanity
 * falls.
 */
public final class SanityOverlay {
    private static final int BANDS = 8;

    private static float sanity = 1.0f;

    private SanityOverlay() {}

    public static void set(float value) {
        sanity = value;
    }

    /** Draws as a HUD layer: nested frames, so the edges stack the most layers and are darkest. */
    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Config.Section config = Config.section("sanity");
        double below = config.num("overlay_below");
        double depth = Math.clamp((below - sanity) / below, 0.0, 1.0);
        if (depth <= 0) {
            return;
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int step = Math.max(1, Math.min(width, height) / (BANDS * 3));
        int color = (int) (255 * config.num("overlay_peak") * depth / BANDS) << 24;
        for (int i = 1; i <= BANDS; i++) {
            int t = (int) (step * i * depth);
            graphics.fill(0, 0, width, t, color);
            graphics.fill(0, height - t, width, height, color);
            graphics.fill(0, t, t, height - t, color);
            graphics.fill(width - t, t, width, height - t, color);
        }
    }
}
