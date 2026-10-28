package dev.keelfy.restless.platform;

import java.nio.file.Path;
import java.util.ServiceLoader;

/**
 * Services core needs from the mod loader. Each loader module ships one implementation,
 * registered in {@code META-INF/services/dev.keelfy.restless.platform.Platform}.
 */
public interface Platform {
    String loaderName();

    boolean isDevelopment();

    /** Directory for user-editable config files. */
    Path configDir();

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
