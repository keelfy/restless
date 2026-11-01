package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * More blocks that look other than they are to the haunted player alone; see {@link Swaps}. Nothing on the server
 * changes (a sign's text is the one thing sent outside {@link Swaps}, and sent again as it is when the time is up).
 */
final class Veils {
    private static final List<Direction> HORIZONTAL =
            List.copyOf(Direction.Plane.HORIZONTAL.stream().toList());

    private Veils() {}

    static List<HauntEvent> all() {
        return List.of(
                // 297: every wool block and carpet nearby is red
                swap(
                        "red_wool",
                        state -> (state.is(BlockTags.WOOL) && !state.is(Blocks.WOOL.red()))
                                || (state.is(BlockTags.WOOL_CARPETS) && !state.is(Blocks.CARPET.red())),
                        state -> (state.is(BlockTags.WOOL_CARPETS) ? Blocks.CARPET.red() : Blocks.WOOL.red())
                                .defaultBlockState()),
                // 298: a sign nearby reads something it does not say
                SimpleEvent.light(
                        "sign_whisper",
                        EventTag.BLOCKS,
                        player -> Nearby.any(player, "sign_whisper", state -> state.getBlock() instanceof SignBlock),
                        Veils::signWhisper),
                // 299: the flowers nearby are wither roses
                swap(
                        "wither_roses",
                        state -> state.is(BlockTags.SMALL_FLOWERS) && !state.is(Blocks.WITHER_ROSE),
                        state -> Blocks.WITHER_ROSE.defaultBlockState()),
                // 301: one kind of block in view is another for a tick
                SimpleEvent.light(
                        "block_flicker",
                        EventTag.BLOCKS,
                        player -> Nearby.any(player, "block_flicker", Veils::flickers),
                        Veils::flicker),
                // 302: a gravestone in the open, for this player alone
                SimpleEvent.light(
                        "restless:lone_gravestone",
                        EventTag.BLOCKS,
                        player -> graveSpot(player) != null,
                        Veils::gravestone),
                // 304: some of the obsidian round a nether portal is gone
                SimpleEvent.light(
                        "broken_portal",
                        EventTag.BLOCKS,
                        player -> !frame(player).isEmpty(),
                        Veils::breakPortal),
                // 307: a ring of light in a dark cave, which goes out as the player steps into it
                SimpleEvent.light(
                        "dark_circle",
                        EventTag.BLOCKS,
                        player -> !circle(player).isEmpty(),
                        Veils::darkCircle),
                // 308: some fence posts are gone
                SimpleEvent.light(
                        "fence_gaps",
                        EventTag.BLOCKS,
                        player -> Nearby.all(player, "fence_gaps", state -> state.is(BlockTags.FENCES))
                                        .size()
                                >= Config.event("fence_gaps").integer("min_fences"),
                        Veils::fenceGaps),
                // 310: cobwebs in the corners of the room
                SimpleEvent.light(
                        "corner_cobwebs",
                        EventTag.BLOCKS,
                        player -> corners(player).size()
                                >= Config.event("corner_cobwebs").integer("min_corners"),
                        Veils::cobwebs),
                // 311: a line of redstone dust on the floor leads to the player
                SimpleEvent.light(
                        "redstone_trail",
                        EventTag.BLOCKS,
                        player -> !trail(player).isEmpty(),
                        Veils::redstoneTrail));
    }

    /** The {@code max_blocks} nearest blocks that match shown as {@code to} of each for {@code ticks}. */
    private static SimpleEvent swap(String id, Predicate<BlockState> match, UnaryOperator<BlockState> to) {
        return SimpleEvent.light(id, EventTag.BLOCKS, player -> Nearby.any(player, id, match), player -> {
            Config.Section config = Config.event(id);
            Map<BlockPos, BlockState> shown = new LinkedHashMap<>();
            for (BlockPos pos : nearest(player, Nearby.all(player, id, match), config.integer("max_blocks"))) {
                shown.put(pos, to.apply(player.level().getBlockState(pos)));
            }
            Swaps.show(player, shown, config.integer("ticks"));
        });
    }

    /**
     * The sign's block entity is sent with other text, and as it is when the time is up. The server keeps the real one.
     */
    private static void signWhisper(ServerPlayer player) {
        Config.Section config = Config.event("sign_whisper");
        BlockPos pos = Nearby.random(player, "sign_whisper", state -> state.getBlock() instanceof SignBlock);
        ServerLevel level = player.level();
        if (pos == null || !(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) {
            return;
        }
        List<List<String>> texts = config.lines("texts");
        List<String> lines = texts.get(player.getRandom().nextInt(texts.size()));
        SignText.Mutable edit = sign.getText(SignTextSlot.FRONT).asMutable();
        int first = (4 - lines.size()) / 2;
        for (int i = 0; i < 4; i++) {
            edit.setLine(i, Component.literal(i >= first && i - first < lines.size() ? lines.get(i - first) : ""));
        }
        SignText.CODEC
                .encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), edit.asImmutable())
                .result()
                .ifPresent(text -> {
                    player.connection.send(ClientboundBlockEntityDataPacket.create(sign, (be, access) -> {
                        CompoundTag tag = be.getUpdateTag(access);
                        tag.put("front_text", text);
                        return tag;
                    }));
                    Scheduler.later(player, config.integer("ticks"), p -> {
                        if (p.level() == level && level.getBlockEntity(pos) instanceof SignBlockEntity now) {
                            p.connection.send(ClientboundBlockEntityDataPacket.create(now));
                        }
                    });
                });
    }

    private static boolean flickers(BlockState state) {
        return Config.event("block_flicker")
                .texts("from")
                .contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
    }

    /** Every block of one kind, picked at random from what is round the player, for a tick another block. */
    private static void flicker(ServerPlayer player) {
        Config.Section config = Config.event("block_flicker");
        ServerLevel level = player.level();
        List<BlockPos> found = Nearby.all(player, "block_flicker", Veils::flickers);
        if (found.isEmpty()) {
            return;
        }
        Block kind = level.getBlockState(found.get(player.getRandom().nextInt(found.size())))
                .getBlock();
        List<String> others = config.texts("to");
        BlockState to = BuiltInRegistries.BLOCK
                .getOptional(Identifier.parse(others.get(player.getRandom().nextInt(others.size()))))
                .orElse(Blocks.TNT)
                .defaultBlockState();
        List<BlockPos> same =
                found.stream().filter(pos -> level.getBlockState(pos).is(kind)).toList();
        Swaps.show(player, nearest(player, same, config.integer("max_blocks")), to, config.integer("ticks"));
    }

    private static @Nullable BlockPos graveSpot(ServerPlayer player) {
        Config.Section config = Config.event("restless:lone_gravestone");
        return Place.standingAhead(
                player,
                config.num("distance"),
                0.0,
                config.num("max_angle"),
                spot -> player.level().canSeeSky(spot));
    }

    private static void gravestone(ServerPlayer player) {
        BlockPos spot = graveSpot(player);
        if (spot != null) {
            Swaps.show(
                    player,
                    spot,
                    Content.GRAVESTONE.get().defaultBlockState(),
                    Config.event("restless:lone_gravestone").integer("ticks"));
        }
    }

    /** The obsidian that touches a portal's blocks. */
    private static Set<BlockPos> frame(ServerPlayer player) {
        ServerLevel level = player.level();
        Set<BlockPos> frame = new LinkedHashSet<>();
        for (BlockPos portal : Nearby.all(player, "broken_portal", state -> state.is(Blocks.NETHER_PORTAL))) {
            for (Direction d : Direction.values()) {
                BlockPos next = portal.relative(d);
                if (level.getBlockState(next).is(Blocks.OBSIDIAN)) {
                    frame.add(next);
                }
            }
        }
        return frame;
    }

    private static void breakPortal(ServerPlayer player) {
        Config.Section config = Config.event("broken_portal");
        List<BlockPos> frame = new ArrayList<>(frame(player));
        Collections.shuffle(frame, new Random(player.getRandom().nextLong()));
        Swaps.show(
                player,
                frame.subList(0, Math.min(frame.size(), config.integer("holes"))),
                Blocks.AIR.defaultBlockState(),
                config.integer("ticks"));
    }

    /**
     * Air round a spot ahead of the player, in a cave where it is dark: nothing lights the player and no sky is
     * overhead.
     */
    private static List<BlockPos> circle(ServerPlayer player) {
        Config.Section config = Config.event("dark_circle");
        ServerLevel level = player.level();
        if (level.canSeeSky(player.blockPosition())
                || level.getBrightness(LightLayer.BLOCK, player.blockPosition()) > config.integer("max_light")) {
            return List.of();
        }
        BlockPos centre = Place.standingAhead(player, config.num("distance"), 0.0, config.num("max_angle"));
        if (centre == null) {
            return List.of();
        }
        List<BlockPos> ring = new ArrayList<>();
        int points = config.integer("points");
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points;
            BlockPos pos = BlockPos.containing(
                    centre.getX() + 0.5 + Math.cos(angle) * config.num("radius"),
                    centre.getY(),
                    centre.getZ() + 0.5 + Math.sin(angle) * config.num("radius"));
            if (level.isLoaded(pos) && level.isEmptyBlock(pos)) {
                ring.add(pos);
            }
        }
        if (ring.size() < config.integer("min_points")) {
            return List.of();
        }
        ring.addFirst(centre);
        return ring;
    }

    /**
     * Light blocks, shown for {@code ticks}. As an illusion each is gone for the player once they come within
     * {@code enter_distance} of the middle.
     */
    private static void darkCircle(ServerPlayer player) {
        Config.Section config = Config.event("dark_circle");
        List<BlockPos> ring = circle(player);
        if (ring.isEmpty()) {
            return;
        }
        int ticks = config.integer("ticks");
        Swaps.show(
                player,
                ring,
                Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, config.integer("light")),
                ticks);
        ServerLevel level = player.level();
        if (Swaps.real(level)) {
            return;
        }
        Vec3 middle = Vec3.atBottomCenterOf(ring.getFirst());
        int interval = config.integer("interval_ticks");
        double near = config.num("enter_distance");
        Scheduler.repeat(player, interval, interval, (p, n) -> {
            if (p.level() != level) {
                return false;
            }
            if (p.position().distanceToSqr(middle) <= near * near) {
                ring.forEach(pos -> p.connection.send(new ClientboundBlockUpdatePacket(level, pos)));
                return false;
            }
            return n * interval < ticks;
        });
    }

    private static void fenceGaps(ServerPlayer player) {
        Config.Section config = Config.event("fence_gaps");
        List<BlockPos> fences = new ArrayList<>(Nearby.all(player, "fence_gaps", state -> state.is(BlockTags.FENCES)));
        Collections.shuffle(fences, new Random(player.getRandom().nextLong()));
        Swaps.show(
                player,
                fences.subList(0, Math.min(fences.size(), config.integer("gaps"))),
                Blocks.AIR.defaultBlockState(),
                config.integer("ticks"));
    }

    /** Empty blocks where the ceiling and two walls meet. */
    private static List<BlockPos> corners(ServerPlayer player) {
        ServerLevel level = player.level();
        int radius = Nearby.radius("corner_cobwebs");
        List<BlockPos> corners = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(
                player.blockPosition().offset(-radius, -radius, -radius),
                player.blockPosition().offset(radius, radius, radius))) {
            if (level.isLoaded(pos)
                    && level.isEmptyBlock(pos)
                    && Place.solid(level, pos.above())
                    && (Place.solid(level, pos.north()) || Place.solid(level, pos.south()))
                    && (Place.solid(level, pos.east()) || Place.solid(level, pos.west()))) {
                corners.add(pos.immutable());
            }
        }
        return corners;
    }

    private static void cobwebs(ServerPlayer player) {
        Config.Section config = Config.event("corner_cobwebs");
        List<BlockPos> corners = new ArrayList<>(corners(player));
        Collections.shuffle(corners, new Random(player.getRandom().nextLong()));
        Swaps.show(
                player,
                corners.subList(0, Math.min(corners.size(), config.integer("max_blocks"))),
                Blocks.COBWEB.defaultBlockState(),
                config.integer("ticks"));
    }

    /**
     * Dust on the floor in a straight line along one of the four directions, from {@code start} blocks away outward,
     * following the ground up and down; the first direction in random order that has room for {@code min_blocks}.
     */
    private static Map<BlockPos, BlockState> trail(ServerPlayer player) {
        Config.Section config = Config.event("redstone_trail");
        ServerLevel level = player.level();
        List<Direction> dirs = new ArrayList<>(HORIZONTAL);
        Collections.shuffle(dirs, new Random(player.getRandom().nextLong()));
        for (Direction d : dirs) {
            BlockState dust = Blocks.REDSTONE_WIRE
                    .defaultBlockState()
                    .setValue(RedstoneWireBlock.PROPERTY_BY_DIRECTION.get(d), RedstoneSide.SIDE)
                    .setValue(RedstoneWireBlock.PROPERTY_BY_DIRECTION.get(d.getOpposite()), RedstoneSide.SIDE);
            Map<BlockPos, BlockState> line = new LinkedHashMap<>();
            int y = player.getBlockY();
            for (int i = config.integer("start"); i < config.integer("start") + config.integer("length"); i++) {
                BlockPos spot = Place.standing(
                        level, player.blockPosition().relative(d, i).atY(y));
                if (spot == null) {
                    break;
                }
                line.put(spot, dust);
                y = spot.getY();
            }
            if (line.size() >= config.integer("min_blocks")) {
                return line;
            }
        }
        return Map.of();
    }

    private static void redstoneTrail(ServerPlayer player) {
        Swaps.show(player, trail(player), Config.event("redstone_trail").integer("ticks"));
    }

    /** The {@code max} of {@code found} nearest the player. */
    private static List<BlockPos> nearest(ServerPlayer player, List<BlockPos> found, int max) {
        return found.stream()
                .sorted(Comparator.comparingDouble(pos -> pos.distSqr(player.blockPosition())))
                .limit(max)
                .toList();
    }
}
