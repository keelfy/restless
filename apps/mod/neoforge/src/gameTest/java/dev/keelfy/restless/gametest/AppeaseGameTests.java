package dev.keelfy.restless.gametest;

import dev.keelfy.restless.banish.Circle;
import dev.keelfy.restless.banish.Phase;
import dev.keelfy.restless.banish.Ritual;
import dev.keelfy.restless.content.block.Altar;
import dev.keelfy.restless.sanity.SanityPayload;
import dev.keelfy.restless.state.HauntData;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

/** What a spirit desires, laid on a bare altar, calms it; each time costs one more; anything else does nothing. */
final class AppeaseGameTests {
    private static final UUID SPIRIT = UUID.fromString("00000000-0000-0000-0000-0000000000a6");
    private static final List<String> TRAITS = List.of("door_slammer", "meddler", "light_eater", "wants_bread");

    private AppeaseGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("appease_desire_calms_and_costs_more_each_time", helper -> {
            ServerPlayer player = ready(helper, TRAITS, 0.8);
            check(helper, offer(helper, player, new ItemStack(Items.BREAD, 1)).isEmpty(), "one bread is taken");
            check(helper, Math.abs(anger(player) - 0.5) < 1e-9, "anger falls by 0.3, got " + anger(player));
            ItemStack one = offer(helper, player, new ItemStack(Items.BREAD, 1));
            check(helper, one.getCount() == 1, "the second time one bread is not enough");
            check(helper, Math.abs(anger(player) - 0.5) < 1e-9, "a short offering changes nothing");
            ItemStack left = offer(helper, player, new ItemStack(Items.BREAD, 3));
            check(helper, left.getCount() == 1, "the second time takes two, left " + left.getCount());
            check(helper, Math.abs(anger(player) - 0.2) < 1e-9, "anger falls again, got " + anger(player));
            check(helper, HauntData.get(helper.getLevel().getServer()).find(SPIRIT).isPresent(), "the spirit stays");
            player.discard();
            helper.succeed();
        });
        tests.put("appease_wrong_desire_does_nothing", helper -> {
            ServerPlayer player = ready(helper, TRAITS, 0.8);
            ItemStack milk = offer(helper, player, new ItemStack(Items.MILK_BUCKET));
            check(helper, milk.getCount() == 1, "what it does not want is not taken");
            check(helper, anger(player) == 0.8, "anger stays");
            HauntData.get(helper.getLevel().getServer())
                    .put(player.getUUID(), List.of(spirit(List.of("door_slammer", "meddler", "light_eater"), 0.8)));
            check(
                    helper,
                    offer(helper, player, new ItemStack(Items.BREAD)).getCount() == 1,
                    "a spirit with no desire takes nothing");
            player.discard();
            helper.succeed();
        });
        tests.put("appease_bargain_asks_for_the_desire", helper -> {
            ServerPlayer player = ready(helper, TRAITS, 0.5);
            RitualGameTests.ringAndCandles(helper);
            BlockPos altar = helper.absolutePos(AltarGameTests.CENTER);
            check(helper, Ritual.begin(player, helper.getLevel(), altar, Altar.Held.IRON), "the ritual must start");
            Circle circle = Circle.at(altar).orElseThrow();
            RitualGameTests.calmSteps(helper, circle, circle.plan().call() + 1);
            check(helper, circle.phase().orElseThrow() == Phase.BARGAIN, "the bargain follows the call");
            BlockPos above = altar.above();
            helper.getLevel()
                    .addFreshEntity(new ItemEntity(
                            helper.getLevel(),
                            above.getX() + 0.5,
                            above.getY(),
                            above.getZ() + 0.5,
                            new ItemStack(Items.BREAD)));
            check(helper, !circle.step(), "the desired bread is the gift and ends the ritual");
            check(helper, Math.abs(anger(player) - 0.2) < 1e-9, "the gift calms it, got " + anger(player));
            player.discard();
            helper.succeed();
        });
    }

    /** Uses {@code stack} on the altar's bowl; what is left in the hand. */
    private static ItemStack offer(GameTestHelper helper, ServerPlayer player, ItemStack stack) {
        BlockPos altar = helper.absolutePos(AltarGameTests.CENTER);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(altar).add(0, 0.5, 0), Direction.UP, altar, false);
        helper.getLevel().getBlockState(altar).useItemOn(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        return player.getItemInHand(InteractionHand.MAIN_HAND);
    }

    private static ServerPlayer ready(GameTestHelper helper, List<String> traits, double anger) {
        AltarGameTests.place(helper);
        ServerPlayer player = HauntEventGameTests.connected(helper);
        ChannelAttributes.getOrCreateCommonChannels(player.connection.getConnection(), ConnectionProtocol.PLAY)
                .add(SanityPayload.TYPE.id());
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(AltarGameTests.CENTER.above())));
        HauntData.get(helper.getLevel().getServer()).put(player.getUUID(), List.of(spirit(traits, anger)));
        return player;
    }

    private static HauntData.Haunting spirit(List<String> traits, double anger) {
        return new HauntData.Haunting(SPIRIT, "poltergeist", 7L, traits, 0L, anger, Long.MAX_VALUE / 2, false, 0, 0L);
    }

    private static double anger(ServerPlayer player) {
        return HauntData.get(player.level().getServer()).find(SPIRIT).orElseThrow().anger();
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }
}
