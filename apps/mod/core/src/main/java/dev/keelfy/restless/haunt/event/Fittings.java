package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.block.Decal;
import dev.keelfy.restless.content.block.Prop;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntSounds;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The house itself getting at the player: frost, claw marks and a cracked mirror laid on glass, doors and walls as
 * {@link Decal}s, small props set down out of view (a doll, a music box, a rocking chair), planks that rot, a candle
 * that lights. Each is a block or a block change that is taken away again; under hardcore the decals and swaps stay.
 */
final class Fittings {
    private static final String FROST_PRINTS = "restless:frost_prints";
    private static final String FROST_WRITING = "restless:frost_writing";
    private static final String CLAW_MARKS = "restless:claw_marks";
    private static final String DOLL = "restless:doll";
    private static final String MUSIC_BOX = "restless:music_box";
    private static final String ROCKING_CHAIR = "restless:rocking_chair";
    private static final String ROTTEN_PLANKS = "restless:rotten_planks";
    private static final String BLACK_CANDLE = "black_candle";
    private static final String CRACKED_MIRROR = "restless:cracked_mirror";
    /** The chair leans forward, comes level, leans back, comes level. */
    private static final int[] ROCK = {1, 0, 2, 0};

    private Fittings() {}

    /** A free block in front of a surface: which way the surface lies and how far behind the block's edge. */
    private record Surface(BlockPos cell, Direction toward, Decal.Depth depth) {}

    /** One word of frost writing and where it goes. */
    private record Placed(Surface where, Decal.Writing.Word word) {}

    static List<HauntEvent> all() {
        return List.of(
                // 502: hands pressed to the glass from the outside
                SimpleEvent.light(
                        FROST_PRINTS,
                        EventTag.PRESENCE,
                        player -> Household.indoors(player)
                                && !windows(player, FROST_PRINTS, false).isEmpty(),
                        Fittings::printHands),
                // 503: words in the frost on the inside of the glass, read from where the player stands
                SimpleEvent.light(
                        FROST_WRITING,
                        EventTag.PRESENCE,
                        player -> Household.indoors(player) && !writing(player).isEmpty(),
                        Fittings::writeInFrost),
                // 504: scratches on the side of a closed door that faces the player
                SimpleEvent.light(
                        CLAW_MARKS,
                        EventTag.DOORS,
                        player -> Household.indoors(player)
                                && !doors(player, CLAW_MARKS).isEmpty(),
                        Fittings::scratchDoor),
                // 505: a doll that is somewhere else every time the player looks away, and faces them
                SimpleEvent.light(
                        DOLL,
                        EventTag.PRESENCE,
                        player -> !floorSpots(player, DOLL).isEmpty(),
                        Fittings::wanderDoll),
                // 506: a music box plays a tune on vanilla bell sounds (no sound file of its own)
                SimpleEvent.light(
                        MUSIC_BOX,
                        EventTag.SOUND,
                        player -> !floorSpots(player, MUSIC_BOX).isEmpty(),
                        Fittings::playMusicBox),
                // 507: a rocking chair rocks by itself, out of view
                SimpleEvent.light(
                        ROCKING_CHAIR,
                        EventTag.BLOCKS,
                        player -> !floorSpots(player, ROCKING_CHAIR).isEmpty(),
                        Fittings::rockChair),
                // 508: planks go soft one next to another, then are put back (stay under hardcore)
                SimpleEvent.light(
                        ROTTEN_PLANKS,
                        EventTag.BLOCKS,
                        player -> Household.indoors(player)
                                && Nearby.any(player, ROTTEN_PLANKS, s -> s.is(BlockTags.PLANKS)),
                        Fittings::rotPlanks),
                // 509: a black candle burns with a soul flame; a candle nearby turns black, else one is set down
                SimpleEvent.light(
                        BLACK_CANDLE,
                        EventTag.LIGHTS,
                        player -> Household.indoors(player)
                                && (!unlitCandles(player).isEmpty()
                                        || !floorSpots(player, BLACK_CANDLE).isEmpty()),
                        Fittings::lightBlackCandle),
                // 510: a cracked mirror on a wall; when the player looks into it someone stands behind them
                SimpleEvent.light(
                        CRACKED_MIRROR,
                        EventTag.BLOCKS,
                        player -> Household.indoors(player) && !walls(player).isEmpty(),
                        Fittings::hangMirror));
    }

