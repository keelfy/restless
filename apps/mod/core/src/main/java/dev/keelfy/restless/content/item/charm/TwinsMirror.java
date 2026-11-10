package dev.keelfy.restless.content.item.charm;

import dev.keelfy.restless.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The phantom's double with the mimic's eye: the copy stands where the holder looks, up to a reach, and holds what the
 * holder holds.
 */
public final class TwinsMirror extends PhantomDouble {
    public TwinsMirror(Properties properties) {
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
        Config.Section config = Config.section("advanced");
        HitResult hit = server.pick(config.num("mirror_reach"), 0.0f, false);
        BlockPos at = hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK
                ? block.getBlockPos().relative(block.getDirection())
                : BlockPos.containing(hit.getLocation());
        if (!cast(server, Config.section("charms").integer("double_ticks"), at, true)) {
            return InteractionResult.FAIL;
        }
        player.getCooldowns()
                .addCooldown(
                        player.getItemInHand(hand), Config.section("charms").integer("double_cooldown_ticks"));
        return InteractionResult.SUCCESS_SERVER;
    }
}
