package dev.keelfy.restless.platform;

import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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

    /** Declares a creative tab of this mod that lists {@code items}; they are looked up when the tab is filled. */
    void registerTab(String name, Component title, Supplier<ItemStack> icon, List<? extends Supplier<? extends Item>> items);

    /** The player's client takes payloads of this type: it has this mod (a test's mock player does not). */
    boolean canReceive(ServerPlayer player, CustomPacketPayload.Type<?> type);

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
