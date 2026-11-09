package dev.keelfy.restless.content;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * The slots of the {@link Stash} and the {@link GraveSatchel}: a hopper's row of five, of which the first {@code slots}
 * take items. Every change is written to the item at once, and the menu closes if the item leaves the hand.
 */
final class Pouch {
    private Pouch() {}

    static void open(ServerPlayer player, InteractionHand hand, int slots) {
        ItemStack pouch = player.getItemInHand(hand);
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, opener) -> new Menu(id, inventory, new Store(player, hand, pouch, slots), pouch),
                Component.translatable(pouch.getItem().getDescriptionId())));
    }

    private static final class Store extends SimpleContainer {
        private final Player player;
        private final InteractionHand hand;
        private final ItemStack pouch;
        private final int slots;

        Store(Player player, InteractionHand hand, ItemStack pouch, int slots) {
            super(HopperMenu.CONTAINER_SIZE);
            this.player = player;
            this.hand = hand;
            this.pouch = pouch;
            this.slots = Math.min(slots, HopperMenu.CONTAINER_SIZE);
            List<ItemStack> held = pouch.getOrDefault(Content.STASHED.get(), ItemContainerContents.EMPTY)
                    .nonEmptyItemCopyStream()
                    .toList();
            for (int i = 0; i < Math.min(this.slots, held.size()); i++) {
                super.setItem(i, held.get(i));
            }
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot < slots && !(stack.getItem() instanceof Stash) && !(stack.getItem() instanceof GraveSatchel);
        }

        @Override
        public boolean stillValid(Player who) {
            return who == player && player.getItemInHand(hand) == pouch;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            List<ItemStack> held = new ArrayList<>();
            for (int i = 0; i < slots; i++) {
                if (!getItem(i).isEmpty()) {
                    held.add(getItem(i).copy());
                }
            }
            if (held.isEmpty()) {
                pouch.remove(Content.STASHED.get());
            } else {
                pouch.set(Content.STASHED.get(), ItemContainerContents.fromItems(held));
            }
        }
    }

    /**
     * A hopper's menu with its own slots, so the closed ones refuse items. The client opens the plain hopper menu; the
     * server decides. The pouch itself must stay where it is while its slots are open.
     */
    private static final class Menu extends AbstractContainerMenu {
        private final Store store;
        private final ItemStack pouch;

        Menu(int id, Inventory inventory, Store store, ItemStack pouch) {
            super(MenuType.HOPPER, id);
            this.store = store;
            this.pouch = pouch;
            store.startOpen(inventory.player);
            for (int x = 0; x < HopperMenu.CONTAINER_SIZE; x++) {
                addSlot(new Slot(store, x, 44 + x * 18, 20) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return store.canPlaceItem(getContainerSlot(), stack);
                    }
                });
            }
            addStandardInventorySlots(inventory, 8, 51);
        }

        @Override
        public boolean stillValid(Player player) {
            return store.stillValid(player);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int slotIndex) {
            ItemStack moved = ItemStack.EMPTY;
            Slot slot = slots.get(slotIndex);
            if (slot.hasItem()) {
                ItemStack stack = slot.getItem();
                moved = stack.copy();
                int size = store.getContainerSize();
                boolean done = slotIndex < size
                        ? moveItemStackTo(stack, size, slots.size(), true)
                        : moveItemStackTo(stack, 0, size, false);
                if (!done) {
                    return ItemStack.EMPTY;
                }
                if (stack.isEmpty()) {
                    slot.setByPlayer(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }
            }
            return moved;
        }

        @Override
        public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
            boolean onPouch = slotIndex >= 0
                    && slotIndex < slots.size()
                    && slots.get(slotIndex).getItem() == pouch;
            boolean swapsPouch =
                    input == ContainerInput.SWAP && player.getInventory().getItem(button) == pouch;
            if (!onPouch && !swapsPouch) {
                super.clicked(slotIndex, button, input, player);
            }
        }

        @Override
        public void removed(Player player) {
            super.removed(player);
            store.stopOpen(player);
        }
    }
}
