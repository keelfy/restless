package dev.keelfy.restless.content.block;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A worn ward two blocks tall: the post below, the body and head above. Both halves share the wear and the side the
 * face looks to, which is toward the player who places it.
 */
public final class Scarecrow extends WornWard {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape POST = Shapes.or(
            Block.box(4, 0, 5, 11, 2.5, 11),
            Block.box(7, 2.5, 7, 9, 5.5, 9),
            Block.box(5.5, 5.5, 6.25, 9, 6, 9),
            Block.box(5.5, 6, 6.25, 10.5, 7.5, 9.5),
            Block.box(4.4, 7.5, 5.4, 11.6, 16, 10.6));
    private static final Map<Direction, VoxelShape> LOWER = turned(POST);
    private static final Map<Direction, VoxelShape> UPPER = turned(Shapes.or(
            Block.box(2.1, 0, 5.4, 13.9, 6, 10.5),
            Block.box(0, 6, 5.5, 16, 7.5, 10.5),
            Block.box(2.1, 7.5, 6.25, 13.9, 8, 9.75),
            Block.box(4.5, 8, 4.5, 15.25, 10.5, 11.5),
            Block.box(4.5, 10.5, 4.5, 11.5, 14, 11.5),
            Block.box(3.5, 14, 3.5, 12.5, 14.5, 12.5),
            Block.box(4.9, 14.5, 4.9, 11.1, 15.75, 11.1)));

    public Scarecrow(BlockBehaviour.Properties properties) {
        super(properties, POST, Kind.SCARECROW, 10);
        registerDefaultState(
                defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HALF, FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        return pos.getY() < level.getMaxY() && level.getBlockState(pos.above()).canBeReplaced(context)
                ? defaultBlockState()
                        .setValue(FACING, context.getHorizontalDirection().getOpposite())
                : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        level.setBlockAndUpdate(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER));
    }

    /** A half whose partner is gone goes too. */
    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction direction,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random) {
        DoubleBlockHalf half = state.getValue(HALF);
        Direction partner = half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN;
        if (direction == partner && (!neighbourState.is(this) || neighbourState.getValue(HALF) == half)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER : UPPER).get(state.getValue(FACING));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** Wears both halves together; the last use removes both. */
    @Override
    public void spend(ServerLevel level, BlockPos pos, BlockState state) {
        BlockPos base = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        BlockState lower = level.getBlockState(base);
        BlockState upper = level.getBlockState(base.above());
        int worn = state.getValue(WEAR) + 1;
        if (worn >= MAX_USES) {
            level.destroyBlock(base.above(), false);
            level.destroyBlock(base, false);
        } else if (lower.is(this) && upper.is(this)) {
            level.setBlock(base, lower.setValue(WEAR, worn), 3);
            level.setBlock(base.above(), upper.setValue(WEAR, worn), 3);
        }
    }
}
