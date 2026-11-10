package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.block.UnlitLights;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The shade eats light; the wraith walks where there is none. */
final class Dusk {
    private Dusk() {}

    static List<HauntEvent> all() {
        return List.of(
                // shade: the torches vanish into one pile in a dark spot
                SimpleEvent.light(
                        "shade_gathered",
                        EventTag.LIGHTS,
                        player -> torches(player).size()
                                        >= Config.event("shade_gathered").integer("min_count")
                                && darkSpot(player) != null,
                        Dusk::gather),
                // shade: whatever light the player looks at goes out
                SimpleEvent.light(
                        "shade_glance",
                        EventTag.LIGHTS,
                        player -> lights(player, "shade_glance").size()
                                >= Config.event("shade_glance").integer("min_count"),
                        Dusk::glance),
                // shade: every light in the room the player is in goes out
                SimpleEvent.harmful(
                        "shade_room",
                        EventTag.LIGHTS,
                        player -> {
                            Set<Long> room = room(player);
                            return room != null && !lit(player, room).isEmpty();
                        },
                        Dusk::darken),
                // wraith: steps behind the player in the dark, that stop when the player does
                SimpleEvent.light(
                        "wraith_steps",
                        EventTag.PRESENCE,
                        player -> player.level().getMaxLocalRawBrightness(player.blockPosition())
                                        < Config.event("wraith_steps").integer("dark_level")
                                && !player.isInWater(),
                        Dusk::follow));
    }

    private static boolean light(BlockState state) {
        return UnlitLights.isTorch(state) || UnlitLights.isLantern(state);
    }

    private static List<BlockPos> lights(ServerPlayer player, String event) {
        return Nearby.all(player, event, Dusk::light);
    }

