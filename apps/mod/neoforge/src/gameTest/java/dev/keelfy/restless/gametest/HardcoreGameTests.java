package dev.keelfy.restless.gametest;

import dev.keelfy.restless.haunt.HauntData;
import dev.keelfy.restless.haunt.Restore;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

/** A swapped block goes back after its delay, unless the world is hardcore. */
final class HardcoreGameTests {
    private static final BlockPos AT = new BlockPos(1, 1, 1);
    private static final int DELAY = 5;

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("swap_reverts", helper -> swapped(helper, false, Blocks.STONE));
        tests.put("swap_stays_in_hardcore", helper -> swapped(helper, true, Blocks.GLASS));
    }

    private static void swapped(GameTestHelper helper, boolean hardcore, net.minecraft.world.level.block.Block expected) {
        HauntData data = HauntData.get(helper.getLevel().getServer());
        data.putHardcore(hardcore);
        helper.setBlock(AT, Blocks.STONE);
        Restore.swap(helper.getLevel(), helper.absolutePos(AT), Blocks.GLASS.defaultBlockState(), DELAY);
        helper.runAfterDelay(DELAY + 5, () -> {
            data.putHardcore(false);
            helper.assertBlockPresent(expected, AT);
            helper.succeed();
        });
    }
}
