package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The drowned stone with the lost one's way: in the off hand it does all the stone does; used in water or rain, it
 * throws the holder forward like a riptide. On dry ground there is no dash.
 */
public final class Tidecaller extends Item {
    public Tidecaller(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!player.isInWater() && !level.isRainingAt(player.blockPosition())) {
            if (player instanceof ServerPlayer server) {
                server.sendSystemMessage(Component.translatable("message.restless.tide_dry"), true);
            }
            return InteractionResult.FAIL;
        }
        Config.Section config = Config.section("advanced");
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(look.scale(config.num("tide_speed")));
        player.needsSync = true;
        player.resetFallDistance();
        level.playSound(
                null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
        player.getCooldowns().addCooldown(player.getItemInHand(hand), config.integer("tide_cooldown_ticks"));
        return InteractionResult.SUCCESS_SERVER;
    }
}
