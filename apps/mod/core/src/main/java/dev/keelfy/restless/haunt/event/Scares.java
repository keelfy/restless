package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.client.SilhouetteRenderer;
import dev.keelfy.restless.haunt.HauntSounds;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.harm.Band;
import dev.keelfy.restless.state.Session;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Scares the player sets off with a habit: breaking a wall, digging down, eating, closing a chest or a door, turning
 * away, mining in a cave, hiding. Each is a black figure of the {@link Jolts} kind that only this player sees; see
 * docs/scary-events.md for the rules they follow. An event that waits for a break, a meal or a torch only marks the
 * player (one mark at a time, in memory, lapsing on its own); the hooks below spend the mark. The others watch the
 * player themselves for a while.
 */
public final class Scares {
    /** What the player is waiting to do, until {@code lapse} (game time); {@code step} counts echoes already given. */
    private record Mark(String id, long lapse, int step) {}

    private static final Map<UUID, Mark> MARKS = Session.SERVER.keep(new ConcurrentHashMap<>());

    private Scares() {}

    static List<HauntEvent> all() {
        return List.of(
                // A wall block at eye level is broken: behind it, in the hole, a face.
                SimpleEvent.light(
                        "mined_face",
                        EventTag.PRESENCE,
                        player -> canArm(player) && underground(player),
                        player -> arm(player, "mined_face")),
                // A floor block beside the player is broken: a figure lies in the hole, face up.
                SimpleEvent.light(
                        "dig_down",
                        EventTag.PRESENCE,
                        player -> canArm(player) && underground(player),
                        player -> arm(player, "dig_down")),
                // The next meal ends with a figure right ahead.
                SimpleEvent.light("eating", EventTag.PRESENCE, Scares::canArm, player -> arm(player, "eating")),
                // A chest, furnace or table left open a while, closed: a figure right ahead.
                SimpleEvent.light(
                        "container_close",
                        EventTag.PRESENCE,
                        player -> Nearby.any(player, "container_close", Scares::container),
                        Scares::watchContainer),
                // A figure far off turns a little each time the player looks away; the last time it is behind them.
                SimpleEvent.light("slow_turn", EventTag.PRESENCE, player -> farSpot(player) != null, Scares::slowTurn),
                // Every block broken in a cave is answered from nearer, the fourth from right behind.
                SimpleEvent.light(
                        "echo_answer",
                        EventTag.PRESENCE,
                        player -> canArm(player) && underground(player),
                        player -> arm(player, "echo_answer")),
                // A whisper to one side; turn to it, and a figure is on the other.
                SimpleEvent.light("name_left_right", EventTag.PRESENCE, SimpleEvent.ALWAYS, Scares::nameLeftRight),
                // A door the player closes flies open on a figure.
                SimpleEvent.light(
                        "door_slam_face",
                        EventTag.PRESENCE,
                        player -> Nearby.any(player, "door_slam_face", s -> s.getBlock() instanceof DoorBlock),
                        Scares::watchDoors),
                // The next torch put up in a dark cave lights a figure.
                SimpleEvent.light(
                        "first_light",
                        EventTag.PRESENCE,
                        player -> canArm(player) && underground(player),
                        player -> arm(player, "first_light")),
                // Keep still in the dark, or be caught.
                SimpleEvent.harmful(
                        "hide",
                        EventTag.PRESENCE,
                        player -> Assaults.healthy(player, "hide")
                                && underground(player)
                                && Place.standingAhead(
                                                player, Config.event("hide").num("distance"), 0, 180)
                                        != null,
                        Scares::hide));
    }

    // ---- marks and hooks ----

    private static boolean underground(ServerPlayer player) {
        return !player.level().canSeeSky(player.blockPosition());
    }

    private static boolean canArm(ServerPlayer player) {
        return current(player) == null;
    }

    private static void arm(ServerPlayer player, String id) {
        long lapse = player.level().getGameTime() + Config.event(id).integer("max_ticks");
        MARKS.put(player.getUUID(), new Mark(id, lapse, 0));
    }

