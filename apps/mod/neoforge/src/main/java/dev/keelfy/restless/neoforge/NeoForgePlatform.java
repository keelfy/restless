package dev.keelfy.restless.neoforge;

import dev.keelfy.restless.platform.Platform;
import dev.keelfy.restless.Restless;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NeoForgePlatform implements Platform {
    private final Map<Registry<?>, DeferredRegister<?>> registers = new HashMap<>();

    @Override
    public String loaderName() {
        return "NeoForge";
    }

    @Override
    public boolean isDevelopment() {
        return !FMLEnvironment.isProduction();
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Supplier<T> register(Registry<T> registry, String name, Function<ResourceKey<T>, T> factory) {
        DeferredRegister<T> register = (DeferredRegister<T>) registers.computeIfAbsent(registry, r -> {
            DeferredRegister<T> created = DeferredRegister.create(registry, Restless.MOD_ID);
            created.register(ModLoadingContext.get().getActiveContainer().getEventBus());
            return created;
        });
        return register.register(name, id -> factory.apply(ResourceKey.create(registry.key(), id)));
    }
}
