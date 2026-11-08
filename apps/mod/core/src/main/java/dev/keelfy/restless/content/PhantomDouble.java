package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.event.DoubleEvent;
import net.minecraft.core.BlockPos;
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
public class PhantomDouble extends Item {
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
        if (!cast(server, config.integer("double_ticks"), server.blockPosition(), false)) {
            return InteractionResult.FAIL;
        }
        player.getCooldowns().addCooldown(player.getItemInHand(hand), config.integer("double_cooldown_ticks"));
        return InteractionResult.SUCCESS_SERVER;
    }

    /**
     * Stands a copy of {@code server} at {@code at} for {@code ticks}: the holder is looked past meanwhile, and every
     * hostile near the holder goes for the copy. @return false when no copy could stand
     */
    static boolean cast(ServerPlayer server, int ticks, BlockPos at, boolean armed) {
        Config.Section config = Config.section("charms");
        var twin = DoubleEvent.decoy(server, ticks, at, armed);
        if (twin == null) {
            return false;
        }
        server.addEffect(new MobEffectInstance(Charms.holder(Content.DECOY), ticks));
        for (Mob mob : server.level()
                .getEntitiesOfClass(
                        Mob.class,
                        server.getBoundingBox().inflate(config.num("double_radius")),
                        m -> m instanceof Enemy)) {
            mob.setTarget(twin);
        }
        server.level()
                .playSound(
                        null, server.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.5f);
        return true;
    }
}
