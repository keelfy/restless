package dev.keelfy.restless.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Exists only so the client can draw what lies on the {@link Altar}; the thing itself is the block's {@link Altar#HELD}
 * state.
 */
public final class AltarEntity extends BlockEntity {
    public AltarEntity(BlockPos pos, BlockState state) {
        super(Content.ALTAR_ENTITY.get(), pos, state);
    }
}
