package dev.keelfy.restless.content.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.keelfy.restless.content.block.MimicBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Draws the block a mimic wears as that block's own model, or its true face while the Dark Lens shows it. */
public final class MimicRenderer implements BlockEntityRenderer<MimicBlockEntity, MimicRenderer.State> {
    private static final BlockDisplayContext CONTEXT = BlockDisplayContext.create();

    public static final class State extends BlockEntityRenderState {
        final BlockModelRenderState model = new BlockModelRenderState();
    }

    private final BlockModelResolver models;

    public MimicRenderer(BlockEntityRendererProvider.Context context) {
        models = context.blockModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            MimicBlockEntity mimic,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(mimic, state, partialTicks, cameraPosition, breakProgress);
        long now = mimic.getLevel() == null ? 0L : mimic.getLevel().getGameTime();
        var shown = mimic.revealed(now) ? mimic.getBlockState() : mimic.look();
        if (shown.isAir()) {
            state.model.clear();
        } else {
            models.update(state.model, shown, CONTEXT);
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.model.isEmpty()) {
            state.model.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
    }
}