    // Shared

    private static Random random(ServerPlayer player) {
        return new Random(player.getRandom().nextLong());
    }

    private static boolean watched(ServerPlayer player, BlockPos pos, double degrees) {
        Vec3 to = Vec3.atCenterOf(pos).subtract(player.getEyePosition()).normalize();
        return player.getLookAngle().dot(to) >= Math.cos(Math.toRadians(degrees));
    }

    /** A roof overhead, read off the heightmap since sky light lags. */
    private static boolean roofed(ServerLevel level, BlockPos pos) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) > pos.getY() + 1;
    }

    private static void blip(ServerLevel level, BlockPos pos, SoundEvent sound, Config.Tone tone) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, tone.volume(), tone.pitch());
    }

    private static Direction toward(ServerPlayer player, BlockPos pos) {
        return Direction.getApproximateNearest(player.getX() - pos.getX() - 0.5, 0.0, player.getZ() - pos.getZ() - 0.5);
    }

    private static BlockState decal(Block block, Surface surface) {
        return block.defaultBlockState()
                .setValue(Decal.FACING, surface.toward())
                .setValue(Decal.DEPTH, surface.depth());
    }

    /** Sets a block in a free spot and takes it away again after {@code ticks}; under hardcore it stays. */
    private static void lay(ServerLevel level, BlockPos cell, BlockState state, int ticks) {
        BlockPos at = cell.immutable();
        if (!level.isEmptyBlock(at)) {
            return;
        }
        level.setBlock(at, state, Block.UPDATE_ALL);
        Restore.revert(level, ticks, force -> {
            if (!force && !level.isLoaded(at)) {
                return false;
            }
            vanish(level, at, state.getBlock());
            return true;
        });
    }

    private static void vanish(ServerLevel level, @Nullable BlockPos pos, Block block) {
        if (pos != null && level.getBlockState(pos).is(block)) {
            level.removeBlock(pos, false);
        }
    }

    /** Takes a prop away after {@code ticks} (it may have moved meanwhile) or at shutdown. */
    private static void clear(ServerLevel level, int ticks, Supplier<@Nullable BlockPos> at, Block block) {
        Restore.after(level, ticks, force -> {
            BlockPos pos = at.get();
            if (pos != null && !force && !level.isLoaded(pos)) {
                return false;
            }
            vanish(level, pos, block);
            return true;
        });
    }

    /** Free floor away from the player and outside their view, under a roof, for a prop to stand on. */
    private static List<BlockPos> floorSpots(ServerPlayer player, String event) {
        Config.Section config = Config.event(event);
        ServerLevel level = player.level();
        double min = config.num("min_distance");
        double degrees = config.num("watch_degrees");
        return Nearby.all(player, event, BlockState::isAir).stream()
                .filter(pos -> Place.floored(level, pos)
                        && roofed(level, pos)
                        && Vec3.atCenterOf(pos).distanceToSqr(player.position()) >= min * min
                        && !watched(player, pos, degrees))
                .toList();
    }

    // 502, 503: glass

    /** The faces of a glass block or pane that a flat picture can lie against. */
    private static List<Direction> faces(BlockState state) {
        if (!(state.getBlock() instanceof IronBarsBlock)) {
            return Direction.Plane.HORIZONTAL.stream().toList();
        }
        boolean northSouth = state.getValue(IronBarsBlock.NORTH) || state.getValue(IronBarsBlock.SOUTH);
        boolean eastWest = state.getValue(IronBarsBlock.EAST) || state.getValue(IronBarsBlock.WEST);
        if (northSouth == eastWest) {
            return List.of();
        }
        return northSouth ? List.of(Direction.EAST, Direction.WEST) : List.of(Direction.NORTH, Direction.SOUTH);
    }

    private static boolean playerSide(ServerPlayer player, BlockPos pos, Direction side) {
        Vec3 offset = player.position().subtract(Vec3.atCenterOf(pos));
        return offset.x * side.getStepX() + offset.z * side.getStepZ() > 0;
    }

    /** Free blocks against the glass near the player, on the player's side or the other one. */
    private static List<Surface> windows(ServerPlayer player, String event, boolean inside) {
        ServerLevel level = player.level();
        List<Surface> found = new ArrayList<>();
        for (BlockPos pos : Nearby.all(player, event, Place::glass)) {
            BlockState state = level.getBlockState(pos);
            Decal.Depth depth = state.getBlock() instanceof IronBarsBlock ? Decal.Depth.PANE : Decal.Depth.FLUSH;
            for (Direction side : faces(state)) {
                BlockPos cell = pos.relative(side);
                if (playerSide(player, pos, side) == inside && level.isEmptyBlock(cell)) {
                    found.add(new Surface(cell, side.getOpposite(), depth));
                }
            }
        }
        return found;
    }

    private static void printHands(ServerPlayer player) {
        Config.Section config = Config.event(FROST_PRINTS);
        List<Surface> spots = new ArrayList<>(windows(player, FROST_PRINTS, false));
        Collections.shuffle(spots, random(player));
        ServerLevel level = player.level();
        Config.Tone tone = config.tone("sound");
        for (int i = 0; i < Math.min(config.integer("count"), spots.size()); i++) {
            Surface spot = spots.get(i);
            Scheduler.later(player, i * config.integer("gap_ticks"), p -> {
                lay(level, spot.cell(), decal(Content.FROST_PRINT.get(), spot), config.integer("restore_ticks"));
                blip(level, spot.cell(), SoundEvents.GLASS_HIT, tone);
            });
        }
    }

    /** The first phrase from the config that fits a run of windows, one word to a window, read left to right. */
    private static List<Placed> writing(ServerPlayer player) {
        List<Surface> panes = new ArrayList<>(windows(player, FROST_WRITING, true));
        if (panes.isEmpty()) {
            return List.of();
        }
        Random random = random(player);
        Set<Surface> all = new HashSet<>(panes);
        List<List<String>> phrases = new ArrayList<>(Config.event(FROST_WRITING).lines("phrases"));
        Collections.shuffle(phrases, random);
        Collections.shuffle(panes, random);
        for (List<String> phrase : phrases) {
            for (Surface start : panes) {
                // The viewer looks toward the glass, so their right is the way round the wall's facing.
                Direction along = start.toward().getClockWise();
                List<Placed> words = new ArrayList<>();
                for (int i = 0; i < phrase.size(); i++) {
                    Surface next = new Surface(start.cell().relative(along, i), start.toward(), start.depth());
                    if (!all.contains(next)) {
                        break;
                    }
                    words.add(new Placed(
                            next, Decal.Writing.Word.valueOf(phrase.get(i).toUpperCase(Locale.ROOT))));
                }
                if (words.size() == phrase.size()) {
                    return words;
                }
            }
        }
        return List.of();
    }

    private static void writeInFrost(ServerPlayer player) {
        Config.Section config = Config.event(FROST_WRITING);
        ServerLevel level = player.level();
        Config.Tone tone = config.tone("sound");
        List<Placed> words = writing(player);
        for (int i = 0; i < words.size(); i++) {
            Placed word = words.get(i);
            Scheduler.later(player, i * config.integer("gap_ticks"), p -> {
                lay(
                        level,
                        word.where().cell(),
                        decal(Content.FROST_WRITING.get(), word.where()).setValue(Decal.Writing.WORD, word.word()),
                        config.integer("restore_ticks"));
                blip(level, word.where().cell(), SoundEvents.GLASS_HIT, tone);
            });
        }
    }

    // 504: doors

    /** The free blocks against the player's side of closed doors, both halves of each. */
    private static List<Surface> doors(ServerPlayer player, String event) {
        ServerLevel level = player.level();
        List<Surface> found = new ArrayList<>();
        for (BlockPos pos : Nearby.all(
                player,
                event,
                state -> state.is(BlockTags.DOORS)
                        && state.hasProperty(DoorBlock.HALF)
                        && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                        && !state.getValue(DoorBlock.OPEN))) {
            VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
            if (shape.isEmpty()) {
                continue;
            }
            AABB bounds = shape.bounds();
            boolean thinX = bounds.getXsize() < 0.5;
            for (Direction side :
                    thinX ? List.of(Direction.EAST, Direction.WEST) : List.of(Direction.NORTH, Direction.SOUTH)) {
                boolean positive = side.getAxisDirection() == Direction.AxisDirection.POSITIVE;
                double gap =
                        thinX ? (positive ? 1 - bounds.maxX : bounds.minX) : (positive ? 1 - bounds.maxZ : bounds.minZ);
                if (!playerSide(player, pos, side)) {
                    continue;
                }
                for (BlockPos half : List.of(pos, pos.above())) {
                    BlockPos cell = half.relative(side);
                    if (level.isEmptyBlock(cell)) {
                        found.add(new Surface(cell, side.getOpposite(), Decal.Depth.nearest(gap)));
                    }
                }
            }
        }
        return found;
    }

    private static void scratchDoor(ServerPlayer player) {
        List<Surface> spots = doors(player, CLAW_MARKS);
        if (spots.isEmpty()) {
            return;
        }
        Config.Section config = Config.event(CLAW_MARKS);
        ServerLevel level = player.level();
        Surface first = spots.get(player.getRandom().nextInt(spots.size()));
        // The other half of the same door gets a mark too.
        for (Surface spot : spots) {
            boolean sameDoor = spot.toward() == first.toward()
                    && spot.depth() == first.depth()
                    && spot.cell().getX() == first.cell().getX()
                    && spot.cell().getZ() == first.cell().getZ()
                    && Math.abs(spot.cell().getY() - first.cell().getY()) <= 1;
            if (sameDoor) {
                lay(level, spot.cell(), decal(Content.CLAW_MARKS.get(), spot), config.integer("restore_ticks"));
            }
        }
        blip(level, first.cell(), HauntSounds.CLAW_SCRATCH, config.tone("sound"));
    }

    // 505: the doll

    private static void wanderDoll(ServerPlayer player) {
        Config.Section config = Config.event(DOLL);
        ServerLevel level = player.level();
        Config.Tone tone = config.tone("sound");
        double degrees = config.num("watch_degrees");
        Random random = random(player);
        BlockPos[] at = {null};
        boolean[] over = {false};
        int[] hops = {config.integer("hops")};
        clear(level, config.integer("ticks"), () -> at[0], Content.DOLL.get());
        Restore.after(level, config.integer("ticks"), force -> {
            over[0] = true;
            return true;
        });
        Scheduler.repeat(player, 0, config.integer("turn_ticks"), (p, n) -> {
            BlockPos here = at[0];
            if (over[0] || here != null && !level.getBlockState(here).is(Content.DOLL.get())) {
                return false;
            }
            // Watched, it keeps still.
            if (here != null && watched(p, here, degrees)) {
                return true;
            }
            if (here == null || n % config.integer("hop_every") == 0 && hops[0] > 0) {
                List<BlockPos> spots = new ArrayList<>(floorSpots(p, DOLL));
                spots.remove(here);
                if (spots.isEmpty()) {
                    return true;
                }
                BlockPos to = spots.get(random.nextInt(spots.size()));
                vanish(level, here, Content.DOLL.get());
                level.setBlock(
                        to,
                        Content.DOLL.get().defaultBlockState().setValue(Prop.FACING, toward(p, to)),
                        Block.UPDATE_ALL);
                blip(level, to, SoundEvents.WOOD_PLACE, tone);
                at[0] = to;
                hops[0]--;
            } else {
                BlockState state = level.getBlockState(here);
                BlockState turned = state.setValue(Prop.FACING, toward(p, here));
                if (turned != state) {
                    Restore.quiet(level, here, turned);
                    blip(level, here, SoundEvents.WOOL_BREAK, tone);
                }
            }
            return true;
        });
    }

    // 506: the music box

    private static void playMusicBox(ServerPlayer player) {
        List<BlockPos> spots = floorSpots(player, MUSIC_BOX);
        if (spots.isEmpty()) {
            return;
        }
        Config.Section config = Config.event(MUSIC_BOX);
        ServerLevel level = player.level();
        BlockPos pos = spots.get(player.getRandom().nextInt(spots.size()));
        List<String> tunes = config.texts("tunes");
        String[] notes =
                tunes.get(player.getRandom().nextInt(tunes.size())).trim().split("\\s+");
        Config.Tone tone = config.tone("sound");
        BlockState open = Content.MUSIC_BOX
                .get()
                .defaultBlockState()
                .setValue(Prop.FACING, toward(player, pos))
                .setValue(Prop.MusicBox.OPEN, true);
        level.setBlock(pos, open, Block.UPDATE_ALL);
        clear(
                level,
                notes.length * config.integer("step_ticks") + config.integer("linger_ticks"),
                () -> pos,
                open.getBlock());
        Scheduler.repeat(player, config.integer("start_ticks"), config.integer("step_ticks"), (p, n) -> {
            if (!level.getBlockState(pos).is(open.getBlock())) {
                return false;
            }
            if (n >= notes.length) {
                Restore.quiet(level, pos, open.setValue(Prop.MusicBox.OPEN, false));
                return false;
            }
            int note = Integer.parseInt(notes[n]);
            level.playSound(
                    null,
                    pos,
                    HauntSounds.MUSIC_BOX,
                    SoundSource.RECORDS,
                    tone.volume(),
                    NoteBlock.getPitchFromNote(note));
            level.sendParticles(
                    ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 0, note / 24.0, 0, 0, 1);
            return true;
        });
    }

    // 507: the rocking chair

    private static void rockChair(ServerPlayer player) {
        List<BlockPos> spots = floorSpots(player, ROCKING_CHAIR);
        if (spots.isEmpty()) {
            return;
        }
        Config.Section config = Config.event(ROCKING_CHAIR);
        ServerLevel level = player.level();
        BlockPos pos = spots.get(player.getRandom().nextInt(spots.size()));
        Config.Tone tone = config.tone("sound");
        BlockState chair = Content.ROCKING_CHAIR.get().defaultBlockState().setValue(Prop.FACING, toward(player, pos));
        level.setBlock(pos, chair, Block.UPDATE_ALL);
        int rocks = config.integer("rocks");
        clear(
                level,
                rocks * config.integer("rock_ticks") + config.integer("linger_ticks"),
                () -> pos,
                chair.getBlock());
        Scheduler.repeat(player, 0, config.integer("rock_ticks"), (p, n) -> {
            if (!level.getBlockState(pos).is(chair.getBlock())) {
                return false;
            }
            int tilt = n >= rocks ? 0 : ROCK[n % ROCK.length];
            Restore.quiet(level, pos, chair.setValue(Prop.Chair.TILT, tilt));
            if (tilt != 0) {
                blip(level, pos, HauntSounds.CHAIR_CREAK, tone);
            }
            return n < rocks;
        });
    }

    // 508: rot

    private static void rotPlanks(ServerPlayer player) {
        List<BlockPos> planks = Nearby.all(player, ROTTEN_PLANKS, s -> s.is(BlockTags.PLANKS));
        if (planks.isEmpty()) {
            return;
        }
        Config.Section config = Config.event(ROTTEN_PLANKS);
        ServerLevel level = player.level();
        Random random = random(player);
        Config.Tone tone = config.tone("sound");
        int max = config.integer("max_blocks");
        int perStep = config.integer("per_step");
        Set<BlockPos> rotten = new HashSet<>();
        List<BlockPos> edge = new ArrayList<>(List.of(planks.get(random.nextInt(planks.size()))));
        Scheduler.repeat(player, 0, config.integer("step_ticks"), (p, n) -> {
            for (int i = 0; i < perStep && !edge.isEmpty() && rotten.size() < max; i++) {
                BlockPos pos = edge.remove(random.nextInt(edge.size()));
                if (!level.getBlockState(pos).is(BlockTags.PLANKS)) {
                    continue;
                }
                Restore.swap(
                        level, pos, Content.ROTTEN_PLANKS.get().defaultBlockState(), config.integer("restore_ticks"));
                rotten.add(pos);
                blip(level, pos, SoundEvents.WOOD_BREAK, tone);
                for (Direction d : Direction.values()) {
                    BlockPos next = pos.relative(d);
                    if (!rotten.contains(next)
                            && !edge.contains(next)
                            && level.getBlockState(next).is(BlockTags.PLANKS)) {
                        edge.add(next);
                    }
                }
            }
            return !edge.isEmpty() && rotten.size() < max;
        });
    }

    // 509: the black candle

    private static List<BlockPos> unlitCandles(ServerPlayer player) {
        return Nearby.all(
                player,
                BLACK_CANDLE,
                s -> s.is(BlockTags.CANDLES) && s.hasProperty(CandleBlock.LIT) && !s.getValue(CandleBlock.LIT));
    }

    private static void lightBlackCandle(ServerPlayer player) {
        Config.Section config = Config.event(BLACK_CANDLE);
        ServerLevel level = player.level();
        List<BlockPos> candles = unlitCandles(player);
        BlockPos pos;
        if (!candles.isEmpty()) {
            pos = candles.get(player.getRandom().nextInt(candles.size()));
            BlockState old = level.getBlockState(pos);
            boolean wet = old.getValue(CandleBlock.WATERLOGGED);
            Restore.swap(
                    level,
                    pos,
                    net.minecraft.world.level.block.Blocks.DYED_CANDLE
                            .black()
                            .defaultBlockState()
                            .setValue(CandleBlock.CANDLES, old.getValue(CandleBlock.CANDLES))
                            .setValue(CandleBlock.WATERLOGGED, wet)
                            .setValue(CandleBlock.LIT, !wet),
                    config.integer("restore_ticks"));
        } else {
            List<BlockPos> spots = floorSpots(player, BLACK_CANDLE);
            if (spots.isEmpty()) {
                return;
            }
            pos = spots.get(player.getRandom().nextInt(spots.size()));
            BlockState candle = net.minecraft.world.level.block.Blocks.DYED_CANDLE
                    .black()
                    .defaultBlockState()
                    .setValue(CandleBlock.LIT, true);
            level.setBlock(pos, candle, Block.UPDATE_ALL);
            BlockPos at = pos;
            clear(level, config.integer("restore_ticks"), () -> at, candle.getBlock());
        }
        BlockPos at = pos.immutable();
        blip(level, at, SoundEvents.FLINTANDSTEEL_USE, config.tone("sound"));
        int interval = config.integer("flame_interval");
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            level.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    at.getX() + 0.5,
                    at.getY() + 0.45,
                    at.getZ() + 0.5,
                    1,
                    0.02,
                    0.04,
                    0.02,
                    0.0);
            return (n + 1) * interval < config.integer("flame_ticks");
        });
    }

    // 510: the mirror

    /** Free blocks at head height against a solid wall, away from the player and out of their view. */
    private static List<Surface> walls(ServerPlayer player) {
        Config.Section config = Config.event(CRACKED_MIRROR);
        ServerLevel level = player.level();
        int head = player.blockPosition().getY() + 1;
        double min = config.num("min_distance");
        List<Surface> found = new ArrayList<>();
        for (BlockPos pos : Nearby.all(player, CRACKED_MIRROR, BlockState::isAir)) {
            if (pos.getY() != head
                    || Vec3.atCenterOf(pos).distanceToSqr(player.position()) < min * min
                    || watched(player, pos, config.num("hide_degrees"))) {
                continue;
            }
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (Place.solid(level, pos.relative(d))) {
                    found.add(new Surface(pos, d, Decal.Depth.FLUSH));
                }
            }
        }
        return found;
    }

    private static void hangMirror(ServerPlayer player) {
        List<Surface> walls = walls(player);
        if (walls.isEmpty()) {
            return;
        }
        Config.Section config = Config.event(CRACKED_MIRROR);
        ServerLevel level = player.level();
        Config.Tone tone = config.tone("sound");
        Surface wall = walls.get(player.getRandom().nextInt(walls.size()));
        BlockPos pos = wall.cell().immutable();
        BlockState mirror = decal(Content.CRACKED_MIRROR.get(), wall).setValue(Decal.Mirror.FIGURE, false);
        level.setBlock(pos, mirror, Block.UPDATE_ALL);
        int wait = config.integer("wait_ticks");
        int figure = config.integer("figure_ticks");
        clear(level, wait + figure + config.integer("linger_ticks"), () -> pos, mirror.getBlock());
        int step = config.integer("look_interval");
        double near = config.num("look_distance");
        Scheduler.repeat(player, step, step, (p, n) -> {
            if (!level.getBlockState(pos).is(mirror.getBlock())) {
                return false;
            }
            if (watched(p, pos, config.num("look_degrees")) && p.distanceToSqr(Vec3.atCenterOf(pos)) <= near * near) {
                Restore.quiet(level, pos, mirror.setValue(Decal.Mirror.FIGURE, true));
                blip(level, pos, HauntSounds.MIRROR_CRACK, tone);
                Scheduler.later(p, figure, q -> {
                    if (level.getBlockState(pos).is(mirror.getBlock())) {
                        Restore.quiet(level, pos, mirror);
                    }
                });
                return false;
            }
            return (n + 2) * step < wait;
        });
    }
}
