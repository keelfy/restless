package dev.keelfy.restless.content.block;

import dev.keelfy.restless.content.item.Relics;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A bed that gives sanity back: whoever sleeps in it wakes with all of theirs, the night does not pass for it, and its
 * spawn point holds once after the cradle is broken, see {@link Relics}.
 */
public final class MarasCradle extends BedBlock {
    public MarasCradle(BlockBehaviour.Properties properties) {
        super(DyeColor.PURPLE, properties);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        InteractionResult result = super.useWithoutItem(state, level, pos, player, hit);
        if (player instanceof ServerPlayer sleeper && sleeper.isSleeping()) {
            sleeper.getSleepingPos().ifPresent(head -> Relics.slept(sleeper, head));
        }
        return result;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        if (state.getValue(PART) == BedPart.HEAD) {
            Relics.cradleBroken(level, pos);
        }
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
    }
}
