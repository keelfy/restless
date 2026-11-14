package dev.keelfy.restless.gametest;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.block.SaltLineBlock;
import dev.keelfy.restless.haunt.event.EventTag;
import dev.keelfy.restless.haunt.event.HauntEvent;
import dev.keelfy.restless.haunt.event.HauntEvents;
import dev.keelfy.restless.ward.Wards;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

final class SaltGameTests {
    private static final BlockPos CENTER = new BlockPos(4, 1, 4);

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("salt_wears_out_and_a_laid_tile_mends_it", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 9, 0, 9)) {
                helper.setBlock(pos, Blocks.STONE);
            }
            // Open the test enclosure itself, otherwise its barrier walls also count as a salt ward.
            for (int x = 9; x <= 11; x++) {
                helper.setBlock(new BlockPos(x, 1, 4), Blocks.AIR);
                helper.setBlock(new BlockPos(x, 2, 4), Blocks.AIR);
            }
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                        helper.setBlock(CENTER.offset(x, 0, z), Content.SALT_LINE.get());
                    }
                }
            }
            BlockPos at = helper.absolutePos(CENTER);
            player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            HauntEvent any = HauntEvents.all().stream()
                    .filter(e -> e.tags().contains(EventTag.LIGHTS))
                    .findFirst()
                    .orElseThrow();
            check(
                    helper,
                    helper.getLevel().getBlockState(at.offset(2, 0, 0)).is(Content.SALT_LINE.get()),
                    "salt must stand");
            check(
                    helper,
                    Wards.ringed(helper.getLevel(), player.blockPosition()),
                    "a fresh ring must close the player in");
            int limit = Config.section("salt").integer("wear_stops");
            for (int i = 0; i < limit; i++) {
                check(helper, Wards.stops(player, any), "a fresh ring must stop event " + (i + 1));
            }
            BlockPos edge = CENTER.offset(2, 0, 0);
            helper.assertBlockProperty(edge, SaltLineBlock.WORN, true);
            check(helper, !Wards.stops(player, any), "a worn ring must stop nothing");
            check(helper, !Wards.ringed(helper.getLevel(), at), "a worn ring must not count as a ring");
            // A tile laid again mends the whole ring, corners too.
            helper.setBlock(edge, Blocks.AIR);
            helper.setBlock(edge, Content.SALT_LINE.get());
            helper.assertBlockProperty(CENTER.offset(-2, 0, -2), SaltLineBlock.WORN, false);
            helper.assertBlockProperty(CENTER.offset(0, 0, 2), SaltLineBlock.WORN, false);
            check(helper, Wards.stops(player, any), "a mended ring must stop events again");
            player.discard();
            helper.succeed();
        });
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }
}
