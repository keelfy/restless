package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The poltergeist's artifact: pulls every dropped item and every orb of experience around to its holder; what lies in
 * lava stays.
 */
public final class TelekinesisAmulet extends Item {
    public TelekinesisAmulet(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Config.Section config = Config.section("artifacts");
        double radius = config.num("amulet_radius");
        for (ItemEntity item : level.getEntitiesOfClass(
                ItemEntity.class, player.getBoundingBox().inflate(radius), e -> !e.isInLava())) {
            item.setPos(player.getX(), player.getY(), player.getZ());
            item.setNoPickUpDelay();
        }
        for (ExperienceOrb orb : level.getEntitiesOfClass(
                ExperienceOrb.class, player.getBoundingBox().inflate(radius))) {
            orb.setPos(player.getX(), player.getY(), player.getZ());
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.6f);
        player.getCooldowns().addCooldown(player.getItemInHand(hand), config.integer("amulet_cooldown_ticks"));
        return InteractionResult.SUCCESS_SERVER;
    }
}
