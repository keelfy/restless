package dev.keelfy.restless.gametest;

import dev.keelfy.restless.banish.Twofold;
import dev.keelfy.restless.haunt.Ticks;
import dev.keelfy.restless.spirit.Focus;
import dev.keelfy.restless.spirit.Traits;
import dev.keelfy.restless.spirit.life.Merging;
import dev.keelfy.restless.state.HauntData;
import dev.keelfy.restless.state.Marks;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;

/** Two old personal spirits of one player become one with both sets of habits and two natures. */
final class MergeGameTests {
    private static final List<String> IRON = List.of("door_slammer", "meddler", "thrower", "knocker", "rain_walker");
    private static final List<String> GLOW = List.of("cold_touch", "blinder", "shadow_caster", "wants_bread");

    private MergeGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("merge_two_old_spirits_into_one_with_two_natures", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            HauntData data = HauntData.get(helper.getLevel().getServer());
            long now = helper.getLevel().getServer().overworld().getGameTime();
            HauntData.Haunting elder = spirit("poltergeist", IRON, now - 20L * Ticks.DAY);
            HauntData.Haunting younger = spirit("shade", GLOW, now - 16L * Ticks.DAY);
            HauntData.Haunting baby = spirit("mara", GLOW, now - 2L * Ticks.DAY);
            data.put(player.getUUID(), List.of(elder, younger, baby));
            BlockPos grave = helper.absolutePos(new BlockPos(1, 1, 1));
            data.putGrave(younger.uuid(), new HauntData.Grave(grave, true));
            try {
                List<HauntData.Haunting> ripe = Merging.ripe(data, player, now);
                check(helper, ripe.size() == 2 && ripe.get(0).uuid().equals(elder.uuid()), "two ripe, eldest first");
                HauntData.Haunting merged = Merging.merge(player, data, ripe.get(0), ripe.get(1));
                List<HauntData.Haunting> own = data.get(player.getUUID());
                check(helper, own.size() == 2, "the younger is swallowed, the baby stays: " + own.size());
                check(helper, merged.traits().containsAll(List.of("cold_touch", "blinder", "shadow_caster")),
                        "the merged spirit carries the younger's habits");
                check(helper, !merged.traits().contains("wants_bread"), "only habits pass over");
                check(helper, Traits.weakness(Traits.of(merged.traits())) == Focus.IRON, "the elder's nature leads");
                check(helper, Twofold.of(data, merged).orElse(null) == Focus.GLOWSTONE, "the second nature");
                check(helper, data.graves().get(younger.uuid()) == null, "only one grave stays");
                check(
                        helper,
                        data.graves().get(elder.uuid()) != null
                                && data.graves().get(elder.uuid()).pos().equals(grave),
                        "the younger's grave passes to the merged spirit");
                var saved = Marks.CODEC.encodeStart(NbtOps.INSTANCE, data.marks(elder.uuid())).getOrThrow();
                Marks loaded = Marks.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
                check(helper, loaded.merged().orElse(null) == Focus.GLOWSTONE, "the second nature survives a save");
                var spirit = HauntData.Haunting.CODEC.encodeStart(NbtOps.INSTANCE, merged).getOrThrow();
                check(
                        helper,
                        HauntData.Haunting.CODEC.parse(NbtOps.INSTANCE, spirit).getOrThrow().traits()
                                .equals(merged.traits()),
                        "the merged habits survive a save");
            } finally {
                data.putMarks(elder.uuid(), Marks.NONE);
                data.putGrave(elder.uuid(), null);
                data.put(player.getUUID(), List.of());
                player.discard();
            }
            helper.succeed();
        });
        tests.put("merge_leaves_echoes_out", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            HauntData data = HauntData.get(helper.getLevel().getServer());
            long now = helper.getLevel().getServer().overworld().getGameTime();
            HauntData.Haunting elder = spirit("poltergeist", IRON, now - 20L * Ticks.DAY);
            HauntData.Haunting echo = spirit("shade", GLOW, now - 20L * Ticks.DAY);
            data.put(player.getUUID(), List.of(elder, echo));
            data.putEcho(echo.uuid(), now + Ticks.DAY);
            try {
                check(helper, Merging.ripe(data, player, now).size() == 1, "an echo never merges");
            } finally {
                data.putEcho(echo.uuid(), 0L);
                data.put(player.getUUID(), List.of());
                player.discard();
            }
            helper.succeed();
        });
    }

    private static HauntData.Haunting spirit(String type, List<String> traits, long bornAt) {
        return new HauntData.Haunting(
                UUID.randomUUID(), type, 3L, traits, bornAt, 0.0, Long.MAX_VALUE / 2, false, 0, 0L);
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }
}
