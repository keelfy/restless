package dev.keelfy.restless.gametest;

import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.Dreamcatcher;
import dev.keelfy.restless.content.DreamcatcherEntity;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class DreamcatcherGameTests {
    private static final BlockPos TARGET = new BlockPos(4, 2, 4);

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            tests.put("dreamcatcher_touch_" + direction.getSerializedName(), helper -> {
                DreamcatcherEntity entity = hanging(helper, direction);
                Vec3 movement = Vec3.atLowerCornerOf(direction.getUnitVec3i()).scale(.2);
                contact(helper, GameType.SURVIVAL, movement, Vec3.ZERO);
                helper.runAfterDelay(4, () -> {
                    float angle = direction.getAxis() == Direction.Axis.X ? entity.swingZ(1) : -entity.swingX(1);
                    check(helper, angle * direction.getAxisDirection().getStep() > 1, "Contact must swing in the player's direction");
                    float initial = angle;
                    boolean crossed = false;
                    for (int tick = 0; tick < 240; tick++) {
                        DreamcatcherEntity.tick(helper.getLevel(), entity.getBlockPos(), entity.getBlockState(), entity);
                        angle = direction.getAxis() == Direction.Axis.X ? entity.swingZ(1) : -entity.swingX(1);
                        crossed |= angle * initial < 0;
                        check(helper, Math.abs(angle) <= 25, "Swing must remain bounded");
                    }
                    check(helper, crossed, "The pendulum must swing back across its resting position");
                    check(helper, entity.swingX(1) == 0 && entity.swingZ(1) == 0, "Swing must settle after contact ends");
                    helper.succeed();
                });
            });
        }
        tests.put("dreamcatcher_ignore_stationary", helper -> ignoresContact(helper, GameType.SURVIVAL, Vec3.ZERO, Vec3.ZERO));
        tests.put("dreamcatcher_ignore_spectator", helper -> ignoresContact(helper, GameType.SPECTATOR, new Vec3(.2, 0, 0), Vec3.ZERO));
        tests.put("dreamcatcher_ignore_nearby", helper -> ignoresContact(helper, GameType.SURVIVAL, new Vec3(.2, 0, 0), new Vec3(0, 0, .9)));
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            tests.put("dreamcatcher_" + facing.getSerializedName(), helper -> {
                helper.setBlock(TARGET.above(), Blocks.STONE);
                place(helper, Direction.DOWN, facing, true);
                helper.assertBlockProperty(TARGET, Dreamcatcher.FACING, facing.getOpposite());
                var state = helper.getLevel().getBlockState(helper.absolutePos(TARGET));
                AABB shape = state.getShape(helper.getLevel(), helper.absolutePos(TARGET)).bounds();
                check(helper, shape.maxY == 1 && shape.minY == .25, "The shape must reach the ceiling");
                check(helper, facing.getAxis() == Direction.Axis.X ? shape.getZsize() > shape.getXsize()
                    : shape.getXsize() > shape.getZsize(), "The shape must rotate with the model");
                helper.getLevel().destroyBlock(helper.absolutePos(TARGET.above()), false);
                helper.assertBlockPresent(Blocks.AIR, TARGET);
                int drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(TARGET)).inflate(2))
                    .stream().filter(entity -> entity.getItem().is(Content.DREAMCATCHER_ITEM.get()))
                    .mapToInt(entity -> entity.getItem().getCount()).sum();
                check(helper, drops == 1, "Losing the ceiling must drop exactly one dreamcatcher");
                helper.succeed();
            });
        }
        for (Direction face : new Direction[]{Direction.UP, Direction.EAST}) {
            tests.put("dreamcatcher_reject_" + face.getSerializedName(), helper -> {
                helper.setBlock(TARGET.above(), Blocks.STONE);
                helper.setBlock(TARGET.relative(face.getOpposite()), Blocks.STONE);
                place(helper, face, Direction.NORTH, false);
                helper.assertBlockPresent(Blocks.AIR, TARGET);
                helper.succeed();
            });
        }
        tests.put("dreamcatcher_missing_ceiling", helper -> {
            place(helper, Direction.DOWN, Direction.NORTH, false);
            helper.assertBlockPresent(Blocks.AIR, TARGET);
            helper.succeed();
        });
        tests.put("dreamcatcher_unsupported_ceiling", helper -> {
            helper.setBlock(TARGET.above(), Blocks.OAK_SLAB.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE,
                    net.minecraft.world.level.block.state.properties.SlabType.TOP));
            place(helper, Direction.DOWN, Direction.NORTH, false);
            helper.assertBlockPresent(Blocks.AIR, TARGET);
            helper.succeed();
        });
    }

    private static DreamcatcherEntity hanging(GameTestHelper helper, Direction facing) {
        helper.setBlock(TARGET.above(), Blocks.STONE);
        helper.setBlock(TARGET, Content.DREAMCATCHER.get().defaultBlockState().setValue(Dreamcatcher.FACING, facing));
        BlockPos pos = helper.absolutePos(TARGET);
        check(helper, helper.getLevel().getBlockState(pos).getCollisionShape(helper.getLevel(), pos).isEmpty(),
            "Players must be able to brush through the hanging ornament");
        return (DreamcatcherEntity) helper.getLevel().getBlockEntity(pos);
    }

    private static void contact(GameTestHelper helper, GameType mode, Vec3 movement, Vec3 offset) {
        Player player = helper.makeMockPlayer(mode);
        Vec3 to = Vec3.atCenterOf(helper.absolutePos(TARGET)).add(0, -.5, 0).add(offset);
        player.setPos(to);
        player.setDeltaMovement(movement);
        player.applyEffectsFromBlocks(to.subtract(movement), to);
    }

    private static void ignoresContact(GameTestHelper helper, GameType mode, Vec3 movement, Vec3 offset) {
        DreamcatcherEntity entity = hanging(helper, Direction.NORTH);
        contact(helper, mode, movement, offset);
        helper.runAfterDelay(4, () -> {
            check(helper, entity.swingX(1) == 0 && entity.swingZ(1) == 0, "No swing without physical contact by a moving player");
            helper.succeed();
        });
    }

    private static void place(GameTestHelper helper, Direction face, Direction facing, boolean expected) {
        BlockPos support = helper.absolutePos(TARGET.relative(face.getOpposite()));
        ItemStack stack = new ItemStack(Content.DREAMCATCHER_ITEM.get(), 2);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(switch (facing) {
            case SOUTH -> 0;
            case WEST -> 90;
            case NORTH -> 180;
            case EAST -> 270;
            default -> throw new IllegalArgumentException("Horizontal direction required");
        });
        var hit = new BlockHitResult(Vec3.atCenterOf(support).add(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(.5)), face, support, false);
        var context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit);
        boolean placed = ((BlockItem) stack.getItem()).place(context).consumesAction();
        check(helper, placed == expected, "Unexpected placement result");
        check(helper, stack.getCount() == (expected ? 1 : 2), "Only successful placement may consume an item");
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) throw helper.assertionException(Component.literal(message));
    }
}
