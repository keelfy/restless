package dev.keelfy.restless.haunt;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jspecify.annotations.Nullable;

/**
 * The client side of the {@code dimming} and {@code blinking} events. Lighting is computed on the client, so the fade is
 * drawn over the world (under the HUD) instead of changing any light level. State is per client and in memory only.
 */
public final class Dimming {
    private static long start;
    private static @Nullable DimPayload fade;

    private Dimming() {}

    public static void start(DimPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && payload.cycle() > 0 && payload.times() > 0) {
            start = minecraft.level.getGameTime();
            fade = payload;
        }
    }

    /** Draws as a HUD layer; does nothing when no fade is running. */
    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        DimPayload now = fade;
        if (now == null || minecraft.level == null) {
            return;
        }
        double elapsed = minecraft.level.getGameTime() - start + delta.getGameTimeDeltaPartialTick(false);
        if (elapsed < 0 || elapsed >= (double) now.cycle() * now.times()) {
            fade = elapsed < 0 ? now : null;
            return;
        }
        double t = elapsed % now.cycle();
        double depth = t < now.fall() ? t / Math.max(1, now.fall())
            : t < now.fall() + now.hold() ? 1
            : (now.cycle() - t) / Math.max(1, now.rise());
        int alpha = (int) (255 * now.peak() * depth);
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24);
    }
}
