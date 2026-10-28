package dev.keelfy.restless.haunt;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Doors the haunting has locked, by the lower half's position, until a game time. In memory only: a restart frees them. */
public final class DoorLocks {
    private static final Map<GlobalPos, Long> LOCKED = new HashMap<>();

    private DoorLocks() {}

    public static void lock(Level level, BlockPos pos, int ticks) {
        long now = level.getGameTime();
        LOCKED.values().removeIf(until -> until <= now);
        LOCKED.put(GlobalPos.of(level.dimension(), lower(level.getBlockState(pos), pos)), now + ticks);
    }

    public static boolean isLocked(Level level, BlockPos pos) {
        if (LOCKED.isEmpty()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof DoorBlock)) {
            return false;
        }
        Long until = LOCKED.get(GlobalPos.of(level.dimension(), lower(state, pos)));
        return until != null && until > level.getGameTime();
    }

    private static BlockPos lower(BlockState state, BlockPos pos) {
        return state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }
}
