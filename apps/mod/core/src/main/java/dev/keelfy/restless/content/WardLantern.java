package dev.keelfy.restless.content;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** The hallowed lantern: no spirit acts near it. */
public final class WardLantern extends ShapedBlock implements WardBlock {
    public WardLantern(BlockBehaviour.Properties properties) {
        super(properties, Block.box(5, 0, 5, 11, 10, 11));
    }

    @Override
    public Kind kind() {
        return Kind.LANTERN;
    }
}
