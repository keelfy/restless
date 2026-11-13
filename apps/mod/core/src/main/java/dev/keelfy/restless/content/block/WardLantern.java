package dev.keelfy.restless.content.block;

import dev.keelfy.restless.content.item.Relics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The hallowed lantern: no spirit acts near it. The hollow lantern is one of a wider reach that also keeps notes. It is
 * turned to face the way the player who places it looks.
 */
public final class WardLantern extends LanternBlock implements WardBlock {
    private static final VoxelShape HALLOWED = Shapes.or(
            Block.box(5, 0, 5, 11, 7.5, 11), Block.box(6, 7.5, 6.5, 10, 8, 9.5), Block.box(6, 8, 7.5, 10, 10, 8.5));
    private static final VoxelShape HALLOWED_HANGING = Shapes.or(
            Block.box(5, 4, 5, 11, 11.5, 11),
            Block.box(6, 11.5, 6.5, 10, 12, 9.5),
            Block.box(6, 12, 7.5, 10, 14, 8.5),
            Block.box(7.5, 14, 7.5, 8.5, 16, 8.5));
    private static final VoxelShape HOLLOW = Shapes.or(Block.box(5, 0, 5, 11, 10, 11));
    private static final VoxelShape HOLLOW_HANGING =
            Shapes.or(Block.box(5, 4, 5, 11, 14, 11), Block.box(7.5, 14, 7.5, 8.5, 16, 8.5));

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final Kind kind;

    public WardLantern(BlockBehaviour.Properties properties) {
        this(properties, Kind.LANTERN);
    }

    public WardLantern(BlockBehaviour.Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    /** A hollow lantern is refused where its chunk already holds one. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (kind == Kind.HOLLOW && Relics.hollowLanternIn(context.getLevel(), context.getClickedPos())) {
            return null;
        }
        BlockState state = super.getStateForPlacement(context);
        return state == null
                ? null
                : state.setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean hanging = state.getValue(HANGING);
        if (kind == Kind.HOLLOW) {
            return hanging ? HOLLOW_HANGING : HOLLOW;
        }
        return hanging ? HALLOWED_HANGING : HALLOWED;
    }

    @Override
    public Kind kind() {
        return kind;
    }
}
