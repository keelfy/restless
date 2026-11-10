package dev.keelfy.restless.content.item.charm;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.sanity.Sanity;
import dev.keelfy.restless.spirit.life.Anger;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The demon's bargain with the wraith's shadow: all health at once and a longer fury, but the debt is paid in sanity at
 * once, not in hearts. The rest is saved with the item.
 */
public final class BlackBargain extends Item {
    public BlackBargain(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer server)) {
            return InteractionResult.PASS;
        }
        ItemStack bargain = player.getItemInHand(hand);
        if (!Rest.ready(bargain, level)) {
            server.sendSystemMessage(Component.translatable("message.restless.charm_resting"), true);
            return InteractionResult.FAIL;
        }
        Config.Section config = Config.section("advanced");
        Rest.start(server, bargain, config.integer("black_rest_ticks"));
        player.setHealth(player.getMaxHealth());
        int hour = config.integer("black_boost_ticks");
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, hour, 1));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, hour, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, hour, 0));
        Sanity.set(server, Sanity.of(server) - config.num("black_sanity_cost"));
        Anger.provoke(server, Anger.Source.BARGAIN, player.blockPosition());
        level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5f, 0.8f);
        return InteractionResult.SUCCESS_SERVER;
    }
}
