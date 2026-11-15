package dev.keelfy.restless.haunt;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.state.HauntData;
import dev.keelfy.restless.state.Session;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Puts back what a haunt event changed. Entries live in memory: they run when due, and all at once when the server
 * stops, so a clean shutdown never leaves a changed world behind. Block swaps are also saved in {@link HauntData}, so
 * after a crash they are put back on the next start; other cleanup (figures, doubles) is lost in a crash.
 */
public final class Restore {
    /** Undoes one change. */
    @FunctionalInterface
    public interface Undo {
        /**
         * @param force true at shutdown: load whatever is needed and finish now @return false to try again in a second
         */
        boolean run(boolean force);
    }

    private static final class Entry {
        final Undo undo;
        final HauntContext context = HauntContext.capture();
        /** The saved record of a block undo; null for one that lives in memory only. */
        HauntData.@Nullable Undo saved;

        long due;

        Entry(Undo undo, long due) {
            this.undo = undo;
            this.due = due;
        }

        boolean run(boolean force) {
            boolean[] done = {false};
            context.during(() -> done[0] = undo.run(force));
            return done[0];
        }
    }

    private static final List<Entry> PENDING = Session.SERVER.keep(new ArrayList<>());
    /** One element once the saved undos have been picked up for this server session. */
    private static final List<Boolean> RESUMED = Session.SERVER.keep(new ArrayList<>());

    private Restore() {}

    /**
     * Runs {@code undo} after {@code delay} ticks, or at shutdown. For cleanup of things an event made (a figure, a
     * double).
     */
    public static void after(ServerLevel level, int delay, Undo undo) {
        PENDING.add(new Entry(undo, level.getServer().getTickCount() + delay));
    }

    /**
     * Like {@link #after}, but for undoing a change to the world: under {@linkplain Hardcore hardcore} the change stays
     * and nothing is registered.
     */
    public static void revert(ServerLevel level, int delay, Undo undo) {
        if (!Hardcore.on(level)) {
            after(level, delay, undo);
        }
    }

    /** Changes a block without neighbour updates, so nothing attached to it pops off; the client still sees it. */
    public static void quiet(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    /**
     * Swaps a block now and swaps the original back after {@code delay} ticks, unless something else changed it
     * meanwhile.
     */
    public static void swap(ServerLevel level, BlockPos pos, BlockState to, int delay) {
        if (Hardcore.on(level)) {
            quiet(level, pos, to);
            return;
        }
        BlockPos at = pos.immutable();
        BlockState from = level.getBlockState(at);
        quiet(level, at, to);
        MinecraftServer server = level.getServer();
        HauntData data = HauntData.get(server);
        var undo = new HauntData.Undo(
                GlobalPos.of(level.dimension(), at),
                from,
                to,
                server.overworld().getGameTime() + delay);
        data.addUndo(undo);
        schedule(server, data, undo, delay);
    }

    /** Queues the undo of a saved block change; it leaves the saved list when it has run. */
    private static void schedule(MinecraftServer server, HauntData data, HauntData.Undo undo, long delay) {
        Entry entry = new Entry(
                force -> {
                    ServerLevel level = server.getLevel(undo.at().dimension());
                    BlockPos at = undo.at().pos();
                    if (level != null) {
                        if (!force && !level.isLoaded(at)) {
                            return false;
                        }
                        if (level.getBlockState(at) == undo.to()) {
                            quiet(level, at, undo.from());
                        }
                    }
                    data.removeUndo(undo);
                    return true;
                },
                server.getTickCount() + delay);
        entry.saved = undo;
        PENDING.add(entry);
    }

    /**
     * Picks up the block undos saved by an earlier session (once per session): a due one runs on the next tick, the
     * rest wait out what is left. Saved as game time, since the tick count starts again at every start.
     */
    private static void resume(MinecraftServer server) {
        if (RESUMED.isEmpty()) {
            RESUMED.add(true);
            resume(server, HauntData.get(server));
        }
    }

    /** Queues every saved undo of {@code data} that no live entry already holds. */
    public static void resume(MinecraftServer server, HauntData data) {
        long now = server.overworld().getGameTime();
        for (HauntData.Undo undo : data.undos()) {
            if (PENDING.stream().noneMatch(e -> e.saved == undo)) {
                schedule(server, data, undo, Math.max(0L, undo.due() - now));
            }
        }
    }

    /** For tests: loses the pending entry of {@code undo} as a crash would, without undoing it. */
    public static void lose(HauntData.Undo undo) {
        PENDING.removeIf(e -> e.saved == undo);
    }

    /** Call once per server tick. */
    public static void tick(MinecraftServer server) {
        resume(server);
        if (PENDING.isEmpty()) {
            return;
        }
        long now = server.getTickCount();
        for (Entry entry : List.copyOf(PENDING)) {
            if (now >= entry.due) {
                if (undo(entry)) {
                    PENDING.remove(entry);
                } else {
                    entry.due = now + Config.section("restore").integer("retry_ticks");
                }
            }
        }
    }

    /** @return whether the entry is finished; one that throws is logged and dropped, never retried */
    private static boolean undo(Entry entry) {
        try {
            return entry.run(false);
        } catch (RuntimeException e) {
            Restless.LOGGER.error("A haunt change could not be undone and was dropped", e);
            return true;
        }
    }

    /** Undoes everything that is still pending; call when the server is stopping. */
    public static void flush() {
        for (Entry entry : List.copyOf(PENDING)) {
            try {
                entry.run(true);
            } catch (RuntimeException e) {
                Restless.LOGGER.warn("A haunt change could not be undone at shutdown", e);
            }
        }
        PENDING.clear();
    }
}
