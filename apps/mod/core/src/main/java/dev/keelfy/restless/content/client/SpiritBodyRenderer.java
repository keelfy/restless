package dev.keelfy.restless.content.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.keelfy.restless.content.entity.SpiritBody;
import dev.keelfy.restless.photo.Apparition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Draws a {@link SpiritBody} the way its photograph is drawn: the same {@link Apparition} is sampled on a grid that
 * always faces the viewer, each cell a see-through pale square as thick as the mist at that point, with the eyes and
 * mouth dark. No texture of its own: one white point of the vanilla snow texture is the whole surface. The mist drifts
 * slowly, and it glows the same in the dark as in the light.
 */
public final class SpiritBodyRenderer extends EntityRenderer<SpiritBody, SpiritBodyRenderer.State> {
    private static final Identifier PALE = Identifier.withDefaultNamespace("textures/block/snow.png");
    private static final int FULL_BRIGHT = 0xF000F0;
    /** How tall the figure stands, in blocks. */
    private static final float TALL = 2.2f;
    /** The grid: cells across and down the figure's frame. */
    private static final int COLUMNS = 28;

    private static final int ROWS = 34;
    /** The frame, in figure heights: across from the centre line and down from the top. */
    private static final float HALF_WIDTH = 0.42f;

    private static final float FROM_T = -0.04f;
    private static final float TO_T = 1.0f;
    /** How see-through the thickest mist still is. */
    private static final float OPACITY = 0.6f;
    /** The mist's tint, a cold white. */
    private static final int RED = 0xDC;

    private static final int GREEN = 0xE8;
    private static final int BLUE = 0xFF;

    public static final class State extends EntityRenderState {
        String kind = "";
        long seed;
        float age;
    }

    public SpiritBodyRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpiritBody entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.kind = entity.kind();
        state.seed = entity.seed();
        state.age = state.ageInTicks;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.isInvisible) {
            return;
        }
        Apparition figure = Apparition.of(state.kind, state.seed);
        float age = state.age;
        pose.pushPose();
        pose.translate(0, TALL / 2, 0);
        pose.rotate(camera.orientation);
        collector.submitCustomGeometry(pose, RenderTypes.entityTranslucent(PALE), (transform, vertices) -> {
            float cellU = 2 * HALF_WIDTH / COLUMNS;
            float cellT = (TO_T - FROM_T) / ROWS;
            // blocks per figure height
            float scale = TALL / (TO_T - FROM_T);
            for (int row = 0; row < ROWS; row++) {
                float t = FROM_T + (row + 0.5f) * cellT;
                // The mist drifts: each row sways a little, the feet more than the head.
                float drift = 0.012f * t * Mth.sin(age * 0.06f + t * 7);
                for (int column = 0; column < COLUMNS; column++) {
                    float u = -HALF_WIDTH + (column + 0.5f) * cellU;
                    float sampleU = u - (float) figure.lean() * t + drift;
                    float mist = (float) Math.min(1, figure.mist(sampleU, t));
                    if (mist < 0.03f) {
                        continue;
                    }
                    float shade = 1 - 0.85f * (float) figure.dark(sampleU, t);
                    int alpha = (int) (255 * OPACITY * mist);
                    int color =
                            alpha << 24 | (int) (RED * shade) << 16 | (int) (GREEN * shade) << 8 | (int) (BLUE * shade);
                    float x = u * scale;
                    float y = TALL / 2 - (t - FROM_T) * scale;
                    float w = cellU * scale / 2;
                    float h = cellT * scale / 2;
                    quad(vertices, transform, color, x - w, y - h, x + w, y + h);
                }
            }
        });
        pose.popPose();
    }

    /** A flat square facing the viewer, seen from both sides. */
    private static void quad(
            VertexConsumer vertices, PoseStack.Pose pose, int color, float x0, float y0, float x1, float y1) {
        vertex(vertices, pose, color, x0, y0);
        vertex(vertices, pose, color, x1, y0);
        vertex(vertices, pose, color, x1, y1);
        vertex(vertices, pose, color, x0, y1);
        vertex(vertices, pose, color, x0, y1);
        vertex(vertices, pose, color, x1, y1);
        vertex(vertices, pose, color, x1, y0);
        vertex(vertices, pose, color, x0, y0);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, int color, float x, float y) {
        vertices.addVertex(pose, x, y, 0)
                .setColor(color)
                .setUv(0.5f, 0.5f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, 0, 0, 1);
    }
}
