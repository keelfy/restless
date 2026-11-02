package dev.keelfy.restless.gametest;

import dev.keelfy.restless.content.Altar;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.haunt.HauntData;
import dev.keelfy.restless.haunt.Ritual;
import dev.keelfy.restless.haunt.SanityPayload;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

/** The altar against a spirit of known nature: right offering, right category in another form, and the wrong one. */
final class RitualGameTests {
    /** Iron by nature: two iron traits and one glowstone. */
    private static final List<String> IRON_TRAITS = List.of("door_slammer", "meddler", "light_eater");

    private static final UUID SPIRIT = UUID.fromString("00000000-0000-0000-0000-000000000042");
    private static final List<BlockPos> CANDLES = List.of(
            AltarGameTests.CENTER.offset(-3, 1, -3),
            AltarGameTests.CENTER.offset(3, 1, -3),
            AltarGameTests.CENTER.offset(-3, 1, 3),
            AltarGameTests.CENTER.offset(3, 1, 3));

    private RitualGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("ritual_off_form_runs_longer", helper -> {
            ServerPlayer player = ready(helper);
            HauntData.Haunting spirit = spirit(player);
            // a poltergeist takes iron whole
            check(helper, Ritual.waves(spirit, Altar.Held.IRON) == 12, "the form it takes keeps the usual length");
            check(helper, Ritual.waves(spirit, Altar.Held.IRON_NUGGET) == 18, "another form must add six waves");
            check(
                    helper,
                    Ritual.waves(spirit.evolve(), Altar.Held.IRON_CHAIN) == 22,
                    "grown and off form must add both");
            check(
                    helper,
                    Ritual.begin(
                            player,
                            helper.getLevel(),
                            helper.absolutePos(AltarGameTests.CENTER),
                            Altar.Held.IRON_CHAIN),
                    "the right category in another form must still start the ritual");
            check(
                    helper,
                    lit(helper) == 4 && wary(player) == 0,
                    "an off-form offering must neither snuff nor frighten the spirit");
            helper.succeed();
        });
        tests.put("ritual_wrong_nature_punishes", helper -> {
            ServerPlayer player = ready(helper);
            ServerLevel level = helper.getLevel();
            BlockPos altar = helper.absolutePos(AltarGameTests.CENTER);
            check(helper, Ritual.begin(player, level, altar, Altar.Held.LANTERN), "a wrong offering is still taken");
            check(helper, lit(helper) == 0, "a wrong category must snuff every candle");
            helper.assertBlockProperty(AltarGameTests.CENTER, Altar.HELD, Altar.Held.EMPTY);
            HauntData.Haunting after =
                    HauntData.get(level.getServer()).find(SPIRIT).orElseThrow();
            check(helper, after.anger() == 1.0, "the spirit must peak in anger");
            check(helper, after.waryUntil() > level.getServer().overworld().getGameTime(), "it must turn wary");
            check(helper, after.bornAt() <= -24000, "it must come of age sooner");
            relight(helper);
            check(
                    helper,
                    !Ritual.begin(player, level, altar, Altar.Held.LANTERN),
                    "a wary spirit must refuse another wrong offering");
            check(
                    helper,
                    !Ritual.begin(player, level, altar, Altar.Held.IRON),
                    "a wary spirit must refuse even its own offering");
            helper.assertBlockProperty(AltarGameTests.CENTER, Altar.HELD, Altar.Held.EMPTY);
            helper.succeed();
        });
    }

    /** An altar with its salt ring and four lit candles, and a player haunted by a poltergeist of iron nature. */
    private static ServerPlayer ready(GameTestHelper helper) {
        AltarGameTests.place(helper);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    helper.setBlock(AltarGameTests.CENTER.offset(x, 0, z), Content.SALT_LINE.get());
                }
            }
        }
        relight(helper);
        ServerPlayer player = HauntEventGameTests.connected(helper);
        ChannelAttributes.getOrCreateCommonChannels(player.connection.getConnection(), ConnectionProtocol.PLAY)
                .add(SanityPayload.TYPE.id());
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(AltarGameTests.CENTER.above())));
        HauntData.get(helper.getLevel().getServer()).put(player.getUUID(), List.of(spirit(null)));
        return player;
    }

    private static HauntData.Haunting spirit(ServerPlayer player) {
        if (player != null) {
            return HauntData.get(player.level().getServer()).find(SPIRIT).orElseThrow();
        }
        return new HauntData.Haunting(
                SPIRIT, "poltergeist", 7L, IRON_TRAITS, 0L, 0.0, Long.MAX_VALUE / 2, false, 0, 0L);
    }

    private static void relight(GameTestHelper helper) {
        for (BlockPos pos : CANDLES) {
            helper.setBlock(pos, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true));
        }
    }

    private static long lit(GameTestHelper helper) {
        return CANDLES.stream()
                .filter(pos -> AbstractCandleBlock.isLit(helper.getLevel().getBlockState(helper.absolutePos(pos))))
                .count();
    }

    private static long wary(ServerPlayer player) {
        return spirit(player).waryUntil();
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }
}
