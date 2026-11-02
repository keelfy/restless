package dev.keelfy.restless.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.keelfy.restless.Restless;
import dev.keelfy.restless.content.PhotoTooltip;
import dev.keelfy.restless.content.Picture;
import dev.keelfy.restless.haunt.PageCanvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws a journal view on the pages of the book in hand. Book pixels are laid over the item's face, the pose being the
 * item's own; each thing drawn sits a hair in front of the last, as the depth test is what orders them.
 */
final class HeldCanvas implements PageCanvas {
    private static final Identifier WHITE = Identifier.fromNamespaceAndPath(Restless.MOD_ID, "textures/misc/white.png");
    /** The paper's face, in item units from the book's middle, and the gap between one thing drawn and the next. */
    private static final float PAPER = 0.0445f;

    private static final float GAP = 0.00012f;

    private final PoseStack pose;
    private final SubmitNodeCollector collector;
    private final int light;
    private final Font font;
    private final int width;
    private final int height;
    private int layer;

    HeldCanvas(PoseStack pose, SubmitNodeCollector collector, int light, int width, int height) {
        this.pose = pose;
        this.collector = collector;
        this.light = light;
        this.font = Minecraft.getInstance().font;
        this.width = width;
        this.height = height;
    }

    /** Book pixels as the pose's units: x to the right and y down from the book's top left corner. */
    private void begin(float z) {
        pose.pushPose();
        pose.translate(-0.5f, 0.5f, PAPER + z);
        pose.scale(1f / width, -1f / height, 1f / width);
    }

    private float next() {
        return ++layer * GAP;
    }

    @Override
    public void fill(int x0, int y0, int x1, int y1, int color) {
        quad(WHITE, color, next(), x0, y0, x1, y1);
    }

    @Override
    public void text(FormattedCharSequence text, int x, int y, int color) {
        begin(next());
        collector.submitText(pose, x, y, text, false, Font.DisplayMode.NORMAL, light, color, 0, 0);
        pose.popPose();
    }

    @Override
    public void item(ItemStack stack, int x, int y) {
        Minecraft minecraft = Minecraft.getInstance();
        ItemStackRenderState state = new ItemStackRenderState();
        minecraft
                .getItemModelResolver()
                .updateForTopItem(state, stack, ItemDisplayContext.GUI, minecraft.level, minecraft.player, 0);
        begin(next());
        pose.translate(x + 8, y + 8, 0);
        pose.scale(16, -16, 1);
        state.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    @Override
    public void picture(Picture picture, int x, int y) {
        int border = PhotoTooltip.BORDER;
        fill(x, y, x + Picture.WIDTH + border * 2, y + Picture.HEIGHT + border * 2, PhotoTooltip.LIGHT);
        quad(
                PhotoTooltip.texture(picture),
                -1,
                next(),
                x + border,
                y + border,
                x + border + Picture.WIDTH,
                y + border + Picture.HEIGHT);
    }

    /** A pointer, drawn over everything. */
    void pointer(int x, int y) {
        fill(x - 1, y - 1, x + 6, y + 6, 0xFFEEDFBA);
        fill(x, y, x + 5, y + 5, 0xFF382B24);
    }

    private void quad(Identifier texture, int color, float z, float x0, float y0, float x1, float y1) {
        begin(z);
        collector.submitCustomGeometry(pose, RenderTypes.text(texture), (last, buffer) -> {
            buffer.addVertex(last, x0, y1, 0).setColor(color).setUv(0, 1).setLight(light);
            buffer.addVertex(last, x1, y1, 0).setColor(color).setUv(1, 1).setLight(light);
            buffer.addVertex(last, x1, y0, 0).setColor(color).setUv(1, 0).setLight(light);
            buffer.addVertex(last, x0, y0, 0).setColor(color).setUv(0, 0).setLight(light);
        });
        pose.popPose();
    }
}
