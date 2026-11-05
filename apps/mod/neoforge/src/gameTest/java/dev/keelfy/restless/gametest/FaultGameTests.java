package dev.keelfy.restless.gametest;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.DoorLockPayload;
import dev.keelfy.restless.haunt.DoorLocks;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.Faults;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntEvents;
import dev.keelfy.restless.haunt.HauntTier;
import dev.keelfy.restless.haunt.Location;
import dev.keelfy.restless.haunt.Rarity;
import dev.keelfy.restless.haunt.Recent;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.SanityPayload;
import dev.keelfy.restless.haunt.Scheduler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

/** A broken event, step or undo is logged and skipped, and the tick goes on; no shipped event throws on a bare plot. */
final class FaultGameTests {
    /**
     * The smoke test fires every event at once, so it runs apart, with room around it; stopping the server wipes the
     * state of every test running beside it, so that test is here too.
     */
    static final Set<String> BATCH =
            Set.of("every_event_survives_a_bare_plot", "stopping_forgets_the_world", ScaleGameTests.MANY);

    private static final int SETTLE_TICKS = 60;

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("throwing_event_is_skipped", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            HauntEvent runs = throwing("fault_run", false);
            HauntEvent checks = throwing("fault_can_run", true);
            check(helper, !Director.fire(player, runs), "a throwing run must report that it did not run");
            check(helper, Faults.broken(runs), "a throwing run must turn the event off");
            check(helper, !Faults.canRun(runs, player), "a broken event must not be rolled again");
            check(helper, !Faults.canRun(checks, player), "a throwing canRun must read as nothing to act on");
            check(helper, Faults.broken(checks), "a throwing canRun must turn the event off");
            check(helper, !Faults.broken(HauntEvents.all().getFirst()), "other events must stay on");
            player.discard();
            helper.succeed();
        });
        tests.put("throwing_step_and_undo_are_dropped", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            int[] after = {0};
            Scheduler.later(player, 1, p -> {
                throw new IllegalStateException("test step");
            });
            Scheduler.later(player, 2, p -> after[0]++);
            Restore.after(helper.getLevel(), 1, force -> {
                throw new IllegalStateException("test undo");
            });
            Restore.after(helper.getLevel(), 2, force -> {
                after[0]++;
                return true;
            });
            helper.runAfterDelay(5, () -> {
                check(helper, after[0] == 2, "the tasks after a failure must still run");
                player.discard();
                helper.succeed();
            });
        });
        tests.put("delayed_step_keeps_the_spirit_point", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            // Like a modded client: firing an event sends the sanity update to it.
            ChannelAttributes.getOrCreateCommonChannels(player.connection.getConnection(), ConnectionProtocol.PLAY)
                    .add(SanityPayload.TYPE.id());
            Vec3 point = new Vec3(10, 20, 30);
            Vec3 fallback = new Vec3(0, 0, 0);
            Vec3[] seen = new Vec3[2];
            HauntEvent schedules = new HauntEvent() {
                @Override
                public Identifier id() {
                    // Firing records the id against the config, so it has to be a shipped one.
                    return HauntEvents.all().getFirst().id();
                }

                @Override
                public HauntTier tier() {
                    return HauntTier.values()[0];
                }

                @Override
                public Set<EventTag> tags() {
                    return Set.of();
                }

                @Override
                public boolean canRun(ServerPlayer p) {
                    return true;
                }

                @Override
                public void run(ServerPlayer p) {
                    Scheduler.later(p, 1, q -> seen[0] = Location.source(fallback));
                    Restore.after(helper.getLevel(), 1, force -> {
                        seen[1] = Location.source(fallback);
                        return true;
                    });
                }
            };
            check(helper, Director.fire(player, schedules, point), "the scheduling event must run");
            helper.runAfterDelay(5, () -> {
                check(helper, point.equals(seen[0]), "a delayed step must sound from its spirit's point: " + seen[0]);
                check(helper, point.equals(seen[1]), "a delayed undo must sound from its spirit's point: " + seen[1]);
                player.discard();
                helper.succeed();
            });
        });
        tests.put("locked_door_carries_the_servers_refusal_sound", helper -> {
            BlockPos door = helper.absolutePos(new BlockPos(2, 1, 2));
            helper.setBlock(new BlockPos(2, 1, 2), Blocks.OAK_DOOR);
            DoorLocks.lock(helper.getLevel(), door, 100);
            Config.Tone sent = Config.event("locked_door").tone("denied_sound");
            check(
                    helper,
                    sent.equals(DoorLocks.denial(helper.getLevel(), door)),
                    "a lock must keep the server's sound");
            DoorLockPayload payload = new DoorLockPayload(door, 100, sent.volume(), sent.pitch());
            ByteBuf buf = Unpooled.buffer();
            DoorLockPayload.CODEC.encode(buf, payload);
            check(helper, payload.equals(DoorLockPayload.CODEC.decode(buf)), "the sound must survive the wire");
            helper.succeed();
        });
        tests.put("stopping_forgets_the_world", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            BlockPos door = helper.absolutePos(new BlockPos(2, 1, 2));
            helper.setBlock(new BlockPos(2, 1, 2), Blocks.OAK_DOOR);
            DoorLocks.lock(helper.getLevel(), door, 100_000);
            Recent.record(player.getUUID(), "some_event", 5);
            Rarity.observe("some_event", false);
            int[] ran = {0};
            Scheduler.later(player, 5, p -> ran[0]++);
            check(helper, DoorLocks.isLocked(helper.getLevel(), door), "the door must be locked before the stop");
            Restless.onServerStopping();
            check(helper, !DoorLocks.isLocked(helper.getLevel(), door), "a stopped server must not keep a door locked");
            check(helper, !Recent.blocks(player.getUUID(), "some_event"), "a stopped server must forget the history");
            check(helper, Rarity.boost("some_event") == 1.0, "a stopped server must forget the availability");
            helper.runAfterDelay(10, () -> {
                check(helper, ran[0] == 0, "a step scheduled before the stop must not outlive it");
                player.discard();
                helper.succeed();
            });
        });
        HauntEventGameTests.PADDING.put("every_event_survives_a_bare_plot", 60);
        HauntEventGameTests.TIMEOUT.put("every_event_survives_a_bare_plot", SETTLE_TICKS + 20);
        tests.put("every_event_survives_a_bare_plot", helper -> {
            for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 9, 0, 9)) {
                helper.setBlock(pos, Blocks.STONE);
            }
            BlockPos feet = helper.absolutePos(HauntEventGameTests.FEET);
            List<String> failed = new ArrayList<>();
            List<ServerPlayer> players = new ArrayList<>();
            ServerPlayer player = null;
            for (HauntEvent event : HauntEvents.all()) {
                if (player == null || !player.isAlive()) {
                    player = HauntEventGameTests.connected(helper);
                    // Like a modded client: the sanity events send their update to it.
                    ChannelAttributes.getOrCreateCommonChannels(
                                    player.connection.getConnection(), ConnectionProtocol.PLAY)
                            .add(SanityPayload.TYPE.id());
                    player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
                    players.add(player);
                }
                String stage = "canRun";
                try {
                    if (event.canRun(player)) {
                        stage = "run";
                        event.run(player);
                    }
                } catch (RuntimeException e) {
                    failed.add(event.id() + " in " + stage + ": " + e);
                }
            }
            check(helper, failed.isEmpty(), "events threw on a bare plot: " + String.join("; ", failed));
            helper.runAfterDelay(SETTLE_TICKS, () -> {
                players.forEach(ServerPlayer::discard);
                helper.succeed();
            });
        });
    }

    private static HauntEvent throwing(String name, boolean inCanRun) {
        Identifier id = Identifier.fromNamespaceAndPath("restless_tests", name);
        return new HauntEvent() {
            @Override
            public Identifier id() {
                return id;
            }

            @Override
            public HauntTier tier() {
                return HauntTier.values()[0];
            }

            @Override
            public Set<EventTag> tags() {
                return Set.of();
            }

            @Override
            public boolean canRun(ServerPlayer player) {
                if (inCanRun) {
                    throw new IllegalStateException("test canRun");
                }
                return true;
            }

            @Override
            public void run(ServerPlayer player) {
                throw new IllegalStateException("test run");
            }
        };
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }
}
