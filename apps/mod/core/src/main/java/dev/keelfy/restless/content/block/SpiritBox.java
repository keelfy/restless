package dev.keelfy.restless.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A spirit radio whose controls face the player who places it. */
public final class SpiritBox extends ShapedBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public SpiritBox(BlockBehaviour.Properties properties) {
        super(properties.strength(1.0f).sound(SoundType.METAL).noOcclusion(), NORTH_SHAPE);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** The model's parts, facing north. */
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(2, 1, 5, 14, 9, 12),
            Block.box(3, 2, 4, 9, 8, 5),
            Block.box(10, 5, 4, 13, 8, 5),
            Block.box(2, 8.5, 4.5, 14, 9, 5),
            Block.box(2, 1, 4.5, 14, 1.5, 5),
            Block.box(10.5, 2, 4, 12.5, 4, 5),
            Block.box(2.5, 9, 5.5, 4.5, 9.5, 7.5),
            Block.box(3, 9.5, 6, 4, 10, 7),
            Block.box(5, 9, 8, 6, 11, 9),
            Block.box(10, 9, 8, 11, 11, 9),
            Block.box(5, 11, 8, 11, 12, 9),
            Block.box(11.5, 9, 9.5, 13, 10, 11),
            Block.box(12, 10, 10, 12.5, 14, 10.5),
            Block.box(11.5, 14, 9.5, 13, 14.5, 11),
            Block.box(3, 0, 6, 4, 1, 7),
            Block.box(3, 0, 10, 4, 1, 11),
            Block.box(12, 0, 6, 13, 1, 7),
            Block.box(12, 0, 10, 13, 1, 11));

    private static final VoxelShape EAST_SHAPE = turn(NORTH_SHAPE);
    private static final VoxelShape SOUTH_SHAPE = turn(EAST_SHAPE);
    private static final VoxelShape WEST_SHAPE = turn(SOUTH_SHAPE);

    private static VoxelShape turn(VoxelShape shape) {
        VoxelShape[] result = {Shapes.empty()};
        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) ->
                result[0] = Shapes.or(result[0], Shapes.box(1 - z2, y1, x1, 1 - z1, y2, x2)));
        return result[0];
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case EAST -> EAST_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
