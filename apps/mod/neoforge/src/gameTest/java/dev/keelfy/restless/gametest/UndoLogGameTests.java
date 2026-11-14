package dev.keelfy.restless.gametest;

import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.state.HauntData;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.Blocks;

/** A swapped block is put back after a crash, from the saved undo log; under hardcore nothing is saved. */
final class UndoLogGameTests {
    private static final BlockPos AT = new BlockPos(1, 1, 1);

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("swap_is_undone_after_a_crash", helper -> {
            var server = helper.getLevel().getServer();
            HauntData data = HauntData.get(server);
            BlockPos at = helper.absolutePos(AT);
            helper.setBlock(AT, Blocks.STONE);
            Restore.swap(helper.getLevel(), at, Blocks.GLASS.defaultBlockState(), 1000);
            HauntData.Undo undo = data.undos().stream()
                    .filter(u -> u.at().pos().equals(at))
                    .findFirst()
                    .orElse(null);
            HauntEventGameTests.check(helper, undo != null, "the swap must be saved");
            var saved = HauntData.codec().encodeStart(NbtOps.INSTANCE, data).getOrThrow();
            HauntData loaded = HauntData.codec().parse(NbtOps.INSTANCE, saved).getOrThrow();
            HauntEventGameTests.check(helper, loaded.undos().contains(undo), "the undo must survive a save and load");
            // Other tests' undos came along in the save; this test only loads its own.
            loaded.undos().stream().filter(u -> !u.equals(undo)).forEach(loaded::removeUndo);
            // A crash: the live entry is lost without its undo, and the server was down past the due time.
            Restore.lose(undo);
            data.removeUndo(undo);
            loaded.removeUndo(undo);
            loaded.addUndo(new HauntData.Undo(undo.at(), undo.from(), undo.to(), 0L));
            helper.assertBlockPresent(Blocks.GLASS, AT);
            Restore.resume(server, loaded);
            helper.runAfterDelay(3, () -> {
                helper.assertBlockPresent(Blocks.STONE, AT);
                HauntEventGameTests.check(helper, loaded.undos().isEmpty(), "a done undo must leave the log");
                helper.succeed();
            });
        });
        HardcoreGameTests.hardcore(tests, "swap_is_not_saved_in_hardcore", helper -> {
            HauntData data = HauntData.get(helper.getLevel().getServer());
            helper.setBlock(AT, Blocks.STONE);
            Restore.swap(helper.getLevel(), helper.absolutePos(AT), Blocks.GLASS.defaultBlockState(), 2);
            HauntEventGameTests.check(
                    helper,
                    data.undos().stream().noneMatch(u -> u.at().pos().equals(helper.absolutePos(AT))),
                    "hardcore must save nothing");
            helper.runAfterDelay(6, () -> {
                helper.assertBlockPresent(Blocks.GLASS, AT);
                helper.succeed();
            });
        });
    }
}