    private static void snuff(ServerLevel level, BlockPos pos, int restoreTicks) {
        Restore.swap(level, pos, UnlitLights.extinguished(level.getBlockState(pos)), restoreTicks);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7F, 1.0F);
    }

    // ---- shade: gathered

    private static List<BlockPos> torches(ServerPlayer player) {
        return Nearby.all(player, "shade_gathered", UnlitLights::isTorch).stream()
                .sorted(Comparator.comparingDouble(pos -> pos.distToCenterSqr(player.position())))
                .toList();
    }

    private static @Nullable BlockPos darkSpot(ServerPlayer player) {
        Config.Section config = Config.event("shade_gathered");
        ServerLevel level = player.level();
        return Place.near(
                player,
                player.blockPosition(),
                config.integer("spot_min"),
                config.integer("spot_max"),
                pos -> Place.floored(level, pos) && level.getMaxLocalRawBrightness(pos) < config.integer("dark_level"));
    }

    private static void gather(ServerPlayer player) {
        BlockPos spot = darkSpot(player);
        Config.Section config = Config.event("shade_gathered");
        List<BlockPos> torches = torches(player);
        if (spot == null || torches.isEmpty()) {
            return;
        }
        List<BlockPos> taken = torches.subList(0, Math.min(torches.size(), config.integer("max_torches")));
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            ServerLevel level = p.level();
            BlockPos pos = taken.get(n);
            BlockState state = level.getBlockState(pos);
            if (UnlitLights.isTorch(state)) {
                for (ItemStack drop : Block.getDrops(state, level, pos, null)) {
                    level.addFreshEntity(new ItemEntity(
                            level, spot.getX() + 0.5, spot.getY() + 0.2, spot.getZ() + 0.5, drop, 0, 0, 0));
                }
                level.removeBlock(pos, false);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7F, 1.0F);
            }
            return n + 1 < taken.size();
        });
    }

    // ---- shade: glance

    private static void glance(ServerPlayer player) {
        Config.Section config = Config.event("shade_glance");
        List<BlockPos> lights = new ArrayList<>(lights(player, "shade_glance"));
        int interval = config.integer("interval_ticks");
        int[] put = {0};
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            ServerLevel level = p.level();
            Vec3 eye = p.getEyePosition();
            for (BlockPos pos : List.copyOf(lights)) {
                Vec3 to = Vec3.atCenterOf(pos).subtract(eye);
                if (to.length() <= config.num("sight")
                        && to.normalize().dot(p.getLookAngle()) >= config.num("look_cos")
                        && light(level.getBlockState(pos))
                        && seen(p, pos)) {
                    snuff(level, pos, config.integer("restore_ticks"));
                    lights.remove(pos);
                    if (++put[0] >= config.integer("max_lights")) {
                        return false;
                    }
                }
            }
            return !lights.isEmpty() && n * interval < config.integer("seconds") * 20;
        });
    }

    private static boolean seen(ServerPlayer player, BlockPos pos) {
        var hit = player.level()
                .clip(new ClipContext(
                        player.getEyePosition(),
                        Vec3.atCenterOf(pos),
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        player));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }

    // ---- shade: room

    /** The floor cells of the room the player stands in, as packed x, z; null if it is open to the sky or too large. */
    private static @Nullable Set<Long> room(ServerPlayer player) {
        Config.Section config = Config.event("shade_room");
        ServerLevel level = player.level();
        BlockPos feet = player.blockPosition();
        if (level.canSeeSky(feet)) {
            return null;
        }
        Set<Long> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(feet);
        seen.add(key(feet));
        while (!queue.isEmpty()) {
            BlockPos at = queue.poll();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos next = at.relative(d);
                if (!seen.add(key(next)) || !(Place.open(level, next) && Place.open(level, next.above()))) {
                    continue;
                }
                if (seen.size() > config.integer("max_cells") || level.canSeeSky(next)) {
                    return null;
                }
                queue.add(next);
            }
        }
        return seen;
    }

    private static long key(BlockPos pos) {
        return new BlockPos(pos.getX(), 0, pos.getZ()).asLong();
    }

    /** The lights standing over the room's floor, not far above or below the player. */
    private static List<BlockPos> lit(ServerPlayer player, Set<Long> room) {
        int reach = Config.event("shade_room").integer("height");
        return Nearby.all(player, "shade_room", Dusk::light).stream()
                .filter(pos -> room.contains(key(pos)) && Math.abs(pos.getY() - player.getBlockY()) <= reach)
                .toList();
    }

    private static void darken(ServerPlayer player) {
        Set<Long> room = room(player);
        Config.Section config = Config.event("shade_room");
        if (room == null) {
            return;
        }
        List<BlockPos> out = lit(player, room);
        ServerLevel level = player.level();
        out.forEach(pos -> snuff(level, pos, config.integer("max_ticks")));
        Scheduler.repeat(player, config.integer("interval_ticks"), config.integer("interval_ticks"), (p, n) -> {
            if (room.contains(key(p.blockPosition())) && p.level() == level) {
                p.addEffect(
                        new MobEffectInstance(MobEffects.DARKNESS, config.integer("darkness_ticks"), 0, false, false));
                return n * config.integer("interval_ticks") < config.integer("max_ticks");
            }
            out.forEach(pos -> relight(level, pos));
            return false;
        });
    }

    /** Lit again, if it is still the torch or lantern put out. */
    private static void relight(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockState back = UnlitLights.relit(state);
        if (back != state) {
            Restore.quiet(level, pos, back);
        }
    }

    // ---- wraith: steps

    private static void follow(ServerPlayer player) {
        Config.Section config = Config.event("wraith_steps");
        Config.Tone tone = config.tone("sound");
        Vec3[] last = {player.position()};
        int[] since = {0};
        int interval = config.integer("interval_ticks");
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            boolean moved = p.position().distanceToSqr(last[0]) > config.num("min_move") * config.num("min_move");
            last[0] = p.position();
            if (moved) {
                since[0] += interval;
                if (since[0] >= config.integer("cadence_ticks")) {
                    since[0] = 0;
                    Vec3 at = Nearby.around(p, config.num("behind"), 180);
                    BlockState ground =
                            p.level().getBlockState(p.blockPosition().below());
                    p.level()
                            .playSound(
                                    null,
                                    at.x,
                                    p.getY(),
                                    at.z,
                                    ground.getSoundType().getStepSound(),
                                    SoundSource.HOSTILE,
                                    tone.volume(),
                                    tone.pitch());
                }
            }
            return n * interval < config.integer("seconds") * 20;
        });
    }
}
