package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** The shade's artifact: its holder sees in the dark, and every living thing around shows through walls for a while. */
public final class DarkEye extends Item {
    public DarkEye(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Config.Section config = Config.section("artifacts");
        int ticks = config.integer("eye_ticks");
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, ticks));
        for (LivingEntity seen : level.getEntitiesOfClass(
                LivingEntity.class, player.getBoundingBox().inflate(config.num("eye_radius")), e -> e != player)) {
            seen.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks));
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 0.8f, 0.6f);
        player.getCooldowns().addCooldown(player.getItemInHand(hand), config.integer("eye_cooldown_ticks"));
        return InteractionResult.SUCCESS_SERVER;
    }
}
