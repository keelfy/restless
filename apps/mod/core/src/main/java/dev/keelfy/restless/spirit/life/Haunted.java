package dev.keelfy.restless.spirit.life;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.spirit.Spirits;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * A haunted thing: an ordinary item from a structure chest whose stack carries the {@link #KEY} flag in its custom
 * data. Nothing about it shows. The first player to hold it gets a personal spirit through {@link Births#lure}, and
 * the flag goes either way: a refused birth (cap, cooldown, creative) binds nothing and does not wait for later.
 */
public final class Haunted {
    public static final String KEY = "restless_haunted";

    private Haunted() {}

    /** Call once per server tick: held stacks are looked at every {@code births.haunted_check_ticks}. */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % Config.section("births").integer("haunted_check_ticks") != 0) {
            return;
        }
        server.getPlayerList().getPlayers().forEach(Haunted::check);
    }

    /** Binds a random personal spirit for each marked stack the player holds, and clears the marks. */
    public static void check(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !marked(stack)) {
                continue;
            }
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(KEY));
            if (stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).isEmpty()) {
                stack.remove(DataComponents.CUSTOM_DATA);
            }
            var types = Spirits.personal();
            Births.lure(player, types.get(player.getRandom().nextInt(types.size())));
        }
    }

    public static boolean marked(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains(KEY);
    }

    /** A marked copy of {@code stack}, for the loot table's counterpart in tests and the debug menu. */
    public static ItemStack mark(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(KEY, true));
        return stack;
    }
}
