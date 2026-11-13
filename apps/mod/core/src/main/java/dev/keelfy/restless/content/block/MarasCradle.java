package dev.keelfy.restless.content.block;

import dev.keelfy.restless.content.item.Relics;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A bed that gives sanity back: whoever sleeps in it wakes with all of theirs, the night does not pass for it, and its
 * spawn point holds once after the cradle is broken, see {@link Relics}.
 */
public final class MarasCradle extends BedBlock {
    /** Each half as its model draws it, facing north: the head with the headboard, the foot with the footboard. */
    private static final Map<Direction, VoxelShape> HEAD = ShapedBlock.turned(Shapes.or(
            Block.box(1, 0, 0, 3, 9, 2),
            Block.box(1, 3, 2, 15, 7, 16),
            Block.box(1.25, 8.6, 10, 14.75, 9.5, 12),
            Block.box(1.25, 9, 12, 14.75, 9.5, 16),
            Block.box(1.5, 7, 2, 14.5, 8.6, 16),
            Block.box(1.5, 8.6, 2, 14.5, 9, 10),
            Block.box(1.5, 8.6, 12, 14.5, 9, 16),
            Block.box(3, 3, 0, 15, 9, 2),
            Block.box(3, 9, 0, 13, 12, 2),
            Block.box(3.5, 9, 2.5, 12.5, 11, 8.5),
            Block.box(4, 9, 2, 12, 11, 2.1),
            Block.box(5, 12, 0, 11, 13.5, 2),
            Block.box(6, 9, 2.1, 10, 11, 2.15),
            Block.box(7, 13.5, 0, 9, 14, 2),
            Block.box(13, 0, 0, 15, 3, 2)));

    private static final Map<Direction, VoxelShape> FOOT = ShapedBlock.turned(Shapes.or(
            Block.box(1, 0, 14, 3, 12.5, 16),
            Block.box(1, 3, 0, 15, 7, 14),
            Block.box(1.25, 9, 0, 14.75, 9.5, 14),
            Block.box(1.5, 7, 0, 14.5, 9, 14),
            Block.box(3, 3, 14, 15, 10.5, 16),
            Block.box(13, 0, 14, 15, 3, 16),
            Block.box(13, 10.5, 14, 15, 12.5, 16)));

    public MarasCradle(BlockBehaviour.Properties properties) {
        super(DyeColor.PURPLE, properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(PART) == BedPart.HEAD ? HEAD : FOOT).get(state.getValue(FACING));
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
