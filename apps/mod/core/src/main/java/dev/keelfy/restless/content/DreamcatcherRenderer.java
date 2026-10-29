package dev.keelfy.restless.content;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Reuses the blockstate model, rotating it around the ceiling attachment in world space. */
public final class DreamcatcherRenderer implements BlockEntityRenderer<DreamcatcherEntity, DreamcatcherRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        final MovingBlockRenderState model = new MovingBlockRenderState();
        float swingX;
        float swingZ;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DreamcatcherEntity entity, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, cameraPosition, breakProgress);
        state.swingX = entity.swingX(partialTicks);
        state.swingZ = entity.swingZ(partialTicks);
        state.model.blockState = entity.getBlockState();
        state.model.blockPos = entity.getBlockPos();
        state.model.randomSeedPos = entity.getBlockPos();
        if (entity.getLevel() instanceof ClientLevel level) {
            state.model.biome = level.getBiome(entity.getBlockPos());
            state.model.cardinalLighting = level.cardinalLighting();
            state.model.lightEngine = level.getLightEngine();
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(.5f, 1, .5f);
        pose.rotateDegrees(Axis.XP, state.swingX);
        pose.rotateDegrees(Axis.ZP, state.swingZ);
        pose.translate(-.5f, -1, -.5f);
        collector.submitMovingBlock(pose, state.model, 0);
        pose.popPose();
    }
}
