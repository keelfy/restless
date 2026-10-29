package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Divination;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Takes a photo of whatever spirit is near. */
public final class CameraObscura extends Item {
    public CameraObscura(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() || !(player instanceof ServerPlayer server)) {
            return InteractionResult.SUCCESS;
        }
        player.getCooldowns().addCooldown(player.getItemInHand(hand), Config.section("camera").integer("cooldown_ticks"));
        Divination.photograph(server).ifPresent(photo -> {
            if (!player.addItem(photo)) {
                level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY(), player.getZ(), photo));
            }
        });
        return InteractionResult.SUCCESS_SERVER;
    }
}
