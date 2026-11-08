package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import java.util.ArrayList;
import java.util.List;
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
 * The hoarder's stash with the lost one's road home: holds three stacks, and carried in the off hand at death it stays
 * with its holder, full. It is not spent: it rests three days after it triggers, see {@link Charms#died}.
 */
public final class GraveSatchel extends Item {
    public GraveSatchel(Item.Properties properties) {
        super(properties);
    }

    static List<ItemStack> held(ItemStack satchel) {
        return satchel.getOrDefault(Content.STASHED.get(), ItemContainerContents.EMPTY)
                .nonEmptyItemCopyStream()
                .toList();
    }

    @Override
    public Component getName(ItemStack stack) {
        int stacks = held(stack).size();
        return stacks == 0
                ? super.getName(stack)
                : Component.translatable("item.restless.grave_satchel.holding", stacks);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack satchel = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(
                hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        List<ItemStack> held = new ArrayList<>(held(satchel));
        if (!other.isEmpty()
                && !(other.getItem() instanceof GraveSatchel)
                && !(other.getItem() instanceof Stash)
                && held.size() < Config.section("advanced").integer("satchel_stacks")) {
            held.add(other.copy());
            other.setCount(0);
            satchel.set(Content.STASHED.get(), ItemContainerContents.fromItems(held));
        } else if (!held.isEmpty()) {
            held.forEach(stack -> Charms.give(player, stack));
            satchel.remove(Content.STASHED.get());
        } else {
            return InteractionResult.PASS;
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1.0f, 0.7f);
        return InteractionResult.SUCCESS_SERVER;
    }
}
