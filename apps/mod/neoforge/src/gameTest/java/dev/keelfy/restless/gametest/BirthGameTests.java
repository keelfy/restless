package dev.keelfy.restless.gametest;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.BirthRules.Verdict;
import dev.keelfy.restless.haunt.Births;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.HauntData;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;

final class BirthGameTests {
    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("birth_rules", helper -> {
            var player = HauntEventGameTests.connected(helper);
            var server = helper.getLevel().getServer();
            var data = HauntData.get(server);
            long now = server.overworld().getGameTime();
            data.put(player.getUUID(), List.of());

            HauntEventGameTests.check(
                    helper,
                    Births.deed(player, Births.Deed.SLAUGHTER, false) == Verdict.BORN,
                    "A deed must bind a spirit");
            var own = data.get(player.getUUID());
            HauntEventGameTests.check(
                    helper,
                    own.size() == 1 && own.get(0).spirit().equals("revenant"),
                    "Slaughter must bring a revenant");
            double start = Config.section("births").num("start_anger");
            HauntEventGameTests.check(
                    helper,
                    Math.abs(own.get(0).anger() - start) < 1e-9 && Director.power(own.get(0), now) > 0.1,
                    "A born spirit must start angry, not harmless");
            HauntEventGameTests.check(
                    helper,
                    own.get(0).nextEventAt() >= now + Config.section("births").integer("grace_ticks"),
                    "A born spirit must wait before its first event");

            HauntEventGameTests.check(
                    helper,
                    Births.deed(player, Births.Deed.FAR_DEATH, false) == Verdict.COOLDOWN
                            && data.get(player.getUUID()).size() == 1,
                    "A second birth inside the cooldown must do nothing");
            HauntEventGameTests.check(
                    helper,
                    Births.deed(player, Births.Deed.PET_SLAIN, false) == Verdict.FED,
                    "The same type must be fed");
            HauntEventGameTests.check(
                    helper,
                    data.get(player.getUUID()).size() == 1
                            && data.get(player.getUUID()).get(0).anger()
                                    > start + Config.section("births").num("repeat_anger") - 1e-9,
                    "Feeding must raise the anger of the spirit that is there");
            player.discard();

            var creative = (net.minecraft.server.level.ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
            HauntEventGameTests.check(
                    helper,
                    Births.deed(creative, Births.Deed.SLAUGHTER, false) == Verdict.NOTHING,
                    "Creative players must not be haunted");
            creative.discard();
            helper.succeed();
        });
    }
}
