package dev.keelfy.restless.neoforge;

import dev.keelfy.restless.platform.Platform;
import net.neoforged.fml.loading.FMLEnvironment;

public final class NeoForgePlatform implements Platform {
    @Override
    public String loaderName() {
        return "NeoForge";
    }

    @Override
    public boolean isDevelopment() {
        return !FMLEnvironment.isProduction();
    }
}
