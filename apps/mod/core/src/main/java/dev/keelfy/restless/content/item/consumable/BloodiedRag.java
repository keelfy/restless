package dev.keelfy.restless.content.item.consumable;

import dev.keelfy.restless.content.entity.RagEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The revenant's rag, thrown like a snowball; what it does where it lands is in {@link RagEntity}. */
public final class BloodiedRag extends Item {
    public BloodiedRag(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack rag = player.getItemInHand(hand);
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.SNOWBALL_THROW,
                SoundSource.NEUTRAL,
                0.5f,
                0.5f);
        if (level instanceof ServerLevel server) {
            Projectile.spawnProjectileFromRotation(RagEntity::new, server, rag, player, 0.0f, 1.2f, 1.0f);
        }
        rag.consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
