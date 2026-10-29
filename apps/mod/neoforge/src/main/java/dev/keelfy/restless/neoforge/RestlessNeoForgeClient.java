package dev.keelfy.restless.neoforge;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.content.AltarEntity;
import dev.keelfy.restless.content.AltarRenderer;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.DreamcatcherEntity;
import dev.keelfy.restless.content.DreamcatcherRenderer;
import dev.keelfy.restless.haunt.Dimming;
import dev.keelfy.restless.haunt.SanityOverlay;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Client-only wiring; the dedicated server never loads this class. */
@EventBusSubscriber(modid = Restless.MOD_ID, value = Dist.CLIENT)
public final class RestlessNeoForgeClient {
    private RestlessNeoForgeClient() {}

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        // Above the camera overlays but under the crosshair and the HUD, so only the world goes dark.
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Identifier.fromNamespaceAndPath(Restless.MOD_ID, "dimming"), Dimming::render);
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Identifier.fromNamespaceAndPath(Restless.MOD_ID, "sanity"), SanityOverlay::render);
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked") // the registry is typed BlockEntityType<?>; this one is built for AltarEntity
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer((BlockEntityType<AltarEntity>) Content.ALTAR_ENTITY.get(), AltarRenderer::new);
        event.registerBlockEntityRenderer((BlockEntityType<DreamcatcherEntity>) Content.DREAMCATCHER_ENTITY.get(), context -> new DreamcatcherRenderer());
    }
}
