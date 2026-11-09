package dev.keelfy.restless.content;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * The client side of a {@link SightingMark}: a waypoint on the screen, as a minimap mod draws one. The icon is drawn
 * over the world, so it shows through blocks, and at one size however far the mark is; beneath it go its name, if it
 * has one, and the distance.
 */
public final class SightingMarkHud {
    private static final int HALF_ICON = 8;
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
        ItemStack icon = new ItemStack(Content.SIGHTING_MARK.get());
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof SightingMark mark)) {
                continue;
            }
            Vec3 offset = mark.position().subtract(eye);
            Vector4f clip = projection.transform(new Vector4f((float) offset.x, (float) offset.y, (float) offset.z, 1));
            if (clip.w <= 0.05f) {
                continue; // behind the camera
            }
            int x = (int) ((clip.x / clip.w + 1) / 2 * width);
            int y = (int) ((1 - clip.y / clip.w) / 2 * height);
            if (x < HALF_ICON || x > width - HALF_ICON || y < HALF_ICON || y > height - HALF_ICON) {
                continue;
            }
            graphics.item(icon, x - HALF_ICON, y - HALF_ICON);
            int line = y + HALF_ICON + 2;
            if (mark.hasCustomName()) {
                graphics.centeredText(font, mark.getCustomName(), x, line, WHITE);
                line += font.lineHeight + 1;
            }
            graphics.centeredText(font, Component.literal(Math.round(offset.length()) + " m"), x, line, PALE);
        }
    }
}
