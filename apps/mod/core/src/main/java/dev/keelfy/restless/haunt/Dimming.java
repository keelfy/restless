package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The client side of the {@code dimming} event. Lighting is computed on the client, so the fade is drawn over the world
 * (under the HUD) instead of changing any light level. State is per client and in memory only.
 */
public final class Dimming {
    /** Share of the duration spent getting darker; the rest is the return. */
    private static final double FALL = 0.6;

    private static long start;
    private static int total;

    private Dimming() {}

    public static void start(int ticks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && ticks > 0) {
            start = minecraft.level.getGameTime();
            total = ticks;
        }
    }

    /** Draws as a HUD layer; does nothing when no fade is running. */
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
        double depth = progress < FALL ? progress / FALL : (1 - progress) / (1 - FALL);
        int alpha = (int) (255 * Config.event("dimming").num("peak") * depth);
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24);
    }
}
