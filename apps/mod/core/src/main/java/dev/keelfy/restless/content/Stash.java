package dev.keelfy.restless.content;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The hoarder's charm: holds one stack. Used, it opens a slot to put the stack in or take it out. Carried anywhere in
 * the inventory when the holder dies, its stack stays with the holder and the stash is spent, see {@link Charms}.
 */
public final class Stash extends Item {
    public Stash(Item.Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        ItemStack held = Charms.stashed(stack);
        return held.isEmpty()
                ? super.getName(stack)
                : Component.translatable("item.restless.stash.holding", held.getCount(), held.getHoverName());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server) {
            Pouch.open(server, hand, 1);
        }
        return InteractionResult.SUCCESS;
    }
}
