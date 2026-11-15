package dev.keelfy.restless.gametest;

import dev.keelfy.restless.banish.Ritual;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.block.Altar;
import dev.keelfy.restless.haunt.Ticks;
import dev.keelfy.restless.spirit.land.Graves;
import dev.keelfy.restless.spirit.life.Births;
import dev.keelfy.restless.spirit.life.Echoes;
import dev.keelfy.restless.state.HauntData;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.phys.Vec3;

/** M2: the echo an altar leaves, none after a grave, its leaving, and how it lives beside a successor. */
final class EchoGameTests {
    /** Iron by nature, as in {@link RitualGameTests}. */
    private static final List<String> IRON_TRAITS = List.of("door_slammer", "meddler", "light_eater");

    private EchoGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("echo_after_altar", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            var server = helper.getLevel().getServer();
            HauntData data = HauntData.get(server);
            long now = server.overworld().getGameTime();
            HauntData.Haunting spirit = spirit(UUID.randomUUID(), "shade");
            data.put(player.getUUID(), List.of(spirit));
            banish(player, data, spirit);
            List<HauntData.Haunting> own = data.get(player.getUUID());
            check(helper, own.size() == 1, "the altar must leave one echo behind");
            HauntData.Haunting echo = own.get(0);
            long leaveAt = data.echo(echo.uuid());
            check(
                    helper,
                    !echo.uuid().equals(spirit.uuid())
                            && echo.spirit().equals("shade")
                            && echo.seed() == spirit.seed()
                            && echo.anger() == 0.0
                            && echo.bornAt() >= now,
                    "the echo must be a new, young, calm spirit of the same type and story");
            check(
                    helper,
                    leaveAt == echo.waryUntil()
                            && leaveAt - echo.bornAt()
                                    == (long) (Config.section("births").num("echo_days") * Ticks.DAY),
                    "the echo must leave after the configured days and refuse the altar until then");
            var successor = data.successor(player.getUUID());
            check(
                    helper,
                    successor != null && successor.gone().equals(List.of("shade")),
                    "an echo must not cancel the successor its spirit's leaving started");
            Births.successor(player, data, now);
            check(
                    helper,
                    data.successor(player.getUUID()) != null
                            && data.successor(player.getUUID()).dueAt() > now,
                    "the successor's wait must start beside the echo");
            banish(player, data, echo);
            check(
                    helper,
                    data.echo(echo.uuid()) == 0L && data.get(player.getUUID()).isEmpty(),
                    "an echo sent off must leave no echo of its own");
            data.putSuccessor(player.getUUID(), null);
            player.discard();
            helper.succeed();
        });
        tests.put("echo_refuses_the_altar", helper -> {
            AltarGameTests.place(helper);
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                        helper.setBlock(AltarGameTests.CENTER.offset(x, 0, z), Content.SALT_LINE.get());
                    }
                }
            }
            for (int x : new int[] {-3, 3}) {
                for (int z : new int[] {-3, 3}) {
                    helper.setBlock(
                            AltarGameTests.CENTER.offset(x, 1, z),
                            Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true));
                }
            }
            ServerPlayer player = HauntEventGameTests.connected(helper);
            player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(AltarGameTests.CENTER.above())));
            HauntData data = HauntData.get(helper.getLevel().getServer());
            HauntData.Haunting spirit = spirit(UUID.randomUUID(), "shade");
            data.put(player.getUUID(), List.of(spirit));
            banish(player, data, spirit);
            check(
                    helper,
                    !Ritual.begin(
                            player, helper.getLevel(), helper.absolutePos(AltarGameTests.CENTER), Altar.Held.IRON),
                    "an echo must refuse the altar, so it brings no essence");
            HauntData.Haunting echo = data.get(player.getUUID()).get(0);
            Echoes.leave(data, echo.uuid());
            data.putSuccessor(player.getUUID(), null);
            player.discard();
            helper.succeed();
        });
        tests.put("echo_none_after_grave", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            HauntData data = HauntData.get(helper.getLevel().getServer());
            HauntData.Haunting spirit = spirit(UUID.randomUUID(), "shade");
            data.put(player.getUUID(), List.of(spirit));
            BlockPos grave = helper.absolutePos(new BlockPos(1, 2, 1));
            data.putGrave(spirit.uuid(), new HauntData.Grave(grave, true));
            check(
                    helper,
                    Graves.visit(player, grave, new ItemStack(Graves.wanted(spirit.seed()))),
                    "the grave must take what the spirit wanted");
            check(
                    helper,
                    data.get(player.getUUID()).isEmpty(),
                    "a spirit laid to rest at its grave must leave no echo");
            data.putSuccessor(player.getUUID(), null);
            player.discard();
            helper.succeed();
        });
        tests.put("echo_leaves_by_itself", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            var server = helper.getLevel().getServer();
            HauntData data = HauntData.get(server);
            long now = server.overworld().getGameTime();
            HauntData.Haunting spirit = spirit(UUID.randomUUID(), "shade");
            data.put(player.getUUID(), List.of(spirit));
            banish(player, data, spirit);
            HauntData.Haunting echo = data.get(player.getUUID()).get(0);
            long leaveAt = data.echo(echo.uuid());
            Births.successor(player, data, now);
            long due = data.successor(player.getUUID()).dueAt();
            Echoes.tick(data, leaveAt - 1);
            check(helper, data.find(echo.uuid()).isPresent(), "an echo must stay until its time is up");
            Echoes.tick(data, leaveAt);
            check(
                    helper,
                    data.find(echo.uuid()).isEmpty()
                            && data.echo(echo.uuid()) == 0L
                            && data.get(player.getUUID()).isEmpty(),
                    "an echo must leave by itself when its time is up");
            var waiting = data.successor(player.getUUID());
            check(
                    helper,
                    waiting != null && waiting.dueAt() == due && waiting.gone().equals(List.of("shade")),
                    "an echo leaving must neither cancel nor restart the successor");

            // An echo still there when the successor is due: the successor comes beside it, and a spirit born of the
            // echo's own type takes its place.
            data.put(player.getUUID(), List.of(spirit));
            banish(player, data, spirit);
            Births.successor(player, data, now);
            due = data.successor(player.getUUID()).dueAt();
            Births.successor(player, data, due);
            List<HauntData.Haunting> own = data.get(player.getUUID());
            check(
                    helper,
                    own.size() == 2
                            && own.stream()
                                            .filter(h -> data.echo(h.uuid()) == 0L)
                                            .count()
                                    == 1
                            && data.successor(player.getUUID()) == null,
                    "a successor must come when due even with an echo about");
            data.put(player.getUUID(), List.of());
            data.putSuccessor(player.getUUID(), null);
            data.put(player.getUUID(), List.of(spirit));
            banish(player, data, spirit);
            UUID replaced = data.get(player.getUUID()).get(0).uuid();
            data.putSuccessor(player.getUUID(), null);
            Births.lure(player, dev.keelfy.restless.spirit.Spirits.byId("shade").orElseThrow());
            own = data.get(player.getUUID());
            check(
                    helper,
                    own.size() == 1 && data.echo(own.get(0).uuid()) == 0L && data.echo(replaced) == 0L,
                    "a spirit born of the echo's type must take the echo's place");
            data.put(player.getUUID(), List.of());
            data.putSuccessor(player.getUUID(), null);
            player.discard();
            helper.succeed();
        });
    }

    /** What {@code Ritual.banish} does at the end of a banishing: the spirit leaves the data, then the echo comes. */
    private static void banish(ServerPlayer player, HauntData data, HauntData.Haunting spirit) {
        data.change(spirit.uuid(), h -> null);
        Echoes.banished(player, spirit, player.blockPosition());
    }

    private static HauntData.Haunting spirit(UUID id, String type) {
        return new HauntData.Haunting(id, type, 0x5EED5L, IRON_TRAITS, 0L, 0.0, Long.MAX_VALUE / 2, false, 0, 0L);
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }
}
