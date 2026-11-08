package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** The trickster's charm: a wager on the next mob the holder kills, settled in {@link Charms#drops}. */
public class TricksterCoin extends Item {
    public TricksterCoin(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Config.Section config = Config.section("charms");
        player.addEffect(new MobEffectInstance(Charms.holder(Content.GAMBLE), window(config)));
        level.playSound(
                null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.4f);
        player.getCooldowns().addCooldown(player.getItemInHand(hand), config.integer("coin_cooldown_ticks"));
        if (player instanceof ServerPlayer server) {
            server.sendSystemMessage(Component.translatable("message.restless.coin_tossed"), true);
            toss(server);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /** How long the wager stands. */
    protected int window(Config.Section config) {
        return config.integer("coin_ticks");
    }

    /** What a toss costs beyond the rest. */
    protected void toss(ServerPlayer player) {}
}
