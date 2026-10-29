package dev.keelfy.restless.content;

import dev.keelfy.restless.haunt.Graves;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A spirit's grave (see {@link Graves}): using it reads the stone, and the thing the spirit wants lays it to rest. */
public final class Gravestone extends ShapedBlock {
    public Gravestone(BlockBehaviour.Properties properties) {
        super(properties, Block.box(2, 0, 6, 14, 15, 10));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player instanceof ServerPlayer visitor && Graves.visit(visitor, pos, stack)) {
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer visitor) {
            Graves.visit(visitor, pos, ItemStack.EMPTY);
        }
        return InteractionResult.SUCCESS;
    }
}
