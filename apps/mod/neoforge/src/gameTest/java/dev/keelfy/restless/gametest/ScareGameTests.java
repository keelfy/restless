package dev.keelfy.restless.gametest;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.haunt.History;
import dev.keelfy.restless.haunt.event.HauntEvent;
import dev.keelfy.restless.haunt.event.HauntEvents;
import dev.keelfy.restless.haunt.event.Scares;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

/** The scares the player sets off: each is armed or started, the habit is acted out, and the figure is sent. */
final class ScareGameTests {
    private static final BlockPos FEET = HauntEventGameTests.FEET;
    private static final int LIGHT_TICKS = 5;

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        HauntEventGameTests.PADDING.put("scare_slow_turn", 40);
        HauntEventGameTests.TIMEOUT.put("scare_hide_still", 300);
        HauntEventGameTests.TIMEOUT.put("scare_container_close", 300);
        HauntEventGameTests.TIMEOUT.put("scare_third_person", 300);
        HauntEventGameTests.PADDING.put("scare_stairs_sitter", 20);
        HauntEventGameTests.PADDING.put("scare_tree_line", 40);
        HauntEventGameTests.TIMEOUT.put("scare_tree_line", 400);
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
        tests.put("scare_behind_trunk", helper -> {
            BlockPos base = helper.absolutePos(FEET).south(2);
            scene(
                    helper,
                    "behind_trunk",
                    () -> {
                        set(helper, base, Blocks.OAK_LOG.defaultBlockState());
                        set(helper, base.above(), Blocks.OAK_LOG.defaultBlockState());
                    },
                    scene -> {
                        Restless.onBreakBlock(scene.player, Blocks.OAK_LOG.defaultBlockState(), base.above());
                        check(helper, scene.figures() == 0, "behind_trunk must wait for the base log");
                        Restless.onBreakBlock(scene.player, Blocks.OAK_LOG.defaultBlockState(), base);
                        helper.runAfterDelay(6, () -> {
                            check(helper, scene.figures() == 1, "behind_trunk must show one figure");
                            check(helper, scene.blockUpdates() >= 1, "behind_trunk must open the log above");
                            check(helper, ran(scene.player, "behind_trunk") == 1, "the scare must count as the run");
                            scene.end(helper);
                        });
                    });
        });
        tests.put("scare_third_person", helper -> {
            int view = Config.event("out_of_body").integer("ticks");
            scene(helper, "third_person", () -> {}, scene -> {
                check(helper, scene.figures() == 1, "third_person must stand a figure in the view");
                helper.runAfterDelay(view + 5, () -> {
                    check(helper, scene.figures() == 2, "third_person must show the figure again when the view ends");
                    check(helper, ran(scene.player, "third_person") == 0, "unseen, it has not run yet");
                    turn(scene.player, 180.0F);
                    helper.runAfterDelay(3, () -> {
                        check(helper, ran(scene.player, "third_person") == 1, "turning round must count the run");
                        scene.end(helper);
                    });
                });
            });
        });
        tests.put("scare_pit_face", helper -> {
            BlockPos pit = helper.absolutePos(FEET).south(2).below();
            scene(
                    helper,
                    "pit_face",
                    () -> {
                        for (int i = 0; i < 10; i++) {
                            set(helper, pit.below(i), Blocks.AIR.defaultBlockState());
                        }
                        set(helper, pit.below(10), Blocks.STONE.defaultBlockState());
                    },
                    scene -> helper.runAfterDelay(5, () -> {
                        check(helper, scene.figures() == 0, "pit_face must wait for a look down");
                        scene.player.setXRot(75.0F);
                        helper.runAfterDelay(12, () -> {
                            check(helper, scene.figures() == 1, "looking down a pit must show a figure");
                            check(helper, ran(scene.player, "pit_face") == 1, "the scare must count as the run");
                            scene.end(helper);
                        });
                    }));
        });
        tests.put("scare_stairs_sitter", helper -> {
            BlockPos base = helper.absolutePos(FEET).south(9);
            scene(
                    helper,
                    "stairs_sitter",
                    () -> {
                        for (int i = 0; i < 3; i++) {
                            set(helper, base.south(i).above(3), Blocks.AIR.defaultBlockState());
                            set(
                                    helper,
                                    base.south(i).above(i),
                                    Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
                        }
                        // The border of barriers around the test is in the way of a look this far.
                        for (BlockPos pos : BlockPos.betweenClosed(
                                helper.absolutePos(FEET).offset(-1, 0, 6),
                                helper.absolutePos(FEET).offset(1, 4, 6))) {
                            set(helper, pos, Blocks.AIR.defaultBlockState());
                        }
                    },
                    scene -> {
                        check(helper, scene.figures() == 1, "stairs_sitter must sit a figure on the top step");
                        helper.runAfterDelay(5, () -> {
                            check(helper, ran(scene.player, "stairs_sitter") == 0, "far off, it has not run yet");
                            scene.player.snapTo(
                                    scene.player.getX(), scene.player.getY(), scene.player.getZ() + 6.0, 0.0F, 0.0F);
                            helper.runAfterDelay(3, () -> {
                                check(helper, ran(scene.player, "stairs_sitter") == 1, "coming near must count it");
                                scene.end(helper);
                            });
                        });
                    });
        });
        tests.put("scare_tree_line", helper -> {
            BlockPos feet = helper.absolutePos(FEET);
            // A roof wide enough that the light engine finds it dark under it.
            Runnable roof = () -> {
                for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-20, 3, -10), feet.offset(20, 3, 45))) {
                    set(helper, pos, Blocks.STONE.defaultBlockState());
                }
            };
            scene(
                    helper,
                    "tree_line",
                    roof,
                    false,
                    // The light engine takes a while to see the roof, and until it has the ground is too bright.
                    scene -> until(
                            helper, () -> scene.figures() == 1, "tree_line must stand the first figure far off", () -> {
                                Vec3 figure = scene.lastFigure();
                                clearBarriers(helper, scene.player.getEyePosition(), figure);
                                scene.player.lookAt(EntityAnchorArgument.Anchor.EYES, figure);
                                helper.runAfterDelay(4, () -> {
                                    check(helper, scene.figures() == 1, "looking at a figure must not add another");
                                    // Straight up is away from it, and the ground ahead stays the same.
                                    scene.player.setXRot(-85.0F);
                                    helper.runAfterDelay(4, () -> {
                                        check(
                                                helper,
                                                scene.figures() == 2,
                                                "a look away from a figure must add another");
                                        scene.end(helper);
                                    });
                                });
                            }));
        });
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
    }

    private static void scene(GameTestHelper helper, String id, Consumer<Scene> then) {
        scene(helper, id, () -> {}, then);
    }

    /**
     * A player on a stone floor under a roof; {@code before} sets the rest, and once the light engine has caught up
     * with the roof the event is run for them and {@code then} acts out the habit.
     */
    private static void scene(GameTestHelper helper, String id, Runnable before, Consumer<Scene> then) {
        scene(helper, id, before, true, then);
    }

    /** As above; {@code gated} is whether the event must pass its own condition (a night event cannot, by day). */
    private static void scene(GameTestHelper helper, String id, Runnable before, boolean gated, Consumer<Scene> then) {
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
        // Whether the sky is seen is a light level, and light is worked out after the blocks are set.
        helper.runAfterDelay(LIGHT_TICKS, () -> {
            HauntEvent event = HauntEvents.byId(Identifier.parse(id)).orElseThrow();
            check(helper, !gated || event.canRun(player), id + " must be able to run in its scene");
            EmbeddedChannel channel =
                    (EmbeddedChannel) player.connection.getConnection().channel();
            channel.outboundMessages().clear();
            event.run(player);
            then.accept(new Scene(player, channel));
        });
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

    /** Runs {@code then} once {@code ok} holds, looking every 5 ticks; fails with {@code message} if it never does. */
    private static void until(GameTestHelper helper, BooleanSupplier ok, String message, Runnable then) {
        until(helper, ok, message, then, 60);
    }

    private static void until(GameTestHelper helper, BooleanSupplier ok, String message, Runnable then, int left) {
        if (ok.getAsBoolean()) {
            then.run();
        } else {
            check(helper, left > 0, message);
            helper.runAfterDelay(5, () -> until(helper, ok, message, then, left - 1));
        }
    }

    /** The barriers that fence the test in are where a look this far goes through: take them out of the way. */
    private static void clearBarriers(GameTestHelper helper, Vec3 from, Vec3 to) {
        Vec3 step = to.subtract(from).normalize().scale(0.5);
        Vec3 at = from;
        for (int i = 0; i < from.distanceTo(to) * 2; i++) {
            BlockPos pos = BlockPos.containing(at);
            if (helper.getLevel().getBlockState(pos).is(Blocks.BARRIER)) {
                set(helper, pos, Blocks.AIR.defaultBlockState());
            }
            at = at.add(step);
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

        /** Where the last figure was put. */
        Vec3 lastFigure() {
            return channel.outboundMessages().stream()
                    .filter(m ->
                            m instanceof ClientboundAddEntityPacket add && add.getType() == Content.SILHOUETTE.get())
                    .map(m -> (ClientboundAddEntityPacket) m)
                    .reduce((first, last) -> last)
                    .map(add -> new Vec3(add.getX(), add.getY() + 1.0, add.getZ()))
                    .orElseThrow();
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
