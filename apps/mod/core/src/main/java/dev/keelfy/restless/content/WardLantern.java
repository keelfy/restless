package dev.keelfy.restless.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The hallowed lantern: no spirit acts near it. */
public final class WardLantern extends LanternBlock implements WardBlock {
    private static final VoxelShape STANDING = Block.box(5, 0, 5, 11, 10, 11);
    private static final VoxelShape HANGING_SHAPE = Shapes.or(
        Block.box(5, 4, 5, 11, 14, 11), Block.box(7.5, 14, 7.5, 8.5, 16, 8.5));

    public WardLantern(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HANGING) ? HANGING_SHAPE : STANDING;
    }

    @Override
    public Kind kind() {
        return Kind.LANTERN;
    }
}
