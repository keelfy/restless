package dev.keelfy.restless.neoforge;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.content.AltarEntity;
import dev.keelfy.restless.content.AltarRenderer;
import dev.keelfy.restless.content.AshFootprint;
import dev.keelfy.restless.content.AshFootprintRenderer;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.Darkroom;
import dev.keelfy.restless.content.DreamcatcherEntity;
import dev.keelfy.restless.content.DreamcatcherRenderer;
import dev.keelfy.restless.content.EyesParticle;
import dev.keelfy.restless.content.EyesType;
import dev.keelfy.restless.content.MimicBlockEntity;
import dev.keelfy.restless.content.MimicRenderer;
import dev.keelfy.restless.content.MistParticle;
import dev.keelfy.restless.content.PhotoTooltip;
import dev.keelfy.restless.content.Picture;
import dev.keelfy.restless.content.RagEntity;
import dev.keelfy.restless.content.SightingMark;
import dev.keelfy.restless.haunt.Dimming;
import dev.keelfy.restless.haunt.HeadTurn;
import dev.keelfy.restless.haunt.RedVignette;
import dev.keelfy.restless.haunt.SanityOverlay;
import dev.keelfy.restless.haunt.ScreenOverlay;
import java.util.List;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Client-only wiring; the dedicated server never loads this class. */
@EventBusSubscriber(modid = Restless.MOD_ID, value = Dist.CLIENT)
public final class RestlessNeoForgeClient {
    private RestlessNeoForgeClient() {}

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
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(JournalHands.POINT);
    }

    @SubscribeEvent
    public static void registerTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(Picture.class, PhotoTooltip::new);
    }

    /** The camera obscura grabs its frame here: the world is drawn, the hand and the HUD are not yet. */
    @SubscribeEvent
    public static void afterLevel(RenderLevelStageEvent.AfterLevel event) {
        Darkroom.afterLevel();
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked") // the registry is typed BlockEntityType<?>; this one is built for AltarEntity
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer((EntityType<AshFootprint>) Content.ASH_FOOTPRINT.get(), AshFootprintRenderer::new);
        event.registerBlockEntityRenderer(
                (BlockEntityType<AltarEntity>) Content.ALTAR_ENTITY.get(), AltarRenderer::new);
        event.registerBlockEntityRenderer(
                (BlockEntityType<DreamcatcherEntity>) Content.DREAMCATCHER_ENTITY.get(),
                context -> new DreamcatcherRenderer());
        event.registerBlockEntityRenderer(
                (BlockEntityType<MimicBlockEntity>) Content.MIMIC_ENTITY.get(), MimicRenderer::new);
        event.registerEntityRenderer((EntityType<RagEntity>) Content.RAG_ENTITY.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(
                (EntityType<SightingMark>) Content.MARK_ENTITY.get(),
                context -> new ThrownItemRenderer<>(context, 1.5f, true));
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

    @SubscribeEvent
    public static void beforeFrame(RenderFrameEvent.Pre event) {
        HeadTurn.frame(event.getPartialTick());
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet((SimpleParticleType) Content.MIST.get(), MistParticle.Provider::new);
        event.registerSpriteSet(EyesType.type(), EyesParticle.Provider::new);
    }
}
