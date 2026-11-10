package dev.keelfy.restless.sanity.client;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.effect.client.EdgeFade;
import dev.keelfy.restless.sanity.Sanity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The client side of {@link Sanity}: below a threshold, darkness creeps in from the screen's edges, deeper as sanity
 * falls.
 */
public final class SanityOverlay {
    private static float sanity = 1.0f;

    private SanityOverlay() {}

    public static void set(float value) {
        sanity = value;
    }

    /** Draws as a HUD layer: smooth fade, see EdgeFade. */
    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Config.Section config = Config.section("sanity");
        double below = config.num("overlay_below");
        double depth = Math.clamp((below - sanity) / below, 0.0, 1.0);
        if (depth <= 0) {
            return;
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        EdgeFade.draw(graphics, 0, (int) (Math.min(width, height) / 3 * depth), config.num("overlay_peak") * depth);
    }
}
