package dev.keelfy.restless.haunt.effect.client;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.haunt.effect.ScreenPayload;
import java.util.Random;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

/**
 * The client side of the {@code screen_handprint}, {@code screen_frost} and {@code screen_face} events: one picture
 * over the view, under the HUD. State is per client and in memory only.
 */
public final class ScreenOverlay {
    private static final Identifier HANDPRINT =
            Identifier.fromNamespaceAndPath(Restless.MOD_ID, "textures/gui/screen/handprint.png");
    private static final Identifier FACE =
            Identifier.fromNamespaceAndPath(Restless.MOD_ID, "textures/gui/screen/face.png");
    private static final int TEXTURE = 64;
    private static final int FROST = 0xDCEBFF;

    private static long start;
    private static @Nullable ScreenPayload shown;

    private ScreenOverlay() {}

    public static void start(ScreenPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && payload.ticks() > 0) {
            start = minecraft.level.getGameTime();
            shown = payload;
        }
    }

    /** Draws as a HUD layer; does nothing when nothing is showing. */
    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        ScreenPayload now = shown;
        if (now == null || minecraft.level == null) {
            return;
        }
        double elapsed = minecraft.level.getGameTime() - start + delta.getGameTimeDeltaPartialTick(false);
        if (elapsed < 0 || elapsed >= now.ticks()) {
            shown = elapsed < 0 ? now : null;
            return;
        }
        double progress = elapsed / now.ticks();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        switch (now.kind()) {
            // Quick in, then a slow clearing, like breath on glass.
            case ScreenPayload.HANDPRINT -> {
                float alpha = (float) (now.strength() * Math.min(progress / 0.08, 1 - progress));
                Random random = new Random(now.seed());
                int size = Math.max(TEXTURE, height / 2);
                int x = (int) (width * (0.15 + 0.55 * random.nextDouble()));
                int y = (int) (height * (0.1 + 0.4 * random.nextDouble()));
                picture(graphics, HANDPRINT, x, y, size, alpha);
            }
            case ScreenPayload.FROST -> {
                double depth = Math.min(1.0, Math.min(progress / 0.3, (1 - progress) / 0.5));
                EdgeFade.draw(graphics, FROST, Math.max(1, Math.min(width, height) / 3), now.strength() * depth);
            }
            case ScreenPayload.FACE -> {
                int size = height * 3 / 4;
                picture(graphics, FACE, (width - size) / 2, (height - size) / 2, size, now.strength());
            }
            default -> {}
        }
    }

    private static void picture(
            GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int size, float alpha) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                x,
                y,
                0,
                0,
                size,
                size,
                TEXTURE,
                TEXTURE,
                TEXTURE,
                TEXTURE,
                ARGB.white(Math.clamp(alpha, 0f, 1f)));
    }
}
