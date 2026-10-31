package dev.keelfy.restless.haunt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Delayed and repeating per-player tasks for multi-step events. Tasks live in memory only: they are dropped when the
 * player dies, leaves, or the world is closed, so a step must never leave the world in a state it cannot recover from.
 */
public final class Scheduler {
    /** One step of a repeating task. */
    @FunctionalInterface
    public interface Step {
        /** @param n 0-based step number @return true to run again after the interval */
        boolean run(ServerPlayer player, int n);
    }

    private static final class Task {
        final ServerPlayer player;
        final int interval;
        final Step step;
        long due;
        int n;
        boolean done;

        Task(ServerPlayer player, long due, int interval, Step step) {
            this.player = player;
            this.due = due;
            this.interval = interval;
            this.step = step;
        }
    }

    private static final List<Task> TASKS = new ArrayList<>();

    private Scheduler() {}

    /**
     * Runs {@code step} after {@code delay} ticks, then every {@code interval} ticks for as long as it returns true.
     */
    public static void repeat(ServerPlayer player, int delay, int interval, Step step) {
        TASKS.add(new Task(player, now(player) + delay, Math.max(1, interval), step));
    }

    public static void later(ServerPlayer player, int delay, Consumer<ServerPlayer> action) {
        repeat(player, delay, 1, (p, n) -> {
            action.accept(p);
            return false;
        });
    }

    /** Call once per server tick. */
    public static void tick(MinecraftServer server) {
        if (TASKS.isEmpty()) {
            return;
        }
        long now = server.getTickCount();
        // Steps may schedule more tasks, so walk a snapshot.
        for (Task task : List.copyOf(TASKS)) {
            if (task.player.isRemoved()
                    || !task.player.isAlive()
                    || task.player.level().getServer() != server) {
                task.done = true;
            } else if (now >= task.due) {
                task.done = !task.step.run(task.player, task.n++);
                task.due = now + task.interval;
            }
        }
        TASKS.removeIf(task -> task.done);
    }

    private static long now(ServerPlayer player) {
        return player.level().getServer().getTickCount();
    }
}
