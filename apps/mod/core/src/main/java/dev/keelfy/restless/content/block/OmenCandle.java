package dev.keelfy.restless.content.block;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Scan;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The banshee's candle. Lit with fire, it burns until a harmful event aimed at a player near it comes: it wails to
 * announce it first, see {@link #warn}, and goes out when the event lands. Fire lights it again. It counts as a candle,
 * so the lights events snuff it too. It is turned to face the way the player who places it looks.
 */
public final class OmenCandle extends AbstractCandleBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final Map<Direction, VoxelShape> SHAPES = ShapedBlock.turned(Shapes.or(
            Block.box(5, 0, 5, 11, 1, 11),
            Block.box(5.5, 1, 5.5, 10.5, 1.5, 10.5),
            Block.box(6, 1.5, 6, 10, 2, 10),
            Block.box(6, 3.5, 7.5, 9.5, 5.5, 8.5),
            Block.box(6.25, 3, 6.25, 9.75, 3.5, 9.75),
            Block.box(6.5, 2, 6.5, 9.5, 3, 9.5),
            Block.box(6.5, 3.5, 6.5, 9.5, 5.5, 7.5),
            Block.box(6.5, 3.5, 8.5, 9.5, 5.5, 9.5),
            Block.box(7, 5.5, 7, 9, 7.5, 9),
            Block.box(7.5, 4.5, 9.5, 8.5, 5.5, 10),
            Block.box(7.75, 7.5, 7.75, 8.25, 8.5, 8.25),
            Block.box(9.5, 4, 7, 10, 5.5, 8)));

    public OmenCandle(BlockBehaviour.Properties properties) {
        super(properties.lightLevel(state -> state.getValue(LIT) ? 3 : 0));
        registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected Iterable<Vec3> getParticleOffsets(BlockState state) {
        return List.of(new Vec3(0.5, 0.6, 0.5));
    }

    /** Only fire in a hand lights it; a burning arrow does not. */
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
            server.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
            if (flint) {
                stack.hurtAndBreak(1, player, hand);
            } else {
                stack.consume(1, player);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** The event it announced has landed: it goes out, to be lit again. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, false), 3);
            level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 0.6f);
            level.sendParticles(
                    ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.1, 0.2, 0.1, 0.02);
        }
    }

    /**
     * Call before a harmful event runs on a player: whether a lit omen candle is near them. It wails, from where it
     * stands, for the warning that follows, and goes out once that is over. A candle already wailing is not used twice.
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
                    && !level.getBlockTicks().hasScheduledTick(pos, state.getBlock())
                    && pos.distSqr(at) <= (double) radius * radius) {
                found[0] = pos.immutable();
            }
        });
        if (found[0] == null) {
            return false;
        }
        level.scheduleTick(found[0], level.getBlockState(found[0]).getBlock(), config.integer("omen_warn_ticks"));
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
