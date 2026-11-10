package dev.keelfy.restless.content.item.charm;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import net.minecraft.network.chat.Component;
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

/** The mimic's charm: hostile things take the holder for one of their own until the holder strikes. */
public class BorrowedFace extends Item {
    public BorrowedFace(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Config.Section config = Config.section("charms");
        player.addEffect(new MobEffectInstance(Charms.holder(Content.BORROWED_FACE), config.integer("face_ticks")));
        for (Mob mob : level.getEntitiesOfClass(
                Mob.class,
                player.getBoundingBox().inflate(config.num("face_radius")),
                m -> m instanceof Enemy && m.getTarget() == player)) {
            mob.setTarget(null);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.0f, 0.7f);
        player.getCooldowns().addCooldown(player.getItemInHand(hand), config.integer("face_cooldown_ticks"));
        if (player instanceof ServerPlayer server) {
            server.sendSystemMessage(Component.translatable("message.restless.face_on"), true);
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
