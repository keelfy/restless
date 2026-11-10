package dev.keelfy.restless.content.item.charm;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.haunt.Anger;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The demon's charm: all health back at once and a short fury (strength, resistance, fire proof), then for a while
 * fewer hearts ({@link Content#BARGAIN_EFFECT} takes them) and with the holder's spirits angrier. It will not strike a
 * second bargain while the first is paid.
 */
public class Bargain extends Item {
    public Bargain(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player.hasEffect(Charms.holder(Content.BARGAIN_EFFECT))) {
            if (player instanceof ServerPlayer server) {
                server.sendSystemMessage(Component.translatable("message.restless.bargain_owed"), true);
            }
            return InteractionResult.FAIL;
        }
        int ticks = Config.section("charms").integer("bargain_ticks");
        player.addEffect(new MobEffectInstance(Charms.holder(Content.BARGAIN_EFFECT), ticks));
        player.setHealth(player.getMaxHealth());
        // The demon's hour: what the holder bought is a short fury, paid for after.
        int hour = Config.section("charms").integer("bargain_boost_ticks");
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, hour, 1));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, hour, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, hour, 0));
        if (player instanceof ServerPlayer server) {
            Anger.provoke(server, Anger.Source.BARGAIN, player.blockPosition());
        }
        level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5f, 1.6f);
        player.getCooldowns().addCooldown(player.getItemInHand(hand), ticks);
        return InteractionResult.SUCCESS_SERVER;
    }
}
