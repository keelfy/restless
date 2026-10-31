package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntTier;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.Mannequin;
import org.jspecify.annotations.Nullable;

/**
 * Another player, in a default skin, doing something as players do, step by step. Gone when it is done, when the player
 * comes close, or when time runs out, with no sound and no trace but what it did.
 */
abstract class StrangerAct implements HauntEvent {
    private final String id;
    /** The figure each player has now; one per event at a time. */
    private final Map<UUID, Mannequin> active = new ConcurrentHashMap<>();

    StrangerAct(String id) {
        this.id = id;
    }

    /** Where the figure stands, or null if there is nowhere fitting. */
    abstract @Nullable BlockPos spot(ServerPlayer player);

    /** Dresses the figure before its first step. */
    void prepare(Mannequin figure) {}

    /** One step of the act. @return false when it is done */
    abstract boolean step(ServerPlayer player, Mannequin figure, Config.Section config, int n);

    @Override
    public Identifier id() {
        return Identifier.parse(id);
    }

    @Override
    public HauntTier tier() {
        return HauntTier.LIGHT;
    }

    @Override
    public Set<EventTag> tags() {
        return Set.of(EventTag.PRESENCE, EventTag.PLAYER);
    }

    @Override
    public boolean canRun(ServerPlayer player) {
        return !active.containsKey(player.getUUID()) && spot(player) != null;
    }

    @Override
    public void run(ServerPlayer player) {
        BlockPos spot = spot(player);
        if (spot == null) {
            return;
        }
        Config.Section config = Config.event(id);
        Mannequin figure = Figure.spawn(player, spot, Figure.stranger(player));
        if (figure == null) {
            return;
        }
        prepare(figure);
        UUID owner = player.getUUID();
        active.put(owner, figure);
        int interval = config.integer("interval_ticks");
        // The act ends itself; this catches the cases where the scheduler drops it (the player died or left).
        Restore.after(player.level(), config.integer("max_ticks") + config.integer("cleanup_ticks"), force -> {
            leave(figure, owner);
            return true;
        });
        Scheduler.repeat(player, interval, interval, (p, n) -> {
            double near = config.num("vanish_distance");
            boolean over = !figure.isAlive()
                    || figure.level() != p.level()
                    || n * interval >= config.integer("max_ticks")
                    || p.distanceToSqr(figure) <= near * near
                    || !step(p, figure, config, n);
            if (over) {
                leave(figure, owner);
            }
            return !over;
        });
    }

    private void leave(Mannequin figure, UUID owner) {
        figure.discard();
        active.remove(owner, figure);
    }
}
