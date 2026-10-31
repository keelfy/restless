package dev.keelfy.restless.gametest;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.UnlitLights;
import dev.keelfy.restless.haunt.HauntData;
import dev.keelfy.restless.haunt.Sanity;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

final class SanityGameTests {
    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("sanity_held_light", helper -> {
            var player = HauntEventGameTests.connected(helper);
            var feet = new BlockPos(4, 2, 4);
            // Seal out skylight and wait for the light engine before comparing sanity rates.
            for (var pos : BlockPos.betweenClosed(feet.offset(-2, -1, -2), feet.offset(2, 3, 2))) {
                helper.setBlock(pos, Blocks.STONE);
            }
            helper.setBlock(feet, Blocks.AIR);
            helper.setBlock(feet.above(), Blocks.AIR);
            var absolute = helper.absolutePos(feet);
            player.snapTo(absolute.getX() + .5, absolute.getY(), absolute.getZ() + .5);
            player.getFoodData().setFoodLevel(10);
            helper.runAfterDelay(5, () -> {
                var config = Config.section("sanity");
                HauntEventGameTests.check(helper,
                    player.level().getMaxLocalRawBrightness(player.blockPosition()) < config.integer("dark_light"),
                    "The sanity test must start in darkness");
                double baseline = drift(player);
                double protectedRate = baseline + config.num("dark_loss") * (1 - config.num("held_light_loss_multiplier"));
                HauntEventGameTests.check(helper, baseline < 0 && protectedRate > baseline && protectedRate < 0,
                    "Held light must slow darkness loss without restoring sanity");
                for (var hand : InteractionHand.values()) {
                    for (var item : new net.minecraft.world.item.Item[]{Items.TORCH, Items.SOUL_TORCH, Items.COPPER_TORCH,
                            Items.REDSTONE_TORCH, Items.LANTERN, Items.SOUL_LANTERN, Items.GLOWSTONE, Items.SEA_LANTERN,
                            Content.HALLOWED_LANTERN_ITEM.get(), Items.LAVA_BUCKET}) {
                        player.setItemInHand(hand, new ItemStack(item));
                        rate(helper, player, protectedRate, "Every held light must reduce darkness loss in either hand");
                    }
                    for (var item : UnlitLights.ITEMS) {
                        player.setItemInHand(hand, new ItemStack(item.get()));
                        rate(helper, player, baseline, "Extinguished lights must not protect sanity");
                    }
                    for (var item : new net.minecraft.world.item.Item[]{Items.STICK, Items.CANDLE, Items.REDSTONE_LAMP}) {
                        player.setItemInHand(hand, new ItemStack(item));
                        rate(helper, player, baseline, "Non-emitting items must not protect sanity");
                    }
                    player.setItemInHand(hand, ItemStack.EMPTY);
                }
                player.getInventory().setItem(9, new ItemStack(Items.LANTERN));
                rate(helper, player, baseline, "A light in the inventory must not protect sanity");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TORCH));
                player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.LANTERN));
                rate(helper, player, protectedRate, "Two held lights must not stack their protection");
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                helper.setBlock(feet.west(), Blocks.GLOWSTONE);
                helper.runAfterDelay(5, () -> {
                    HauntEventGameTests.check(helper,
                        player.level().getMaxLocalRawBrightness(player.blockPosition()) >= config.integer("lit_light"),
                        "The recovery check must run in a lit room");
                    double recovery = drift(player);
                    player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH));
                    rate(helper, player, recovery, "Held light must not alter recovery in a lit area");
                    player.discard();
                    helper.succeed();
                });
            });
        });
    }

    private static void rate(GameTestHelper helper, ServerPlayer player, double expected, String message) {
        HauntEventGameTests.check(helper, Math.abs(drift(player) - expected) < 1e-9, message);
    }

    private static double drift(ServerPlayer player) {
        try {
            var method = Sanity.class.getDeclaredMethod("drift", ServerPlayer.class, HauntData.class, Config.Section.class);
            method.setAccessible(true);
            return (double) method.invoke(null, player, HauntData.get(player.level().getServer()), Config.section("sanity"));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot inspect the server's sanity rate", e);
        }
    }
}
