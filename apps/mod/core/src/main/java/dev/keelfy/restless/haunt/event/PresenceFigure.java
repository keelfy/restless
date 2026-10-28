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
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A figure that only stands there and watches. It appears off to one side, and is gone once the player looks straight
 * at it or comes close, with no sound and no trace. It cannot hurt anyone.
 */
final class PresenceFigure implements HauntEvent {
    private final String id;
    private final Predicate<ServerPlayer> place;
    private final Function<ServerPlayer, ResolvableProfile> skin;
    /** The figure each player has now; one per event at a time. */
    private final Map<UUID, Mannequin> active = new ConcurrentHashMap<>();

    private PresenceFigure(String id, Predicate<ServerPlayer> place, Function<ServerPlayer, ResolvableProfile> skin) {
        this.id = id;
        this.place = place;
        this.skin = skin;
    }

    /** 94: the player's own likeness at the edge of vision. */
    static HauntEvent edge() {
        return new PresenceFigure("edge_figure", player -> true, player -> ResolvableProfile.createResolved(player.getGameProfile()));
    }

    /** 95: a pale stranger far off in the fog, which is rain or darkness under open sky. */
    static HauntEvent fog() {
        return new PresenceFigure("fog_silhouette",
            player -> player.level().canSeeSky(player.blockPosition()) && (player.level().isRaining() || player.level().isDarkOutside()),
            player -> Mannequin.DEFAULT_PROFILE);
    }

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
        return Set.of(EventTag.PRESENCE);
    }

    @Override
    public boolean canRun(ServerPlayer player) {
        return !active.containsKey(player.getUUID()) && place.test(player) && spot(player, 1.0) != null;
    }

    @Override
    public void run(ServerPlayer player) {
        double side = player.getRandom().nextBoolean() ? 1.0 : -1.0;
        BlockPos spot = spot(player, side);
        if (spot == null) {
            return;
        }
        Config.Section config = Config.event(id);
        Mannequin figure = Figure.spawn(player, spot, skin.apply(player));
        if (figure == null) {
            return;
        }
        UUID owner = player.getUUID();
        active.put(owner, figure);
        // The watch ends itself; this catches the cases where the scheduler drops it (the player died or left).
        Restore.after(player.level(), config.integer("max_ticks") + config.integer("cleanup_ticks"), force -> {
            leave(figure, owner);
            return true;
        });
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            boolean over = !figure.isAlive() || figure.level() != p.level()
                || n * config.integer("interval_ticks") >= config.integer("max_ticks") || noticed(p, figure, config);
            if (over) {
                leave(figure, owner);
            } else {
                Figure.face(figure, p);
            }
            return !over;
        });
    }

    private @Nullable BlockPos spot(ServerPlayer player, double side) {
        Config.Section config = Config.event(id);
        double angle = config.num("min_angle") + player.getRandom().nextDouble() * (config.num("max_angle") - config.num("min_angle"));
        return Place.standingAt(player, config.num("distance"), side * angle);
    }

    /** The player looks straight at the figure, or has come within the event's vanish distance. */
    private static boolean noticed(ServerPlayer player, Mannequin figure, Config.Section config) {
        double near = config.num("vanish_distance");
        if (near > 0 && player.distanceToSqr(figure) <= near * near) {
            return true;
        }
        double degrees = config.num("vanish_degrees");
        Vec3 toFigure = figure.getEyePosition().subtract(player.getEyePosition()).normalize();
        return degrees > 0 && player.getLookAngle().dot(toFigure) >= Math.cos(Math.toRadians(degrees)) && player.hasLineOfSight(figure);
    }

    private void leave(Mannequin figure, UUID owner) {
        figure.discard();
        active.remove(owner, figure);
    }
}
