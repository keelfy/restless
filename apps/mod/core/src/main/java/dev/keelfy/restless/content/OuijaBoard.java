package dev.keelfy.restless.content;

import dev.keelfy.restless.haunt.Divination;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A flat board; using it lets the planchette spell. */
public final class OuijaBoard extends CarpetBlock {
    public OuijaBoard(BlockBehaviour.Properties properties) {
        super(properties.strength(0.5f).sound(SoundType.WOOD).noOcclusion());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server) {
            Divination.spell(server);
        }
        return InteractionResult.SUCCESS;
    }
}
