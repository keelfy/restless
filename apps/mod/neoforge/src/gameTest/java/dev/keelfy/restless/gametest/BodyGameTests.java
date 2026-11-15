package dev.keelfy.restless.gametest;

import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.divine.Body;
import dev.keelfy.restless.haunt.Location;
import dev.keelfy.restless.spirit.Spirits;
import dev.keelfy.restless.state.HauntData;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** The spirit's body: seen at its point through the spirit candle or the camera, and not without them. */
final class BodyGameTests {
    private BodyGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("spirit_body_shows_only_through_the_candle_or_camera", helper -> {
            HauntData data = HauntData.get(helper.getLevel().getServer());
            ServerPlayer holder = HauntEventGameTests.connected(helper);
            ServerPlayer bare = HauntEventGameTests.connected(helper);
            HauntData.Haunting mine = spirit();
            HauntData.Haunting theirs = spirit();
            data.put(holder.getUUID(), List.of(mine));
            data.put(bare.getUUID(), List.of(theirs));
            Vec3 point = Location.roam(mine.uuid(), holder);
            Location.roam(theirs.uuid(), bare);
            holder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Content.SPIRIT_CANDLE.get()));

            Body.update(holder, data);
            Body.update(bare, data);
            check(
                    helper,
                    Body.at(holder, mine.uuid()).equals(Optional.of(point)),
                    "a candle holder must see the body at the spirit's point");
            check(helper, Body.at(bare, theirs.uuid()).isEmpty(), "a player without a tool must not see it");

            bare.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Content.CAMERA_OBSCURA.get()));
            Body.update(bare, data);
            check(helper, Body.at(bare, theirs.uuid()).isPresent(), "the camera must show the body too");

            holder.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            Body.update(holder, data);
            check(helper, Body.at(holder, mine.uuid()).isEmpty(), "lowering the candle must take the body away");

            for (ServerPlayer player : List.of(holder, bare)) {
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                Body.update(player, data);
                data.put(player.getUUID(), List.of());
                player.discard();
            }
            Location.forget(mine.uuid());
            Location.forget(theirs.uuid());
            helper.succeed();
        });
    }

    private static HauntData.Haunting spirit() {
        return new HauntData.Haunting(
                UUID.randomUUID(), Spirits.personal().getFirst().id(), 7L, List.of(), 0L, 1.0, 0L, false, 0, 0L);
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }
}
