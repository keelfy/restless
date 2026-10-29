package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Restore;
import java.util.Comparator;
import java.util.List;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * 122: another player stands at the edge of the player's field and harvests the ripe crops within its reach, one swing
 * at a time, with nothing dropped: stolen. They grow back at their age later, and stay gone under hardcore.
 */
final class HarvestEvent extends StrangerAct {
    private static final String ID = "harvest";

    HarvestEvent() {
        super(ID);
    }

    @Override
    boolean step(ServerPlayer player, Mannequin figure, Config.Section config, int n) {
        ServerLevel level = player.level();
        BlockPos crop = ripe(level, figure.blockPosition(), config.integer("reach")).stream()
            .min(Comparator.comparingDouble(pos -> pos.distToCenterSqr(figure.position()))).orElse(null);
        if (crop == null) {
            return false;
        }
        figure.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(crop));
        figure.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
        BlockState grown = level.getBlockState(crop);
        level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, crop, Block.getId(grown));
        Restore.quiet(level, crop, Blocks.AIR.defaultBlockState());
        Restore.revert(level, config.integer("restore_ticks"), force -> {
            if (!force && !level.isLoaded(crop)) {
                return false;
            }
            // Put back only on the bare farmland it was taken from.
            if (level.isEmptyBlock(crop) && grown.canSurvive(level, crop)) {
                Restore.quiet(level, crop, grown);
            }
            return true;
        });
        return true;
    }

    /** Standing room next to a ripe crop near the player, not right beside the player. */
    @Override
    @Nullable BlockPos spot(ServerPlayer player) {
        Config.Section config = Config.event(ID);
        ServerLevel level = player.level();
        BlockPos crop = Nearby.random(player, ID, HarvestEvent::isRipe);
        if (crop == null) {
            return null;
        }
        double min = config.num("min_distance");
        int reach = config.integer("reach");
        for (BlockPos pos : BlockPos.betweenClosed(crop.offset(-reach, -1, -reach), crop.offset(reach, 1, reach))) {
            if (level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above()) && level.getBlockState(pos.below()).isSolidRender()
                && player.distanceToSqr(Vec3.atBottomCenterOf(pos)) >= min * min) {
                return pos.immutable();
            }
        }
        return null;
    }

    private static List<BlockPos> ripe(ServerLevel level, BlockPos around, int reach) {
        return Nearby.all(level, around, reach, HarvestEvent::isRipe);
    }

    private static boolean isRipe(BlockState state) {
        return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
    }
}
