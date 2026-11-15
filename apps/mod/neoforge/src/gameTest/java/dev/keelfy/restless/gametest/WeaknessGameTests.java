package dev.keelfy.restless.gametest;

import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.event.EventTag;
import dev.keelfy.restless.haunt.event.HauntEvent;
import dev.keelfy.restless.haunt.event.HauntEvents;
import dev.keelfy.restless.haunt.event.kit.Swaps;
import dev.keelfy.restless.haunt.recall.Said;
import dev.keelfy.restless.spirit.Focus;
import dev.keelfy.restless.spirit.Traits;
import dev.keelfy.restless.state.HauntData;
import dev.keelfy.restless.ward.Wards;
import dev.keelfy.restless.ward.Weakness;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** The material of a spirit's nature holds it back: iron, glowstone, holy water, a mirror. */
final class WeaknessGameTests {
    private WeaknessGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("weakness_iron_stops_an_objects_spirit_only", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            UUID objects = spirit(player, Focus.IRON, "door_slammer", "restless_hands");
            UUID darkness = spirit(player, Focus.GLOWSTONE, "light_eater", "cold_touch");
            HauntEvent door = tagged(EventTag.DOORS);
            check(helper, !stops(player, objects, door), "no iron near: the door event must run");
            player.level().setBlock(player.blockPosition().east(2), Blocks.IRON_DOOR.defaultBlockState(), 3);
            check(helper, stops(player, objects, door), "an iron door must stop an objects spirit's door event");
            check(helper, !stops(player, darkness, door), "an iron door must not stop a darkness spirit");
            check(helper, !stops(player, objects, tagged(EventTag.SOUND)), "iron must stop only door and item events");
            helper.succeed();
        });
        tests.put("weakness_glowstone_stops_a_darkness_spirit_only", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            UUID darkness = spirit(player, Focus.GLOWSTONE, "light_eater", "cold_touch");
            UUID objects = spirit(player, Focus.IRON, "door_slammer", "restless_hands");
            HauntEvent lights = tagged(EventTag.LIGHTS);
            check(helper, !stops(player, darkness, lights), "no glowstone near: the light event must run");
            player.level().setBlock(player.blockPosition().east(2), Blocks.GLOWSTONE.defaultBlockState(), 3);
            check(helper, stops(player, darkness, lights), "glowstone must stop a darkness spirit's light event");
            check(helper, !stops(player, objects, lights), "glowstone must not stop an objects spirit");
            helper.succeed();
        });
        tests.put("weakness_holy_water_ends_a_malice_spirit_event_only", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            UUID malice = spirit(player, Focus.HOLY_WATER, "vengeful");
            UUID deceit = spirit(player, Focus.MIRROR, "whisperer", "liar");
            Said.during(deceit, () -> Scheduler.later(player, 200, p -> {}));
            check(helper, !Weakness.pour(player), "water must not end a deceit spirit's event");
            Said.during(malice, () -> Scheduler.later(player, 200, p -> {}));
            check(helper, Weakness.pour(player), "water must end a malice spirit's running event");
            check(helper, !Weakness.pour(player), "the ended event must not end twice");
            helper.succeed();
        });
        tests.put("weakness_mirror_unmasks_a_deceit_spirit_only", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            UUID deceit = spirit(player, Focus.MIRROR, "whisperer", "liar");
            UUID malice = spirit(player, Focus.HOLY_WATER, "vengeful");
            BlockPos at = player.blockPosition().east(2);
            Said.during(malice, () -> Swaps.show(player, at, Blocks.GOLD_BLOCK.defaultBlockState(), 200));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Content.MIRROR.get()));
            check(helper, !Weakness.mirror(player), "a mirror must not unmask a malice spirit's fake");
            Said.during(deceit, () -> Swaps.show(player, at.north(), Blocks.GOLD_BLOCK.defaultBlockState(), 200));
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            check(helper, !Weakness.mirror(player), "without a mirror in the hand nothing shows");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Content.MIRROR.get()));
            check(helper, Weakness.mirror(player), "a mirror in the hand must unmask a deceit spirit's fake");
            check(helper, !Weakness.mirror(player), "an unmasked fake must not show twice");
            helper.succeed();
        });
    }

    /** A spirit with these traits near the player; its nature must be {@code nature}. */
    private static UUID spirit(ServerPlayer player, Focus nature, String... traits) {
        HauntData.Haunting born = new HauntData.Haunting(
                UUID.randomUUID(), "poltergeist", 1L, List.of(traits), 0L, 0.0, Long.MAX_VALUE, false, 0, 0L);
        if (Traits.weakness(Traits.of(born.traits())) != nature) {
            throw new IllegalStateException("traits " + born.traits() + " are not of nature " + nature);
        }
        HauntData data = HauntData.get(player.level().getServer());
        List<HauntData.Haunting> all =
                new java.util.ArrayList<>(data.get(player.getUUID()) == null ? List.of() : data.get(player.getUUID()));
        all.add(born);
        data.put(player.getUUID(), all);
        return born.uuid();
    }

    private static HauntEvent tagged(EventTag tag) {
        return HauntEvents.all().stream()
                .filter(e -> e.tags().contains(tag))
                .findFirst()
                .orElseThrow();
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
