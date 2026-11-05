package dev.keelfy.restless.neoforge;

import dev.keelfy.restless.Restless;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only: the mod list's config button opens the screen made from {@link RestlessSettings}. */
@Mod(value = Restless.MOD_ID, dist = Dist.CLIENT)
public final class RestlessNeoForgeScreens {
    public RestlessNeoForgeScreens(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
