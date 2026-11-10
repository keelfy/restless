package dev.keelfy.restless.content.item.consumable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * A potion of the consumables: drunk like one, leaves the glass bottle, and does something only when the drink's rule
 * lets it (it may refuse, and then nothing is spent).
 */
public final class Draught extends Item {
    /** What drinking does. */
    @FunctionalInterface
    public interface Drink {
        /** @return whether it took effect, and so the draught is spent */
        boolean drunk(ServerPlayer player, ServerLevel level);
    }

    private static final int DRINK_TICKS = 32;

    private final Drink drink;

    public Draught(Item.Properties properties, Drink drink) {
        super(properties);
        this.drink = drink;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player && level instanceof ServerLevel world && drink.drunk(player, world)) {
            return ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE));
        }
        return stack;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return DRINK_TICKS;
    }
}
