package dev.keelfy.restless.content;

import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

/** A pinch of salt on the floor: flat, walkable, falls apart when its floor goes. Spirits do not cross it; see {@code Wards}. */
public final class SaltLineBlock extends CarpetBlock {
    public SaltLineBlock(BlockBehaviour.Properties properties) {
        super(properties.noCollision().instabreak().sound(SoundType.SAND).pushReaction(PushReaction.POPPED));
    }
}
