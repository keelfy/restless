package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.Hardcore;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The player's own view of the world lies to them; see {@link Swaps} and {@link PacketEntity}. Nothing on the
 * server changes, except under {@linkplain Hardcore hardcore}, where the blocks and the mob are made real (the dragon
 * stays a sight). The ghost tree
 * is made of blocks with no collision, so the client lets the player walk through it, as the server does.
 */
final class Illusions {
    private Illusions() {}

    static List<HauntEvent> all() {
        return List.of(
            // 105: every window nearby is gone, or fenced, or walled up with what is around it
            SimpleEvent.light("false_windows", EventTag.BLOCKS, player -> Nearby.any(player, "false_windows", Illusions::isGlass), Illusions::falseWindows),
            // 106: blocks one uses look like others of the kind: a crafting table like a furnace, a stonecutter like a loom
            SimpleEvent.light("false_looks", EventTag.BLOCKS, player -> Nearby.any(player, "false_looks", Illusions::lookable), player -> {
                Config.Section config = Config.event("false_looks");
                Map<BlockPos, BlockState> shown = new LinkedHashMap<>();
                for (BlockPos pos : nearest(player, Nearby.all(player, "false_looks", Illusions::lookable), config.integer("max_blocks"))) {
                    look(player, player.level().getBlockState(pos)).ifPresent(state -> shown.put(pos, state));
                }
                Swaps.show(player, shown, config.integer("ticks"));
            }),
            // 107: a tree where there was none, pale oak, oak or birch, which the player walks through
            SimpleEvent.light("ghost_tree", EventTag.BLOCKS, player -> treeSpot(player) != null, player -> {
                BlockPos root = treeSpot(player);
                if (root != null) {
                    Swaps.show(player, ghostTree(player, root), Config.event("ghost_tree").integer("ticks"));
                }
            }),
            // 126: a mob standing out in the field that is not there, facing the player; gone when approached
            SimpleEvent.light("hallucination", EventTag.MOBS, player -> mobSpot(player) != null, Illusions::hallucination),
            // 125: an animal nearby looks like another to this player; under hardcore it is changed for real
            SimpleEvent.light("disguise", EventTag.MOBS, player -> !disguisable(player).isEmpty(), Illusions::disguise),
            // 127: the ender dragon flies over, high up, for this player alone
            SimpleEvent.light("dragon_flyover", EventTag.PRESENCE,
                player -> player.level().dimension() == Level.OVERWORLD && player.level().canSeeSky(player.blockPosition()), Illusions::dragonFlyover)
        );
    }

    private static @Nullable BlockPos mobSpot(ServerPlayer player) {
        Config.Section config = Config.event("hallucination");
        return Place.standingAhead(player, config.num("distance"), 0.0, config.num("max_angle"));
    }

