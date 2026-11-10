package dev.keelfy.restless.content.block;

import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.item.consumable.Look;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A solid block that looks like another: the client draws what its {@link MimicBlockEntity} holds, not this block. It
 * is a full cube whatever it looks like, breaks as slowly as what it shows, and when broken gives the clay back, still
 * pressed to the same look.
 */
public class MimicBlock extends Block implements EntityBlock {
    public MimicBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MimicBlockEntity(pos, state);
    }

    /** The client draws it from the block entity. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MimicBlockEntity mimic
                && !mimic.look().isAir()) {
            return mimic.look().getDestroyProgress(player, level, pos);
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()
                && !player.isCreative()
                && level.getBlockEntity(pos) instanceof MimicBlockEntity mimic
                && !mimic.look().isAir()) {
            ItemStack clay = new ItemStack(Content.MIMIC_CLAY.get());
            clay.set(Content.LOOK.get(), new Look(mimic.look()));
            popResource(level, pos, clay);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
