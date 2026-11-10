package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.block.Footprints;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Said;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.Session;
import dev.keelfy.restless.haunt.Trail;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Signature events that are a figure in the world: the phantom's copy of the player, the wraith at the edge of the
 * light, the lost one's wanderer. Each is a {@link StrangerAct}, so it ends when the player comes close, and with no
 * trace.
 */
final class Doubles {
    private Doubles() {}

    static List<HauntEvent> all() {
        return List.of(
                new Rerun(),
                new Home(),
                new Edge(),
                new Wanderer(),
                // lost one: prints lead from where the player last died
                SimpleEvent.light(
                        "lost_one_death_path",
                        EventTag.PRESENCE,
                        player -> prints(player).size()
                                >= Config.event("lost_one_death_path").integer("min_prints"),
                        Doubles::walkFromDeath));
    }

    /** The player is looking straight at the figure, which is in sight. */
    static boolean looked(ServerPlayer player, Mannequin figure, double cos) {
        Vec3 to = figure.getEyePosition().subtract(player.getEyePosition());
        return to.normalize().dot(player.getLookAngle()) >= cos && player.hasLineOfSight(figure);
    }

    private static ResolvableProfile own(ServerPlayer player) {
        return ResolvableProfile.createResolved(player.getGameProfile());
    }

    // ---- lost one: the way back from where the player died

    private record Print(BlockPos pos, Direction facing) {}

    /** Prints on the line from the player toward the spot of their last death, each pointing back at the player. */
    private static List<Print> prints(ServerPlayer player) {
        Config.Section config = Config.event("lost_one_death_path");
        ServerLevel level = player.level();
        var death = player.getLastDeathLocation().orElse(null);
        if (death == null || death.dimension() != level.dimension()) {
            return List.of();
        }
        double dx = death.pos().getX() + 0.5 - player.getX();
        double dz = death.pos().getZ() + 0.5 - player.getZ();
        double away = Math.hypot(dx, dz);
        if (away < config.num("min_distance") || away > config.num("max_distance")) {
            return List.of();
        }
        Direction facing = Direction.getApproximateNearest(-dx, 0, -dz);
        List<Print> prints = new ArrayList<>();
        for (int k = 0; k < config.integer("count"); k++) {
            double d = config.num("start") + k * config.num("spacing");
            BlockPos stand = Place.standing(
                    level,
                    BlockPos.containing(player.getX() + dx / away * d, player.getY(), player.getZ() + dz / away * d));
            if (stand != null && level.isEmptyBlock(stand) && Place.solid(level, stand.below())) {
                prints.add(new Print(stand, facing));
            }
        }
        return prints;
    }

    private static void walkFromDeath(ServerPlayer player) {
        List<Print> path = prints(player);
        ServerLevel level = player.level();
        Config.Section config = Config.event("lost_one_death_path");
        Config.Tone tone = config.tone("sound");
        for (int i = 0; i < path.size(); i++) {
            Print print = path.get(path.size() - 1 - i);
            Scheduler.later(player, i * config.integer("step_ticks"), p -> {
                if (!level.isEmptyBlock(print.pos())) {
                    return;
                }
                level.setBlock(
                        print.pos(),
                        Content.FOOTPRINTS.get().defaultBlockState().setValue(Footprints.FACING, print.facing()),
                        Block.UPDATE_ALL);
                level.playSound(
                        null, print.pos(), SoundEvents.MUD_STEP, SoundSource.BLOCKS, tone.volume(), tone.pitch());
                Restore.revert(level, config.integer("restore_ticks"), force -> {
                    if (!force && !level.isLoaded(print.pos())) {
                        return false;
                    }
                    if (level.getBlockState(print.pos()).is(Content.FOOTPRINTS.get())) {
                        level.removeBlock(print.pos(), false);
                    }
                    return true;
                });
            });
        }
    }

    // ---- phantom: the last half minute of the player's own path, walked again

    private static final class Rerun extends StrangerAct {
        private final Map<UUID, List<Vec3>> routes = Session.SERVER.keep(new ConcurrentHashMap<>());

        Rerun() {
            super("phantom_rerun");
        }

        @Override
        ResolvableProfile profile(ServerPlayer player) {
            return own(player);
        }

        /** The path from the oldest sample to now, with steps in between so the walk is smooth. */
        private @Nullable List<Vec3> route(ServerPlayer player) {
            Config.Section config = Config.event("phantom_rerun");
            List<Vec3> samples = Trail.of(player);
            if (samples.size() < config.integer("min_samples")
                    || samples.getFirst().distanceTo(samples.getLast()) < config.num("min_distance")
                    || samples.getFirst().distanceTo(player.position()) > config.num("max_distance")) {
                return null;
            }
            int parts = config.integer("steps_per_sample");
            List<Vec3> route = new ArrayList<>();
            for (int i = 0; i + 1 < samples.size(); i++) {
                for (int k = 0; k < parts; k++) {
                    route.add(samples.get(i).lerp(samples.get(i + 1), (double) k / parts));
                }
            }
            route.add(samples.getLast());
            return route;
        }

