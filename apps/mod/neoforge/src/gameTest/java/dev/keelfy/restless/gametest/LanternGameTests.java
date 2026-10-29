package dev.keelfy.restless.gametest;

import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.UnlitLights;
import dev.keelfy.restless.content.WardBlock;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class LanternGameTests {
    private static final BlockPos TARGET = new BlockPos(4, 2, 4);

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("unlit_lights", helper -> {
            for (var item : UnlitLights.ITEMS) {
                var block = ((BlockItem) item.get()).getBlock();
                check(helper, block.defaultBlockState().getLightEmission() == 0, "Unlit blocks must emit no light");
                check(helper, !UnlitLights.isTorch(block.defaultBlockState()) && !UnlitLights.isLantern(block.defaultBlockState()),
                    "Already extinguished blocks must not be event targets");
            }
            var wall = Blocks.SOUL_WALL_TORCH.defaultBlockState()
                .setValue(net.minecraft.world.level.block.WallTorchBlock.FACING, Direction.WEST);
            check(helper, UnlitLights.extinguished(wall).getValue(net.minecraft.world.level.block.WallTorchBlock.FACING) == Direction.WEST,
                "Extinguishing must preserve wall attachment");
            var lantern = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("waxed_oxidized_copper_lantern")).defaultBlockState()
                .setValue(LanternBlock.HANGING, true).setValue(LanternBlock.WATERLOGGED, true);
            var unlit = UnlitLights.extinguished(lantern);
            check(helper, unlit != lantern && unlit.getValue(LanternBlock.HANGING) && unlit.getValue(LanternBlock.WATERLOGGED),
                "Extinguishing must preserve suspension and water");
            var ward = Content.HALLOWED_LANTERN.get().defaultBlockState();
            check(helper, UnlitLights.extinguished(ward) == ward && !UnlitLights.isLantern(ward),
                "Hallowed lantern must be excluded");
            helper.succeed();
        });
        for (boolean hanging : new boolean[]{false, true}) {
            String name = hanging ? "hanging" : "standing";
            Direction face = hanging ? Direction.DOWN : Direction.UP;
            tests.put("lantern_" + name, helper -> {
                BlockPos support = TARGET.relative(face.getOpposite());
                helper.setBlock(support, Blocks.STONE);
                place(helper, face, true);
                helper.assertBlockProperty(TARGET, LanternBlock.HANGING, hanging);
                var pos = helper.absolutePos(TARGET);
                var state = helper.getLevel().getBlockState(pos);
                var shape = state.getShape(helper.getLevel(), pos).bounds();
                check(helper, shape.minY == (hanging ? .25 : 0) && shape.maxY == (hanging ? 1 : .625),
                    "The shape must match the model and reach its support");
                check(helper, state.getLightEmission() == 12, "The lantern must retain its light level");
                check(helper, state.getBlock() instanceof WardBlock ward && ward.kind() == WardBlock.Kind.LANTERN,
                    "Both placements must remain protective wards");
                helper.getLevel().destroyBlock(helper.absolutePos(support), false);
                helper.assertBlockPresent(Blocks.AIR, TARGET);
                int drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2))
                    .stream().filter(entity -> entity.getItem().is(Content.HALLOWED_LANTERN_ITEM.get()))
                    .mapToInt(entity -> entity.getItem().getCount()).sum();
                check(helper, drops == 1, "Losing support must drop exactly one lantern");
                helper.succeed();
            });
            tests.put("lantern_" + name + "_waterlogged", helper -> {
                helper.setBlock(TARGET.relative(face.getOpposite()), Blocks.STONE);
                helper.setBlock(TARGET, Blocks.WATER);
                place(helper, face, true);
                helper.assertBlockProperty(TARGET, LanternBlock.HANGING, hanging);
                helper.assertBlockProperty(TARGET, LanternBlock.WATERLOGGED, true);
                check(helper, helper.getLevel().getFluidState(helper.absolutePos(TARGET)).is(Fluids.WATER),
                    "Placement must preserve water");
                helper.succeed();
            });
        }
        tests.put("lantern_missing_support", helper -> {
            place(helper, Direction.DOWN, false);
            helper.assertBlockPresent(Blocks.AIR, TARGET);
            helper.succeed();
        });
        tests.put("lantern_unsupported_ceiling", helper -> {
            helper.setBlock(TARGET.above(), Blocks.OAK_SLAB.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE,
                    net.minecraft.world.level.block.state.properties.SlabType.TOP));
            place(helper, Direction.DOWN, false);
            helper.assertBlockPresent(Blocks.AIR, TARGET);
            helper.succeed();
        });
    }

    private static void place(GameTestHelper helper, Direction face, boolean expected) {
        BlockPos support = helper.absolutePos(TARGET.relative(face.getOpposite()));
        ItemStack stack = new ItemStack(Content.HALLOWED_LANTERN_ITEM.get(), 2);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setXRot(face == Direction.DOWN ? -90 : 90);
        var hit = new BlockHitResult(Vec3.atCenterOf(support).add(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(.5)),
            face, support, false);
        var context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit);
        boolean placed = ((BlockItem) stack.getItem()).place(context).consumesAction();
        check(helper, placed == expected, "Unexpected lantern placement result");
        check(helper, stack.getCount() == (expected ? 1 : 2), "Only successful placement may consume an item");
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) throw helper.assertionException(Component.literal(message));
    }
}
