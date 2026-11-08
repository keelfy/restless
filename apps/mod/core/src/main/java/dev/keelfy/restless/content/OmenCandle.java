package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Scan;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The banshee's candle. Lit with fire, it burns for {@code consumables.omen_ticks} and is gone; while it burns, a
 * harmful event aimed at a player near it is announced by its wail first, see {@link #warn}. It counts as a candle, so
 * the lights events snuff it and fire relights it.
 */
public final class OmenCandle extends AbstractCandleBlock {
    private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 7, 10);

    public OmenCandle(BlockBehaviour.Properties properties) {
        super(properties.lightLevel(state -> state.getValue(LIT) ? 3 : 0));
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected Iterable<Vec3> getParticleOffsets(BlockState state) {
        return List.of(new Vec3(0.5, 0.55, 0.5));
    }

    /** Only fire in a hand lights it; a burning arrow does not, for the day it burns is counted from its lighting. */
    @Override
    protected boolean canBeLit(BlockState state) {
        return false;
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        boolean flint = stack.is(Items.FLINT_AND_STEEL);
        if (state.getValue(LIT) || (!flint && !stack.is(Items.FIRE_CHARGE))) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            server.setBlock(pos, state.setValue(LIT, true), 3);
            server.scheduleTick(pos, this, Config.section("consumables").integer("omen_ticks"));
            server.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
            if (flint) {
                stack.hurtAndBreak(1, player, hand);
            } else {
                stack.consume(1, player);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** A day has burned down: it goes out for good. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            level.removeBlock(pos, false);
            level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 0.6f);
            level.sendParticles(
                    ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.1, 0.2, 0.1, 0.02);
        }
    }

    /**
     * Call before a harmful event runs on a player: whether a lit omen candle is near them. It wails, from where it
     * stands, for the warning that follows.
     */
    public static boolean warn(ServerPlayer player) {
        ServerLevel level = player.level();
        Config.Section config = Config.section("consumables");
        int radius = config.integer("omen_radius");
        BlockPos at = player.blockPosition();
        BlockPos[] found = {null};
        Scan.each(level, at, radius, (pos, state) -> {
            if (found[0] == null
                    && state.getBlock() instanceof OmenCandle
                    && state.getValue(LIT)
                    && pos.distSqr(at) <= (double) radius * radius) {
                found[0] = pos.immutable();
            }
        });
        if (found[0] == null) {
            return false;
        }
        level.playSound(null, found[0], SoundEvents.GHAST_SCREAM, SoundSource.BLOCKS, 1.5f, 0.7f);
        level.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                found[0].getX() + 0.5,
                found[0].getY() + 0.7,
                found[0].getZ() + 0.5,
                12,
                0.1,
                0.3,
                0.1,
                0.05);
        return true;
    }
}
