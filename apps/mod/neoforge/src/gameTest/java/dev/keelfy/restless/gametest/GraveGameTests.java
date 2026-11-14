package dev.keelfy.restless.gametest;

import dev.keelfy.restless.spirit.Spirits;
import dev.keelfy.restless.spirit.land.Graves;
import dev.keelfy.restless.spirit.land.Quest;
import dev.keelfy.restless.state.HauntData;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.LongStream;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Unfinished business: each {@link Quest} from reading the gravestone to the spirit at rest. */
final class GraveGameTests {
    private GraveGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("grave_item_quest_rests_on_the_wanted_thing", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            HauntData data = HauntData.get(helper.getLevel().getServer());
            HauntData.Haunting spirit = spirit(Quest.ITEM);
            data.put(player.getUUID(), List.of(spirit));
            BlockPos grave = grave(helper, data, spirit);
            ItemStack other = new ItemStack(Graves.wanted(spirit.seed()) == Items.DIRT ? Items.STONE : Items.DIRT);
            check(helper, !Graves.visit(player, grave, other), "the wrong thing must only read the stone");
            check(helper, data.find(spirit.uuid()).isPresent(), "reading must not lay it to rest");
            check(
                    helper,
                    Graves.visit(player, grave, new ItemStack(Graves.wanted(spirit.seed()))),
                    "the wanted thing must be taken");
            check(helper, data.find(spirit.uuid()).isEmpty(), "the wanted thing must lay it to rest");
            done(data, player);
            helper.succeed();
        });
        tests.put("grave_rival_quest_rests_once_the_rival_is_gone", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            HauntData data = HauntData.get(helper.getLevel().getServer());
            HauntData.Haunting spirit = spirit(Quest.RIVAL);
            HauntData.Haunting rival = spirit(Quest.ITEM);
            data.put(player.getUUID(), List.of(spirit, rival));
            BlockPos grave = grave(helper, data, spirit);
            Graves.visit(player, grave, ItemStack.EMPTY);
            check(
                    helper,
                    data.graves()
                            .get(spirit.uuid())
                            .mark()
                            .filter(rival.uuid()::equals)
                            .isPresent(),
                    "reading must name the other spirit as the killer");
            Graves.visit(player, grave, ItemStack.EMPTY);
            check(helper, data.find(spirit.uuid()).isPresent(), "it must not rest while the rival haunts");
            data.change(rival.uuid(), h -> null);
            Graves.visit(player, grave, ItemStack.EMPTY);
            check(helper, data.find(spirit.uuid()).isEmpty(), "it must rest once the rival is gone");
            done(data, player);
            helper.succeed();
        });
    }

    /** A personal spirit whose seed draws {@code quest}. */
    static HauntData.Haunting spirit(Quest quest) {
        long seed = LongStream.iterate(1, s -> s + 1)
                .filter(s -> Quest.of(s) == quest)
                .findFirst()
                .orElseThrow();
        return new HauntData.Haunting(
                UUID.randomUUID(), Spirits.personal().getFirst().id(), seed, List.of(), 0L, 1.0, 0L, false, 0, 0L);
    }

    /** A built grave for the spirit inside the test area. */
    static BlockPos grave(GameTestHelper helper, HauntData data, HauntData.Haunting spirit) {
        BlockPos grave = helper.absolutePos(new BlockPos(1, 2, 1));
        data.putGrave(spirit.uuid(), new HauntData.Grave(grave, true));
        return grave;
    }

    static void done(HauntData data, ServerPlayer player) {
        data.put(player.getUUID(), List.of());
        player.discard();
    }

    static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }
}
