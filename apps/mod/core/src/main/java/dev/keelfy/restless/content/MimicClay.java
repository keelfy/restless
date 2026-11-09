package dev.keelfy.restless.content;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * The trickster's clay. Pressed against a block it takes that block's look; with a look, it sets down a
 * {@link MimicBlock} that wears it, turned the way the player sets it down. Sneaking presses it again, over the old
 * look.
 */
public final class MimicClay extends Item {
    public MimicClay(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack clay = context.getItemInHand();
        ServerPlayer player = context.getPlayer() instanceof ServerPlayer p ? p : null;
        Look held = clay.get(Content.LOOK.get());
        BlockState clicked = level.getBlockState(context.getClickedPos());
        if (held != null && clicked.getBlock() instanceof HoardersHollow) {
            return disguise(level, context.getClickedPos(), clicked, clay, held, player);
        }
        if (held != null && !(player != null && player.isShiftKeyDown())) {
            return place(level, context, clay, held, player);
        }
        BlockState seen = look(level, context.getClickedPos());
        if (seen == null) {
            if (player != null) {
                player.sendSystemMessage(Component.translatable("message.restless.clay_unseen"), true);
            }
            return InteractionResult.FAIL;
        }
        clay.set(Content.LOOK.get(), new Look(seen));
        level.playSound(null, context.getClickedPos(), SoundEvents.MUD_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
        if (player != null) {
            player.sendSystemMessage(
                    Component.translatable(
                            "message.restless.clay_pressed", seen.getBlock().getName()),
                    true);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /** A hoarder's hollow takes the look the clay holds. */
    private static InteractionResult disguise(
            ServerLevel level, BlockPos pos, BlockState state, ItemStack clay, Look look, ServerPlayer player) {
        level.setBlock(pos, state.setValue(HoardersHollow.DISGUISED, true), 3);
        if (level.getBlockEntity(pos) instanceof MimicBlockEntity entity) {
            entity.setLook(look.state());
        }
        level.playSound(null, pos, SoundEvents.MUD_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f);
        if (player != null) {
            clay.consume(1, player);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /** What a block shows as: its own state, or the look a mimic wears. Nothing that is drawn by more than a model. */
    private static BlockState look(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof MimicBlock && level.getBlockEntity(pos) instanceof MimicBlockEntity mimic) {
            return mimic.look().isAir() ? null : mimic.look();
        }
        return state.getRenderShape() == RenderShape.MODEL && !state.isAir() ? state : null;
    }

    private static InteractionResult place(
            ServerLevel level, UseOnContext context, ItemStack clay, Look look, ServerPlayer player) {
        BlockPlaceContext at = new BlockPlaceContext(context);
        BlockState mimic = Content.MIMIC_BLOCK.get().defaultBlockState();
        if (!at.canPlace() || !level.isUnobstructed(mimic, at.getClickedPos(), CollisionContext.empty())) {
            return InteractionResult.FAIL;
        }
        level.setBlock(at.getClickedPos(), mimic, 3);
        if (level.getBlockEntity(at.getClickedPos()) instanceof MimicBlockEntity entity) {
            // Turned as the block it copies would turn if set down just so: logs along the face, stairs to the player.
            BlockState turned = look.state().getBlock().getStateForPlacement(at);
            entity.setLook(turned != null ? turned : look.state());
        }
        level.playSound(null, at.getClickedPos(), SoundEvents.MUD_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f);
        if (player != null) {
            clay.consume(1, player);
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
