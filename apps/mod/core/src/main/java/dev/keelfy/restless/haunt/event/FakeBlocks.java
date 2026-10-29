package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.haunt.Scheduler;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Blocks only one player sees: their client is told of other blocks, the server keeps its own, and the client is told
 * the truth again when it is due. A click on one shows the real block, as does the chunk being sent again.
 */
final class FakeBlocks {
    private FakeBlocks() {}

    /** Shows {@code shown} to this player alone for {@code ticks}, then the real blocks again. */
    static void show(ServerPlayer player, Map<BlockPos, BlockState> shown, int ticks) {
        shown.forEach((pos, state) -> player.connection.send(new ClientboundBlockUpdatePacket(pos, state)));
        ServerLevel level = player.level();
        List<BlockPos> where = List.copyOf(shown.keySet());
        // Elsewhere the client has dropped these chunks, and gets the real blocks when it is sent them again.
        Scheduler.later(player, ticks, p -> {
            if (p.level() == level) {
                where.forEach(pos -> p.connection.send(new ClientboundBlockUpdatePacket(level, pos)));
            }
        });
    }
}
