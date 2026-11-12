package dev.keelfy.restless.content.block;

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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** A worn ward two blocks tall: the post below, the body and head above. Both halves share the wear. */
public final class Scarecrow extends WornWard {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    private static final VoxelShape LOWER = Block.box(4, 0, 4, 12, 16, 12);
    private static final VoxelShape UPPER = Block.box(0, 0, 5, 16, 16, 11);

    public Scarecrow(BlockBehaviour.Properties properties) {
        super(properties, LOWER, Kind.SCARECROW, 10);
        registerDefaultState(defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HALF);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        return pos.getY() < level.getMaxY() && level.getBlockState(pos.above()).canBeReplaced(context)
                ? defaultBlockState()
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
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER : UPPER;
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
