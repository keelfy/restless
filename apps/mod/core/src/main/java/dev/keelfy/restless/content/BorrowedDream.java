package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Anger;
import dev.keelfy.restless.haunt.Sanity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** The mara's charm: sanity back to full at once, paid for by the holder's spirits being angrier. Once a day. */
public final class BorrowedDream extends Item {
    public BorrowedDream(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer server) {
            Sanity.set(server, 1.0);
            Anger.provoke(server, Anger.Source.DREAM, player.blockPosition());
            server.sendSystemMessage(Component.translatable("message.restless.dream_taken"), true);
        }
        level.playSound(
                null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 0.5f);
        player.getCooldowns()
                .addCooldown(
                        player.getItemInHand(hand), Config.section("charms").integer("dream_cooldown_ticks"));
        return InteractionResult.SUCCESS_SERVER;
    }
}