    private static @Nullable Mark current(ServerPlayer player) {
        Mark mark = MARKS.get(player.getUUID());
        if (mark != null && player.level().getGameTime() > mark.lapse) {
            MARKS.remove(player.getUUID());
            return null;
        }
        return mark;
    }

    /** Call when a player is back after dying: what they were waiting to do is forgotten. */
    public static void clear(ServerPlayer player) {
        MARKS.remove(player.getUUID());
    }

    /** Call when a player breaks a block, before it is gone; {@code state} is what stood there. */
    public static void broke(ServerPlayer player, BlockState state, BlockPos pos) {
        Mark mark = current(player);
        if (mark == null) {
            return;
        }
        boolean spent = switch (mark.id) {
            case "mined_face" -> minedFace(player, state, pos);
            case "dig_down" -> digDown(player, pos);
            case "echo_answer" -> echo(player, state, mark);
            default -> false;
        };
        if (spent) {
            MARKS.remove(player.getUUID());
        }
    }

    /** Call after a player finishes eating or drinking; {@code eaten} is the stack as it was before. */
    public static void ate(ServerPlayer player, ItemStack eaten) {
        Mark mark = current(player);
        if (mark == null || !mark.id.equals("eating") || !eaten.has(DataComponents.FOOD)) {
            return;
        }
        Config.Section config = Config.event("eating");
        BlockPos spot =
                Place.standingAhead(player, config.num("distance"), config.num("min_angle"), config.num("max_angle"));
        if (spot != null) {
            MARKS.remove(player.getUUID());
            flash(player, Vec3.atBottomCenterOf(spot), -1, config.tone("sound"), config.integer("hold_ticks"));
        }
    }

    /** Call before a player uses a block with their main hand. */
    public static void used(ServerPlayer player) {
        Mark mark = current(player);
        if (mark == null || !mark.id.equals("first_light")) {
            return;
        }
        Config.Section config = Config.event("first_light");
        ServerLevel level = player.level();
        if (!(player.getMainHandItem().is(Items.TORCH)
                        || player.getMainHandItem().is(Items.SOUL_TORCH))
                || !underground(player)
                || level.getMaxLocalRawBrightness(player.blockPosition()) > config.integer("dark_max")) {
            return;
        }
        MARKS.remove(player.getUUID());
        Scheduler.later(player, config.integer("delay_ticks"), p -> {
            BlockPos spot =
                    Place.standingAhead(p, config.num("distance"), config.num("min_angle"), config.num("max_angle"));
            if (spot != null) {
                flash(p, Vec3.atBottomCenterOf(spot), -1, null, config.integer("hold_ticks"));
            }
        });
    }

    // ---- scares ----

    /** The figure at {@code at} facing the player, with a bang if given, for {@code hold} ticks; -1 picks any shape. */
    private static void flash(ServerPlayer player, Vec3 at, int shape, Config.@Nullable Tone bang, int hold) {
        if (bang != null) {
            Nearby.sound(
                    player, HauntSounds.JOLT, at.add(0, Config.section("place").num("sound_height"), 0), bang);
        }
        PacketEntity figure =
                PacketEntity.show(player, Content.SILHOUETTE.get(), at, PacketEntity.yawToward(at, player.position()));
        if (shape < 0) {
            Jolts.shape(player, figure);
        } else {
            Jolts.shape(player, figure, shape);
        }
        Scheduler.later(player, hold, figure::remove);
    }

    private static boolean minedFace(ServerPlayer player, BlockState state, BlockPos pos) {
        Config.Section config = Config.event("mined_face");
        ServerLevel level = player.level();
        Direction toward = Direction.fromYRot(player.getYRot());
        BlockPos behind = pos.relative(toward);
        BlockPos deep = pos.relative(toward, 2);
        double ahead = (pos.getX() + 0.5 - player.getX()) * toward.getStepX()
                + (pos.getZ() + 0.5 - player.getZ()) * toward.getStepZ();
        if (pos.getY() != player.blockPosition().getY() + 1
                || ahead < 0.5
                || player.position().distanceTo(Vec3.atCenterOf(pos)) > config.num("reach")
                || !state.isSolidRender()
                || !Place.solid(level, behind)
                || !Place.solid(level, deep)) {
            return false;
        }
        int hold = config.integer("hold_ticks");
        Scheduler.later(player, 1, p -> {
            Swaps.show(p, List.of(behind, deep), Blocks.AIR.defaultBlockState(), hold + 2);
            flash(p, Vec3.atBottomCenterOf(behind), SilhouetteRenderer.FACE, config.tone("sound"), hold);
        });
        return true;
    }

