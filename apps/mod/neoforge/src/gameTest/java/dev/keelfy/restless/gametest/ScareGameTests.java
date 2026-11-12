package dev.keelfy.restless.gametest;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.haunt.History;
import dev.keelfy.restless.haunt.event.HauntEvent;
import dev.keelfy.restless.haunt.event.HauntEvents;
import dev.keelfy.restless.haunt.event.Scares;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** The scares the player sets off: each is armed or started, the habit is acted out, and the figure is sent. */
final class ScareGameTests {
    private static final BlockPos FEET = HauntEventGameTests.FEET;
    private static final int LIGHT_TICKS = 5;
    /** Looks at every {@code LIGHT_TICKS} for the light to be worked out, up to {@code LIGHT_SLACK} ticks in all. */
    private static final int LIGHT_SLACK = 100;

    private static final int LIGHT_WAIT = LIGHT_SLACK / LIGHT_TICKS;

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        HauntEventGameTests.PADDING.put("scare_slow_turn", 40);
        HauntEventGameTests.TIMEOUT.put("scare_hide_still", 300);
        HauntEventGameTests.TIMEOUT.put("scare_container_close", 300);
        tests.put(
                "scare_mined_face",
                helper -> scene(helper, "mined_face", scene -> {
                    BlockPos wall = helper.absolutePos(FEET).above().south();
                    for (int i = 0; i < 3; i++) {
                        set(helper, wall.south(i), Blocks.STONE.defaultBlockState());
                    }
                    Restless.onBreakBlock(scene.player, Blocks.STONE.defaultBlockState(), wall);
                    set(helper, wall, Blocks.AIR.defaultBlockState());
                    helper.runAfterDelay(6, () -> {
                        check(helper, scene.figures() == 1, "mined_face must show one figure");
                        check(helper, scene.blockUpdates() >= 2, "mined_face must open the rock behind the hole");
                        scene.end(helper);
                    });
                }));
        tests.put(
                "scare_dig_down",
                helper -> scene(helper, "dig_down", scene -> {
                    BlockPos hole = helper.absolutePos(FEET).below().east(2);
                    for (int i = 0; i < 3; i++) {
                        set(helper, hole.below(i), Blocks.STONE.defaultBlockState());
                    }
                    Restless.onBreakBlock(scene.player, Blocks.STONE.defaultBlockState(), hole);
                    helper.runAfterDelay(6, () -> {
                        check(helper, scene.figures() == 1, "dig_down must show one figure");
                        check(helper, scene.blockUpdates() >= 2, "dig_down must open the rock under the hole");
                        scene.end(helper);
                    });
                }));
        tests.put(
                "scare_dig_down_not_under_feet",
                helper -> scene(helper, "dig_down", scene -> {
                    BlockPos under = helper.absolutePos(FEET).below();
                    Restless.onBreakBlock(scene.player, Blocks.STONE.defaultBlockState(), under);
                    helper.runAfterDelay(6, () -> {
                        check(helper, scene.figures() == 0, "dig_down must not fire for the block under the player");
                        scene.end(helper);
                    });
                }));
        tests.put(
                "scare_eating",
                helper -> scene(helper, "eating", scene -> {
                    Scares.ate(scene.player, new ItemStack(Items.STICK));
                    check(helper, scene.figures() == 0, "eating must wait for food");
                    Scares.ate(scene.player, new ItemStack(Items.BREAD));
                    check(helper, scene.figures() == 1, "eating must show a figure when the meal ends");
                    scene.end(helper);
                }));
        tests.put("scare_container_close", helper -> {
            BlockPos chest = helper.absolutePos(FEET).south();
            scene(helper, "container_close", () -> set(helper, chest, Blocks.CHEST.defaultBlockState()), scene -> {
                // The chest is right ahead, so the nearest spot for the figure is taken by it.
                scene.player.containerMenu = new CraftingMenu(1, scene.player.getInventory());
                helper.runAfterDelay(90, () -> {
                    check(helper, scene.figures() == 0, "container_close must wait for the container to close");
                    check(helper, ran(scene.player, "container_close") == 0, "a waiting event has not run yet");
                    scene.player.containerMenu = scene.player.inventoryMenu;
                    helper.runAfterDelay(4, () -> {
                        check(helper, scene.figures() == 1, "container_close must show a figure when it is closed");
                        check(helper, ran(scene.player, "container_close") == 1, "the scare must count as the run");
                        scene.end(helper);
                    });
                });
            });
        });
        tests.put(
                "scare_echo_answer",
                helper -> scene(helper, "echo_answer", scene -> {
                    HauntEvent event =
                            HauntEvents.byId(Identifier.parse("echo_answer")).orElseThrow();
                    for (int i = 0; i < 4; i++) {
                        check(
                                helper,
                                !event.canRun(scene.player),
                                "echo_answer must hold its mark to the fourth break");
                        Restless.onBreakBlock(scene.player, Blocks.STONE.defaultBlockState(), helper.absolutePos(FEET));
                    }
                    check(helper, event.canRun(scene.player), "the fourth echo must spend the mark");
                    scene.end(helper);
                }));
        tests.put("scare_door_slam_face", helper -> {
            BlockPos door = helper.absolutePos(FEET).south(2);
            scene(
                    helper,
                    "door_slam_face",
                    () -> door(helper, door, true),
                    scene -> helper.runAfterDelay(4, () -> {
                        door(helper, door, false);
                        helper.runAfterDelay(8, () -> {
                            check(
                                    helper,
                                    scene.figures() == 1,
                                    "a door shut near the player must fly open on a figure");
                            scene.end(helper);
                        });
                    }));
        });
        tests.put(
                "scare_name_left_right",
                helper -> scene(helper, "name_left_right", scene -> {
                    // The whisper is on a random side: turn to the right, and if nothing came, to the left.
                    helper.runAfterDelay(5, () -> {
                        turn(scene.player, 60.0F);
                        helper.runAfterDelay(3, () -> {
                            if (scene.figures() == 0) {
                                turn(scene.player, -60.0F);
                            }
                            helper.runAfterDelay(3, () -> {
                                check(helper, scene.figures() == 1, "turning to the whisper must show a figure");
                                scene.end(helper);
                            });
                        });
                    });
                }));
        tests.put(
                "scare_slow_turn",
                helper -> scene(helper, "slow_turn", scene -> {
                    check(helper, scene.figures() == 1, "slow_turn must stand a figure far off");
                    scene.end(helper);
                }));
        tests.put(
                "scare_hide",
                helper -> scene(helper, "hide", scene -> {
                    // A mock player counts as not yet loaded, and a player who has not loaded cannot be hurt.
                    scene.player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
                    helper.runAfterDelay(5, () -> {
                        scene.player.snapTo(scene.player.getX() + 3, scene.player.getY(), scene.player.getZ());
                        helper.runAfterDelay(15, () -> {
                            check(helper, scene.figures() == 1, "moving while hiding must bring the figure");
                            check(
                                    helper,
                                    scene.player.getHealth() < scene.player.getMaxHealth(),
                                    "the figure must hit, health=" + scene.player.getHealth() + " figures="
                                            + scene.figures());
                            check(helper, scene.player.isAlive(), "hide must not kill");
                            scene.end(helper);
                        });
                    });
                }));
        tests.put(
                "scare_hide_still",
                helper -> scene(
                        helper,
                        "hide",
                        scene -> helper.runAfterDelay(230, () -> {
                            check(helper, scene.figures() == 0, "keeping still must bring no figure");
                            check(
                                    helper,
                                    scene.player.getHealth() == scene.player.getMaxHealth(),
                                    "keeping still must cost nothing");
                            scene.end(helper);
                        })));
        // The light engine is slow when the server is busy: every scene gets time to wait for it.
        for (String name : tests.keySet()) {
            if (name.startsWith("scare_")) {
                HauntEventGameTests.TIMEOUT.merge(name, 40 + LIGHT_SLACK, Integer::sum);
            }
        }
    }

    private static void scene(GameTestHelper helper, String id, Consumer<Scene> then) {
        scene(helper, id, () -> {}, then);
    }

    /**
     * A player on a stone floor under a roof; {@code before} sets the rest, and once the light engine has caught up
     * with the roof the event is run for them and {@code then} acts out the habit.
     */
    private static void scene(GameTestHelper helper, String id, Runnable before, Consumer<Scene> then) {
        BlockPos feet = helper.absolutePos(FEET);
        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-12, -1, -3), feet.offset(12, -1, 34))) {
            set(helper, pos, Blocks.STONE.defaultBlockState());
        }
        set(helper, feet.above(3), Blocks.STONE.defaultBlockState());
        before.run();
        ServerPlayer player = HauntEventGameTests.connected(helper);
        // Mock players share an id, and so a mark left by the last test.
        Scares.clear(player);
        player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0.0F, 0.0F);
        player.setOnGround(true);
        // Whether the sky is seen is a light level, and light is worked out after the blocks are set, later when the
        // server is busy: look until the event can run, as far as the default time of a test allows.
        HauntEvent event = HauntEvents.byId(Identifier.parse(id)).orElseThrow();
        helper.runAfterDelay(
                LIGHT_TICKS,
                () -> until(
                        helper,
                        () -> event.canRun(player),
                        id + " must be able to run in its scene",
                        () -> {
                            EmbeddedChannel channel = (EmbeddedChannel)
                                    player.connection.getConnection().channel();
                            channel.outboundMessages().clear();
                            event.run(player);
                            then.accept(new Scene(player, channel));
                        },
                        LIGHT_TICKS,
                        LIGHT_WAIT));
    }

    /** How often the event is in the history of the player, which it enters when its scare comes. */
    private static long ran(ServerPlayer player, String id) {
        return History.last(player.getName().getString(), 100).stream()
                .filter(e -> e.event().equals("minecraft:" + id))
                .count();
    }

    private static void turn(ServerPlayer player, float yaw) {
        player.snapTo(player.getX(), player.getY(), player.getZ(), yaw, 0.0F);
    }

    private static void set(GameTestHelper helper, BlockPos pos, BlockState state) {
        helper.getLevel().setBlock(pos, state, 2);
    }

    private static void door(GameTestHelper helper, BlockPos pos, boolean open) {
        BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.OPEN, open);
        set(helper, pos, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(helper, pos.above(), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /** Runs {@code then} once {@code ok} holds, looking every {@code interval} ticks up to {@code left} times. */
    private static void until(
            GameTestHelper helper, BooleanSupplier ok, String message, Runnable then, int interval, int left) {
        if (ok.getAsBoolean()) {
            then.run();
        } else {
            check(helper, left > 0, message);
            helper.runAfterDelay(interval, () -> until(helper, ok, message, then, interval, left - 1));
        }
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }

    private record Scene(ServerPlayer player, EmbeddedChannel channel) {
        long figures() {
            return channel.outboundMessages().stream()
                    .filter(m ->
                            m instanceof ClientboundAddEntityPacket add && add.getType() == Content.SILHOUETTE.get())
                    .count();
        }

        long blockUpdates() {
            return channel.outboundMessages().stream()
                    .filter(m -> m instanceof ClientboundBlockUpdatePacket)
                    .count();
        }

        void end(GameTestHelper helper) {
            player.discard();
            helper.succeed();
        }
    }
}
