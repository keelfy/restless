package dev.keelfy.restless.gametest;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.block.SaltLineBlock;
import dev.keelfy.restless.divine.Divination;
import dev.keelfy.restless.haunt.Calendar;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.event.EventTag;
import dev.keelfy.restless.haunt.event.HauntEvent;
import dev.keelfy.restless.haunt.event.HauntEvents;
import dev.keelfy.restless.haunt.event.HauntTier;
import dev.keelfy.restless.spirit.Clue;
import dev.keelfy.restless.spirit.Trait;
import dev.keelfy.restless.spirit.Traits;
import dev.keelfy.restless.state.HauntData;
import dev.keelfy.restless.ward.Wards;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

/**
 * The date is forced only inside one test body, which never waits a tick, so no other test sees it; the forced date is
 * always cleared again.
 */
final class CalendarGameTests {
    private static final BlockPos CENTER = new BlockPos(4, 1, 4);
    private static final Calendar.Date BLOOD = new Calendar.Date(6, false, true, false);
    private static final Calendar.Date DEAD = new Calendar.Date(27, false, false, true);

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("blood_night_wears_salt_twice_as_fast", helper -> {
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
            BlockPos edge = CENTER.offset(2, 0, 0);
            int factor = Config.section("calendar").integer("blood_wear_factor");
            try {
                Calendar.force(new Calendar.Date(1, false, false, false));
                check(helper, Wards.stops(player, any), "a fresh ring must stop an event");
                check(helper, stops(helper, edge) == 1, "an ordinary night wears one step: " + stops(helper, edge));
                Calendar.force(BLOOD);
                check(helper, Wards.stops(player, any), "the ring still stops an event on a blood night");
                check(
                        helper,
                        stops(helper, edge) == 1 + factor,
                        "a blood night wears " + factor + " steps at once: " + stops(helper, edge));
            } finally {
                Calendar.force(null);
            }
            player.discard();
            helper.succeed();
        });
        tests.put("day_of_the_dead_has_no_harm_and_no_lies", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            var server = helper.getLevel().getServer();
            HauntData data = HauntData.get(server);
            List<String> liars =
                    Traits.all().stream().filter(Trait::lies).map(Trait::id).toList();
            check(helper, !liars.isEmpty(), "the test needs a lying trait");
            // Old and angry: the spirit is at full power, so harm would be likely on any other day.
            var spirit = new HauntData.Haunting(
                    UUID.randomUUID(),
                    "revenant",
                    0x5EED5L,
                    liars,
                    -1000L * 24000,
                    1.0,
                    Long.MAX_VALUE / 2,
                    false,
                    0,
                    0L);
            data.put(player.getUUID(), List.of(spirit));
            try {
                Calendar.force(DEAD);
                for (int i = 0; i < 12; i++) {
                    Director.act(player, spirit, null, null)
                            .ifPresent(event -> check(
                                    helper,
                                    event.tier() != HauntTier.HARMFUL,
                                    "no harmful event on the day of the dead: " + event.id()));
                }
                List<Trait> owned = Traits.of(liars);
                for (int i = 0; i < 80; i++) {
                    Divination.read(player, Clue.VOICE)
                            .ifPresent(reading -> check(
                                    helper, owned.contains(reading.trait()), "no tool lies on the day of the dead"));
                }
                Calendar.force(new Calendar.Date(1, false, false, false));
                boolean lied = false;
                for (int i = 0; i < 80 && !lied; i++) {
                    lied = Divination.read(player, Clue.VOICE)
                            .map(reading -> !owned.contains(reading.trait()))
                            .orElse(false);
                }
                check(helper, lied, "on an ordinary day a liar's voice must lie sometimes");
            } finally {
                Calendar.force(null);
                data.put(player.getUUID(), List.of());
            }
            player.discard();
            helper.succeed();
        });
    }

    private static int stops(GameTestHelper helper, BlockPos edge) {
        return helper.getBlockState(edge).getValue(SaltLineBlock.STOPS);
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }
}
