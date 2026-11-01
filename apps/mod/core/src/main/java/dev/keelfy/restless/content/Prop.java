package dev.keelfy.restless.content;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A small thing that stands on the floor and turns to face somewhere: a doll, a music box, a rocking chair. Put there
 * by the haunt events, never crafted: no item, it drops nothing.
 */
public class Prop extends ShapedBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public Prop(BlockBehaviour.Properties properties, VoxelShape shape) {
        super(properties.instabreak().noOcclusion(), shape);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** A music box, shut or open. */
    public static final class MusicBox extends Prop {
        public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

        public MusicBox(BlockBehaviour.Properties properties, VoxelShape shape) {
            super(properties, shape);
            registerDefaultState(defaultBlockState().setValue(OPEN, false));
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
