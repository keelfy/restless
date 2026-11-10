package dev.keelfy.restless.content.item.charm;

import dev.keelfy.restless.platform.Platform;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Where a worn charm is looked for. With an accessory-slot mod it is in its slot and nowhere else; without one the
 * charm works from the off hand, or from anywhere in the inventory for the charms that only need to be carried.
 */
public final class Worn {
    private Worn() {}

    /** The charm's stack if it is worn: in its accessory slot, or else in the off hand. Empty if not. */
    public static ItemStack held(Entity entity, Supplier<Item> item) {
        if (!(entity instanceof LivingEntity living)) {
            return ItemStack.EMPTY;
        }
        Platform platform = Platform.get();
        if (platform.hasAccessories()) {
            return platform.accessory(living, item.get());
        }
        ItemStack stack = living.getItemInHand(InteractionHand.OFF_HAND);
        return stack.is(item.get()) ? stack : ItemStack.EMPTY;
    }

    /** Like {@link #held}, but without an accessory mod the charm counts anywhere in a player's inventory. */
    public static ItemStack carried(Entity entity, Supplier<Item> item) {
        if (entity instanceof LivingEntity living && Platform.get().hasAccessories()) {
            return Platform.get().accessory(living, item.get());
        }
        return Charms.carried(entity, item);
    }

    public static boolean has(Entity entity, Supplier<Item> item) {
        return !held(entity, item).isEmpty();
    }

    /** Takes a worn charm off the player, wherever {@link #held} found it. */
    public static void clear(ServerPlayer player, ItemStack stack) {
        if (Platform.get().hasAccessories()) {
            Platform.get().clearAccessory(player, stack);
        } else if (player.getItemInHand(InteractionHand.OFF_HAND) == stack) {
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }
    }
}
