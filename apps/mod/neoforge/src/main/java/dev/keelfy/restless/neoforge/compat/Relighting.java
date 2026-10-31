package dev.keelfy.restless.neoforge.compat;

import dev.keelfy.restless.content.UnlitLights;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** Shared viewer data follows the same block mapping and ignition rules as world interaction. */
record Relighting(ItemStack input, ItemStack output) {
    static List<Relighting> recipes() {
        return UnlitLights.ITEMS.stream().map(item -> {
            var block = ((BlockItem) item.get()).getBlock();
            return new Relighting(new ItemStack(item.get()),
                new ItemStack(UnlitLights.relit(block.defaultBlockState()).getBlock()));
        }).toList();
    }

    static List<ItemStack> igniters() {
        return BuiltInRegistries.ITEM.stream().map(ItemStack::new).filter(UnlitLights::canIgnite).toList();
    }
}
