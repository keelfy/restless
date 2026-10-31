package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Hardcore;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Where every event that shows other blocks in place of the real ones goes through. By default it is an illusion only
 * the haunted player sees: their client is told of other blocks, the server keeps its own, and the client is told the
 * truth again when it is due (a click on one shows the real block, as does the chunk being sent again). With
 * {@code swaps.mode} set to {@code real} the blocks are changed in the world for everyone and put back through
 * {@link Restore#swap}; under {@linkplain Hardcore hardcore} they are changed for good. A block that holds something (a
 * chest, a furnace) is never changed for real, since its contents would have nowhere to go: it is only shown.
 */
public final class Swaps {
    private Swaps() {}

    /** Whether swaps are made in the world rather than shown: the config says so, or the world is hardcore. */
    static boolean real(ServerLevel level) {
        return real(Hardcore.on(level), Config.section("swaps").text("mode"));
    }

    static boolean real(boolean hardcore, String mode) {
        return hardcore
                || switch (mode) {
                    case "illusion" -> false;
                    case "real" -> true;
                    default -> throw new IllegalStateException("swaps.mode must be illusion or real");
                };
    }

    /**
     * The player starts to break the block at {@code pos}: their client is told the real block, so a shown one that is
     * not there is gone at the click instead of standing until its time is up.
     */
    public static void reveal(ServerPlayer player, BlockPos pos) {
        player.connection.send(new ClientboundBlockUpdatePacket(player.level(), pos));
    }

    /** One block shown as {@code to} for {@code ticks}. */
    static void show(ServerPlayer player, BlockPos pos, BlockState to, int ticks) {
        show(player, Map.of(pos, to), ticks);
    }

    /** Each of {@code at} shown as {@code to} for {@code ticks}. */
    static void show(ServerPlayer player, Iterable<BlockPos> at, BlockState to, int ticks) {
        Map<BlockPos, BlockState> shown = new LinkedHashMap<>();
        at.forEach(pos -> shown.put(pos, to));
        show(player, shown, ticks);
    }

    /** Each block shown as its state for {@code ticks}, then the real ones again. */
    static void show(ServerPlayer player, Map<BlockPos, BlockState> shown, int ticks) {
        ServerLevel level = player.level();
        if (!real(level)) {
            illusion(player, shown, ticks);
            return;
        }
        Map<BlockPos, BlockState> fake = new LinkedHashMap<>();
        shown.forEach((pos, state) -> {
            if (level.getBlockEntity(pos) == null) {
                Restore.swap(level, pos, state, ticks);
            } else {
                fake.put(pos, state);
            }
        });
        illusion(player, fake, ticks);
    }

    private static void illusion(ServerPlayer player, Map<BlockPos, BlockState> shown, int ticks) {
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
