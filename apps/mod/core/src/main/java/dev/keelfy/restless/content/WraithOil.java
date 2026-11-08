package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * The wraith's oil: used with a weapon in the other hand, it coats the weapon for a number of hits. A coated weapon
 * hits harder in the dark and through armor, see {@link Charms#hurt}. The oil is spent and the bottle comes back.
 */
public final class WraithOil extends Item {
    public WraithOil(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        InteractionHand other =
                hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack weapon = player.getItemInHand(other);
        if (!weapon.has(DataComponents.WEAPON)) {
            if (player instanceof ServerPlayer server) {
                server.sendSystemMessage(Component.translatable("message.restless.oil_no_weapon"), true);
            }
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        weapon.set(
                Content.CHARGES.get(), new Charges(Config.section("consumables").integer("oil_charges")));
        level.playSound(null, player.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0f, 0.6f);
        ItemStack oil = player.getItemInHand(hand);
        oil.consume(1, player);
        Charms.give(player, new ItemStack(Items.GLASS_BOTTLE));
        return InteractionResult.SUCCESS_SERVER;
    }
}
