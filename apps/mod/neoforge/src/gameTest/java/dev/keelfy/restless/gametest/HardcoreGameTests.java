package dev.keelfy.restless.gametest;

import static dev.keelfy.restless.gametest.HauntEventGameTests.FEET;
import static dev.keelfy.restless.gametest.HauntEventGameTests.check;
import static dev.keelfy.restless.gametest.HauntEventGameTests.runs;

import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.haunt.HauntData;
import dev.keelfy.restless.haunt.Restore;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.AABB;

/**
 * A swapped block goes back after its delay, unless the world is hardcore; and under hardcore the tricks with a real
 * counterpart come true. The hardcore tests run in a batch of their own ({@link #BATCH}), since the switch is one for
 * the whole server.
 */
final class HardcoreGameTests {
    private static final BlockPos AT = new BlockPos(1, 1, 1);
    private static final int DELAY = 5;
    static final Set<String> BATCH = new HashSet<>();

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("swap_reverts", helper -> swapped(helper, Blocks.STONE));
        hardcore(tests, "swap_stays_in_hardcore", helper -> swapped(helper, Blocks.GLASS));
        for (String id : List.of("false_hearts", "false_hunger", "guardian_face", "silent_lightning")) {
            hardcore(tests, "hardcore_" + id, helper -> runs(helper, id, player -> {}, () -> {}));
        }
        hardcore(tests, "hardcore_false_wound", helper -> {
            ServerPlayer[] who = new ServerPlayer[1];
            runs(helper, "false_wound", player -> {
                // A mock client never says it has loaded, and until then the player cannot be hurt.
                player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
                who[0] = player;
            },
                () -> check(helper, who[0].getHealth() < who[0].getMaxHealth() && who[0].getHealth() >= 2.0F, "false_wound must really wound, above a heart"));
        });
        hardcore(tests, "hardcore_false_night", helper -> runs(helper, "false_night", player -> {
            // The earlier batch may leave a different world time; this event requires daylight.
            var level = helper.getLevel();
            var clock = level.registryAccess().getOrThrow(WorldClocks.OVERWORLD);
            level.getServer().clockManager().moveToTimeMarker(clock, ClockTimeMarkers.NOON);
            level.environmentAttributes().invalidateTickCache();
            level.updateSkyBrightness();
        }, () -> {
            check(helper, helper.getLevel().isDarkOutside(), "false_night must really make it night");
            // Back to day, for the tests that need daylight.
            var clock = helper.getLevel().registryAccess().getOrThrow(WorldClocks.OVERWORLD);
            helper.getLevel().getServer().clockManager().moveToTimeMarker(clock, ClockTimeMarkers.NOON);
        }));
        hardcore(tests, "hardcore_false_slot", helper -> {
            ServerPlayer[] who = new ServerPlayer[1];
            runs(helper, "false_slot", player -> who[0] = player,
                () -> check(helper, !who[0].getInventory().isEmpty(), "false_slot must really put the item there"));
        });
        hardcore(tests, "hardcore_cracking_floor", helper -> runs(helper, "cracking_floor",
            player -> helper.getLevel().setBlock(helper.absolutePos(FEET.below(2)), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS),
            () -> helper.assertBlockPresent(Blocks.AIR, FEET.below())));
        hardcore(tests, "hardcore_ceiling_crack", helper -> runs(helper, "ceiling_crack",
            player -> helper.setBlock(FEET.above(3), Blocks.STONE), 90, () -> helper.assertBlockPresent(Blocks.AIR, FEET.above(3))));
        HauntEventGameTests.TIMEOUT.put("hardcore_ceiling_crack", 120);
        hardcore(tests, "hardcore_dispenser_shot", helper -> runs(helper, "dispenser_shot", player -> {
            helper.setBlock(FEET.west(3), Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
            helper.getBlockEntity(FEET.west(3), DispenserBlockEntity.class).setItem(0, new ItemStack(Items.ARROW, 2));
        }, () -> check(helper, helper.getBlockEntity(FEET.west(3), DispenserBlockEntity.class).getItem(0).getCount() == 1
            && !helper.getLevel().getEntitiesOfClass(Arrow.class, new AABB(helper.absolutePos(FEET)).inflate(6)).isEmpty(),
            "dispenser_shot must really spend an arrow")));
        hardcore(tests, "hardcore_false_windows", helper -> runs(helper, "false_windows",
            player -> helper.setBlock(FEET.north(2), Blocks.GLASS), () -> helper.assertBlockNotPresent(Blocks.GLASS, FEET.north(2))));
        hardcore(tests, "hardcore_false_looks", helper -> runs(helper, "false_looks", player -> {
            helper.setBlock(FEET.west(2), Blocks.CRAFTING_TABLE);
            helper.setBlock(FEET.east(2), Blocks.CHEST);
        }, () -> {
            helper.assertBlockNotPresent(Blocks.CRAFTING_TABLE, FEET.west(2));
            // Its contents would have nowhere to go.
            helper.assertBlockPresent(Blocks.CHEST, FEET.east(2));
        }));
        hardcore(tests, "hardcore_ghost_tree", helper -> runs(helper, "ghost_tree", player -> floor(helper, 14), () -> {
            BlockPos centre = helper.absolutePos(FEET);
            check(helper, BlockPos.betweenClosedStream(centre.offset(-14, 0, -14), centre.offset(14, 6, 14))
                .anyMatch(pos -> HauntEventGameTests.ghostLog(helper.getLevel().getBlockState(pos))), "ghost_tree must really stand");
        }));
        HauntEventGameTests.PADDING.put("hardcore_ghost_tree", 30);
        hardcore(tests, "hardcore_torch_line", helper -> runs(helper, "torch_line", player -> HauntEventGameTests.darkWood(helper), () -> {
            BlockPos centre = helper.absolutePos(FEET);
            long torches = BlockPos.betweenClosedStream(centre.offset(-2, 0, 0), centre.offset(2, 0, 34))
                .filter(pos -> helper.getLevel().getBlockState(pos).is(Blocks.TORCH)).count();
            check(helper, torches >= 3, "torch_line must really place its torches, found " + torches);
        }));
        HauntEventGameTests.PADDING.put("hardcore_torch_line", 40);
        hardcore(tests, "hardcore_moving_tree", helper -> runs(helper, "moving_tree", player -> HauntEventGameTests.tree(helper), () -> {
            BlockPos centre = helper.absolutePos(FEET);
            check(helper, !helper.getLevel().getBlockState(centre.east(6)).is(BlockTags.LOGS), "moving_tree must stay moved");
        }));
        HauntEventGameTests.PADDING.put("hardcore_moving_tree", 20);
        hardcore(tests, "hardcore_disguise", helper -> {
            Entity[] cow = new Entity[1];
            runs(helper, "disguise", player -> cow[0] = EntityTypes.COW.spawn(helper.getLevel(), helper.absolutePos(FEET).east(4), EntitySpawnReason.COMMAND), () -> {
                check(helper, !cow[0].isAlive(), "disguise must really replace the cow");
                List<Entity> sheep = helper.getLevel().getEntitiesOfClass(Entity.class, new AABB(helper.absolutePos(FEET)).inflate(10), e -> e.getType() == EntityTypes.SHEEP);
                check(helper, sheep.size() == 1, "disguise must leave one sheep, found " + sheep.size());
                sheep.forEach(Entity::discard);
            });
        });
        HauntEventGameTests.PADDING.put("hardcore_disguise", 20);
        hardcore(tests, "hardcore_hallucination", helper -> runs(helper, "hallucination", player -> floor(helper, 22), () -> {
            List<LivingEntity> mobs = helper.getLevel().getEntitiesOfClass(LivingEntity.class, new AABB(helper.absolutePos(FEET)).inflate(30),
                e -> !(e instanceof ServerPlayer));
            check(helper, mobs.size() == 1, "hallucination must really be there, found " + mobs.size());
            mobs.forEach(LivingEntity::discard);
        }));
        HauntEventGameTests.PADDING.put("hardcore_hallucination", 50);
    }

    /** A test run with the world hardcore, in the hardcore batch. */
    static void hardcore(Map<String, Consumer<GameTestHelper>> tests, String name, Consumer<GameTestHelper> test) {
        BATCH.add(name);
        tests.put(name, test);
    }

    /** Sets the switch for {@code name}'s batch first: batches run one after another, so each batch sees its own. */
    static Consumer<GameTestHelper> inItsBatch(String name, Consumer<GameTestHelper> test) {
        return helper -> {
            HauntData.get(helper.getLevel().getServer()).putHardcore(BATCH.contains(name));
            test.accept(helper);
        };
    }

    private static void floor(GameTestHelper helper, int reach) {
        BlockPos centre = helper.absolutePos(FEET);
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-reach, -1, -reach), centre.offset(reach, -1, reach))) {
            helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static void swapped(GameTestHelper helper, Block expected) {
        helper.setBlock(AT, Blocks.STONE);
        Restore.swap(helper.getLevel(), helper.absolutePos(AT), Blocks.GLASS.defaultBlockState(), DELAY);
        helper.runAfterDelay(DELAY + 5, () -> {
            helper.assertBlockPresent(expected, AT);
            helper.succeed();
        });
    }
}
