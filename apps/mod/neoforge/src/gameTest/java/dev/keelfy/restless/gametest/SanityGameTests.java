package dev.keelfy.restless.gametest;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.UnlitLights;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.HauntData;
import dev.keelfy.restless.haunt.HauntEvents;
import dev.keelfy.restless.haunt.Sanity;
import dev.keelfy.restless.haunt.SanityPayload;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

final class SanityGameTests {
    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("event_sanity_crash", helper -> {
            var player = HauntEventGameTests.connected(helper);
            // The mock client must accept the same sanity updates as a connected modded client.
            ChannelAttributes.getOrCreateCommonChannels(player.connection.getConnection(), ConnectionProtocol.PLAY)
                .add(SanityPayload.TYPE.id());
            var event = HauntEvents.byId(Identifier.withDefaultNamespace("sanity_crash")).orElseThrow();
            Sanity.set(player, 1.0);
            HauntEventGameTests.check(helper, event.canRun(player), "The event must run at full sanity");
            Director.fire(player, event, null);
            double expected = Math.max(0, 1 - Config.event("sanity_crash").num("loss")
                - Config.section("sanity").num("event_loss"));
            HauntEventGameTests.check(helper, Math.abs(Sanity.of(player) - expected) < 1e-9,
                "The event must remove its configured loss plus ordinary event shock");
            HauntEventGameTests.check(helper,
                HauntData.get(player.level().getServer()).notebook(player.getUUID()).chapters().contains("sanity"),
                "The sanity chapter must unlock when sanity crashes");
            Sanity.set(player, 0.2);
            Director.fire(player, event, null);
            HauntEventGameTests.check(helper, Sanity.of(player) == 0, "Low sanity must clamp at zero");
            HauntEventGameTests.check(helper, !event.canRun(player), "The event must skip players already at zero");
            event.run(player);
            HauntEventGameTests.check(helper, Sanity.of(player) == 0, "Repeating the event must never restore sanity");
            player.discard();
            helper.succeed();
        });
        tests.put("event_sanity_crash_modes", helper -> {
            var event = HauntEvents.byId(Identifier.withDefaultNamespace("sanity_crash")).orElseThrow();
            for (var mode : new GameType[]{GameType.CREATIVE, GameType.SPECTATOR}) {
                var player = (ServerPlayer) helper.makeMockServerPlayer(mode);
                double before = Sanity.of(player);
                HauntEventGameTests.check(helper, !event.canRun(player), "Protected game modes must be excluded");
                Director.fire(player, event, null);
                HauntEventGameTests.check(helper, Sanity.of(player) == before,
                    "Even a forced sanity crash must preserve creative and spectator sanity");
                player.discard();
            }
            helper.succeed();
        });
        tests.put("sanity_held_light", helper -> {
            var player = HauntEventGameTests.connected(helper);
            var feet = new BlockPos(4, 2, 4);
            // Seal out skylight; the sequence waits for actual light propagation rather than a fixed tick count.
            for (var pos : BlockPos.betweenClosed(feet.offset(-2, -1, -2), feet.offset(2, 3, 2))) {
                helper.setBlock(pos, Blocks.STONE);
            }
            helper.setBlock(feet, Blocks.AIR);
            helper.setBlock(feet.above(), Blocks.AIR);
            var absolute = helper.absolutePos(feet);
            player.snapTo(absolute.getX() + .5, absolute.getY(), absolute.getZ() + .5);
            player.getFoodData().setFoodLevel(10);
            var config = Config.section("sanity");
            helper.startSequence().thenWaitUntil(() -> HauntEventGameTests.check(helper,
                    player.level().getMaxLocalRawBrightness(player.blockPosition()) < config.integer("dark_light"),
                    "The sanity test must start in darkness"))
                .thenExecute(() -> {
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
                })
                .thenWaitUntil(() -> HauntEventGameTests.check(helper,
                        player.level().getMaxLocalRawBrightness(player.blockPosition()) >= config.integer("lit_light"),
                        "The recovery check must run in a lit room"))
                .thenExecute(() -> {
                    double recovery = drift(player);
                    player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH));
                    rate(helper, player, recovery, "Held light must not alter recovery in a lit area");
                    player.discard();
                }).thenSucceed();
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
