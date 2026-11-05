package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Doors the haunting has locked, by the lower half's position, until a game time. In memory only: a restart frees them,
 * and so does leaving the world ({@link Session}). The server tells nearby clients too ({@link DoorLockPayload}); each
 * side keeps its own map, so a single-player game does not mix them up.
 */
public final class DoorLocks {
    private static final Map<GlobalPos, Long> ON_SERVER = Session.SERVER.keep(new ConcurrentHashMap<>());
    private static final Map<GlobalPos, Long> ON_CLIENT = Session.CLIENT.keep(new ConcurrentHashMap<>());

    private DoorLocks() {}

    /** Locks the door at {@code pos} (either half) and tells the clients in range. */
    public static void lock(ServerLevel level, BlockPos pos, int ticks) {
        BlockPos lower = lower(level.getBlockState(pos), pos);
        put(level, lower, ticks);
        double range = Config.event("locked_door").num("sync_range");
        level.getServer()
                .getPlayerList()
                .broadcast(
                        null,
                        lower.getX(),
                        lower.getY(),
                        lower.getZ(),
                        range,
                        level.dimension(),
                        new ClientboundCustomPayloadPacket(new DoorLockPayload(lower, ticks)));
    }

    /** Records a lock; a client calls this when the server's message arrives. */
    public static void put(Level level, BlockPos lower, int ticks) {
        Map<GlobalPos, Long> locked = on(level);
        long now = level.getGameTime();
        locked.values().removeIf(until -> until <= now);
        locked.put(GlobalPos.of(level.dimension(), lower), now + ticks);
    }

    public static boolean isLocked(Level level, BlockPos pos) {
        Map<GlobalPos, Long> locked = on(level);
        if (locked.isEmpty()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof DoorBlock)) {
            return false;
        }
        Long until = locked.get(GlobalPos.of(level.dimension(), lower(state, pos)));
        return until != null && until > level.getGameTime();
    }

    private static Map<GlobalPos, Long> on(Level level) {
        return level.isClientSide() ? ON_CLIENT : ON_SERVER;
    }

    private static BlockPos lower(BlockState state, BlockPos pos) {
        return state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }
}
