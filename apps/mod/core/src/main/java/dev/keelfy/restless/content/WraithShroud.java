package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Sanity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The wraith's charm: for a few seconds the holder is no more than a shade. Blows pass through them, and theirs pass
 * through whatever they strike (see {@link Charms#hurt}). It costs sanity and rests long.
 */
public final class WraithShroud extends Item {
    public WraithShroud(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Config.Section config = Config.section("charms");
        player.addEffect(new MobEffectInstance(Charms.holder(Content.PHASED), config.integer("shroud_ticks")));
        if (player instanceof ServerPlayer server) {
            Sanity.set(server, Sanity.of(server) - config.num("shroud_sanity_cost"));
        }
        level.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.0f, 0.5f);
        player.getCooldowns().addCooldown(player.getItemInHand(hand), config.integer("shroud_cooldown_ticks"));
        return InteractionResult.SUCCESS_SERVER;
    }
}
