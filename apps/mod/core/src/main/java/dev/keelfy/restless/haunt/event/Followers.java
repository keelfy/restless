package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.HauntSounds;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.event.kit.Figure;
import dev.keelfy.restless.haunt.event.kit.Gait;
import dev.keelfy.restless.haunt.event.kit.Nearby;
import dev.keelfy.restless.haunt.event.kit.PacketEntity;
import dev.keelfy.restless.haunt.event.kit.Place;
import dev.keelfy.restless.state.Session;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The other player, going about it: fishing, standing in a window, following, swimming past, dropping a note, standing
 * on the horizon, sneaking up, falling, and riding a boat or a minecart. Each is a mannequin in a default skin that is
 * gone, silently, when the player comes near or time runs out.
 */
final class Followers {
    private Followers() {}

    private static final Predicate<BlockState> WINDOW =
            state -> state.is(BlockTags.IMPERMEABLE) || state.is(Blocks.GLASS_PANE);
    private static final Predicate<BlockState> WATER =
            state -> state.getFluidState().is(FluidTags.WATER);

    static List<HauntEvent> all() {
        return List.of(
                // 367: fishes at the nearest lake
                new Act("lake_fisher", Followers::shoreSpot, Followers::takeRod, Followers::fish, player -> true),
                // 368: stands in a window, inside the house, while the player is outside
                new Act(
                        "window_figure",
                        Followers::windowSpot,
                        figure -> {},
                        Followers::watch,
                        player -> player.level().canSeeSky(player.blockPosition())),
                // 370: follows far behind, moving only when the player moves
                new Act(
                        "trailing_figure",
                        player -> ahead(player, "trailing_figure"),
                        figure -> {},
                        Followers::trail,
                        player -> true),
                // 376: swims past underwater
                new Act(
                        "deep_swimmer",
                        Followers::swimSpot,
                        Followers::dive,
                        Followers::swim,
                        player -> player.isUnderWater()),
                // 378: drops a torch or a note and runs
                new Act(
                        "note_dropper",
                        player -> ahead(player, "note_dropper"),
                        figure -> {},
                        Followers::dropAndRun,
                        player -> true),
                // 380: six figures on the horizon, gone at once
                new Horizon(),
                // 382: sneaks up behind with a sword, frozen while looked at
                new Act(
                        "blade_stalker",
                        player -> ahead(player, "blade_stalker"),
                        Followers::drawSword,
                        Followers::stalk,
                        player -> true),
                // 386: runs off a cliff, a death cry, no body
                new Act("cliff_faller", Followers::cliffSpot, Followers::runUp, Followers::plunge, player -> true),
                // 383: rides a boat across the lake
                new Act(
                        "lake_ferry",
                        Followers::ferrySpot,
                        figure -> board(figure, EntityTypes.OAK_BOAT, "lake_ferry", -0.1, Followers::bestWater),
                        ride(Followers::sail),
                        player -> true),
                // 416: rides a minecart through the cave
                new Act(
                        "cave_cart",
                        Followers::caveSpot,
                        figure -> board(figure, EntityTypes.MINECART, "cave_cart", 0.0, (f, c) -> side(f)),
                        ride(Followers::roll),
                        player -> !player.level().canSeeSky(player.blockPosition())
                                && player.getBlockY()
                                        < Config.event("cave_cart").integer("max_y")));
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

        Act(String id, Spot spot, Consumer<Mannequin> dress, Move move, Predicate<ServerPlayer> where) {
            super(id);
            this.spot = spot;
            this.dress = dress;
            this.move = move;
            this.where = where;
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

    /** Per-figure state of the acts that walk, kept only while the figure lives. */
    private static final Map<Mannequin, Gait> GAITS =
            Session.SERVER.keep(Collections.synchronizedMap(new WeakHashMap<>()));
    /** The yaw a figure travels along, in degrees as entities face. */
    private static final Map<Mannequin, Float> HEADINGS =
            Session.SERVER.keep(Collections.synchronizedMap(new WeakHashMap<>()));

    private static Gait gait(Mannequin figure, Config.Section config) {
        return GAITS.computeIfAbsent(figure, f -> new Gait(config.integer("climb"), config.integer("drop")));
    }

    /** A quarter turn, to either side, from facing the player. */
    private static float side(Mannequin figure) {
        return Mth.wrapDegrees(figure.getYRot() + (figure.getRandom().nextBoolean() ? 90.0F : -90.0F));
    }

    /** The player looks within {@code degrees} of the entity, with a clear line to it. */
    private static boolean looked(ServerPlayer player, Entity entity, double degrees) {
        Vec3 to = entity.getEyePosition().subtract(player.getEyePosition()).normalize();
        return player.getLookAngle().dot(to) >= Math.cos(Math.toRadians(degrees)) && player.hasLineOfSight(entity);
    }

    private static boolean watch(ServerPlayer player, Mannequin figure, Config.Section config, int n) {
        Figure.face(figure, player);
        return true;
    }

    /** Standing room {@code distance} blocks away within the event's angles of where the player looks. */
    private static @Nullable BlockPos ahead(ServerPlayer player, String id) {
        Config.Section config = Config.event(id);
        return Place.standingAhead(player, config.num("distance"), config.num("min_angle"), config.num("max_angle"));
    }

    // 367
    private static void takeRod(Mannequin figure) {
        figure.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
    }

    /** Water sources with air above and water on at least three sides for {@code lake_span} blocks. */
    private static List<BlockPos> lakes(ServerPlayer player, String id) {
        ServerLevel level = player.level();
        int span = Config.event(id).integer("lake_span");
        return Nearby.all(player, id, WATER).stream()
                .filter(pos -> level.isEmptyBlock(pos.above())
                        && level.getFluidState(pos).isSource()
                        && wide(level, pos, span))
                .toList();
    }

    private static boolean wide(ServerLevel level, BlockPos water, int span) {
        int sides = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos far = water.relative(d, span);
            if (level.isLoaded(far)
                    && level.getFluidState(far).is(FluidTags.WATER)
                    && level.isEmptyBlock(far.above())) {
                sides++;
            }
        }
        return sides >= 3;
    }

    private static @Nullable BlockPos shoreSpot(ServerPlayer player) {
        Config.Section config = Config.event("lake_fisher");
        ServerLevel level = player.level();
        double min = config.num("min_distance");
        List<BlockPos> lakes = new ArrayList<>(lakes(player, "lake_fisher"));
        Collections.shuffle(lakes, new Random(player.getRandom().nextLong()));
        for (BlockPos water : lakes.subList(0, Math.min(lakes.size(), config.integer("tries")))) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos bank = water.relative(d);
                for (int dy = 1; dy >= 0; dy--) {
                    BlockPos stand = bank.above(dy);
                    if (Place.floored(level, stand)
                            && level.isEmptyBlock(stand.above())
                            && player.distanceToSqr(Vec3.atBottomCenterOf(stand)) >= min * min) {
                        return stand;
                    }
                }
            }
        }
        return null;
    }

    private static boolean fish(ServerPlayer player, Mannequin figure, Config.Section config, int n) {
        ServerLevel level = player.level();
        BlockPos water = Nearby.all(level, figure.blockPosition(), config.integer("reach"), WATER).stream()
                .filter(pos -> level.isEmptyBlock(pos.above()) && pos.distToCenterSqr(figure.position()) >= 9.0)
                .min(Comparator.comparingDouble(pos -> pos.distToCenterSqr(figure.position())))
                .orElse(null);
        if (water == null) {
            return false;
        }
        Vec3 bobber = new Vec3(water.getX() + 0.5, water.getY() + 1.0, water.getZ() + 0.5);
        figure.lookAt(EntityAnchorArgument.Anchor.EYES, bobber);
        Config.Tone tone = config.tone("sound");
        if (n == 0) {
            figure.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
            level.playSound(
                    null,
                    bobber.x,
                    bobber.y,
                    bobber.z,
                    SoundEvents.FISHING_BOBBER_THROW,
                    SoundSource.NEUTRAL,
                    tone.volume(),
                    tone.pitch());
        }
        level.sendParticles(ParticleTypes.FISHING, bobber.x, bobber.y, bobber.z, 4, 0.15, 0.0, 0.15, 0.02);
        if (n > 0 && n % config.integer("bite_every") == 0) {
            figure.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
            level.playSound(
                    null,
                    bobber.x,
                    bobber.y,
                    bobber.z,
                    SoundEvents.FISHING_BOBBER_SPLASH,
                    SoundSource.NEUTRAL,
                    tone.volume(),
                    tone.pitch());
        }
        return true;
    }

    // 368
    private static @Nullable BlockPos windowSpot(ServerPlayer player) {
        Config.Section config = Config.event("window_figure");
        ServerLevel level = player.level();
        double min = config.num("min_distance");
        List<BlockPos> windows = new ArrayList<>(Nearby.all(player, "window_figure", WINDOW));
        Collections.shuffle(windows, new Random(player.getRandom().nextLong()));
        for (BlockPos window : windows) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos outside = window.relative(d);
                BlockPos inside = window.relative(d.getOpposite());
                if (!level.isEmptyBlock(outside)
                        || player.distanceToSqr(Vec3.atCenterOf(outside))
                                >= player.distanceToSqr(Vec3.atCenterOf(inside))) {
                    continue;
                }
                for (int dy = 1; dy >= 0; dy--) {
                    BlockPos stand = inside.below(dy);
                    if (Place.floored(level, stand)
                            && level.isEmptyBlock(stand.above())
                            && !level.canSeeSky(stand)
                            && player.distanceToSqr(Vec3.atBottomCenterOf(stand)) >= min * min) {
                        return stand;
                    }
                }
            }
        }
        return null;
    }

    // 370
    private static final class Trail {
        @Nullable Vec3 last;
    }

    private static final Map<Mannequin, Trail> TRAILS =
            Session.SERVER.keep(Collections.synchronizedMap(new WeakHashMap<>()));

    private static boolean trail(ServerPlayer player, Mannequin figure, Config.Section config, int n) {
        if (looked(player, figure, config.num("vanish_degrees"))) {
            return false;
        }
        Trail trail = TRAILS.computeIfAbsent(figure, f -> new Trail());
        Vec3 was = trail.last;
        trail.last = player.position();
        if (was != null) {
            double moved = Math.hypot(player.getX() - was.x, player.getZ() - was.z);
            if (moved > config.num("min_move")) {
                gait(figure, config)
                        .toward(player.level(), figure, player.position(), Math.min(moved, config.num("max_stride")));
            }
        }
        figure.setSprinting(player.isSprinting());
        Figure.face(figure, player);
        return true;
    }

    // 376
    private static @Nullable BlockPos swimSpot(ServerPlayer player) {
        Config.Section config = Config.event("deep_swimmer");
        ServerLevel level = player.level();
        for (int i = 0; i < config.integer("tries"); i++) {
            double angle = config.num("min_angle")
                    + player.getRandom().nextDouble() * (config.num("max_angle") - config.num("min_angle"));
            Vec3 at = Nearby.around(
                    player, config.num("distance"), player.getRandom().nextBoolean() ? angle : -angle);
            BlockPos pos = BlockPos.containing(at.x, player.getEyeY() - 0.3, at.z);
            if (level.isLoaded(pos)
                    && level.getFluidState(pos).is(FluidTags.WATER)
                    && level.getFluidState(pos.above()).is(FluidTags.WATER)) {
                return pos;
            }
        }
        return null;
    }

    private static void dive(Mannequin figure) {
        float heading = side(figure);
        HEADINGS.put(figure, heading);
        figure.setPose(Pose.SWIMMING);
        Figure.turn(figure, heading);
    }

    private static boolean swim(ServerPlayer player, Mannequin figure, Config.Section config, int n) {
        ServerLevel level = player.level();
        float heading = HEADINGS.getOrDefault(figure, figure.getYRot());
        double radians = Math.toRadians(heading);
        double stride = config.num("stride");
        double x = figure.getX() - Math.sin(radians) * stride;
        double z = figure.getZ() + Math.cos(radians) * stride;
        BlockPos at = BlockPos.containing(x, figure.getY() + 0.3, z);
        if (!level.isLoaded(at) || !level.getFluidState(at).is(FluidTags.WATER)) {
            return false;
        }
        figure.setPos(x, figure.getY(), z);
        Figure.turn(figure, heading);
        if (n % 3 == 0) {
            level.sendParticles(ParticleTypes.BUBBLE, x, figure.getY() + 0.3, z, 2, 0.2, 0.1, 0.2, 0.0);
        }
        return true;
    }

    // 378
    private static boolean dropAndRun(ServerPlayer player, Mannequin figure, Config.Section config, int n) {
        int pause = config.integer("pause_steps");
        if (n < pause) {
            Figure.face(figure, player);
            return true;
        }
        ServerLevel level = player.level();
        if (n == pause) {
            ItemStack stack = new ItemStack(Items.TORCH);
            List<String> notes = config.texts("notes");
            if (!notes.isEmpty() && player.getRandom().nextBoolean()) {
                stack = new ItemStack(Items.PAPER);
                stack.set(
                        DataComponents.CUSTOM_NAME,
                        Component.literal(notes.get(player.getRandom().nextInt(notes.size()))));
            }
            ItemEntity item = new ItemEntity(level, figure.getX(), figure.getY() + 0.5, figure.getZ(), stack);
            item.setDefaultPickUpDelay();
            level.addFreshEntity(item);
            figure.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
            figure.setSprinting(true);
        }
        float away = PacketEntity.yawToward(player.position(), figure.position());
        Gait gait = gait(figure, config);
        if (!gait.along(level, figure, away, config.num("stride"))) {
            return false;
        }
        Figure.turn(figure, gait.heading());
        return true;
    }

    // 380
    /** Several figures far off across the player's view, all facing them, gone together when one is looked at. */
    private static final class Horizon implements HauntEvent {
        private static final String ID = "horizon_choir";
        private final Map<UUID, List<Mannequin>> active = new ConcurrentHashMap<>();

        @Override
        public Identifier id() {
            return Identifier.parse(ID);
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
                    && player.level().canSeeSky(player.blockPosition())
                    && row(player).size() >= Config.event(ID).integer("min_count");
        }

        private List<BlockPos> row(ServerPlayer player) {
            Config.Section config = Config.event(ID);
            ServerLevel level = player.level();
            int count = config.integer("count");
            double spread = config.num("spread_angle");
            List<BlockPos> spots = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                double angle = count == 1 ? 0.0 : -spread + 2.0 * spread * i / (count - 1);
                double distance = config.num("distance") + player.getRandom().nextDouble() * config.num("jitter");
                Vec3 at = Nearby.around(player, distance, angle);
                BlockPos column = BlockPos.containing(at.x, player.getY(), at.z);
                if (!level.isLoaded(column)) {
                    continue;
                }
                BlockPos stand =
                        Place.standing(level, level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column));
                if (stand != null) {
                    spots.add(stand);
                }
            }
            return spots;
        }

        @Override
        public void run(ServerPlayer player) {
            Config.Section config = Config.event(ID);
            List<Mannequin> figures = new ArrayList<>();
            for (BlockPos spot : row(player)) {
                Mannequin figure = Figure.spawn(player, spot, Figure.stranger(player));
                if (figure != null) {
                    figures.add(figure);
                }
            }
            UUID owner = player.getUUID();
            if (figures.size() < config.integer("min_count")) {
                figures.forEach(Entity::discard);
                return;
            }
            active.put(owner, figures);
            int interval = config.integer("interval_ticks");
            // The row ends itself; this catches the cases where the scheduler drops it (the player died or left).
            Restore.after(player.level(), config.integer("max_ticks") + config.integer("cleanup_ticks"), force -> {
                leave(figures, owner);
                return true;
            });
            Scheduler.repeat(player, interval, interval, (p, n) -> {
                double near = config.num("vanish_distance");
                boolean over = n * interval >= config.integer("max_ticks")
                        || figures.stream()
                                .anyMatch(f -> !f.isAlive()
                                        || f.level() != p.level()
                                        || p.distanceToSqr(f) <= near * near
                                        || looked(p, f, config.num("vanish_degrees")));
                if (over) {
                    leave(figures, owner);
                } else {
                    figures.forEach(f -> Figure.face(f, p));
                }
                return !over;
            });
        }

        private void leave(List<Mannequin> figures, UUID owner) {
            figures.forEach(Entity::discard);
            active.remove(owner, figures);
        }
    }

    // 382
    private static final Map<Mannequin, int[]> STARES =
            Session.SERVER.keep(Collections.synchronizedMap(new WeakHashMap<>()));

    private static void drawSword(Mannequin figure) {
        figure.setPose(Pose.CROUCHING);
        figure.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
    }

    private static boolean stalk(ServerPlayer player, Mannequin figure, Config.Section config, int n) {
        Figure.face(figure, player);
        if (looked(player, figure, config.num("look_degrees"))) {
            // Frozen while seen, and gone once it has been seen long enough.
            int[] seen = STARES.computeIfAbsent(figure, f -> new int[1]);
            return ++seen[0] * config.integer("interval_ticks") < config.integer("hold_ticks");
        }
        gait(figure, config).toward(player.level(), figure, player.position(), config.num("stride"));
        return true;
    }

    // 386
    /** A walk toward a drop, then the fall. */
    private static final class Fall {
        final float yaw;
        boolean falling;
        double speed;
        double fallen;

        Fall(float yaw) {
            this.yaw = yaw;
        }
    }

    private static final Map<Mannequin, Fall> FALLS =
            Session.SERVER.keep(Collections.synchronizedMap(new WeakHashMap<>()));

    /** A direction in which flat ground ends in a drop of the configured depth onto solid ground. */
    private static @Nullable Direction cliff(ServerLevel level, BlockPos base, Config.Section config) {
        int maxDrop = config.integer("max_drop");
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int k = 1; k <= config.integer("run_up"); k++) {
                BlockPos c = base.relative(d, k);
                if (!level.isLoaded(c) || !Place.open(level, c) || !Place.open(level, c.above())) {
                    break;
                }
                if (level.getBlockState(c.below()).isSolidRender()) {
                    continue;
                }
                int depth = 0;
                BlockPos below = c.below();
                while (depth <= maxDrop && level.isEmptyBlock(below)) {
                    depth++;
                    below = below.below();
                }
                if (depth >= config.integer("min_drop")
                        && depth <= maxDrop
                        && level.isLoaded(below)
                        && level.getBlockState(below).isSolidRender()) {
                    return d;
                }
                break;
            }
        }
        return null;
    }

    private static @Nullable BlockPos cliffSpot(ServerPlayer player) {
        Config.Section config = Config.event("cliff_faller");
        for (int i = 0; i < config.integer("tries"); i++) {
            BlockPos base = Place.spawnSpot(player, config.integer("min_range"), config.integer("max_range"));
            if (base != null && cliff(player.level(), base, config) != null) {
                return base;
            }
        }
        return null;
    }

    private static void runUp(Mannequin figure) {
        Direction d = cliff((ServerLevel) figure.level(), figure.blockPosition(), Config.event("cliff_faller"));
        float yaw = d == null ? figure.getYRot() : d.toYRot();
        FALLS.put(figure, new Fall(yaw));
        Figure.turn(figure, yaw);
        figure.setSprinting(true);
    }

    private static boolean plunge(ServerPlayer player, Mannequin figure, Config.Section config, int n) {
        Fall fall = FALLS.get(figure);
        if (fall == null) {
            return false;
        }
        ServerLevel level = player.level();
        double radians = Math.toRadians(fall.yaw);
        double stride = config.num("stride") * (fall.falling ? 0.5 : 1.0);
        double x = figure.getX() - Math.sin(radians) * stride;
        double z = figure.getZ() + Math.cos(radians) * stride;
        if (!fall.falling) {
            fall.falling = !level.getBlockState(BlockPos.containing(x, figure.getY() - 0.5, z))
                    .isSolidRender();
            figure.setPos(x, figure.getY(), z);
            return true;
        }
        fall.speed = (fall.speed + 0.08) * 0.98;
        fall.fallen += fall.speed;
        figure.setPos(x, figure.getY() - fall.speed, z);
        if (fall.fallen < config.num("cry_after")) {
            return level.getBlockState(BlockPos.containing(x, figure.getY() - 0.5, z))
                    .isAir();
        }
        Config.Tone tone = config.tone("sound");
        level.playSound(
                null,
                x,
                figure.getY(),
                z,
                HauntSounds.CUT_OFF_SCREAM,
                SoundSource.PLAYERS,
                tone.volume(),
                tone.pitch());
        return false;
    }

    // 383, 416
    /** The vehicle each riding figure sits in; it is removed with the figure. */
    private static final Map<Mannequin, Entity> VEHICLES = Session.SERVER.keep(new ConcurrentHashMap<>());

    private interface Heading {
        float pick(Mannequin figure, Config.Section config);
    }

    /** Puts a vehicle under the figure and sets it riding; with no vehicle, the figure is dropped at once. */
    private static void board(Mannequin figure, EntityType<?> type, String id, double lift, Heading heading) {
        ServerLevel level = (ServerLevel) figure.level();
        Config.Section config = Config.event(id);
        Entity vehicle = type.create(level, EntitySpawnReason.EVENT);
        if (vehicle == null) {
            figure.discard();
            return;
        }
        vehicle.setPos(figure.getX(), figure.getY() + lift, figure.getZ());
        vehicle.setNoGravity(true);
        vehicle.noPhysics = true;
        vehicle.setPermanentlyInvulnerable(true);
        vehicle.setSilent(true);
        float yaw = heading.pick(figure, config);
        vehicle.setYRot(yaw);
        level.addFreshEntity(vehicle);
        if (!figure.startRiding(vehicle, true, false)) {
            vehicle.discard();
            figure.discard();
            return;
        }
        HEADINGS.put(figure, yaw);
        Figure.turn(figure, yaw);
        VEHICLES.put(figure, vehicle);
        // Backstop: whatever ends the figure, the vehicle goes within a second or at the latest with the act.
        long end = level.getServer().getTickCount() + config.integer("max_ticks") + config.integer("cleanup_ticks");
        Restore.after(level, config.integer("interval_ticks"), force -> {
            if (!force && figure.isAlive() && level.getServer().getTickCount() < end) {
                return false;
            }
            VEHICLES.remove(figure);
            vehicle.discard();
            return true;
        });
    }

    /** The figure's step turned into the vehicle's: the vehicle goes when the player is near or the ride is over. */
    private static Move ride(Move along) {
        return (player, figure, config, n) -> {
            Entity vehicle = VEHICLES.get(figure);
            double near = config.num("near_distance");
            boolean go = vehicle != null
                    && vehicle.isAlive()
                    && player.distanceToSqr(vehicle) > near * near
                    && along.step(player, figure, config, n);
            if (!go && vehicle != null) {
                VEHICLES.remove(figure);
                vehicle.discard();
            }
            return go;
        };
    }

    // 383
    private static @Nullable BlockPos ferrySpot(ServerPlayer player) {
        Config.Section config = Config.event("lake_ferry");
        double min = config.num("min_distance");
        List<BlockPos> lakes = lakes(player, "lake_ferry").stream()
                .filter(pos -> player.distanceToSqr(Vec3.atCenterOf(pos)) >= min * min)
                .toList();
        return lakes.isEmpty()
                ? null
                : lakes.get(player.getRandom().nextInt(lakes.size())).above();
    }

    /** Of the two ways across the player's view, the one with more open water before the shore. */
    private static float bestWater(Mannequin figure, Config.Section config) {
        float left = side(figure);
        float right = Mth.wrapDegrees(left + 180.0F);
        return crossing(figure, left, config) >= crossing(figure, right, config) ? left : right;
    }

    private static int crossing(Mannequin figure, float yaw, Config.Section config) {
        ServerLevel level = (ServerLevel) figure.level();
        double radians = Math.toRadians(yaw);
        int span = 0;
        for (int i = 1; i <= config.integer("look_ahead"); i++) {
            BlockPos at = BlockPos.containing(
                    figure.getX() - Math.sin(radians) * i, figure.getY(), figure.getZ() + Math.cos(radians) * i);
            if (!level.isLoaded(at)
                    || !level.getFluidState(at.below()).is(FluidTags.WATER)
                    || !level.isEmptyBlock(at)) {
                break;
            }
            span = i;
        }
        return span;
    }

    private static boolean sail(ServerPlayer player, Mannequin figure, Config.Section config, int n) {
        Entity boat = VEHICLES.get(figure);
        ServerLevel level = player.level();
        float heading = HEADINGS.getOrDefault(figure, figure.getYRot());
        double radians = Math.toRadians(heading);
        double x = boat.getX() - Math.sin(radians) * config.num("stride");
        double z = boat.getZ() + Math.cos(radians) * config.num("stride");
        BlockPos at = BlockPos.containing(x, boat.getY() + 0.5, z);
        if (!level.isLoaded(at) || !level.getFluidState(at.below()).is(FluidTags.WATER) || !level.isEmptyBlock(at)) {
            return false;
        }
        boat.setPos(x, boat.getY(), z);
        boat.setYRot(heading);
        boat.setDeltaMovement(Vec3.ZERO);
        Figure.turn(figure, heading);
        if (n % config.integer("sound_every") == 0) {
            Config.Tone tone = config.tone("sound");
            level.playSound(
                    null,
                    x,
                    boat.getY(),
                    z,
                    SoundEvents.BOAT_PADDLE_WATER,
                    SoundSource.NEUTRAL,
                    tone.volume(),
                    tone.pitch());
        }
        return true;
    }

    // 416
    private static @Nullable BlockPos caveSpot(ServerPlayer player) {
        ServerLevel level = player.level();
        Config.Section config = Config.event("cave_cart");
        return Place.standingAhead(
                player,
                config.num("distance"),
                config.num("min_angle"),
                config.num("max_angle"),
                pos -> !level.canSeeSky(pos));
    }

    private static boolean roll(ServerPlayer player, Mannequin figure, Config.Section config, int n) {
        Entity cart = VEHICLES.get(figure);
        ServerLevel level = player.level();
        Gait gait = gait(figure, config);
        if (!gait.along(level, cart, HEADINGS.getOrDefault(figure, figure.getYRot()), config.num("stride"))) {
            return false;
        }
        cart.setYRot(gait.heading());
        cart.setDeltaMovement(Vec3.ZERO);
        Figure.turn(figure, gait.heading());
        if (n % config.integer("sound_every") == 0) {
            Config.Tone tone = config.tone("sound");
            level.playSound(
                    null,
                    cart.getX(),
                    cart.getY(),
                    cart.getZ(),
                    SoundEvents.MINECART_RIDING,
                    SoundSource.NEUTRAL,
                    tone.volume(),
                    tone.pitch());
        }
        return true;
    }
}
