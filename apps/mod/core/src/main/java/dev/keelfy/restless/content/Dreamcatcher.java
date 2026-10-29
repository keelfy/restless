package dev.keelfy.restless.content;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Keeps spirits from a sleeper near it. */
public final class Dreamcatcher extends ShapedBlock implements WardBlock {
    public Dreamcatcher(BlockBehaviour.Properties properties) {
        super(properties, Block.box(3, 0, 7, 13, 12, 9));
    }

    @Override
    public Kind kind() {
        return Kind.DREAMCATCHER;
    }
}
