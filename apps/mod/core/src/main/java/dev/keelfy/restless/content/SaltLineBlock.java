package dev.keelfy.restless.content;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;

/** A pinch of salt on the floor: flat, walkable, falls apart when its floor goes. Spirits do not cross it; see {@code Wards}. */
public final class SaltLineBlock extends CarpetBlock {
    private static final Map<Direction, BooleanProperty> CONNECTIONS = Map.of(
        Direction.NORTH, BlockStateProperties.NORTH,
        Direction.EAST, BlockStateProperties.EAST,
        Direction.SOUTH, BlockStateProperties.SOUTH,
        Direction.WEST, BlockStateProperties.WEST);

    public SaltLineBlock(BlockBehaviour.Properties properties) {
        super(properties.noCollision().instabreak().sound(SoundType.SAND).pushReaction(PushReaction.POPPED));
        registerDefaultState(stateDefinition.any()
            .setValue(BlockStateProperties.NORTH, false)
            .setValue(BlockStateProperties.EAST, false)
            .setValue(BlockStateProperties.SOUTH, false)
            .setValue(BlockStateProperties.WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.NORTH, BlockStateProperties.EAST,
            BlockStateProperties.SOUTH, BlockStateProperties.WEST);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (var connection : CONNECTIONS.entrySet()) {
            state = state.setValue(connection.getValue(), context.getLevel()
                .getBlockState(context.getClickedPos().relative(connection.getKey())).is(this));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                     BlockPos pos, Direction direction, BlockPos neighbourPos,
                                     BlockState neighbourState, RandomSource random) {
        BlockState updated = super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
        BooleanProperty connection = CONNECTIONS.get(direction);
        return updated.is(this) && connection != null
            ? updated.setValue(connection, neighbourState.is(this)) : updated;
    }
}
