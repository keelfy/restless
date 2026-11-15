package dev.keelfy.restless.gametest;

import dev.keelfy.restless.haunt.effect.DoorLocks;
import dev.keelfy.restless.haunt.event.HauntEvent;
import dev.keelfy.restless.haunt.event.HauntEvents;
import dev.keelfy.restless.haunt.recall.Said;
import dev.keelfy.restless.sanity.Sanity;
import dev.keelfy.restless.state.HauntData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Above anger 0.7 a habit's tell turns harsh: one variant per nature, mild below the threshold, harsh above. */
final class HarshGameTests {
    private HarshGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        variant(
                tests,
                "door_slammer",
                List.of("door_slammer", "restless_hands"),
                player -> {
                    BlockPos door = player.blockPosition().east(2);
                    player.level().setBlock(door, Blocks.OAK_DOOR.defaultBlockState(), 3);
                    player.level()
                            .setBlock(
                                    door.above(),
                                    Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER),
                                    3);
                },
                player -> DoorLocks.isLocked(
                        player.level(), player.blockPosition().east(2)));
        variant(
                tests,
                "cold_touch",
                List.of("cold_touch", "light_eater"),
                player -> player.setTicksFrozen(0),
                player -> player.getTicksFrozen() > 0);
        variant(
                tests,
                "vengeful",
                List.of("vengeful", "hunter"),
                player -> {
                    // A mock client never says it has loaded, and until then the player cannot be hurt.
                    player.connection.handleAcceptPlayerLoad(
                            new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
                    player.setHealth(player.getMaxHealth());
                },
                player -> player.getHealth() < player.getMaxHealth());
        variant(
                tests,
                "whisperer",
                List.of("whisperer", "liar"),
                player -> Sanity.set(player, 1.0),
                player -> Sanity.of(player) < 1.0);
    }

    /**
     * @param setup readies the scene, run before each try
     * @param harsh whether the harsh form left its mark
     */
    private static void variant(
            Map<String, Consumer<GameTestHelper>> tests,
            String habit,
            List<String> traits,
            Consumer<ServerPlayer> setup,
            Predicate<ServerPlayer> harsh) {
        tests.put("harsh_" + habit, helper -> {
            HauntEvent tell = HauntEvents.all().stream()
                    .filter(e -> habit.equals(e.habit()))
                    .findFirst()
                    .orElseThrow();
            ServerPlayer player = HauntEventGameTests.connected(helper);
            setup.accept(player);
            Said.during(spirit(player, traits, 0.5), () -> tell.run(player));
            HauntEventGameTests.check(helper, !harsh.test(player), habit + " must stay mild below anger 0.7");
            setup.accept(player);
            Said.during(spirit(player, traits, 0.9), () -> tell.run(player));
            HauntEventGameTests.check(helper, harsh.test(player), habit + " must turn harsh above anger 0.7");
            helper.succeed();
        });
    }

    private static UUID spirit(ServerPlayer player, List<String> traits, double anger) {
        HauntData.Haunting born =
                new HauntData.Haunting(UUID.randomUUID(), "shade", 3L, traits, 0L, anger, Long.MAX_VALUE, false, 0, 0L);
        HauntData.get(player.level().getServer()).put(player.getUUID(), new ArrayList<>(List.of(born)));
        return born.uuid();
    }
}
