package dev.keelfy.restless.content;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Draws whatever the altar holds as its real item, lying on top; a new focus needs no model. */
public final class AltarRenderer implements BlockEntityRenderer<AltarEntity, AltarRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        float animationTicks;
    }

    private final ItemModelResolver items;

    public AltarRenderer(BlockEntityRendererProvider.Context context) {
        items = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(AltarEntity altar, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(altar, state, partialTicks, cameraPosition, breakProgress);
        state.animationTicks = altar.getLevel() == null ? 0 :
            (altar.getLevel().getGameTime() % 24000) + partialTicks;
        items.updateForTopItem(state.item, altar.getBlockState().getValue(Altar.HELD).stack(), ItemDisplayContext.FIXED,
            altar.getLevel(), null, (int) altar.getBlockPos().asLong());
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5f, 1.05f + (float) Math.sin(state.animationTicks * 0.06f) * 0.035f, 0.5f);
        pose.rotateDegrees(Axis.YP, state.animationTicks * 1.5f);
        pose.scale(0.5f, 0.5f, 0.5f);
        state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
}
