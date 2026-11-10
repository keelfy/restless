package dev.keelfy.restless.gametest;

import static dev.keelfy.restless.gametest.CharmGameTests.stack;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.banish.Ritual;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.block.HoardersHollow;
import dev.keelfy.restless.content.block.MarasCradle;
import dev.keelfy.restless.content.block.MimicBlockEntity;
import dev.keelfy.restless.content.item.Relics;
import dev.keelfy.restless.content.item.charm.Rest;
import dev.keelfy.restless.haunt.event.EventTag;
import dev.keelfy.restless.haunt.event.HauntEvent;
import dev.keelfy.restless.haunt.event.HauntEvents;
import dev.keelfy.restless.sanity.Sanity;
import dev.keelfy.restless.state.HauntData;
import dev.keelfy.restless.ward.Wards;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The relics: what each does that no charm does, and the limits the design set on it. */
final class RelicGameTests {
    private RelicGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("ferrymans_coin_fades_instead_of_dying_and_then_rests", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            CharmGameTests.sanityChannel(player);
            ServerLevel level = helper.getLevel();
            player.setItemInHand(InteractionHand.OFF_HAND, stack("ferrymans_coin"));
            check(
                    helper,
                    !Restless.onDeath(player, level.damageSources().fellOutOfWorld()),
                    "the void must not be turned");
            check(helper, Restless.onDeath(player, level.damageSources().generic()), "a lethal blow must be turned");
            check(
                    helper,
                    player.getHealth() == player.getMaxHealth() / 2,
                    "the holder must come back with half health");
            check(helper, Relics.faded(player), "the holder must be faded");
            check(helper, Sanity.of(player) == 0.0, "no sanity must be left");
            check(
                    helper,
                    Restless.onHurt(player, level.damageSources().generic(), 5.0f) == 0.0f,
                    "a faded player must be touched by nothing");
            check(
                    helper,
                    !Rest.ready(player.getOffhandItem(), level),
                    "the coin must rest for the days it was promised");
            check(
                    helper,
                    !Restless.onDeath(player, level.damageSources().generic()),
                    "a resting coin must not turn a second blow");
            helper.succeed();
        });
        tests.put("exorcists_censer_shortens_the_ritual_and_angers_the_spirit", helper -> {
            var spirit = new HauntData.Haunting(
                    java.util.UUID.randomUUID(),
                    "poltergeist",
                    7L,
                    List.of(),
                    0L,
                    0.0,
                    Long.MAX_VALUE / 2,
                    false,
                    0,
                    0L);
            check(
                    helper,
                    Ritual.waves(spirit, dev.keelfy.restless.content.block.Altar.Held.IRON) == 12,
                    "a plain ritual");
            check(
                    helper,
                    Ritual.waves(spirit, dev.keelfy.restless.content.block.Altar.Held.IRON, true) == 8,
                    "the censer must cut the ritual to eight waves");
            check(
                    helper,
                    Ritual.waves(spirit.evolve(), dev.keelfy.restless.content.block.Altar.Held.IRON, true) == 12,
                    "a grown spirit must still add its waves");
            helper.succeed();
        });
        tests.put("hollow_lantern_wards_wide_and_stands_once_in_a_chunk", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            ServerLevel level = helper.getLevel();
            // Test spots land anywhere in the world: pin the lantern inside one chunk so its neighbours share it.
            BlockPos spot = helper.absolutePos(new BlockPos(2, 2, 2));
            BlockPos lantern = new BlockPos((spot.getX() & ~15) + 2, spot.getY(), (spot.getZ() & ~15) + 2);
            check(helper, !Relics.hollowLanternIn(level, lantern), "no hollow lantern stands yet");
            level.setBlock(lantern, Content.HOLLOW_LANTERN.get().defaultBlockState(), 3);
            check(helper, Relics.hollowLanternIn(level, lantern.offset(1, 0, 3)), "the chunk must now hold one");
            check(
                    helper,
                    !Relics.hollowLanternIn(level, lantern.offset(48, 0, 0)),
                    "another chunk must still take one");
            HauntEvent any = HauntEvents.all().stream()
                    .filter(e -> e.tags().contains(EventTag.LIGHTS))
                    .findFirst()
                    .orElseThrow();
            player.setPos(Vec3.atBottomCenterOf(lantern.offset(20, -1, 0)));
            check(helper, Wards.stops(player, any), "its ward must reach twenty blocks");
            player.setPos(Vec3.atBottomCenterOf(lantern.offset(30, -1, 0)));
            check(helper, !Wards.stops(player, any), "its ward must end near twenty-four");
            level.setBlock(lantern, Blocks.AIR.defaultBlockState(), 3);
            helper.succeed();
        });
        tests.put("hoarders_hollow_shares_one_store_and_wears_clay", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            ServerLevel level = helper.getLevel();
            var store = Relics.hollow(player);
            store.setItem(4, new ItemStack(Items.DIAMOND, 5));
            check(
                    helper,
                    Relics.hollow(player).getItem(4).is(Items.DIAMOND),
                    "a second hollow must open on the same store");
            check(helper, Relics.pilfer(player), "a hoarder must find a stack to take");
            check(helper, Relics.hollow(player).isEmpty(), "the stack must be gone from every hollow");
            check(helper, !Relics.pilfer(player), "an empty store has nothing to take");
            BlockPos at = helper.absolutePos(new BlockPos(3, 2, 3));
            level.setBlock(at, Content.HOARDERS_HOLLOW.get().defaultBlockState(), 3);
            ItemStack clay = new ItemStack(Content.MIMIC_CLAY.get());
            clay.set(
                    Content.LOOK.get(),
                    new dev.keelfy.restless.content.item.consumable.Look(Blocks.STONE.defaultBlockState()));
            player.setItemInHand(InteractionHand.MAIN_HAND, clay);
            clay.useOn(new UseOnContext(
                    level,
                    player,
                    InteractionHand.MAIN_HAND,
                    clay,
                    new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false)));
            BlockState state = level.getBlockState(at);
            check(helper, state.getValue(HoardersHollow.DISGUISED), "clay must disguise the hollow");
            check(
                    helper,
                    level.getBlockEntity(at) instanceof MimicBlockEntity mimic
                            && mimic.look().is(Blocks.STONE),
                    "the hollow must wear stone");
            level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
            helper.succeed();
        });
        tests.put("maras_cradle_heals_the_mind_and_its_spawn_holds_once", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            CharmGameTests.sanityChannel(player);
            ServerLevel level = helper.getLevel();
            HauntData data = HauntData.get(level.getServer());
            BlockPos head = helper.absolutePos(new BlockPos(4, 2, 4));
            BlockPos foot = head.relative(Direction.SOUTH);
            BlockState base = Content.MARAS_CRADLE.get().defaultBlockState();
            level.setBlock(
                    head,
                    base.setValue(MarasCradle.PART, BedPart.HEAD).setValue(MarasCradle.FACING, Direction.NORTH),
                    3);
            level.setBlock(
                    foot,
                    base.setValue(MarasCradle.PART, BedPart.FOOT).setValue(MarasCradle.FACING, Direction.NORTH),
                    3);
            Relics.slept(player, head);
            check(
                    helper,
                    GlobalPos.of(level.dimension(), head).equals(data.cradle(player.getUUID())),
                    "the cradle must be remembered as the spawn");
            level.setBlock(head, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(foot, Blocks.AIR.defaultBlockState(), 3);
            TeleportTransition missing = TeleportTransition.missingRespawnBlock(player, TeleportTransition.DO_NOTHING);
            TeleportTransition spared = Relics.respawn(player, missing);
            check(helper, spared != null, "a broken cradle must hold the spawn once");
            check(
                    helper,
                    spared.position().equals(Vec3.atBottomCenterOf(head)),
                    "the player must come back where the cradle stood");
            check(helper, Relics.respawn(player, missing) == null, "it must hold only once");
            helper.succeed();
        });
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }
}
