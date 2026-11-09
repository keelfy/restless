package dev.keelfy.restless.gametest;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;

/** The data tags that replaced hardcoded block and item groups hold exactly what the code used to check. */
final class TagGameTests {
    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("tags_match_old_checks", helper -> {
            blocks(
                    helper,
                    "glass",
                    b -> b.defaultBlockState().is(BlockTags.IMPERMEABLE)
                            || BuiltInRegistries.BLOCK.getKey(b).getPath().endsWith("glass_pane"));
            blocks(
                    helper,
                    "burstable_lights",
                    Set.of(
                            Blocks.TORCH,
                            Blocks.WALL_TORCH,
                            Blocks.SOUL_TORCH,
                            Blocks.SOUL_WALL_TORCH,
                            Blocks.LANTERN,
                            Blocks.SOUL_LANTERN)::contains);
            blocks(helper, "knockable", Set.of(Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL)::contains);
            blocks(helper, "diggable", Set.of(Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL)::contains);
            blocks(helper, "furnaces", Set.of(Blocks.FURNACE, Blocks.SMOKER, Blocks.BLAST_FURNACE)::contains);
            blocks(
                    helper,
                    "hand_built",
                    b -> b.defaultBlockState().is(BlockTags.PLANKS)
                            || b.defaultBlockState().is(BlockTags.WOOL)
                            || b.defaultBlockState().is(BlockTags.STONE_BRICKS)
                            || b == Blocks.BRICKS
                            || b == Blocks.BOOKSHELF);
            items(
                    helper,
                    "stews",
                    Set.of(Items.MUSHROOM_STEW, Items.BEETROOT_SOUP, Items.RABBIT_STEW, Items.SUSPICIOUS_STEW)
                            ::contains);
            items(
                    helper,
                    "household_food",
                    Set.of(
                            Items.BREAD,
                            Items.COOKIE,
                            Items.MILK_BUCKET,
                            Items.MUSHROOM_STEW,
                            Items.RABBIT_STEW,
                            Items.BEETROOT_SOUP)::contains);
            items(helper, "potions", Set.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION)::contains);
            items(
                    helper,
                    "held_lights",
                    Set.of(Items.TORCH, Items.SOUL_TORCH, Items.LANTERN, Items.SOUL_LANTERN)::contains);
            // Vanilla tags that stand in for class checks.
            for (Block block : BuiltInRegistries.BLOCK) {
                HauntEventGameTests.check(
                        helper,
                        block.defaultBlockState().is(BlockTags.DOORS) == block instanceof DoorBlock,
                        "doors tag must match DoorBlock for " + BuiltInRegistries.BLOCK.getKey(block));
                HauntEventGameTests.check(
                        helper,
                        block.defaultBlockState().is(BlockTags.FENCE_GATES) == block instanceof FenceGateBlock,
                        "fence_gates tag must match FenceGateBlock for " + BuiltInRegistries.BLOCK.getKey(block));
            }
            helper.succeed();
        });
    }

    private static void blocks(GameTestHelper helper, String tag, Predicate<Block> old) {
        TagKey<Block> key = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("restless", tag));
        for (Block block : BuiltInRegistries.BLOCK) {
            HauntEventGameTests.check(
                    helper,
                    block.defaultBlockState().is(key) == old.test(block),
                    tag + " tag differs from the old check for " + BuiltInRegistries.BLOCK.getKey(block));
        }
    }

    private static void items(GameTestHelper helper, String tag, Predicate<Item> old) {
        TagKey<Item> key = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("restless", tag));
        for (Item item : BuiltInRegistries.ITEM) {
            HauntEventGameTests.check(
                    helper,
                    item.getDefaultInstance().is(key) == old.test(item),
                    tag + " tag differs from the old check for " + BuiltInRegistries.ITEM.getKey(item));
        }
    }
}
