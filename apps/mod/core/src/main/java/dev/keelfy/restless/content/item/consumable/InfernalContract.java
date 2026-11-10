package dev.keelfy.restless.content.item.consumable;

import dev.keelfy.restless.haunt.Anger;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

/**
 * The demon's contract: used with an enchanted item in the other hand, one enchantment already at its highest level
 * goes one past it. The item takes the Curse of Vanishing and the signer's spirits are angrier. An item is signed once
 * (nothing may stand above its peak already), and a book holds no enchantment of its own to sign.
 */
public final class InfernalContract extends Item {
    public InfernalContract(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        InteractionHand other =
                hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack target = player.getItemInHand(other);
        if (!(player instanceof ServerPlayer server)) {
            return InteractionResult.SUCCESS;
        }
        ItemEnchantments enchantments = target.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        List<Holder<Enchantment>> peaks = new ArrayList<>();
        for (var entry : enchantments.entrySet()) {
            int max = entry.getKey().value().getMaxLevel();
            if (entry.getIntValue() > max) {
                server.sendSystemMessage(Component.translatable("message.restless.contract_signed"), true);
                return InteractionResult.FAIL;
            }
            if (entry.getIntValue() == max && !entry.getKey().is(Enchantments.VANISHING_CURSE)) {
                peaks.add(entry.getKey());
            }
        }
        if (peaks.isEmpty()) {
            server.sendSystemMessage(Component.translatable("message.restless.contract_no_peak"), true);
            return InteractionResult.FAIL;
        }
        Holder<Enchantment> raised = peaks.get(server.getRandom().nextInt(peaks.size()));
        Holder<Enchantment> curse = server.level()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.VANISHING_CURSE);
        EnchantmentHelper.updateEnchantments(target, mutable -> {
            mutable.set(raised, raised.value().getMaxLevel() + 1);
            mutable.set(curse, 1);
        });
        Anger.provoke(server, Anger.Source.CONTRACT, server.blockPosition());
        level.playSound(null, server.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5f, 1.8f);
        server.sendSystemMessage(
                Component.translatable("message.restless.contract_sealed", target.getHoverName()), true);
        player.getItemInHand(hand).consume(1, player);
        return InteractionResult.SUCCESS_SERVER;
    }
}
