package dev.keelfy.restless.content.client;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.entity.SightingMark;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * The client side of a {@link SightingMark}: a waypoint on the screen, as a minimap mod draws one. The icon is drawn
 * over the world, so it shows through blocks, and at one size however far the mark is; beneath it go its name, if it
 * has one, and the distance. It is placed at the exact fractional screen position, so it glides as you turn instead of
 * stepping a whole GUI pixel at a time; its size and opacity come from the {@code consumables} config.
 */
public final class SightingMarkHud {
    private static final Identifier ICON =
            Identifier.fromNamespaceAndPath(Restless.MOD_ID, "textures/item/sighting_mark.png");
    private static final int TEXTURE = 16;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int PALE = 0xFFC9D6F2;

    private SightingMarkHud() {}

    /** Draws as a HUD layer. */
    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Camera camera = minecraft.gameRenderer.mainCamera();
        Matrix4f projection = camera.getViewRotationProjectionMatrix(new Matrix4f());
        Vec3 eye = camera.position();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        Font font = minecraft.font;
        Config.Section config = Config.section("consumables");
        float scale = (float) config.num("mark_icon_scale");
        int color = ARGB.white(Math.clamp((float) config.num("mark_icon_alpha"), 0f, 1f));
        float half = TEXTURE * scale / 2;
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof SightingMark mark)) {
                continue;
            }
            Vec3 offset = mark.position().subtract(eye);
            Vector4f clip = projection.transform(new Vector4f((float) offset.x, (float) offset.y, (float) offset.z, 1));
            if (clip.w <= 0.05f) {
                continue; // behind the camera
            }
            float x = (clip.x / clip.w + 1) / 2 * width;
            float y = (1 - clip.y / clip.w) / 2 * height;
            if (x < half || x > width - half || y < half || y > height - half) {
                continue;
            }
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y);
            graphics.pose().pushMatrix();
            graphics.pose().scale(scale, scale);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    ICON,
                    -TEXTURE / 2,
                    -TEXTURE / 2,
                    0,
                    0,
                    TEXTURE,
                    TEXTURE,
                    TEXTURE,
                    TEXTURE,
                    TEXTURE,
                    TEXTURE,
                    color);
            graphics.pose().popMatrix();
            int line = Math.round(half) + 2;
            if (mark.hasCustomName()) {
                graphics.centeredText(font, mark.getCustomName(), 0, line, WHITE);
                line += font.lineHeight + 1;
            }
            graphics.centeredText(font, Component.literal(Math.round(offset.length()) + " m"), 0, line, PALE);
            graphics.pose().popMatrix();
        }
    }
}