    private static void hallucination(ServerPlayer player) {
        Config.Section config = Config.event("hallucination");
        BlockPos spot = mobSpot(player);
        List<String> mobs = config.texts("mobs");
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(mobs.get(player.getRandom().nextInt(mobs.size())))).orElse(null);
        if (spot == null || type == null) {
            return;
        }
        Vec3 at = Vec3.atBottomCenterOf(spot);
        if (Hardcore.on(player.level())) {
            // It was there after all.
            type.spawn(player.level(), spot, EntitySpawnReason.EVENT);
            return;
        }
        PacketEntity mob = PacketEntity.show(player, type, at, PacketEntity.yawToward(at, player.position()));
        int interval = config.integer("interval_ticks");
        double near = config.num("vanish_distance");
        Scheduler.repeat(player, interval, interval, (p, n) -> {
            boolean over = n * interval >= config.integer("max_ticks") || p.position().distanceToSqr(at) <= near * near;
            if (over) {
                mob.remove(p);
            }
            return !over;
        });
    }

    private static List<Mob> disguisable(ServerPlayer player) {
        Config.Section looks = Config.event("disguise").section("looks");
        double radius = Config.event("disguise").num("radius");
        return player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius),
            mob -> mob.isAlive() && looks.has(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString()));
    }

    /**
     * The real animal is hidden from this client (only its invisible flag is sent, so no entity data mismatches) and a
     * packet entity of the other kind follows it about, until {@code ticks} are up or it is gone.
     */
    @SuppressWarnings("unchecked")
    private static void disguise(ServerPlayer player) {
        Config.Section config = Config.event("disguise");
        List<Mob> found = disguisable(player);
        Mob mob = found.get(player.getRandom().nextInt(found.size()));
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(config.section("looks").text(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString()))).orElse(null);
        if (type == null) {
            return;
        }
        if (Hardcore.on(player.level())) {
            // It was that all along.
            mob.convertTo((EntityType<Mob>) type, ConversionParams.single(mob, true, true), converted -> {});
            return;
        }
        byte flags = (byte) mob.getEntityData().getNonDefaultValues().stream().filter(value -> value.id() == SHARED_FLAGS).map(value -> (Byte) value.value()).findFirst().orElse((byte) 0);
        player.connection.send(sharedFlags(mob, (byte) (flags | INVISIBLE)));
        PacketEntity copy = PacketEntity.show(player, type, mob.position(), mob.getYRot());
        Scheduler.repeat(player, 1, 1, (p, n) -> {
            boolean over = n >= config.integer("ticks") || !mob.isAlive() || p.level() != mob.level();
            if (over) {
                copy.remove(p);
                p.connection.send(sharedFlags(mob, flags));
                return false;
            }
            copy.move(p, mob.position(), mob.getYRot());
            return true;
        });
    }

    private static final int SHARED_FLAGS = 0;
    private static final byte INVISIBLE = 1 << 5;

    private static ClientboundSetEntityDataPacket sharedFlags(Mob mob, byte flags) {
        return new ClientboundSetEntityDataPacket(mob.getId(), List.of(new SynchedEntityData.DataValue<>(SHARED_FLAGS, EntityDataSerializers.BYTE, flags)));
    }

    /** A straight line high over the player, at a random bearing and a little to one side, flown in {@code ticks}. */
    private static void dragonFlyover(ServerPlayer player) {
        Config.Section config = Config.event("dragon_flyover");
        double bearing = player.getRandom().nextDouble() * Math.PI * 2;
        Vec3 along = new Vec3(Math.cos(bearing), 0, Math.sin(bearing));
        Vec3 side = new Vec3(-along.z, 0, along.x).scale((player.getRandom().nextDouble() * 2 - 1) * config.num("side"));
        Vec3 over = player.position().add(side).add(0, config.num("height"), 0);
        Vec3 from = over.subtract(along.scale(config.num("length") / 2));
        Vec3 to = over.add(along.scale(config.num("length") / 2));
        // The dragon's yaw is the other way round from other entities'.
        float yaw = PacketEntity.yawToward(from, to) + 180.0F;
        PacketEntity dragon = PacketEntity.show(player, EntityTypes.ENDER_DRAGON, from, yaw);
        Config.Tone growl = config.tone("sound");
        player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.ENDER_DRAGON_GROWL), SoundSource.HOSTILE,
            over.x, over.y, over.z, growl.volume(), growl.pitch(), player.getRandom().nextLong()));
        int ticks = config.integer("ticks");
        Scheduler.repeat(player, 1, 1, (p, n) -> {
            if (n >= ticks) {
                dragon.remove(p);
                return false;
            }
            dragon.move(p, from.lerp(to, (n + 1) / (double) ticks), yaw);
            return true;
        });
    }

    /** Standing room in front of the player, with room for the tallest trunk. */
    private static @Nullable BlockPos treeSpot(ServerPlayer player) {
        Config.Section config = Config.event("ghost_tree");
        int tallest = config.integer("trunk") + config.integer("trunk_extra");
        return Place.standingAhead(player, config.num("distance"), 0.0, config.num("max_angle"), root -> {
            for (int dy = 0; dy < tallest; dy++) {
                if (!Place.open(player.level(), root.above(dy))) {
                    return false;
                }
            }
            return true;
        });
    }

    /**
     * A small round tree on {@code root}, of the ghost blocks of a random wood, leaves only where there is air. The
     * crown is the vanilla oak and birch blob: two wide layers and two narrow, corners off at random and always on top.
     */
    private static Map<BlockPos, BlockState> ghostTree(ServerPlayer player, BlockPos root) {
        Config.Section config = Config.event("ghost_tree");
        ServerLevel level = player.level();
        List<String> woods = config.texts("woods");
        Content.GhostWood wood = Content.GHOST_WOODS.getOrDefault(woods.get(player.getRandom().nextInt(woods.size())), Content.GHOST_WOODS.get("pale_oak"));
        int trunk = config.integer("trunk") + player.getRandom().nextInt(config.integer("trunk_extra") + 1);
        Map<BlockPos, BlockState> tree = new LinkedHashMap<>();
        BlockState leaves = wood.leaves().get().defaultBlockState();
        for (int dy = trunk - 3; dy <= trunk; dy++) {
            int radius = dy < trunk - 1 ? 2 : 1;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = root.offset(dx, dy, dz);
                    boolean corner = Math.abs(dx) == radius && Math.abs(dz) == radius;
                    if (!(corner && (dy == trunk || player.getRandom().nextBoolean())) && level.isEmptyBlock(pos)) {
                        tree.put(pos, leaves);
                    }
                }
            }
        }
        for (int dy = 0; dy < trunk; dy++) {
            tree.put(root.above(dy), wood.log().get().defaultBlockState());
        }
        return tree;
    }

    private static void falseWindows(ServerPlayer player) {
        Config.Section config = Config.event("false_windows");
        ServerLevel level = player.level();
        List<String> modes = config.texts("modes");
        String mode = modes.get(player.getRandom().nextInt(modes.size()));
        Map<BlockPos, BlockState> shown = new LinkedHashMap<>();
        for (BlockPos pos : nearest(player, Nearby.all(player, "false_windows", Illusions::isGlass), config.integer("max_blocks"))) {
            shown.put(pos, switch (mode) {
                // Connected to the walls around it, as a fence put there would be.
                case "fence" -> Block.updateFromNeighbourShapes(Blocks.OAK_FENCE.defaultBlockState(), level, pos);
                case "wall" -> commonNeighbour(level, pos);
                default -> Blocks.AIR.defaultBlockState();
            });
        }
        Swaps.show(player, shown, config.integer("ticks"));
    }

    /** Glass blocks and panes, plain, stained or tinted. */
    private static boolean isGlass(BlockState state) {
        return state.is(BlockTags.IMPERMEABLE) || BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().endsWith("glass_pane");
    }

    /** The full block most of its neighbours are, so the hole looks bricked up; air if none is. */
    private static BlockState commonNeighbour(ServerLevel level, BlockPos pos) {
        Map<BlockState, Integer> counts = new HashMap<>();
        for (Direction d : Direction.values()) {
            BlockState next = level.getBlockState(pos.relative(d));
            if (next.isSolidRender() && !isGlass(next) && !next.hasBlockEntity()) {
                counts.merge(next, 1, Integer::sum);
            }
        }
        return counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(Blocks.AIR.defaultBlockState());
    }

    private static boolean lookable(BlockState state) {
        return Config.event("false_looks").texts("blocks").contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
    }

    /** {@code state} as another of {@code false_looks.blocks}, at random, keeping what it shares with it (its facing). */
    private static Optional<BlockState> look(ServerPlayer player, BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        List<String> others = Config.event("false_looks").texts("blocks").stream().filter(other -> !other.equals(id)).toList();
        if (others.isEmpty()) {
            return Optional.empty();
        }
        return BuiltInRegistries.BLOCK.getOptional(Identifier.parse(others.get(player.getRandom().nextInt(others.size()))))
            .map(to -> to.defaultBlockState().withPropertiesOf(state));
    }

    /** The {@code max} of {@code found} nearest the player. */
    private static List<BlockPos> nearest(ServerPlayer player, List<BlockPos> found, int max) {
        return found.stream().sorted(Comparator.comparingDouble(pos -> pos.distSqr(player.blockPosition()))).limit(max).toList();
    }
}
