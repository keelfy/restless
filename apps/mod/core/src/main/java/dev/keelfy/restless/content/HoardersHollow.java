package dev.keelfy.restless.content;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A mimic that is also a chest: it opens on the player's one store, the same in every hollow they have, kept in the
 * saved data. It shows its own face until clay is pressed to it, then looks like what the clay holds.
 */
public final class HoardersHollow extends MimicBlock {
    /** Whether clay has been pressed to it; its own face is drawn only while it has not. */
    public static final BooleanProperty DISGUISED = BooleanProperty.create("disguised");

    public HoardersHollow(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DISGUISED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DISGUISED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(DISGUISED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server) {
            server.openMenu(new SimpleMenuProvider(
                    (id, inventory, opener) -> ChestMenu.threeRows(id, inventory, Relics.hollow(server)),
                    Component.translatable("container.restless.hoarders_hollow")));
        }
        return InteractionResult.SUCCESS;
    }
}
