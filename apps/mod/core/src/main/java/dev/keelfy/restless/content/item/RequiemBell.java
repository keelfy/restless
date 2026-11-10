package dev.keelfy.restless.content.item;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.item.charm.Charms;
import dev.keelfy.restless.haunt.Requiem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/**
 * What a {@link Reliquary} becomes once the Requiem is sung. Rung while sneaking, for a few seconds, it takes the
 * release back: the dead remember the player, and three personal spirits are born again.
 */
public final class RequiemBell extends Item {
    public RequiemBell(Item.Properties properties) {
        super(properties);
    }

    /** Turns every reliquary the player carries into a bell; one is given if they carry none. */
    public static void replaceReliquary(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        boolean found = false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(Content.RELIQUARY.get())) {
                inventory.setItem(slot, new ItemStack(Content.REQUIEM_BELL.get()));
                found = true;
            }
        }
        if (!found) {
            Charms.give(player, new ItemStack(Content.REQUIEM_BELL.get()));
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer server) {
                server.sendSystemMessage(Component.translatable("message.restless.bell_sneak"), true);
            }
            return InteractionResult.FAIL;
        }
        if (player instanceof ServerPlayer server && !Requiem.released(server)) {
            server.sendSystemMessage(Component.translatable("message.restless.bell_silent"), true);
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    /** Letting go of sneak before the bell has rung its time stops it. */
    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int ticksRemaining) {
        if (!level.isClientSide() && !user.isShiftKeyDown()) {
            user.stopUsingItem();
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            Requiem.takeBack(player);
        }
        return stack;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.TOOT_HORN;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return Config.section("requiem").integer("bell_ticks");
    }
}
