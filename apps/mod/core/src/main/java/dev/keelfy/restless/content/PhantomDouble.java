package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.event.DoubleEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The phantom's charm: a false copy of the holder stands where they are for a few seconds, and every hostile thing near
 * goes for it. Other players see the copy too.
 */
public final class PhantomDouble extends Item {
    public PhantomDouble(Item.Properties properties) {
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
        Config.Section config = Config.section("charms");
        int ticks = config.integer("double_ticks");
        var twin = DoubleEvent.decoy(server, ticks);
        if (twin == null) {
            return InteractionResult.FAIL;
        }
        server.addEffect(new MobEffectInstance(Charms.holder(Content.DECOY), ticks));
        for (Mob mob : level.getEntitiesOfClass(
                Mob.class, server.getBoundingBox().inflate(config.num("double_radius")), m -> m instanceof Enemy)) {
            mob.setTarget(twin);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.5f);
        player.getCooldowns().addCooldown(player.getItemInHand(hand), config.integer("double_cooldown_ticks"));
        return InteractionResult.SUCCESS_SERVER;
    }
}
