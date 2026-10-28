package dev.keelfy.restless.neoforge;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.haunt.Dimming;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
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
    }
}
