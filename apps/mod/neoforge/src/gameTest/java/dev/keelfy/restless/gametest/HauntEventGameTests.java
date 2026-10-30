package dev.keelfy.restless.gametest;

import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntEvents;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;

/** Each packet-lie and sound event runs on a connected player and leaves the world as it was (or, for 110, as it says). */
final class HauntEventGameTests {
    static final BlockPos FEET = new BlockPos(4, 1, 4);
    /** Long enough for every lie to send its truth back. */
    private static final int SETTLE_TICKS = 30;

    /** Tests whose figures stand far off: they get room around them, so no other test's figure is in their reach. */
    static final Map<String, Integer> PADDING = new HashMap<>();

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        for (String id : List.of("false_wound", "false_hearts", "false_hunger", "guardian_face", "false_totem", "subliminal_title",
            "red_vignette", "own_voice", "false_death", "private_rain", "silent_lightning", "distant_mob", "bottle_shatter",
            "stranger_joins", "stranger_death", "false_night", "false_slot", "turned_head", "dragon_flyover")) {
            tests.put("event_" + id, helper -> runs(helper, id, player -> {}, () -> {}));
        }
        tests.put("event_false_advancement", helper -> runs(helper, "false_advancement", player -> {}, () -> {
            for (String name : List.of("who_are_you", "not_alone", "behind_you")) {
                AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(Identifier.fromNamespaceAndPath("restless", "whisper/" + name));
                check(helper, advancement != null, "advancement whisper/" + name + " must load");
                check(helper, helper.getLevel().players().stream().noneMatch(p -> p.getAdvancements().getOrStartProgress(advancement).isDone()),
                    "false_advancement must take the advancement back");
            }
        }));
        // A field off the test floor, past the event's distance from the player, and room enough that no other test's crops are near.
        PADDING.put("event_harvest", 40);
        PADDING.put("event_ghost_tree", 30);
        PADDING.put("event_hallucination", 50);
        tests.put("event_harvest", helper -> runs(helper, "harvest", player -> {
            BlockPos field = helper.absolutePos(FEET).east(13);
            for (BlockPos pos : BlockPos.betweenClosed(field.offset(-3, -1, -2), field.offset(3, -1, 2))) {
                helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
            }
            for (BlockPos crop : List.of(field, field.south())) {
                helper.getLevel().setBlock(crop.below(), Blocks.FARMLAND.defaultBlockState(), 2);
                helper.getLevel().setBlock(crop, ((CropBlock) Blocks.WHEAT).getStateForAge(CropBlock.MAX_AGE), 2);
            }
        }, () -> {
            BlockPos field = helper.absolutePos(FEET).east(13);
            check(helper, helper.getLevel().isEmptyBlock(field) && helper.getLevel().isEmptyBlock(field.south()), "harvest must take the ripe wheat");
            AABB around = new AABB(field).inflate(8);
            check(helper, helper.getLevel().getEntitiesOfClass(Mannequin.class, around).isEmpty(), "harvest must leave once the field is bare");
        }));
        tests.put("event_false_windows", helper -> runs(helper, "false_windows",
            player -> helper.setBlock(FEET.north(2), Blocks.GLASS), () -> helper.assertBlockPresent(Blocks.GLASS, FEET.north(2))));
        tests.put("event_false_looks", helper -> runs(helper, "false_looks",
            player -> helper.setBlock(FEET.west(2), Blocks.CRAFTING_TABLE), () -> helper.assertBlockPresent(Blocks.CRAFTING_TABLE, FEET.west(2))));
        tests.put("event_ghost_tree", helper -> runs(helper, "ghost_tree", player -> {
            // Ground 12 blocks out on every side; the tree itself is only sent, so the server's air must stay.
            BlockPos centre = helper.absolutePos(FEET);
            for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-14, -1, -14), centre.offset(14, -1, 14))) {
                helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
            }
        }, () -> {
            BlockPos centre = helper.absolutePos(FEET);
            for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-14, 0, -14), centre.offset(14, 6, 14))) {
                check(helper, !helper.getLevel().getBlockState(pos).is(Content.GHOST_LOG.get()), "ghost_tree must not be on the server");
            }
        }));
        tests.put("event_hallucination", helper -> runs(helper, "hallucination", player -> {
            BlockPos centre = helper.absolutePos(FEET);
            for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-22, -1, -22), centre.offset(22, -1, 22))) {
                helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
            }
        }, () -> check(helper, helper.getLevel().getEntitiesOfClass(Entity.class, new AABB(helper.absolutePos(FEET)).inflate(30), e -> !(e instanceof ServerPlayer)).isEmpty(),
            "hallucination must leave nothing on the server")));
        PADDING.put("event_moving_tree", 20);
        tests.put("event_moving_tree", helper -> runs(helper, "moving_tree", player -> tree(helper), () -> {
            BlockPos centre = helper.absolutePos(FEET);
            check(helper, !helper.getLevel().getBlockState(centre.east(6)).is(BlockTags.LOGS), "moving_tree must leave the trunk's old place");
            check(helper, BlockPos.betweenClosedStream(centre.offset(-12, 0, -12), centre.offset(12, 6, 12))
                .filter(pos -> helper.getLevel().getBlockState(pos).is(BlockTags.LOGS)).count() == 3, "moving_tree must keep the trunk whole");
        }));
        tests.put("event_drowned_fish", helper -> {
            Entity[] cod = new Entity[1];
            runs(helper, "drowned_fish", player -> cod[0] = EntityTypes.COD.spawn(helper.getLevel(), helper.absolutePos(FEET).east(4), EntitySpawnReason.COMMAND),
                () -> check(helper, !cod[0].isAlive(), "drowned_fish must kill the fish"));
        });
        tests.put("event_broken_boat", helper -> {
            Entity[] boat = new Entity[1];
            runs(helper, "broken_boat", player -> boat[0] = EntityTypes.OAK_BOAT.spawn(helper.getLevel(), helper.absolutePos(FEET).east(4), EntitySpawnReason.COMMAND),
                () -> check(helper, !boat[0].isAlive(), "broken_boat must break the boat"));
        });
        tests.put("event_disguise", helper -> {
            Entity[] cow = new Entity[1];
            runs(helper, "disguise", player -> cow[0] = EntityTypes.COW.spawn(helper.getLevel(), helper.absolutePos(FEET).east(4), EntitySpawnReason.COMMAND),
                () -> check(helper, cow[0].isAlive() && helper.getLevel().getEntitiesOfClass(Entity.class, new AABB(helper.absolutePos(FEET)).inflate(10),
                    e -> e.getType() == EntityTypes.SHEEP).isEmpty(), "disguise must leave the cow a cow on the server"));
        });
        tests.put("event_cracking_floor", helper -> runs(helper, "cracking_floor", player -> {},
            () -> helper.assertBlockPresent(Blocks.STONE, FEET.below())));
        tests.put("event_hostile_animal", helper -> runs(helper, "hostile_animal",
            player -> helper.spawn(EntityTypes.COW, FEET.east(2)), () -> {}));
        tests.put("event_note_block_voice", helper -> runs(helper, "note_block_voice",
            player -> helper.setBlock(FEET.west(2), Blocks.NOTE_BLOCK), () -> helper.assertBlockPresent(Blocks.NOTE_BLOCK, FEET.west(2))));
        tests.put("event_spider_overhead", helper -> runs(helper, "spider_overhead",
            player -> helper.setBlock(FEET.above(4), Blocks.STONE), () -> {}));
        tests.put("event_lost_growth", helper -> runs(helper, "lost_growth", player -> {
            helper.setBlock(FEET.south(2).below(), Blocks.FARMLAND);
            helper.setBlock(FEET.south(2), ((CropBlock) Blocks.WHEAT).getStateForAge(7));
            // Random ticks may grow it a stage again before the check, so "lost" is: no longer ripe.
        }, () -> check(helper, helper.getBlockState(FEET.south(2)).getValue(CropBlock.AGE) < CropBlock.MAX_AGE, "lost_growth must take the growth")));
        farOff(tests, "distant_player", 60, Mannequin.class, (helper, figure) -> {
            var look = figure.getProfile().skinPatch();
            check(helper, look.body().isPresent() && look.model().isPresent(), "the stranger must wear one of the default skins");
            check(helper, look.body().get().id().getPath().contains(look.model().get().getSerializedName()), "the skin must match its arm width");
        });
        farOff(tests, "distant_walker", 45, Mannequin.class);
        farOff(tests, "nametag", 30, Display.TextDisplay.class);
        farOff(tests, "pillar", 24, Mannequin.class, (helper, figure) ->
            check(helper, helper.getLevel().getBlockState(figure.blockPosition().below()).is(Blocks.COBBLESTONE), "pillar must stand on its cobblestone"));
    }

    /**
     * An event that puts one {@code kind} up to {@code distance} blocks out, given a ring of standing room there wherever
     * it picks its side; the one is checked and removed.
     */
    private static void farOff(Map<String, Consumer<GameTestHelper>> tests, String id, int distance, Class<? extends Entity> kind) {
        farOff(tests, id, distance, kind, (helper, found) -> {});
    }

    private static <T extends Entity> void farOff(Map<String, Consumer<GameTestHelper>> tests, String id, int distance, Class<T> kind,
                                                  BiConsumer<GameTestHelper, T> also) {
        PADDING.put("event_" + id, 2 * (distance + 10));
        tests.put("event_" + id, helper -> runs(helper, id, player -> {
            BlockPos centre = helper.absolutePos(FEET);
            int reach = distance + 4;
            // A player's view distance keeps the ring loaded in a game; here only the test's own chunks are.
            forceChunks(helper, centre, reach, true);
            for (int x = -reach; x <= reach; x++) {
                for (int z = -reach; z <= reach; z++) {
                    double ring = Math.hypot(x, z);
                    if (ring >= distance - 2 && ring <= distance + 2) {
                        helper.getLevel().setBlock(centre.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
                    }
                }
            }
        }, () -> {
            AABB around = new AABB(helper.absolutePos(FEET)).inflate(distance + 10);
            List<T> found = helper.getLevel().getEntitiesOfClass(kind, around);
            check(helper, found.size() == 1, id + " must leave one " + kind.getSimpleName() + ", found " + found.size());
            also.accept(helper, found.get(0));
            found.get(0).discard();
            forceChunks(helper, helper.absolutePos(FEET), distance + 4, false);
        }));
    }

    private static void forceChunks(GameTestHelper helper, BlockPos centre, int reach, boolean forced) {
        for (int x = (centre.getX() - reach) >> 4; x <= (centre.getX() + reach) >> 4; x++) {
            for (int z = (centre.getZ() - reach) >> 4; z <= (centre.getZ() + reach) >> 4; z++) {
                helper.getLevel().setChunkForced(x, z, forced);
            }
        }
    }

    /** A three-log oak with a crown, six blocks east of the player, on dirt as far as it can walk. */
    static void tree(GameTestHelper helper) {
        BlockPos centre = helper.absolutePos(FEET);
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-12, -1, -12), centre.offset(12, -1, 12))) {
            helper.getLevel().setBlock(pos, Blocks.DIRT.defaultBlockState(), 2);
        }
        BlockPos trunk = centre.east(6);
        for (int dy = 0; dy < 3; dy++) {
            helper.getLevel().setBlock(trunk.above(dy), Blocks.OAK_LOG.defaultBlockState(), 2);
        }
        for (BlockPos pos : BlockPos.betweenClosed(trunk.offset(-1, 2, -1), trunk.offset(1, 3, 1))) {
            if (!pos.equals(trunk.above(2))) {
                helper.getLevel().setBlock(pos, Blocks.OAK_LEAVES.defaultBlockState(), 2);
            }
        }
    }

    /** Sets the scene, checks the event can run, runs it, lets its scheduled steps finish, then checks the result. */
    static void runs(GameTestHelper helper, String id, Consumer<ServerPlayer> scene, Runnable after) {
        for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 9, 0, 9)) {
            helper.setBlock(pos, Blocks.STONE);
        }
        ServerPlayer player = connected(helper);
        BlockPos feet = helper.absolutePos(FEET);
        player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
        scene.accept(player);
        HauntEvent event = HauntEvents.byId(Identifier.withDefaultNamespace(id)).orElseThrow();
        check(helper, event.canRun(player), id + " must be able to run in its scene");
        event.run(player);
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            after.run();
            check(helper, player.isAlive(), id + " must not kill");
            player.discard();
            helper.succeed();
        });
    }

    /**
     * A survival player with a connection that swallows packets. Kept out of the player list, which would send it the
     * mod's payloads, and a mock connection has no channels for them.
     */
    private static ServerPlayer connected(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
        return player;
    }

    static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) throw helper.assertionException(Component.literal(message));
    }
}
