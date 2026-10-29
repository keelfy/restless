package dev.keelfy.restless.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A ward that wears out: every event it stops costs a use, and the last one breaks it. */
public final class WornWard extends ShapedBlock implements WardBlock {
    public static final int MAX_USES = 10;
    public static final IntegerProperty WEAR = IntegerProperty.create("wear", 0, MAX_USES - 1);

    private final Kind kind;
    private final int uses;

    public WornWard(BlockBehaviour.Properties properties, VoxelShape shape, Kind kind, int uses) {
        super(properties, shape);
        this.kind = kind;
        this.uses = uses;
        registerDefaultState(stateDefinition.any().setValue(WEAR, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(WEAR);
    }

    @Override
    public Kind kind() {
        return kind;
    }

    /** Takes one use; the ward is gone when none are left. */
    public void spend(ServerLevel level, BlockPos pos, BlockState state) {
        int worn = state.getValue(WEAR) + 1;
        if (worn >= uses) {
            level.destroyBlock(pos, false);
        } else {
            level.setBlock(pos, state.setValue(WEAR, worn), 3);
        }
    }
}