    private static boolean digDown(ServerPlayer player, BlockPos pos) {
        Config.Section config = Config.event("dig_down");
        ServerLevel level = player.level();
        double away = Math.hypot(pos.getX() + 0.5 - player.getX(), pos.getZ() + 0.5 - player.getZ());
        BlockPos pit = pos.below(2);
        if (pos.getY() != player.blockPosition().getY() - 1
                || away < config.num("min_distance")
                || away > config.num("reach")
                || !Place.solid(level, pos.below())
                || !Place.solid(level, pit)) {
            return false;
        }
        // Room along the line to the player for a figure lying on its back, where there is rock to open.
        Direction line =
                Direction.getApproximateNearest(player.getX() - pos.getX() - 0.5, 0, player.getZ() - pos.getZ() - 0.5);
        Set<BlockPos> hollow = new HashSet<>(List.of(pos.below(), pit));
        for (BlockPos end : List.of(pit.relative(line), pit.relative(line.getOpposite()))) {
            if (Place.solid(level, end)) {
                hollow.add(end);
            }
        }
        int hold = config.integer("hold_ticks");
        Scheduler.later(player, 1, p -> {
            Swaps.show(p, hollow, Blocks.AIR.defaultBlockState(), hold + 2);
            flash(p, Vec3.atBottomCenterOf(pit), SilhouetteRenderer.LYING, config.tone("sound"), hold);
        });
        return true;
    }

    /** One break sound in answer, from farther off each time at first, and the last from right behind. */
    private static boolean echo(ServerPlayer player, BlockState state, Mark mark) {
        Config.Section config = Config.event("echo_answer");
        if (!underground(player)) {
            return false;
        }
        double[] far = {
            config.num("far_distance"),
            config.num("mid_distance"),
            config.num("near_distance"),
            config.num("behind_distance")
        };
        boolean last = mark.step == far.length - 1;
        Scheduler.later(
                player,
                config.integer("delay_ticks"),
                p -> Nearby.sound(
                        p,
                        state.getSoundType().getBreakSound(),
                        last ? Nearby.around(p, far[mark.step], 180) : Nearby.randomAround(p, far[mark.step]),
                        config.tone("sound")));
        if (!last) {
            MARKS.put(player.getUUID(), new Mark(mark.id, mark.lapse, mark.step + 1));
        }
        return last;
    }

    private static boolean container(BlockState state) {
        return state.getBlock() instanceof ChestBlock
                || state.getBlock() instanceof BarrelBlock
                || state.getBlock() instanceof AbstractFurnaceBlock
                || state.is(Blocks.CRAFTING_TABLE);
    }

    private static void watchContainer(ServerPlayer player) {
        Config.Section config = Config.event("container_close");
        ServerLevel level = player.level();
        int[] open = {0};
        Scheduler.repeat(player, 1, 1, (p, n) -> {
            if (p.level() != level || n >= config.integer("max_ticks")) {
                return false;
            }
            if (p.containerMenu != p.inventoryMenu) {
                open[0]++;
                return true;
            }
            if (open[0] < config.integer("open_ticks")) {
                open[0] = 0;
                return true;
            }
            BlockPos spot =
                    Place.standingAhead(p, config.num("distance"), config.num("min_angle"), config.num("max_angle"));
            if (spot != null) {
                flash(p, Vec3.atBottomCenterOf(spot), -1, config.tone("sound"), config.integer("hold_ticks"));
            }
            return false;
        });
    }

    private static @Nullable BlockPos farSpot(ServerPlayer player) {
        Config.Section config = Config.event("slow_turn");
        return Place.standingAhead(player, config.num("distance"), config.num("min_angle"), config.num("max_angle"));
    }

