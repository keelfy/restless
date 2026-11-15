package dev.keelfy.restless.gametest;

import dev.keelfy.restless.spirit.Spirits;
import dev.keelfy.restless.spirit.land.Graves;
import dev.keelfy.restless.spirit.land.Letter;
import dev.keelfy.restless.spirit.land.Quest;
import dev.keelfy.restless.spirit.land.Remains;
import dev.keelfy.restless.state.HauntData;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.LongStream;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
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
        tests.put("grave_remains_quest_rests_on_the_carried_bones", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            HauntData data = HauntData.get(helper.getLevel().getServer());
            HauntData.Haunting spirit = spirit(Quest.REMAINS);
            data.put(player.getUUID(), List.of(spirit));
            BlockPos grave = grave(helper, data, spirit);
            Graves.visit(player, grave, ItemStack.EMPTY);
            check(helper, data.graves().get(spirit.uuid()).stage() == 1, "reading must begin the search");
            double far =
                    Math.sqrt(Remains.spot(spirit.seed(), grave).distSqr(new BlockPos(grave.getX(), 0, grave.getZ())));
            check(helper, far >= 49 && far <= 151, "the bones must lie 50 to 150 blocks away, got " + far);
            check(helper, Graves.pace(player, spirit) == 1.0, "no bones carried must keep the normal pace");
            ItemStack bones = Remains.bones(spirit);
            player.getInventory().add(bones.copy());
            check(helper, Graves.pace(player, spirit) < 1.0, "carried bones must speed the spirit up");
            check(helper, !Graves.visit(player, grave, new ItemStack(Items.BONE)), "other bones must not count");
            check(helper, Graves.visit(player, grave, bones), "its own bones must be taken");
            check(helper, data.find(spirit.uuid()).isEmpty(), "the bones must lay it to rest");
            done(data, player);
            helper.succeed();
        });
        tests.put("grave_letter_quest_rests_once_the_letter_is_handed_over", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            HauntData data = HauntData.get(helper.getLevel().getServer());
            HauntData.Haunting spirit = spirit(Quest.LETTER);
            data.put(player.getUUID(), List.of(spirit));
            BlockPos grave = grave(helper, data, spirit);
            Graves.visit(player, grave, ItemStack.EMPTY);
            check(helper, data.graves().get(spirit.uuid()).stage() == 1, "reading must give the letter");
            check(helper, player.getInventory().contains(s -> s.is(Items.PAPER)), "the reader must get a letter");
            check(helper, !Letter.handOver(player, data, spirit), "no villager near must not take it");
            player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 1, 2))));
            helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, 2, 1, 2);
            check(helper, Letter.handOver(player, data, spirit), "a villager near must take the letter");
            check(helper, !player.getInventory().contains(s -> s.is(Items.PAPER)), "the letter must be gone");
            check(helper, data.find(spirit.uuid()).isPresent(), "handing over must not rest it yet");
            Graves.visit(player, grave, ItemStack.EMPTY);
            check(helper, data.find(spirit.uuid()).isEmpty(), "the next visit must lay it to rest");
            done(data, player);
            helper.succeed();
        });
        tests.put("grave_killer_quest_rests_once_a_player_kills_the_killer", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            HauntData data = HauntData.get(helper.getLevel().getServer());
            HauntData.Haunting spirit = spirit(Quest.KILLER);
            data.put(player.getUUID(), List.of(spirit));
            BlockPos grave = grave(helper, data, spirit);
            Graves.visit(player, grave, ItemStack.EMPTY);
            UUID mark = data.graves().get(spirit.uuid()).mark().orElseThrow();
            Mob mob = (Mob) helper.getLevel().getEntity(mark);
            check(helper, mob != null && mob.hasCustomName(), "reading must call up a named killer");
            mob.kill(helper.getLevel());
            Graves.killed(null, mob);
            check(helper, data.graves().get(spirit.uuid()).stage() == 0, "a death by other cause must reset it");
            Graves.visit(player, grave, ItemStack.EMPTY);
            mark = data.graves().get(spirit.uuid()).mark().orElseThrow();
            Mob again = (Mob) helper.getLevel().getEntity(mark);
            check(helper, again != null, "the next reading must call it up again");
            again.hurtServer(helper.getLevel(), player.damageSources().playerAttack(player), 1000f);
            Graves.killed(player, again);
            check(helper, data.graves().get(spirit.uuid()).stage() == 2, "a player's kill must settle it");
            Graves.visit(player, grave, ItemStack.EMPTY);
            check(helper, data.find(spirit.uuid()).isEmpty(), "the next visit must lay it to rest");
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
