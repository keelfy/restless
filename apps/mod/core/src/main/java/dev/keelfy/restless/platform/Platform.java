package dev.keelfy.restless.platform;

import java.nio.file.Path;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/**
 * Services core needs from the mod loader. Each loader module ships one implementation,
 * registered in {@code META-INF/services/dev.keelfy.restless.platform.Platform}.
 */
public interface Platform {
    String loaderName();

    boolean isDevelopment();

    /** Directory for user-editable config files. */
    Path configDir();

    /**
     * Declares an entry of this mod in a vanilla registry. The loader adds it when that registry opens;
     * the factory gets the key the entry must be built with, and the supplier is valid after that.
     */
    <T> Supplier<T> register(Registry<T> registry, String name, Function<ResourceKey<T>, T> factory);

    static Platform get() {
        return Holder.INSTANCE;
    }

    final class Holder {
        private static final Platform INSTANCE = ServiceLoader.load(Platform.class, Platform.class.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No Platform implementation on the classpath"));

        private Holder() {
        }
    }
}
