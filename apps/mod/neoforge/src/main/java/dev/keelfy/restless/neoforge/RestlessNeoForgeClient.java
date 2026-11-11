package dev.keelfy.restless.neoforge;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.EyesType;
import dev.keelfy.restless.content.Presence;
import dev.keelfy.restless.content.block.AltarEntity;
import dev.keelfy.restless.content.block.DreamcatcherEntity;
import dev.keelfy.restless.content.block.MimicBlockEntity;
import dev.keelfy.restless.content.client.AltarRenderer;
import dev.keelfy.restless.content.client.AshFootprintRenderer;
import dev.keelfy.restless.content.client.DreamcatcherRenderer;
import dev.keelfy.restless.content.client.EyesParticle;
import dev.keelfy.restless.content.client.MimicRenderer;
import dev.keelfy.restless.content.client.MistParticle;
import dev.keelfy.restless.content.client.PresenceParticle;
import dev.keelfy.restless.content.client.SightingMarkHud;
import dev.keelfy.restless.content.client.SilhouetteRenderer;
import dev.keelfy.restless.content.entity.AshFootprint;
import dev.keelfy.restless.content.entity.RagEntity;
import dev.keelfy.restless.content.entity.SightingMark;
import dev.keelfy.restless.content.entity.Silhouette;
import dev.keelfy.restless.divine.client.SpiritBoxVoice;
import dev.keelfy.restless.haunt.effect.client.Dimming;
import dev.keelfy.restless.haunt.effect.client.HeadTurn;
import dev.keelfy.restless.haunt.effect.client.RedVignette;
import dev.keelfy.restless.haunt.effect.client.ScreenOverlay;
import dev.keelfy.restless.photo.Picture;
import dev.keelfy.restless.photo.client.Darkroom;
import dev.keelfy.restless.photo.client.PhotoTooltip;
import dev.keelfy.restless.sanity.client.SanityOverlay;
import java.util.List;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Client-only wiring; the dedicated server never loads this class. */
@EventBusSubscriber(modid = Restless.MOD_ID, value = Dist.CLIENT)
public final class RestlessNeoForgeClient {
    private RestlessNeoForgeClient() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        SpiritBoxVoice.tick();
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        // Above the camera overlays but under the crosshair and the HUD, so only the world goes dark.
        event.registerAbove(
                VanillaGuiLayers.CAMERA_OVERLAYS,
                Identifier.fromNamespaceAndPath(Restless.MOD_ID, "dimming"),
                Dimming::render);
        event.registerAbove(
                VanillaGuiLayers.CAMERA_OVERLAYS,
                Identifier.fromNamespaceAndPath(Restless.MOD_ID, "sanity"),
                SanityOverlay::render);
        event.registerAbove(
                VanillaGuiLayers.CAMERA_OVERLAYS,
                Identifier.fromNamespaceAndPath(Restless.MOD_ID, "red_vignette"),
                RedVignette::render);
        event.registerAbove(
                VanillaGuiLayers.CAMERA_OVERLAYS,
                Identifier.fromNamespaceAndPath(Restless.MOD_ID, "camera_flash"),
                Darkroom::render);
        event.registerAbove(
                VanillaGuiLayers.CAMERA_OVERLAYS,
                Identifier.fromNamespaceAndPath(Restless.MOD_ID, "screen"),
                ScreenOverlay::render);
        // Last of ours, so a mark stays readable in the dark.
        event.registerAbove(
                VanillaGuiLayers.CAMERA_OVERLAYS,
                Identifier.fromNamespaceAndPath(Restless.MOD_ID, "sighting_marks"),
                SightingMarkHud::render);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(JournalHands.POINT);
    }

    @SubscribeEvent
    public static void registerTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(Picture.class, PhotoTooltip::new);
    }

    /** The photo shown in an item frame, picked by the photo's item model. */
    @SubscribeEvent
    public static void registerSpecialModels(RegisterSpecialModelRendererEvent event) {
        event.register(Identifier.fromNamespaceAndPath(Restless.MOD_ID, "photo"), PhotoFrame.CODEC);
    }

    /** The camera obscura grabs its frame here: the world is drawn, the hand and the HUD are not yet. */
    @SubscribeEvent
    public static void afterLevel(RenderLevelStageEvent.AfterLevel event) {
        Darkroom.afterLevel();
    }

    /** No hand in the frame of a photo, even when a shader pack draws the hand before the frame is grabbed. */
    @SubscribeEvent
    public static void noHandInPhoto(RenderHandEvent event) {
        if (Darkroom.exposing()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked") // the registry is typed BlockEntityType<?>; this one is built for AltarEntity
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer((EntityType<AshFootprint>) Content.ASH_FOOTPRINT.get(), AshFootprintRenderer::new);
        event.registerEntityRenderer((EntityType<Silhouette>) Content.SILHOUETTE.get(), SilhouetteRenderer::new);
        event.registerBlockEntityRenderer(
                (BlockEntityType<AltarEntity>) Content.ALTAR_ENTITY.get(), AltarRenderer::new);
        event.registerBlockEntityRenderer(
                (BlockEntityType<DreamcatcherEntity>) Content.DREAMCATCHER_ENTITY.get(),
                context -> new DreamcatcherRenderer());
        event.registerBlockEntityRenderer(
                (BlockEntityType<MimicBlockEntity>) Content.MIMIC_ENTITY.get(), MimicRenderer::new);
        event.registerEntityRenderer((EntityType<RagEntity>) Content.RAG_ENTITY.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer((EntityType<SightingMark>) Content.MARK_ENTITY.get(), NoopRenderer::new);
        event.registerEntityRenderer(EntityTypes.ITEM_FRAME, PhotoFrameRenderer::new);
        event.registerEntityRenderer(EntityTypes.GLOW_ITEM_FRAME, PhotoFrameRenderer::new);
    }

    /** Ghost leaves are tinted as the leaves they copy; pale oak's are not tinted at all. */
    @SubscribeEvent
    public static void registerBlockTints(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(
                List.of(BlockTintSources.foliage()),
                Content.GHOST_WOODS.get("oak").leaves().get());
        // The birch colour vanilla gives its leaves, which do not follow the biome.
        event.register(
                List.of(BlockTintSources.constant(-8345771)),
                Content.GHOST_WOODS.get("birch").leaves().get());
    }

    /**
     * An invisible mannequin is the phantom's ghost double: draw it as vanilla draws a teammate's invisible body, a
     * faint translucent one, with no shadow.
     */
    @SubscribeEvent
    public static void ghost(RenderLivingEvent.Pre<?, ?, ?> event) {
        LivingEntityRenderState state = event.getRenderState();
        if (state.entityType == EntityTypes.MANNEQUIN && state.isInvisible) {
            state.isInvisibleToPlayer = false;
            state.shadowRadius = 0.0f;
        }
    }

    @SubscribeEvent
    public static void beforeFrame(RenderFrameEvent.Pre event) {
        HeadTurn.frame(event.getPartialTick());
    }

    /** The haze shader's pipelines, so the renderer compiles them with the vanilla ones. */
    @SubscribeEvent
    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(HazeLayer.PIPELINE);
        event.registerOitPipelineSet(HazeLayer.OIT);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet((SimpleParticleType) Content.MIST.get(), MistParticle.Provider::new);
        event.registerSpriteSet(EyesType.type(), EyesParticle.Provider::new);
        event.registerSpriteSet(Presence.type(), sprites -> new PresenceParticle.Provider(sprites, HazeLayer.LAYER));
    }
}
