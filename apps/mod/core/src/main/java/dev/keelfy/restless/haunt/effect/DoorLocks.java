package dev.keelfy.restless.haunt.effect;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.state.Session;
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
    private static final Map<GlobalPos, Lock> ON_SERVER = Session.SERVER.keep(new ConcurrentHashMap<>());
    private static final Map<GlobalPos, Lock> ON_CLIENT = Session.CLIENT.keep(new ConcurrentHashMap<>());

    private DoorLocks() {}

    /** Locks the door at {@code pos} (either half) and tells the clients in range. */
    public static void lock(ServerLevel level, BlockPos pos, int ticks) {
        BlockPos lower = lower(level.getBlockState(pos), pos);
        Config.Section config = Config.event("locked_door");
        Config.Tone denied = config.tone("denied_sound");
        put(level, lower, ticks, denied);
        double range = config.num("sync_range");
        level.getServer()
                .getPlayerList()
                .broadcast(
                        null,
                        lower.getX(),
                        lower.getY(),
                        lower.getZ(),
                        range,
                        level.dimension(),
                        new ClientboundCustomPayloadPacket(
                                new DoorLockPayload(lower, ticks, denied.volume(), denied.pitch())));
    }

    /** Records a lock; a client calls this when the server's message arrives. */
    public static void put(Level level, BlockPos lower, int ticks, Config.Tone denied) {
        Map<GlobalPos, Lock> locked = on(level);
        long now = level.getGameTime();
        locked.values().removeIf(lock -> lock.until <= now);
        locked.put(GlobalPos.of(level.dimension(), lower), new Lock(now + ticks, denied));
    }

    public static boolean isLocked(Level level, BlockPos pos) {
        return denial(level, pos) != null;
    }

    /** The sound a locked door at {@code pos} makes when refused, or {@code null} if it is not locked. */
    public static Config.Tone denial(Level level, BlockPos pos) {
        Map<GlobalPos, Lock> locked = on(level);
        if (locked.isEmpty()) {
            return null;
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof DoorBlock)) {
            return null;
        }
        Lock lock = locked.get(GlobalPos.of(level.dimension(), lower(state, pos)));
        return lock != null && lock.until > level.getGameTime() ? lock.denied : null;
    }

    private static Map<GlobalPos, Lock> on(Level level) {
        return level.isClientSide() ? ON_CLIENT : ON_SERVER;
    }

    private record Lock(long until, Config.Tone denied) {}

    private static BlockPos lower(BlockState state, BlockPos pos) {
        return state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }
}