    /**
     * A figure far ahead with its back turned. Each time the player looks at it and then away, it has turned
     * {@code turn_degrees} more; after {@code turns} turns it faces them, and the next time they look away it is behind
     * them ({@link Jolts#show}).
     */
    private static void slowTurn(ServerPlayer player) {
        Config.Section config = Config.event("slow_turn");
        BlockPos spot = farSpot(player);
        if (spot == null) {
            return;
        }
        ServerLevel level = player.level();
        Vec3 at = Vec3.atBottomCenterOf(spot);
        float[] yaw = {PacketEntity.yawToward(at, player.position()) + 180.0F};
        PacketEntity figure = PacketEntity.show(player, Content.SILHOUETTE.get(), at, yaw[0]);
        Jolts.shape(player, figure);
        boolean[] seen = {false};
        int[] turns = {0};
        Scheduler.repeat(player, 1, 1, (p, n) -> {
            boolean struck = false;
            boolean over = p.level() != level
                    || n >= config.integer("max_ticks")
                    || p.position().distanceTo(at) <= config.num("leave_distance");
            if (!over) {
                if (Jolts.watched(p, at.add(0, 1.5, 0), config.num("look_angle"))) {
                    seen[0] = true;
                } else if (seen[0]) {
                    seen[0] = false;
                    turns[0]++;
                    struck = turns[0] > config.integer("turns");
                    over = struck;
                    if (!struck) {
                        yaw[0] += (float) config.num("turn_degrees");
                        figure.move(p, at, yaw[0]);
                    }
                }
            }
            if (over) {
                figure.remove(p);
            }
            if (struck) {
                BlockPos behind = Place.standingAhead(p, config.num("behind_distance"), 150, 180);
                if (behind != null) {
                    Jolts.show(p, config, behind, true, config.integer("wait_ticks"));
                }
            }
            return !over;
        });
    }

    private static void nameLeftRight(ServerPlayer player) {
        Config.Section config = Config.event("name_left_right");
        ServerLevel level = player.level();
        int side = player.getRandom().nextBoolean() ? 1 : -1;
        float yaw = player.getYRot();
        Vec3 whisper = Nearby.around(player, config.num("side_distance"), side * 90.0);
        Scheduler.repeat(player, 1, 1, (p, n) -> {
            if (p.level() != level || n >= config.integer("max_ticks")) {
                return false;
            }
            if (side * Mth.wrapDegrees(p.getYRot() - yaw) >= config.num("turn_degrees")) {
                BlockPos spot = Place.standingAt(p, config.num("distance"), -side * config.num("angle"));
                if (spot != null) {
                    flash(p, Vec3.atBottomCenterOf(spot), -1, config.tone("bang"), config.integer("hold_ticks"));
                }
                return false;
            }
            if (n % config.integer("cue_gap_ticks") == 0) {
                Nearby.sound(p, HauntSounds.WHISPER, whisper, config.tone("sound"));
            }
            return true;
        });
    }

    private static Set<BlockPos> openDoors(ServerPlayer player) {
        return new HashSet<>(Nearby.all(
                player,
                "door_slam_face",
                s -> s.getBlock() instanceof DoorBlock
                        && s.getValue(DoorBlock.OPEN)
                        && s.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER));
    }

    private static void watchDoors(ServerPlayer player) {
        Config.Section config = Config.event("door_slam_face");
        ServerLevel level = player.level();
        Set<BlockPos> open = openDoors(player);
        Scheduler.repeat(player, 2, 2, (p, n) -> {
            if (p.level() != level || n * 2 >= config.integer("max_ticks")) {
                return false;
            }
            Set<BlockPos> now = openDoors(p);
            for (BlockPos pos : open) {
                if (!now.contains(pos) && slammed(p, pos, config)) {
                    return false;
                }
            }
            open.clear();
            open.addAll(now);
            return true;
        });
    }

