package dev.keelfy.restless.content.block;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A small thing that stands on the floor and turns to face somewhere: a doll, a music box, a rocking chair. Put there
 * by the haunt events, never crafted: no item, it drops nothing.
 */
public class Prop extends ShapedBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final VoxelShape DOLL = Shapes.or(
            Block.box(3, 4, 7, 13, 8, 9),
            Block.box(3, 8, 7, 5, 9, 9),
            Block.box(5, 0, 6, 11, 4, 10),
            Block.box(5, 4, 6, 11, 8, 7),
            Block.box(5, 4, 9, 11, 8, 10),
            Block.box(6, 8, 6, 10, 12, 10),
            Block.box(11, 8, 7, 13, 9, 9));
    public static final VoxelShape MUSIC_BOX = Shapes.or(Block.box(3, 0, 4, 13, 7, 12));
    public static final VoxelShape MUSIC_BOX_OPEN =
            Shapes.or(Block.box(3, 0, 4, 13, 5, 12), Block.box(3, 5, 11, 13, 13, 12), Block.box(7, 5, 7, 9, 8, 9));
    public static final VoxelShape CHAIR = Shapes.or(
            Block.box(2, 0, 1, 4, 2, 15),
            Block.box(3, 2, 4, 4, 8, 6),
            Block.box(3, 2, 10, 4, 8, 12),
            Block.box(3, 6, 3, 13, 8, 4),
            Block.box(3, 6, 6, 13, 8, 10),
            Block.box(3, 6, 12, 13, 16, 13),
            Block.box(3, 8, 11, 13, 16, 12),
            Block.box(4, 6, 4, 13, 8, 6),
            Block.box(4, 6, 10, 13, 8, 12),
            Block.box(12, 0, 1, 14, 2, 15),
            Block.box(12, 2, 4, 13, 6, 6),
            Block.box(12, 2, 10, 13, 6, 12));

    private final Map<Direction, VoxelShape> shapes;

    /** The shape is drawn facing north, as the model is. */
    public Prop(BlockBehaviour.Properties properties, VoxelShape shape) {
        super(properties.instabreak().noOcclusion(), shape);
        shapes = turned(shape);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state.getValue(FACING));
    }

    /** A music box, shut or open. */
    public static final class MusicBox extends Prop {
        public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

        private static final Map<Direction, VoxelShape> OPEN_SHAPES = turned(MUSIC_BOX_OPEN);

        public MusicBox(BlockBehaviour.Properties properties, VoxelShape shape) {
            super(properties, shape);
            registerDefaultState(defaultBlockState().setValue(OPEN, false));
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getValue(OPEN)
                    ? OPEN_SHAPES.get(state.getValue(FACING))
                    : super.getShape(state, level, pos, context);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(OPEN);
        }
    }

    /** A rocking chair: 0 is level, 1 leans forward, 2 leans back. */
    public static final class Chair extends Prop {
        public static final IntegerProperty TILT = IntegerProperty.create("tilt", 0, 2);

        public Chair(BlockBehaviour.Properties properties, VoxelShape shape) {
            super(properties, shape);
            registerDefaultState(defaultBlockState().setValue(TILT, 0));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(TILT);
        }
    }
}
