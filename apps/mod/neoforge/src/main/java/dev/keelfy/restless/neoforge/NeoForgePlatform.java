package dev.keelfy.restless.neoforge;

import dev.keelfy.restless.platform.Platform;
import dev.keelfy.restless.Restless;
import java.nio.file.Path;
import java.util.AbstractList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

    @Override
    public void registerTab(String name, Component title, Supplier<ItemStack> icon, List<? extends Supplier<? extends Item>> items) {
        List<net.minecraft.core.Holder<Item>> holders = new AbstractList<>() {
            @Override
            public net.minecraft.core.Holder<Item> get(int index) {
                return BuiltInRegistries.ITEM.wrapAsHolder(items.get(index).get());
            }

            @Override
            public int size() {
                return items.size();
            }
        };
        register(BuiltInRegistries.CREATIVE_MODE_TAB, name,
            key -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0).title(title).icon(icon).displayItems(holders).build());
    }

    @Override
    public boolean canReceive(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return player.connection.hasChannel(type);
    }
}