    /** The door at {@code pos} has just shut near the player: it shows open, with a figure standing in it. */
    private static boolean slammed(ServerPlayer player, BlockPos pos, Config.Section config) {
        ServerLevel level = player.level();
        BlockState lower = level.getBlockState(pos);
        BlockState upper = level.getBlockState(pos.above());
        if (!(lower.getBlock() instanceof DoorBlock)
                || lower.getValue(DoorBlock.OPEN)
                || player.position().distanceTo(Vec3.atCenterOf(pos)) > config.num("reach")) {
            return false;
        }
        int hold = config.integer("hold_ticks");
        Map<BlockPos, BlockState> shown = new LinkedHashMap<>();
        shown.put(pos, lower.setValue(DoorBlock.OPEN, true));
        if (upper.getBlock() instanceof DoorBlock) {
            shown.put(pos.above(), upper.setValue(DoorBlock.OPEN, true));
        }
        Swaps.show(player, shown, hold + 2);
        Vec3 away = Vec3.atCenterOf(pos)
                .subtract(player.position())
                .multiply(1, 0, 1)
                .normalize();
        Vec3 at = Vec3.atBottomCenterOf(pos).add(away.scale(0.6));
        flash(player, at, -1, config.tone("sound"), hold);
        return true;
    }

    /**
     * An action bar word and a heartbeat for {@code hide_ticks}. Move more than {@code still_radius} and a figure that
     * was out of sight is at the player's face and hits them (threat band); keep still and heavy steps pass outside.
     */
    private static void hide(ServerPlayer player) {
        Config.Section config = Config.event("hide");
        ServerLevel level = player.level();
        Vec3 start = player.position();
        player.sendSystemMessage(Component.translatable("message.restless.hide"), true);
        Scheduler.repeat(player, 1, 1, (p, n) -> {
            if (p.level() != level) {
                return false;
            }
            if (p.position().distanceTo(start) > config.num("still_radius")) {
                caught(p, config);
                return false;
            }
            if (n % config.integer("beat_gap_ticks") == 0) {
                Nearby.sound(p, SoundEvents.WARDEN_HEARTBEAT, p.position(), config.tone("beat_sound"));
            }
            if (n >= config.integer("hide_ticks")) {
                passes(p, config);
                return false;
            }
            return true;
        });
    }

    private static void caught(ServerPlayer player, Config.Section config) {
        BlockPos spot = Place.standingAhead(player, config.num("distance"), 0, 180);
        if (spot == null) {
            return;
        }
        ServerLevel level = player.level();
        Vec3 home = Vec3.atBottomCenterOf(spot);
        PacketEntity figure = PacketEntity.show(
                player, Content.SILHOUETTE.get(), home, PacketEntity.yawToward(home, player.position()));
        Jolts.shape(player, figure);
        int lunge = config.integer("lunge_ticks");
        Scheduler.repeat(player, 1, 1, (p, n) -> {
            Vec3 look = p.getLookAngle();
            Vec3 face = p.position().add(new Vec3(look.x, 0, look.z).normalize().scale(config.num("face_distance")));
            if (p.level() == level && n <= lunge) {
                figure.move(p, home.lerp(face, (double) n / lunge), PacketEntity.yawToward(face, p.position()));
                if (n == lunge) {
                    Nearby.sound(p, HauntSounds.JOLT, face.add(0, p.getEyeHeight(), 0), config.tone("sound"));
                    float damage = Math.min(config.integer("damage"), Assaults.room(p, Band.THREAT));
                    if (damage > 0) {
                        p.hurtServer(level, Taken.by(level, Taken.TAKEN), damage);
                    }
                }
            }
            boolean over = p.level() != level || n >= lunge + config.integer("hold_ticks");
            if (over) {
                figure.remove(p);
            }
            return !over;
        });
    }

    private static void passes(ServerPlayer player, Config.Section config) {
        double bearing = player.getRandom().nextDouble() * Nearby.FULL_TURN;
        Scheduler.repeat(player, 1, config.integer("step_gap_ticks"), (p, n) -> {
            double distance = config.num("step_distance") + n * config.num("step_growth");
            Nearby.sound(p, SoundEvents.RAVAGER_STEP, Nearby.around(p, distance, bearing), config.tone("step_sound"));
            return n + 1 < config.integer("steps");
        });
    }
}
