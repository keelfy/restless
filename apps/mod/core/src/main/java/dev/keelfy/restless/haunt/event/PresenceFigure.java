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
import net.minecraft.util.Mth;
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
    private final Set<EventTag> tags;
    private final Predicate<ServerPlayer> place;
    private final Function<ServerPlayer, ResolvableProfile> skin;
    /** Walks across the player's view instead of standing and watching. */
    private final boolean walks;
    /** The figure each player has now; one per event at a time. */
    private final Map<UUID, Mannequin> active = new ConcurrentHashMap<>();

    private PresenceFigure(String id, Set<EventTag> tags, Predicate<ServerPlayer> place, Function<ServerPlayer, ResolvableProfile> skin, boolean walks) {
        this.id = id;
        this.tags = tags;
        this.place = place;
        this.skin = skin;
        this.walks = walks;
    }

    /** 94: the player's own likeness at the edge of vision. */
    static HauntEvent edge() {
        return new PresenceFigure("edge_figure", Set.of(EventTag.PRESENCE), player -> true, player -> ResolvableProfile.createResolved(player.getGameProfile()), false);
    }

    /** 95: a pale stranger far off in the fog, which is rain or darkness under open sky. */
    static HauntEvent fog() {
        return new PresenceFigure("fog_silhouette", Set.of(EventTag.PRESENCE),
            player -> player.level().canSeeSky(player.blockPosition()) && (player.level().isRaining() || player.level().isDarkOutside()),
            Figure::stranger, false);
    }

    /** 123: another player, in a default skin, far off and turned toward the player until approached. */
    static HauntEvent distantPlayer() {
        return new PresenceFigure("distant_player", Set.of(EventTag.PRESENCE, EventTag.PLAYER), player -> true, Figure::stranger, false);
    }

    /** 120: another player, in a default skin, walking past far off; gone when approached or when the ground runs out. */
    static HauntEvent distantWalker() {
        return new PresenceFigure("distant_walker", Set.of(EventTag.PRESENCE, EventTag.PLAYER), player -> true, Figure::stranger, true);
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
        return tags;
    }

    @Override
    public boolean canRun(ServerPlayer player) {
        return !active.containsKey(player.getUUID()) && place.test(player) && spot(player) != null;
    }

    @Override
    public void run(ServerPlayer player) {
        BlockPos spot = spot(player);
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
        // Across the view: a quarter turn, to either side, from facing the player; walking, or now and then running and jumping.
        float heading = Mth.wrapDegrees(figure.getYRot() + (player.getRandom().nextBoolean() ? 90.0F : -90.0F));
        Gait gait = walks ? new Gait(config.integer("climb"), config.integer("drop")) : null;
        boolean runs = walks && player.getRandom().nextDouble() < config.num("run_chance");
        double stride = walks ? config.num(runs ? "run_stride" : "stride") : 0.0;
        figure.setSprinting(runs);
        // The watch ends itself; this catches the cases where the scheduler drops it (the player died or left).
        Restore.after(player.level(), config.integer("max_ticks") + config.integer("cleanup_ticks"), force -> {
            leave(figure, owner);
            return true;
        });
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            boolean over = !figure.isAlive() || figure.level() != p.level()
                || n * config.integer("interval_ticks") >= config.integer("max_ticks") || noticed(p, figure, config)
                || gait != null && !stride(p, figure, gait, heading, stride, runs, config);
            if (over) {
                leave(figure, owner);
            } else if (gait != null) {
                Figure.turn(figure, gait.heading());
            } else {
                Figure.face(figure, p);
            }
            return !over;
        });
    }

    /** One tick of the walk; a runner jumps now and then, as players do. False when the way on is blocked every way. */
    private static boolean stride(ServerPlayer player, Mannequin figure, Gait gait, float heading, double stride, boolean runs, Config.Section config) {
        if (runs && player.getRandom().nextDouble() < config.num("hop_chance")) {
            gait.hop();
        }
        return gait.along(player.level(), figure, heading, stride);
    }

    private @Nullable BlockPos spot(ServerPlayer player) {
        Config.Section config = Config.event(id);
        return Place.standingAhead(player, config.num("distance"), config.num("min_angle"), config.num("max_angle"));
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
