package dev.keelfy.restless.content;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

/**
 * The hoarder's charm: holds one stack. Used with something in the other hand, it takes that in; used with a full
 * stash, it gives the stack back. Carried in the off hand when the holder dies, its stack stays with the holder and the
 * stash is spent, see {@link Charms}.
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
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stash = player.getItemInHand(hand);
        ItemStack held = Charms.stashed(stash);
        ItemStack other = player.getItemInHand(
                hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!held.isEmpty()) {
            stash.remove(Content.STASHED.get());
            Charms.give(player, held);
        } else if (!other.isEmpty() && !(other.getItem() instanceof Stash)) {
            stash.set(Content.STASHED.get(), ItemContainerContents.fromItems(java.util.List.of(other.copy())));
            other.setCount(0);
        } else {
            return InteractionResult.PASS;
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1.0f, 0.8f);
        return InteractionResult.SUCCESS_SERVER;
    }
}
