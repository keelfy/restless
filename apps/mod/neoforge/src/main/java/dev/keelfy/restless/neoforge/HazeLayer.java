package dev.keelfy.restless.neoforge;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

/**
 * The particle layer that draws {@link dev.keelfy.restless.content.client.PresenceParticle} with the {@code haze}
 * fragment shader instead of the plain sprite one. It is the vanilla translucent particle pipeline with only the
 * fragment shader swapped, in both the ordinary and the order-independent-transparency variants. The loader registers
 * {@link #PIPELINE} and {@link #OIT}.
 */
public final class HazeLayer {
    private static final Identifier SHADER = Identifier.fromNamespaceAndPath("restless", "core/haze");

    public static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("restless", "pipeline/haze_particle"))
            .withFragmentShader(SHADER)
            .withCull(false)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build();

    public static final OitPipelineSet OIT = OitPipelineSet.builder(
                    Identifier.fromNamespaceAndPath("restless", "haze_particle"),
                    RenderPipeline.builder(RenderPipelines.OIT_PARTICLE_SNIPPET)
                            .withFragmentShader(SHADER)
                            .withCull(false))
            .build();

    public static final SingleQuadParticle.Layer LAYER =
            new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, PIPELINE, OIT);

    private HazeLayer() {}
}
