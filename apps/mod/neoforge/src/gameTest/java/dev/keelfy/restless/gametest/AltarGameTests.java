package dev.keelfy.restless.gametest;

import dev.keelfy.restless.content.Altar;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.haunt.Scan;
import dev.keelfy.restless.haunt.Wards;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Runs against a real server world; this source set is excluded from the shipped mod. */
@Mod("restless_tests")
public final class AltarGameTests {
    private static final BlockPos CENTER = new BlockPos(4, 1, 4);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();

    public AltarGameTests(IEventBus bus) {
        DreamcatcherGameTests.register(TESTS);
        LanternGameTests.register(TESTS);
        HeldLightGameTests.register(TESTS);
        HauntEventGameTests.register(TESTS);
        HardcoreGameTests.register(TESTS);
        JournalGameTests.register(TESTS);
        SanityGameTests.register(TESTS);
        TESTS.put("placement", helper -> {
            place(helper);
            check(
                    helper,
                    Altar.isComplete(helper.getLevel(), helper.absolutePos(CENTER)),
                    "All nine parts must be present");
            int entities = 0;
            for (BlockPos pos : BlockPos.betweenClosed(CENTER.offset(-1, 0, -1), CENTER.offset(1, 0, 1))) {
                if (helper.getLevel().getBlockEntity(helper.absolutePos(pos)) != null) entities++;
            }
            check(helper, entities == 1, "Only the bowl may have a block entity");
            helper.succeed();
        });
        TESTS.put("obstruction", helper -> {
            floor(helper);
            helper.setBlock(CENTER.offset(1, 0, 1), Blocks.STONE);
            ItemStack stack = placeItem(helper, CENTER, false);
            check(helper, stack.getCount() == 2, "Failed placement must not consume an item");
            helper.assertBlockPresent(Blocks.AIR, CENTER);
            helper.assertBlockPresent(Blocks.STONE, CENTER.offset(1, 0, 1));
            helper.succeed();
        });
        TESTS.put("missing_floor", helper -> {
            floor(helper);
            helper.setBlock(CENTER.offset(-1, -1, -1), Blocks.AIR);
            placeItem(helper, CENTER, false);
            helper.assertBlockPresent(Blocks.AIR, CENTER);
            helper.succeed();
        });
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos offset = new BlockPos(x, 0, z);
                String suffix = "_" + (x + 1) + "_" + (z + 1);
                TESTS.put("break" + suffix, helper -> {
                    place(helper);
                    BlockPos center = helper.absolutePos(CENTER);
                    helper.getLevel()
                            .setBlockAndUpdate(
                                    center,
                                    helper.getLevel().getBlockState(center).setValue(Altar.HELD, Altar.Held.WATER));
                    helper.getLevel().destroyBlock(center.offset(offset), true);
                    assertRemoved(helper);
                    check(
                            helper,
                            drops(helper, Content.ALTAR_ITEM.get()) == 1,
                            "Breaking any part must drop exactly one altar; got "
                                    + drops(helper, Content.ALTAR_ITEM.get()));
                    check(helper, drops(helper, Items.POTION) == 1, "Breaking must return the offering once");
                    helper.succeed();
                });
                TESTS.put("creative_break" + suffix, helper -> {
                    place(helper);
                    BlockPos at = helper.absolutePos(CENTER.offset(offset));
                    BlockState state = helper.getLevel().getBlockState(at);
                    Player player = helper.makeMockPlayer(GameType.CREATIVE);
                    GameType.CREATIVE.updatePlayerAbilities(player.getAbilities());
                    state.getBlock().playerWillDestroy(helper.getLevel(), at, state, player);
                    helper.getLevel().destroyBlock(at, false);
                    assertRemoved(helper);
                    check(
                            helper,
                            drops(helper, Content.ALTAR_ITEM.get()) == 0,
                            "Creative removal must not drop the altar");
                    helper.succeed();
                });
                TESTS.put("floor_break" + suffix, helper -> {
                    place(helper);
                    helper.getLevel()
                            .destroyBlock(
                                    helper.absolutePos(CENTER.offset(offset).below()), false);
                    assertRemoved(helper);
                    check(
                            helper,
                            drops(helper, Content.ALTAR_ITEM.get()) == 1,
                            "Losing support must dismantle and drop one altar; got "
                                    + drops(helper, Content.ALTAR_ITEM.get()));
                    helper.succeed();
                });
            }
        }
        TESTS.put("neighbouring_altars", helper -> {
            place(helper);
            BlockPos other = CENTER.offset(3, 0, 0);
            placeItem(helper, other, true);
            helper.getLevel().destroyBlock(helper.absolutePos(CENTER.east()), true);
            assertRemoved(helper);
            check(
                    helper,
                    Altar.isComplete(helper.getLevel(), helper.absolutePos(other)),
                    "Dismantling must not touch the adjacent altar");
            helper.succeed();
        });
        TESTS.put("salt_ring", helper -> {
            place(helper);
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (Math.abs(x) == 2 || Math.abs(z) == 2)
                        helper.setBlock(CENTER.offset(x, 0, z), Content.SALT_LINE.get());
                }
            }
            check(
                    helper,
                    Wards.ringed(helper.getLevel(), helper.absolutePos(CENTER)),
                    "The platform must let the salt scan reach the ring");
            helper.setBlock(CENTER.offset(2, 0, 0), Blocks.AIR);
            // Open the test enclosure itself, otherwise its barrier walls also count as a salt ward.
            for (int x = 9; x <= 11; x++) {
                helper.setBlock(new BlockPos(x, 1, 4), Blocks.AIR);
                helper.setBlock(new BlockPos(x, 2, 4), Blocks.AIR);
            }
            check(
                    helper,
                    !Wards.ringed(helper.getLevel(), helper.absolutePos(CENTER)),
                    "The platform must not seal a gap in the salt");
            helper.succeed();
        });
        TESTS.put("offering_from_corner", helper -> {
            place(helper);
            Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
            ItemStack water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
            BlockPos corner = helper.absolutePos(CENTER.offset(-1, 0, -1));
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(corner), Direction.UP, corner, false);
            helper.getLevel()
                    .getBlockState(corner)
                    .useItemOn(water, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
            helper.assertBlockProperty(CENTER, Altar.HELD, Altar.Held.WATER);
            check(helper, water.isEmpty(), "The offering must be consumed once");
            helper.getLevel()
                    .getBlockTicks()
                    .clearArea(
                            new net.minecraft.world.level.levelgen.structure.BoundingBox(helper.absolutePos(CENTER)));
            helper.getLevel().scheduleTick(helper.absolutePos(CENTER), Content.ALTAR.get(), 1);
            helper.runAfterDelay(2, () -> {
                helper.assertBlockProperty(CENTER, Altar.HELD, Altar.Held.HOLY_WATER);
                helper.getLevel().getBlockState(corner).useWithoutItem(helper.getLevel(), player, hit);
                helper.assertBlockProperty(CENTER, Altar.HELD, Altar.Held.EMPTY);
                check(
                        helper,
                        player.getInventory().countItem(Content.HOLY_WATER.get()) == 1,
                        "Any part must return the blessed offering once");
                helper.succeed();
            });
        });
        for (boolean breakCorner : new boolean[] {false, true}) {
            TESTS.put("corner_candles_" + (breakCorner ? "corner_break" : "center_break"), helper -> {
                place(helper);
                var level = helper.getLevel();
                BlockPos center = helper.absolutePos(CENTER);
                level.setBlockAndUpdate(center, level.getBlockState(center).setValue(Altar.HELD, Altar.Held.IRON));
                Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                for (int x : new int[] {-1, 1}) {
                    for (int z : new int[] {-1, 1}) {
                        BlockPos corner = center.offset(x, 0, z);
                        BlockHitResult hit =
                                new BlockHitResult(Vec3.atBottomCenterOf(corner.above()), Direction.UP, corner, false);
                        ItemStack candle = new ItemStack(Items.CANDLE);
                        check(
                                helper,
                                level.getBlockState(corner)
                                                .useItemOn(candle, level, player, InteractionHand.MAIN_HAND, hit)
                                        == net.minecraft.world.InteractionResult.PASS,
                                "A candle must reach item placement without taking the offering");
                        check(
                                helper,
                                ((BlockItem) candle.getItem())
                                        .place(new BlockPlaceContext(
                                                level, player, InteractionHand.MAIN_HAND, candle, hit))
                                        .consumesAction(),
                                "A corner stand must support a real candle");
                        check(helper, candle.isEmpty(), "Placing a candle must consume it once");
                        BlockPos above = corner.above();
                        check(
                                helper,
                                level.getBlockState(above).canSurvive(level, above),
                                "The tray must support the candle");
                        level.setBlockAndUpdate(
                                above, level.getBlockState(above).setValue(CandleBlock.LIT, true));
                    }
                }
                int[] lit = {0};
                Scan.each(level, center, 3, (pos, state) -> {
                    if (AbstractCandleBlock.isLit(state)) lit[0]++;
                });
                check(helper, lit[0] == 4, "The ritual scan must see four lit candles on the stands");
                helper.assertBlockProperty(CENTER, Altar.HELD, Altar.Held.IRON);
                BlockPos candle = center.offset(-1, 1, -1);
                AbstractCandleBlock.extinguish(null, level.getBlockState(candle), level, candle);
                check(
                        helper,
                        !AbstractCandleBlock.isLit(level.getBlockState(candle)),
                        "A spirit must be able to snuff a stand's candle");
                level.destroyBlock(breakCorner ? center.offset(-1, 0, -1) : center, true);
                helper.runAfterDelay(2, () -> {
                    for (int x : new int[] {-1, 1}) {
                        for (int z : new int[] {-1, 1}) helper.assertBlockPresent(Blocks.AIR, CENTER.offset(x, 1, z));
                    }
                    check(helper, drops(helper, Items.CANDLE) == 4, "Dismantling must return each placed candle once");
                    helper.succeed();
                });
            });
        }

        DeferredRegister<Consumer<GameTestHelper>> functions =
                DeferredRegister.create(Registries.TEST_FUNCTION, "restless_tests");
        TESTS.forEach((name, test) -> functions.register(name, () -> HardcoreGameTests.inItsBatch(name, test)));
        functions.register(bus);
        bus.addListener((RegisterGameTestsEvent event) -> {
            var environment = event.registerEnvironment(id("altar"));
            // Hardcore is one switch for the whole server, so the tests that need it run in a batch of their own.
            var hardcore = event.registerEnvironment(id("hardcore"));
            TESTS.forEach((name, test) -> event.registerTest(
                    id(name),
                    new FunctionGameTestInstance(
                            ResourceKey.create(Registries.TEST_FUNCTION, id(name)),
                            new TestData<>(
                                    HardcoreGameTests.BATCH.contains(name) ? hardcore : environment,
                                    net.minecraft.world.level.Level.OVERWORLD,
                                    id("empty"),
                                    HauntEventGameTests.TIMEOUT.getOrDefault(name, 40),
                                    0,
                                    true,
                                    net.minecraft.world.level.block.Rotation.NONE,
                                    false,
                                    1,
                                    1,
                                    true,
                                    HauntEventGameTests.PADDING.getOrDefault(name, 0)))));
        });
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath("restless_tests", name);
    }

    private static void floor(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 9, 0, 9)) helper.setBlock(pos, Blocks.STONE);
    }

    private static void place(GameTestHelper helper) {
        floor(helper);
        ItemStack stack = placeItem(helper, CENTER, true);
        check(helper, stack.getCount() == 1, "Placing nine parts must consume one item");
    }

    private static ItemStack placeItem(GameTestHelper helper, BlockPos relative, boolean expected) {
        BlockPos floor = helper.absolutePos(relative.below());
        ItemStack stack = new ItemStack(Content.ALTAR_ITEM.get(), 2);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false);
        var context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit);
        boolean placed = ((BlockItem) stack.getItem()).place(context).consumesAction();
        check(helper, placed == expected, "Unexpected placement result");
        return stack;
    }

    private static int drops(GameTestHelper helper, net.minecraft.world.item.Item item) {
        return helper
                .getLevel()
                .getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(CENTER)).inflate(2))
                .stream()
                .filter(entity -> entity.getItem().is(item))
                .mapToInt(entity -> entity.getItem().getCount())
                .sum();
    }

    private static void assertRemoved(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(CENTER.offset(-1, 0, -1), CENTER.offset(1, 0, 1))) {
            helper.assertBlockPresent(Blocks.AIR, pos);
        }
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) throw helper.assertionException(net.minecraft.network.chat.Component.literal(message));
    }
}