        @Override
        @Nullable BlockPos spot(ServerPlayer player) {
            List<Vec3> route = route(player);
            return route == null ? null : Place.standing(player.level(), BlockPos.containing(route.getFirst()));
        }

        @Override
        void begin(ServerPlayer player, Mannequin figure) {
            List<Vec3> route = route(player);
            if (route != null) {
                routes.put(figure.getUUID(), route);
            }
        }

        @Override
        boolean step(ServerPlayer player, Mannequin figure, Config.Section config, int n) {
            List<Vec3> route = routes.get(figure.getUUID());
            if (route == null || n + 1 >= route.size() || looked(player, figure, config.num("look_cos"))) {
                routes.remove(figure.getUUID());
                return false;
            }
            Vec3 from = route.get(n);
            Vec3 to = route.get(n + 1);
            figure.setPos(to);
            Figure.turn(figure, (float) (Mth.atan2(to.z - from.z, to.x - from.x) * Mth.RAD_TO_DEG) - 90.0F);
            return true;
        }
    }

    // ---- phantom: at the bed when the player comes home

    private static final class Home extends StrangerAct {
        Home() {
            super("phantom_home");
        }

        @Override
        ResolvableProfile profile(ServerPlayer player) {
            return own(player);
        }

        private static @Nullable BlockPos bed(ServerPlayer player) {
            var respawn = player.getRespawnConfig();
            if (respawn == null
                    || respawn.respawnData().dimension() != player.level().dimension()) {
                return null;
            }
            BlockPos bed = respawn.respawnData().pos();
            Config.Section config = Config.event("phantom_home");
            double away = Math.sqrt(bed.distToCenterSqr(player.position()));
            ServerLevel level = player.level();
            return away >= config.num("min_distance")
                            && away <= config.num("max_distance")
                            && level.isLoaded(bed)
                            && level.getBlockState(bed).is(BlockTags.BEDS)
                    ? bed
                    : null;
        }

        @Override
        @Nullable BlockPos spot(ServerPlayer player) {
            BlockPos bed = bed(player);
            ServerLevel level = player.level();
            return bed == null ? null : Place.near(player, bed, 1, 2, pos -> Place.floored(level, pos));
        }

        @Override
        boolean step(ServerPlayer player, Mannequin figure, Config.Section config, int n) {
            Figure.face(figure, player);
            return true;
        }
    }

    // ---- wraith: where the dark begins

    private static final class Edge extends StrangerAct {
        Edge() {
            super("wraith_edge");
        }

        @Override
        @Nullable BlockPos spot(ServerPlayer player) {
            Config.Section config = Config.event("wraith_edge");
            ServerLevel level = player.level();
            if (level.getMaxLocalRawBrightness(player.blockPosition()) < config.integer("light_level")) {
                return null;
            }
            return Place.near(
                    player,
                    player.blockPosition(),
                    config.integer("min_distance"),
                    config.integer("max_distance"),
                    pos -> Place.floored(level, pos)
                            && level.getMaxLocalRawBrightness(pos) < config.integer("dark_level"));
        }

        @Override
        boolean step(ServerPlayer player, Mannequin figure, Config.Section config, int n) {
            Figure.face(figure, player);
            return !looked(player, figure, config.num("look_cos"));
        }
    }

    // ---- lost one: someone else who cannot find the way

    private static final class Wanderer extends StrangerAct {
        private final Map<UUID, BlockPos> centres = Session.SERVER.keep(new ConcurrentHashMap<>());

        Wanderer() {
            super("lost_one_wanderer");
        }

        @Override
        @Nullable BlockPos spot(ServerPlayer player) {
            Config.Section config = Config.event("lost_one_wanderer");
            return Said.left(player, "lost_one_wanderer", config.texts("lines"))
                    ? Place.spawnSpot(player, config.integer("min_distance"), config.integer("max_distance"))
                    : null;
        }

        @Override
        void begin(ServerPlayer player, Mannequin figure) {
            centres.put(figure.getUUID(), figure.blockPosition());
        }

        @Override
        boolean step(ServerPlayer player, Mannequin figure, Config.Section config, int n) {
            BlockPos centre = centres.get(figure.getUUID());
            if (centre == null) {
                return false;
            }
            if (player.distanceToSqr(figure) <= square(config.num("speak_distance"))) {
                String line = Said.pick(player, "lost_one_wanderer", config.texts("lines"));
                if (line != null) {
                    player.sendSystemMessage(Component.translatable("chat.type.text", Figure.nickname(player), line));
                }
                centres.remove(figure.getUUID());
                return false;
            }
            double angle = n * config.num("turn");
            double x = centre.getX() + 0.5 + Math.cos(angle) * config.num("circle");
            double z = centre.getZ() + 0.5 + Math.sin(angle) * config.num("circle");
            Integer ground = Walk.ground(player.level(), x, z, centre.getY() + 1, 3);
            if (ground == null) {
                return true;
            }
            figure.setPos(x, ground, z);
            Figure.turn(figure, (float) (angle * Mth.RAD_TO_DEG));
            return true;
        }

        private static double square(double n) {
            return n * n;
        }
    }
}
