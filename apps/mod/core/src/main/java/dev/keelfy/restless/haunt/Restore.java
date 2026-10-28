package dev.keelfy.restless.haunt;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.config.Config;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Puts back what a haunt event changed. Entries live in memory: they run when due, and all at once when the server
 * stops, so a clean shutdown never leaves a changed world behind. After a crash the changes stay.
 */
public final class Restore {
    /** Undoes one change. */
    @FunctionalInterface
    public interface Undo {
        /** @param force true at shutdown: load whatever is needed and finish now @return false to try again in a second */
        boolean run(boolean force);
    }

    private static final class Entry {
        final Undo undo;
        long due;

        Entry(Undo undo, long due) {
            this.undo = undo;
            this.due = due;
        }
    }

    private static final List<Entry> PENDING = new ArrayList<>();

    private Restore() {}

    public static void after(ServerLevel level, int delay, Undo undo) {
        PENDING.add(new Entry(undo, level.getServer().getTickCount() + delay));
    }

    /** Changes a block without neighbour updates, so nothing attached to it pops off; the client still sees it. */
    public static void quiet(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    /** Swaps a block now and swaps the original back after {@code delay} ticks, unless something else changed it meanwhile. */
    public static void swap(ServerLevel level, BlockPos pos, BlockState to, int delay) {
        BlockPos at = pos.immutable();
        BlockState from = level.getBlockState(at);
        quiet(level, at, to);
        after(level, delay, force -> {
            if (!force && !level.isLoaded(at)) {
                return false;
            }
            if (level.getBlockState(at) == to) {
                quiet(level, at, from);
            }
            return true;
        });
    }

    /** Call once per server tick. */
    public static void tick(MinecraftServer server) {
        if (PENDING.isEmpty()) {
            return;
        }
        long now = server.getTickCount();
        for (Entry entry : List.copyOf(PENDING)) {
            if (now >= entry.due) {
                if (entry.undo.run(false)) {
                    PENDING.remove(entry);
                } else {
                    entry.due = now + Config.section("restore").integer("retry_ticks");
                }
            }
        }
    }

    /** Undoes everything that is still pending; call when the server is stopping. */
    public static void flush() {
        for (Entry entry : List.copyOf(PENDING)) {
            try {
                entry.undo.run(true);
            } catch (RuntimeException e) {
                Restless.LOGGER.warn("A haunt change could not be undone at shutdown", e);
            }
        }
        PENDING.clear();
    }
}
