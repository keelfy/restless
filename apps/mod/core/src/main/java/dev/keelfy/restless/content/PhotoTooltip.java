package dev.keelfy.restless.content;

import com.mojang.blaze3d.platform.NativeImage;
import dev.keelfy.restless.Restless;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/** Shows a developed photo's picture, toned sepia, in its tooltip. */
public record PhotoTooltip(Picture picture) implements ClientTooltipComponent {
    public static final int BORDER = 3;
    private static final int DARK = 0xFF1E140C;
    private static final int LIGHT = 0xFFEEDFC0;
    /** The last pictures shown, kept as textures; the oldest is released past this many. */
    private static final int CACHED = 16;

    private static final Map<Picture, Identifier> TEXTURES = new LinkedHashMap<>(CACHED, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Picture, Identifier> eldest) {
            if (size() <= CACHED) {
                return false;
            }
            Minecraft.getInstance().getTextureManager().release(eldest.getValue());
            return true;
        }
    };
    private static int next;

    @Override
    public int getHeight(Font font) {
        return Picture.HEIGHT + BORDER * 2 + 2;
    }

    @Override
    public int getWidth(Font font) {
        return Picture.WIDTH + BORDER * 2;
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        draw(graphics, picture, x, y);
    }

    /** The picture in its pale border, {@code WIDTH + 2 * BORDER} wide, with its corner at ({@code x}, {@code y}). */
    public static void draw(GuiGraphicsExtractor graphics, Picture picture, int x, int y) {
        graphics.fill(x, y, x + Picture.WIDTH + BORDER * 2, y + Picture.HEIGHT + BORDER * 2, LIGHT);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture(picture),
                x + BORDER,
                y + BORDER,
                0,
                0,
                Picture.WIDTH,
                Picture.HEIGHT,
                Picture.WIDTH,
                Picture.HEIGHT);
    }

    /** The picture as a sepia texture, made on first use and kept for the last few shown. */
    public static Identifier texture(Picture picture) {
        return TEXTURES.computeIfAbsent(picture, p -> {
            NativeImage image = new NativeImage(Picture.WIDTH, Picture.HEIGHT, false);
            for (int y = 0; y < Picture.HEIGHT; y++) {
                for (int x = 0; x < Picture.WIDTH; x++) {
                    float grey = (p.pixels()[x + y * Picture.WIDTH] & 0xFF) / 255f;
                    image.setPixel(x, y, ARGB.srgbLerp(grey, DARK, LIGHT));
                }
            }
            Identifier id = Identifier.fromNamespaceAndPath(Restless.MOD_ID, "photo/" + next++);
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "Photo " + id, image));
            return id;
        });
    }
}
