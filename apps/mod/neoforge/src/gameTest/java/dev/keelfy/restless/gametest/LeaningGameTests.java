package dev.keelfy.restless.gametest;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.haunt.event.HauntEvent;
import dev.keelfy.restless.haunt.event.HauntEvents;
import dev.keelfy.restless.haunt.recall.Said;
import dev.keelfy.restless.spirit.life.Leanings;
import dev.keelfy.restless.state.HauntData;
import dev.keelfy.restless.ward.Wards;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** What a spirit fears, wants and hates: a cat keeps a cat-fearing spirit off, bread calms it, a bell angers it. */
final class LeaningGameTests {
    private LeaningGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("leaning_fear_keeps_the_spirit_off_beside_a_cat", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            UUID afraid = spirit(player, 0.0, "door_slammer", "restless_hands", "fears_cats");
            UUID bold = spirit(player, 0.0, "door_slammer", "restless_hands", "fears_fire");
            HauntEvent event = HauntEvents.all().getFirst();
            check(helper, !stops(player, afraid, event), "no cat near: the event must run");
            EntityTypes.CAT.spawn(player.level(), player.blockPosition().east(2), EntitySpawnReason.COMMAND);
            check(helper, stops(player, afraid, event), "a cat beside the player must stop a cat-fearing spirit");
            check(helper, !stops(player, bold, event), "a cat must not stop a spirit that fears fire");
            helper.succeed();
        });
        tests.put("leaning_desire_takes_bread_and_calms_the_spirit", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            UUID hungry = spirit(player, 0.6, "door_slammer", "restless_hands", "wants_bread");
            UUID other = spirit(player, 0.6, "door_slammer", "restless_hands", "wants_milk");
            ItemEntity bread = new ItemEntity(
                    player.level(), player.getX() + 1, player.getY(), player.getZ(), new ItemStack(Items.BREAD));
            player.level().addFreshEntity(bread);
            Leanings.offered(player);
            check(helper, bread.isRemoved(), "the bread must be taken");
            check(helper, anger(player, hungry) < 0.6, "the bread must calm the spirit that wants it");
            check(helper, anger(player, other) == 0.6, "the bread must not calm a spirit that wants milk");
            helper.succeed();
        });
        tests.put("leaning_hate_angers_the_spirit_when_the_bell_rings", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            UUID hater = spirit(player, 0.0, "door_slammer", "restless_hands", "hates_bells");
            UUID other = spirit(player, 0.0, "door_slammer", "restless_hands", "hates_music");
            BlockPos bell = player.blockPosition().east(1);
            player.level().setBlock(bell, Blocks.BELL.defaultBlockState(), 3);
            Restless.onUseBlock(player, InteractionHand.MAIN_HAND, bell);
            check(helper, anger(player, hater) > 0.0, "ringing a bell must anger the spirit that hates bells");
            check(helper, anger(player, other) == 0.0, "a bell must not anger a spirit that hates music");
            double once = anger(player, hater);
            Restless.onUseBlock(player, InteractionHand.MAIN_HAND, bell);
            check(helper, anger(player, hater) == once, "a second ring at once must not anger it again");
            helper.succeed();
        });
    }

    private static UUID spirit(ServerPlayer player, double anger, String... traits) {
        HauntData.Haunting born = new HauntData.Haunting(
                UUID.randomUUID(), "shade", 1L, List.of(traits), 0L, anger, Long.MAX_VALUE, false, 0, 0L);
        HauntData data = HauntData.get(player.level().getServer());
        List<HauntData.Haunting> all =
                new ArrayList<>(data.get(player.getUUID()) == null ? List.of() : data.get(player.getUUID()));
        all.add(born);
        data.put(player.getUUID(), all);
        return born.uuid();
    }

    private static double anger(ServerPlayer player, UUID spirit) {
        return HauntData.get(player.level().getServer())
                .find(spirit)
                .orElseThrow()
                .anger();
    }

    private static boolean stops(ServerPlayer player, UUID spirit, HauntEvent event) {
        boolean[] stopped = {false};
        Said.during(spirit, () -> stopped[0] = Wards.stops(player, event));
        return stopped[0];
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }
}
