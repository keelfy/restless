package dev.keelfy.restless.neoforge.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

/** Reads and empties Curios accessory slots. Loaded only when Curios is installed; see {@code NeoForgePlatform}. */
public final class CuriosCompat {
    private CuriosCompat() {}

    /**
     * The stack of {@code item} worn in a slot, the stack itself, looked up fresh every time (the by-item lookup
     * caches), so a change to it is kept; empty if none.
     */
    public static ItemStack find(LivingEntity entity, Item item) {
        return CuriosApi.getCuriosInventory(entity)
                .flatMap(inventory -> inventory.findFirstCurio(stack -> stack.is(item)))
                .map(SlotResult::stack)
                .orElse(ItemStack.EMPTY);
    }

    /** Empties the slot that holds exactly {@code stack}. */
    public static void clear(LivingEntity entity, ItemStack stack) {
        CuriosApi.getCuriosInventory(entity)
                .ifPresent(inventory -> inventory
                        .findCurios(held -> held == stack)
                        .forEach(result -> inventory.setEquippedCurio(
                                result.slotContext().identifier(),
                                result.slotContext().index(),
                                ItemStack.EMPTY)));
    }
}
