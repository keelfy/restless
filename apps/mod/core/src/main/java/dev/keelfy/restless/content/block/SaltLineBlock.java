package dev.keelfy.restless.content.block;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.ward.Enclosure;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;

/**
 * A pinch of salt on the floor: flat, walkable, falls apart when its floor goes. Spirits do not cross it; see
 * {@code Wards}. Every event a ring of it stops wears each tile one step; at {@code salt.wear_stops} the tiles turn
 * grey and stop nothing, until a fresh tile is laid next to them.
 */
public final class SaltLineBlock extends CarpetBlock {
    /** Events the tile's ring has stopped; the cap is the most {@code salt.wear_stops} may be. */
    public static final IntegerProperty STOPS = IntegerProperty.create("stops", 0, 15);

    public static final BooleanProperty WORN = BooleanProperty.create("worn");

    private static final Map<Direction, BooleanProperty> CONNECTIONS = Map.of(
            Direction.NORTH, BlockStateProperties.NORTH,
            Direction.EAST, BlockStateProperties.EAST,
            Direction.SOUTH, BlockStateProperties.SOUTH,
            Direction.WEST, BlockStateProperties.WEST);

    public SaltLineBlock(BlockBehaviour.Properties properties) {
        super(properties.noCollision().instabreak().sound(SoundType.SAND).pushReaction(PushReaction.POPPED));
        registerDefaultState(stateDefinition
                .any()
                .setValue(BlockStateProperties.NORTH, false)
                .setValue(BlockStateProperties.EAST, false)
                .setValue(BlockStateProperties.SOUTH, false)
                .setValue(BlockStateProperties.WEST, false)
                .setValue(STOPS, 0)
                .setValue(WORN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(
                BlockStateProperties.NORTH,
                BlockStateProperties.EAST,
                BlockStateProperties.SOUTH,
                BlockStateProperties.WEST,
                STOPS,
                WORN);
    }

    /** One event was stopped by the ring these tiles form: each tile wears a step, and the last step turns it grey. */
    public static void wear(Level level, Iterable<BlockPos> tiles) {
        int limit = Math.min(
                Config.section("salt").integer("wear_stops"),
                STOPS.getPossibleValues().size() - 1);
        for (BlockPos pos : tiles) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(Content.SALT_LINE.get()) || state.getValue(WORN)) {
                continue;
            }
            int stops = state.getValue(STOPS) + 1;
            level.setBlock(
                    pos, state.setValue(STOPS, Math.min(stops, 15)).setValue(WORN, Enclosure.wornOut(stops, limit)), 2);
        }
    }

    /** A fresh tile mends the salt it touches: every tile joined to it, corners included, is fresh again. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        // Wear and new connections change the state of a tile already there: only a newly laid tile mends.
        if (level.isClientSide() || state.getValue(WORN) || oldState.is(this)) {
            return;
        }
        int budget = Config.section("salt").integer("max_cells");
        Set<BlockPos> seen = new HashSet<>(Set.of(pos));
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(seen);
        while (!queue.isEmpty() && seen.size() <= budget) {
            BlockPos at = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos next = at.offset(dx, 0, dz);
                    BlockState tile = level.getBlockState(next);
                    if (tile.is(this) && seen.add(next)) {
                        queue.add(next);
                        if (tile.getValue(WORN) || tile.getValue(STOPS) > 0) {
                            level.setBlock(next, tile.setValue(WORN, false).setValue(STOPS, 0), 2);
                        }
                    }
                }
            }
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (var connection : CONNECTIONS.entrySet()) {
            state = state.setValue(
                    connection.getValue(),
                    context.getLevel()
                            .getBlockState(context.getClickedPos().relative(connection.getKey()))
                            .is(this));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction direction,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random) {
        BlockState updated =
                super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
        BooleanProperty connection = CONNECTIONS.get(direction);
        return updated.is(this) && connection != null ? updated.setValue(connection, neighbourState.is(this)) : updated;
    }
}
