package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntTier;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Said;
import dev.keelfy.restless.haunt.ScenarioContext;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.Session;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** Another player, near or far, doing something as players do; and what such a player leaves in chat. */
final class Strangers {
    private Strangers() {}

    /** How long the haze of a figure that is gone stays where it stood. */
    private static final int LINGER_TICKS = 60;

    private static final Predicate<BlockState> TABLE = state -> state.is(Blocks.CRAFTING_TABLE);
    private static final Predicate<BlockState> SHRIEKER = state -> state.is(Blocks.SCULK_SHRIEKER);

    static List<HauntEvent> all() {
        return List.of(
                // 369: crouched on a roof, watching
                new Act("roof_figure", Strangers::roofSpot, Strangers::onRoof, Strangers::watch, player -> true).hazy(),
                // 371: waves from the top of a hill
                new Act("hill_waver", Strangers::hillSpot, figure -> {}, Strangers::wave, player -> true).hazy(),
                // 372: mines toward the player from the far side of a wall
                new Act("wall_miner", Strangers::wallSpot, figure -> {}, Strangers::mine, player -> true),
                // 373: stands over a crafting table, looking down at it
                new Act(
                        "table_figure",
                        player -> beside(player, "table_figure", TABLE),
                        figure -> {},
                        (player, figure, config, n) -> {
                            BlockPos table = nearest(figure, config.integer("reach"), TABLE);
                            if (table == null) {
                                return false;
                            }
                            figure.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(table));
                            return true;
                        },
                        player -> true),
                // 374: torches the player never placed, along a tunnel ahead
                new SimpleEvent(
                        Identifier.parse("cave_torches"),
                        HauntTier.LIGHT,
                        Set.of(EventTag.PRESENCE, EventTag.PLAYER),
                        player -> !player.level().canSeeSky(player.blockPosition())
                                && !torches(player).isEmpty(),
                        player -> Swaps.show(
                                player,
                                torches(player),
                                Blocks.TORCH.defaultBlockState(),
                                Config.event("cave_torches").integer("restore_ticks"))),
                // 377: the player's own skin, walking far away
                new Runner("own_walker", player -> ResolvableProfile.createResolved(player.getGameProfile()), false),
                // 379: a stranger sprinting across the path in the dark
                new Runner("night_runner", Figure::stranger, true),
                // 384: sleeping in a bed that is not the player's
                new Act(
                        "bed_sleeper",
                        Strangers::bedSpot,
                        Strangers::lieDown,
                        (player, figure, config, n) -> true,
                        player -> true),
                // 385: crouched by a sculk shrieker, gone when it shrieks
                new Act(
                        "shrieker_figure",
                        player -> beside(player, "shrieker_figure", SHRIEKER),
                        figure -> figure.setPose(Pose.CROUCHING),
                        (player, figure, config, n) -> {
                            BlockPos shrieker = nearest(figure, config.integer("reach"), SHRIEKER);
                            if (shrieker == null
                                    || player.level().getBlockState(shrieker).getValue(SculkShriekerBlock.SHRIEKING)) {
                                return false;
                            }
                            figure.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(shrieker));
                            return true;
                        },
                        player -> Where.biome(player, Biomes.DEEP_DARK)),
                // 387: a chat line from a made-up name
                saying(
                        "hello_chat",
                        "lines",
                        (player, line) -> Component.translatable("chat.type.text", Figure.nickname(player), line)),
                // 388: fake command feedback about a teleport
                saying(
                        "ghost_teleport",
                        "names",
                        (player, name) -> Component.translatable(
                                "commands.teleport.success.entity.single",
                                ScenarioContext.speaker(name),
                                player.getDisplayName())),
                // 391: a made-up name has made an advancement
                saying(
                        "stranger_advancement",
                        "titles",
                        (player, title) -> Component.translatable(
                                "chat.type.advancement.task",
                                Figure.nickname(player),
                                ComponentUtils.wrapInSquareBrackets(Component.literal(title))
                                        .withStyle(ChatFormatting.GREEN))),
                // 389: where the player last died
                new Act("grave_figure", Strangers::graveSpot, figure -> {}, Strangers::watch, player -> true).hazy(),
                // 390: next to the bed for a second after waking
                new SimpleEvent(
                        Identifier.parse("bedside_figure"),
                        HauntTier.LIGHT,
                        Set.of(EventTag.PRESENCE, EventTag.PLAYER),
                        player -> !WAITING.contains(player.getUUID()),
                        Strangers::bedside));
    }

    // --- acts ------------------------------------------------------------------------------------------------------

    private interface Spot {
        @Nullable BlockPos find(ServerPlayer player);
    }

    private interface Move {
        boolean step(ServerPlayer player, Mannequin figure, Config.Section config, int n);
    }

    /** A {@link StrangerAct} put together from its parts. */
    private static final class Act extends StrangerAct {
        private final Spot spot;
        private final Consumer<Mannequin> dress;
        private final Move move;
        private final Predicate<ServerPlayer> where;
        private boolean hazy;

        Act(String id, Spot spot, Consumer<Mannequin> dress, Move move, Predicate<ServerPlayer> where) {
            super(id);
            this.spot = spot;
            this.dress = dress;
            this.move = move;
            this.where = where;
        }

        /** Leaves a haze where the figure stood. */
        Act hazy() {
            hazy = true;
            return this;
        }

        @Override
        void vanished(ServerPlayer player, Mannequin figure) {
            if (hazy) {
                Haze.linger(player, figure.position(), LINGER_TICKS);
            }
        }

        @Override
        public boolean canRun(ServerPlayer player) {
            return where.test(player) && super.canRun(player);
        }

        @Override
        @Nullable BlockPos spot(ServerPlayer player) {
            return spot.find(player);
        }

        @Override
        void prepare(Mannequin figure) {
            dress.accept(figure);
        }

        @Override
        boolean step(ServerPlayer player, Mannequin figure, Config.Section config, int n) {
            return move.step(player, figure, config, n);
        }
    }

    private static boolean watch(ServerPlayer player, Mannequin figure, Config.Section config, int n) {
        Figure.face(figure, player);
        return true;
    }

    private static boolean wave(ServerPlayer player, Mannequin figure, Config.Section config, int n) {
        Figure.face(figure, player);
        figure.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
        return true;
    }

    // 369
    private static boolean isRoof(BlockState state) {
        return state.is(BlockTags.STAIRS) || state.is(BlockTags.SLABS) || state.is(BlockTags.PLANKS);
    }

    private static @Nullable BlockPos roofSpot(ServerPlayer player) {
        Config.Section config = Config.event("roof_figure");
        ServerLevel level = player.level();
        double min = config.num("min_distance");
        List<BlockPos> roofs = Nearby.all(player, "roof_figure", Strangers::isRoof).stream()
                .filter(pos -> level.canSeeSky(pos.above())
                        && level.isEmptyBlock(pos.above())
                        && level.isEmptyBlock(pos.above(2))
                        && player.distanceToSqr(Vec3.atCenterOf(pos)) >= min * min)
                .toList();
        return roofs.isEmpty()
                ? null
                : roofs.get(player.getRandom().nextInt(roofs.size())).above();
    }

    /** Down onto the roof it stands on (a slab or stair is not a full block high), crouched. */
    private static void onRoof(Mannequin figure) {
        BlockPos roof = figure.blockPosition().below();
        VoxelShape shape = figure.level().getBlockState(roof).getCollisionShape(figure.level(), roof);
        if (!shape.isEmpty()) {
            double top = roof.getY() + shape.max(Direction.Axis.Y);
            figure.setPos(figure.getX(), top, figure.getZ());
        }
        figure.setPose(Pose.CROUCHING);
    }

    // 371
    private static @Nullable BlockPos hillSpot(ServerPlayer player) {
        Config.Section config = Config.event("hill_waver");
        ServerLevel level = player.level();
        for (int i = 0; i < config.integer("tries"); i++) {
            double angle = config.num("min_angle")
                    + player.getRandom().nextDouble() * (config.num("max_angle") - config.num("min_angle"));
            Vec3 at = Nearby.around(
                    player, config.num("distance"), player.getRandom().nextBoolean() ? angle : -angle);
            BlockPos column = BlockPos.containing(at.x, player.getY(), at.z);
            if (!level.isLoaded(column)) {
                continue;
            }
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            if (top.getY() - player.getBlockY() < config.integer("min_rise")) {
                continue;
            }
            BlockPos stand = Place.standing(level, top);
            if (stand != null && level.canSeeSky(stand)) {
                return stand;
            }
        }
        return null;
    }

    // 372
    private static final int CRACK_ID = 1_100_000;

    private static @Nullable BlockPos wallSpot(ServerPlayer player) {
        Place.Wall wall = Place.wall(player);
        return wall == null ? null : wall.behind();
    }

    private static boolean mine(ServerPlayer player, Mannequin figure, Config.Section config, int n) {
        ServerLevel level = player.level();
        BlockPos wall = java.util.stream.StreamSupport.stream(Direction.Plane.HORIZONTAL.spliterator(), false)
                .map(figure.blockPosition()::relative)
                .filter(pos -> player.level().getBlockState(pos).isSolidRender())
                .min(Comparator.comparingDouble(pos -> pos.distToCenterSqr(player.position())))
                .orElse(figure.blockPosition());
        BlockState state = level.getBlockState(wall);
        int id = player.getId() + CRACK_ID;
        if (state.isAir()) {
            return false;
        }
        if (n == 0) {
            // The cracks go on the client; clear them if the act is cut short.
            Scheduler.later(
                    player,
                    (config.integer("stages") + 2) * config.integer("interval_ticks"),
                    p -> p.connection.send(new ClientboundBlockDestructionPacket(id, wall, -1)));
        }
        if (n >= config.integer("stages")) {
            player.connection.send(new ClientboundBlockDestructionPacket(id, wall, -1));
            return false;
        }
        figure.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(wall));
        figure.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
        player.connection.send(new ClientboundBlockDestructionPacket(id, wall, n));
        Config.Tone tone = config.tone("sound");
        level.playSound(
                null, wall, state.getSoundType().getHitSound(), SoundSource.BLOCKS, tone.volume(), tone.pitch());
        return true;
    }

    // 373, 385
    /** Standing room next to a matching block near the player, at least {@code min_distance} from the player. */
    private static @Nullable BlockPos beside(ServerPlayer player, String id, Predicate<BlockState> match) {
        Config.Section config = Config.event(id);
        ServerLevel level = player.level();
        double min = config.num("min_distance");
        int reach = config.integer("reach");
        List<BlockPos> targets = new ArrayList<>(Nearby.all(player, id, match));
        Collections.shuffle(targets, new Random(player.getRandom().nextLong()));
        for (BlockPos target : targets) {
            for (BlockPos pos :
                    BlockPos.betweenClosed(target.offset(-reach, -1, -reach), target.offset(reach, 1, reach))) {
                if (level.isEmptyBlock(pos)
                        && level.isEmptyBlock(pos.above())
                        && level.getBlockState(pos.below()).isSolidRender()
                        && player.distanceToSqr(Vec3.atBottomCenterOf(pos)) >= min * min) {
                    return pos.immutable();
                }
            }
        }
        return null;
    }

    private static @Nullable BlockPos nearest(Mannequin figure, int reach, Predicate<BlockState> match) {
        return Nearby.all((ServerLevel) figure.level(), figure.blockPosition(), reach, match).stream()
                .min(Comparator.comparingDouble(pos -> pos.distToCenterSqr(figure.position())))
                .orElse(null);
    }

    // 384
    private static @Nullable BlockPos bedSpot(ServerPlayer player) {
        Config.Section config = Config.event("bed_sleeper");
        ServerLevel level = player.level();
        double min = config.num("min_distance");
        List<BlockPos> beds = Nearby.all(
                        player,
                        "bed_sleeper",
                        state -> state.getBlock() instanceof BedBlock
                                && state.getValue(BedBlock.PART) == BedPart.HEAD
                                && !state.getValue(BedBlock.OCCUPIED))
                .stream()
                .filter(pos ->
                        level.isEmptyBlock(pos.above()) && player.distanceToSqr(Vec3.atCenterOf(pos)) >= min * min)
                .toList();
        return beds.isEmpty() ? null : beds.get(player.getRandom().nextInt(beds.size()));
    }

    /** On the bed block it was put on: asleep, as the game draws a sleeper, without marking the bed taken. */
    private static void lieDown(Mannequin figure) {
        BlockPos bed = figure.blockPosition();
        figure.noPhysics = true;
        figure.setDeltaMovement(Vec3.ZERO);
        figure.setPose(Pose.SLEEPING);
        figure.setSleepingPos(bed);
        figure.setPos(bed.getX() + 0.5, bed.getY() + BED_HEIGHT, bed.getZ() + 0.5);
    }

    /** Where a sleeper's feet are over the bed block's floor, as vanilla puts them. */
    private static final double BED_HEIGHT = 0.6875;

    // 389
    private static @Nullable BlockPos graveSpot(ServerPlayer player) {
        Config.Section config = Config.event("grave_figure");
        ServerLevel level = player.level();
        GlobalPos death = player.getLastDeathLocation().orElse(null);
        if (death == null || death.dimension() != level.dimension() || !level.isLoaded(death.pos())) {
            return null;
        }
        double away = Math.hypot(death.pos().getX() - player.getX(), death.pos().getZ() - player.getZ());
        return away >= config.num("min_distance") && away <= config.num("max_distance")
                ? Place.standing(level, death.pos())
                : null;
    }

    // 374
    /** Floor spots along a dark bearing from the player, a tunnel's worth: none unless enough of them are found. */
    private static List<BlockPos> torches(ServerPlayer player) {
        Config.Section config = Config.event("cave_torches");
        ServerLevel level = player.level();
        List<BlockPos> best = List.of();
        for (int i = 0; i < config.integer("tries"); i++) {
            double yaw = player.getRandom().nextDouble() * Math.PI * 2;
            List<BlockPos> line = new ArrayList<>();
            for (double d = config.num("min_distance"); d <= config.num("max_distance"); d += config.num("spacing")) {
                BlockPos stand = Place.standing(
                        level,
                        BlockPos.containing(
                                player.getX() - Math.sin(yaw) * d, player.getY(), player.getZ() + Math.cos(yaw) * d));
                if (stand != null
                        && !level.canSeeSky(stand)
                        && level.getMaxLocalRawBrightness(stand) <= config.integer("max_light")) {
                    line.add(stand);
                }
            }
            if (line.size() > best.size()) {
                best = line;
            }
        }
        return best.size() >= config.integer("min_count") ? best : List.of();
    }

    // 390
    /** Players with a waking waited for. */
    private static final Set<UUID> WAITING = Session.SERVER.keep(ConcurrentHashMap.newKeySet());

    private static void bedside(ServerPlayer player) {
        Config.Section config = Config.event("bedside_figure");
        UUID owner = player.getUUID();
        WAITING.add(owner);
        int interval = config.integer("interval_ticks");
        BlockPos[] bed = {null};
        // The scheduler drops the wait if the player dies or leaves; this frees the player then.
        Restore.after(player.level(), config.integer("wait_ticks") + config.integer("cleanup_ticks"), force -> {
            WAITING.remove(owner);
            return true;
        });
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            if (p.isSleeping()) {
                p.getSleepingPos().ifPresent(pos -> bed[0] = pos);
            } else if (bed[0] != null) {
                appear(p, bed[0], config);
                WAITING.remove(owner);
                return false;
            }
            boolean over = n * interval >= config.integer("wait_ticks");
            if (over) {
                WAITING.remove(owner);
            }
            return !over;
        });
    }

    private static void appear(ServerPlayer player, BlockPos bed, Config.Section config) {
        ServerLevel level = player.level();
        BlockPos spot = Place.near(player, bed, 1, config.integer("reach"), pos -> Place.floored(level, pos));
        if (spot == null) {
            return;
        }
        Mannequin figure = Figure.spawn(player, spot, Figure.stranger(player));
        if (figure != null) {
            Restore.after(level, config.integer("stay_ticks"), force -> {
                if (!force && figure.isAlive()) {
                    Haze.linger(player, figure.position(), LINGER_TICKS);
                }
                figure.discard();
                return true;
            });
        }
    }

    // --- chat ------------------------------------------------------------------------------------------------------

    /** A chat-style line from the {@code key} list of {@code id}'s config, said once each before the event rests. */
    private static HauntEvent saying(String id, String key, BiFunction<ServerPlayer, String, Component> line) {
        return new SimpleEvent(
                Identifier.parse(id),
                HauntTier.LIGHT,
                Set.of(EventTag.PRESENCE, EventTag.PLAYER),
                player -> Said.left(player, id, Config.event(id).texts(key)),
                player -> {
                    String said = Said.pick(player, id, Config.event(id).texts(key));
                    if (said != null) {
                        player.sendSystemMessage(line.apply(player, said));
                    }
                });
    }

    // --- walkers ---------------------------------------------------------------------------------------------------

    /**
     * A figure that crosses the player's view by hand, as {@code distant_walker} does, but in a skin of its own
     * choosing and, if it sprints, only where it is dark.
     */
    private static final class Runner implements HauntEvent {
        private final String id;
        private final Function<ServerPlayer, ResolvableProfile> skin;
        private final boolean dark;
        private final Map<UUID, Mannequin> active = new ConcurrentHashMap<>();

        Runner(String id, Function<ServerPlayer, ResolvableProfile> skin, boolean dark) {
            this.id = id;
            this.skin = skin;
            this.dark = dark;
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
            return Set.of(EventTag.PRESENCE, EventTag.PLAYER);
        }

        @Override
        public boolean busy(ServerPlayer player) {
            return active.containsKey(player.getUUID());
        }

        @Override
        public boolean canRun(ServerPlayer player) {
            return !active.containsKey(player.getUUID())
                    && (!dark
                            || player.level().getMaxLocalRawBrightness(player.blockPosition())
                                    <= Config.event(id).integer("max_light"))
                    && spot(player) != null;
        }

        private @Nullable BlockPos spot(ServerPlayer player) {
            Config.Section config = Config.event(id);
            return Place.standingAhead(
                    player, config.num("distance"), config.num("min_angle"), config.num("max_angle"));
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
            float heading =
                    Mth.wrapDegrees(figure.getYRot() + (player.getRandom().nextBoolean() ? 90.0F : -90.0F));
            Gait gait = new Gait(config.integer("climb"), config.integer("drop"));
            double stride = config.num("stride");
            double near = config.num("vanish_distance");
            int interval = config.integer("interval_ticks");
            figure.setSprinting(dark);
            // The run ends itself; this catches the cases where the scheduler drops it (the player died or left).
            Restore.after(player.level(), config.integer("max_ticks") + config.integer("cleanup_ticks"), force -> {
                leave(figure, owner);
                return true;
            });
            Scheduler.repeat(player, 0, interval, (p, n) -> {
                if (dark && p.getRandom().nextDouble() < config.num("hop_chance")) {
                    gait.hop();
                }
                boolean over = !figure.isAlive()
                        || figure.level() != p.level()
                        || n * interval >= config.integer("max_ticks")
                        || p.distanceToSqr(figure) <= near * near
                        || !gait.along(p.level(), figure, heading, stride);
                if (over) {
                    leave(figure, owner);
                } else {
                    Figure.turn(figure, gait.heading());
                }
                return !over;
            });
        }

        private void leave(Mannequin figure, UUID owner) {
            figure.discard();
            active.remove(owner, figure);
        }
    }
}
