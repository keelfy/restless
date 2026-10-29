package dev.keelfy.restless.gametest;

import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntEvents;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;

/** Each packet-lie and sound event runs on a connected player and leaves the world as it was (or, for 110, as it says). */
final class HauntEventGameTests {
    private static final BlockPos FEET = new BlockPos(4, 1, 4);
    /** Long enough for every lie to send its truth back. */
    private static final int SETTLE_TICKS = 30;

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        for (String id : List.of("false_wound", "false_hearts", "false_hunger", "guardian_face", "false_totem", "subliminal_title",
            "red_vignette", "own_voice", "false_death", "private_rain", "silent_lightning", "distant_mob", "bottle_shatter")) {
            tests.put("event_" + id, helper -> runs(helper, id, player -> {}, () -> {}));
        }
        tests.put("event_cracking_floor", helper -> runs(helper, "cracking_floor", player -> {},
            () -> helper.assertBlockPresent(Blocks.STONE, FEET.below())));
        tests.put("event_hostile_animal", helper -> runs(helper, "hostile_animal",
            player -> helper.spawn(EntityTypes.COW, FEET.east(2)), () -> {}));
        tests.put("event_note_block_voice", helper -> runs(helper, "note_block_voice",
            player -> helper.setBlock(FEET.west(2), Blocks.NOTE_BLOCK), () -> helper.assertBlockPresent(Blocks.NOTE_BLOCK, FEET.west(2))));
        tests.put("event_spider_overhead", helper -> runs(helper, "spider_overhead",
            player -> helper.setBlock(FEET.above(4), Blocks.STONE), () -> {}));
        tests.put("event_lost_growth", helper -> runs(helper, "lost_growth", player -> {
            helper.setBlock(FEET.south(2).below(), Blocks.FARMLAND);
            helper.setBlock(FEET.south(2), ((CropBlock) Blocks.WHEAT).getStateForAge(7));
        }, () -> helper.assertBlockProperty(FEET.south(2), CropBlock.AGE, 0)));
    }

    /** Sets the scene, checks the event can run, runs it, lets its scheduled steps finish, then checks the result. */
    private static void runs(GameTestHelper helper, String id, Consumer<ServerPlayer> scene, Runnable after) {
        for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 9, 0, 9)) {
            helper.setBlock(pos, Blocks.STONE);
        }
        ServerPlayer player = connected(helper);
        BlockPos feet = helper.absolutePos(FEET);
        player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
        scene.accept(player);
        HauntEvent event = HauntEvents.byId(Identifier.withDefaultNamespace(id)).orElseThrow();
        check(helper, event.canRun(player), id + " must be able to run in its scene");
        event.run(player);
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            after.run();
            check(helper, player.isAlive(), id + " must not kill");
            player.discard();
            helper.succeed();
        });
    }

    /**
     * A survival player with a connection that swallows packets. Kept out of the player list, which would send it the
     * mod's payloads, and a mock connection has no channels for them.
     */
    private static ServerPlayer connected(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
        return player;
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) throw helper.assertionException(Component.literal(message));
    }
}
