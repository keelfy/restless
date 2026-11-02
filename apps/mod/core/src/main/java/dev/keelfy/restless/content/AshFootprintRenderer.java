package dev.keelfy.restless.content;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** A horizontal decal, lit by the world, with no cube, shadow or billboard rotation. */
public final class AshFootprintRenderer extends EntityRenderer<AshFootprint, AshFootprintRenderer.State> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("restless", "textures/entity/ash_footprints.png");

    public static final class State extends EntityRenderState {
        int shape;
        boolean left;
        float yaw;
    }

    public AshFootprintRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(AshFootprint entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.shape = entity.shape();
        state.left = entity.left();
        state.yaw = entity.getYRot();
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.isInvisible) {
            return;
        }
        float u = ((state.shape - 1) % 2) * .5f;
        float v = ((state.shape - 1) / 2) * .5f;
        float left = state.left ? u : u + .5f;
        float right = state.left ? u + .5f : u;
        int light = state.lightCoords;
        pose.pushPose();
        pose.rotateDegrees(Axis.YP, -state.yaw);
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TEXTURE), (transform, vertices) -> {
            vertex(vertices, transform, -.25f, -.25f, left, v, light);
            vertex(vertices, transform, -.25f, .25f, left, v + .5f, light);
            vertex(vertices, transform, .25f, .25f, right, v + .5f, light);
            vertex(vertices, transform, .25f, -.25f, right, v, light);
        });
        pose.popPose();
    }

    private static void vertex(
            VertexConsumer vertices, PoseStack.Pose pose, float x, float z, float u, float v, int light) {
        vertices.addVertex(pose, x, 0, z)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0, 1, 0);
    }
}
