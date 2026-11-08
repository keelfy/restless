package dev.keelfy.restless.gametest;

import static dev.keelfy.restless.gametest.HauntEventGameTests.FEET;
import static dev.keelfy.restless.gametest.HauntEventGameTests.check;
import static dev.keelfy.restless.gametest.HauntEventGameTests.runs;

import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntEvents;
import dev.keelfy.restless.spirit.SpiritType;
import dev.keelfy.restless.spirit.Spirits;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

/** Events only one spirit type rolls: each runs in its scene for its type, and no other type can roll it. */
final class SignatureGameTests {
    private SignatureGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put(
                "signature_phantom_self_join",
                helper -> signature(helper, "phantom_self_join", Spirits.PHANTOM, player -> {}, () -> {}));
        tests.put(
                "signature_nix_catch",
                helper -> signature(
                        helper,
                        "nix_catch",
                        Spirits.NIX,
                        player -> {
                            for (int z = 2; z <= 6; z++) {
                                helper.setBlock(new BlockPos(7, 0, z), Blocks.WATER);
                            }
                        },
                        () -> check(
                                helper,
                                helper.getLevel()
                                                .getEntitiesOfClass(
                                                        ItemEntity.class,
                                                        new AABB(helper.absolutePos(FEET)).inflate(6),
                                                        i -> i.getItem().is(Items.COD)
                                                                || i.getItem().is(Items.SALMON))
                                                .size()
                                        >= 3,
                                "nix_catch must leave three fish on the shore")));
        tests.put(
                "signature_nix_call",
                helper -> signature(
                        helper,
                        "nix_call",
                        Spirits.NIX,
                        player -> {
                            for (int x = 5; x <= 9; x++) {
                                for (int z = 0; z <= 9; z++) {
                                    helper.setBlock(new BlockPos(x, 0, z), Blocks.WATER);
                                }
                            }
                        },
                        () -> {}));
        tests.put(
                "signature_mimic_voice",
                helper -> signature(
                        helper,
                        "mimic_voice",
                        Spirits.MIMIC,
                        player -> helper.spawn(EntityTypes.COW, FEET.east(2)),
                        () -> check(
                                helper,
                                !helper.getLevel()
                                        .getEntitiesOfClass(Cow.class, new AABB(helper.absolutePos(FEET)).inflate(6))
                                        .isEmpty(),
                                "mimic_voice must not hurt the animal")));
        HauntEventGameTests.TIMEOUT.put("signature_wraith_taken", 120);
        tests.put("signature_wraith_taken", helper -> {
            // A sealed room: nothing but the animal and the dark. The light engine needs a few ticks to see it.
            for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 9, 4, 9)) {
                if (pos.getY() == 0 || pos.getX() % 9 == 0 || pos.getZ() % 9 == 0 || pos.getY() == 4) {
                    helper.setBlock(pos, Blocks.STONE);
                }
            }
            helper.runAfterDelay(
                    40,
                    () -> signature(
                            helper,
                            "wraith_taken",
                            Spirits.WRAITH,
                            player -> helper.spawn(EntityTypes.COW, FEET.east(2)),
                            () -> check(
                                    helper,
                                    helper.getLevel()
                                            .getEntitiesOfClass(
                                                    Cow.class, new AABB(helper.absolutePos(FEET)).inflate(6))
                                            .isEmpty(),
                                    "wraith_taken must take the animal")));
        });
        tests.put(
                "signature_domovoi_supper",
                helper -> signature(
                        helper,
                        "domovoi_supper",
                        Spirits.DOMOVOI,
                        player -> {
                            helper.setBlock(FEET.west(2), Blocks.CHEST);
                            helper.getBlockEntity(FEET.west(2), ChestBlockEntity.class)
                                    .setItem(3, new ItemStack(Items.MILK_BUCKET));
                        },
                        () -> check(
                                helper,
                                helper.getBlockEntity(FEET.west(2), ChestBlockEntity.class)
                                        .getItem(3)
                                        .is(Items.BUCKET),
                                "the milk bucket must be left empty")));
        tests.put(
                "signature_domovoi_hearth",
                helper -> signature(
                        helper,
                        "domovoi_hearth",
                        Spirits.DOMOVOI,
                        player -> helper.setBlock(
                                FEET.west(2),
                                Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false)),
                        () -> check(
                                helper,
                                helper.getBlockState(FEET.west(2)).getValue(CampfireBlock.LIT),
                                "the campfire must be lit")));
        tests.put(
                "signature_domovoi_hearth_furnace",
                helper -> signature(
                        helper,
                        "domovoi_hearth",
                        Spirits.DOMOVOI,
                        player -> {
                            helper.setBlock(FEET.west(2), Blocks.FURNACE);
                            helper.getBlockEntity(FEET.west(2), AbstractFurnaceBlockEntity.class)
                                    .setItem(0, new ItemStack(Items.RAW_IRON));
                        },
                        () -> check(
                                helper,
                                helper.getBlockState(FEET.west(2)).getValue(BlockStateProperties.LIT),
                                "the furnace must start")));
        tests.put(
                "signature_hoarder_payment",
                helper -> signature(
                        helper,
                        "hoarder_payment",
                        Spirits.HOARDER,
                        player -> {
                            helper.setBlock(FEET.west(2), Blocks.CHEST);
                            helper.getBlockEntity(FEET.west(2), ChestBlockEntity.class)
                                    .setItem(5, new ItemStack(Items.DIAMOND, 7));
                        },
                        () -> {
                            ItemStack now = helper.getBlockEntity(FEET.west(2), ChestBlockEntity.class)
                                    .getItem(5);
                            check(helper, !now.is(Items.DIAMOND) && now.getCount() == 7, "the stack must turn to junk");
                        }));
        tests.put(
                "signature_demon_offer",
                helper -> signature(helper, "demon_offer", Spirits.DEMON, player -> {}, () -> sign(helper, true)));
        tests.put(
                "signature_lost_one_sign",
                helper ->
                        signature(helper, "lost_one_sign", Spirits.LOST_ONE, player -> {}, () -> sign(helper, false)));
        tests.put("signature_filter", SignatureGameTests::filter);
    }

    /** A sign of the right wood stands somewhere near the player; the spot may be off the test floor, even below it. */
    private static void sign(GameTestHelper helper, boolean crimson) {
        for (BlockPos pos : BlockPos.betweenClosed(-14, -10, -14, 24, 40, 24)) {
            if (helper.getLevel()
                    .getBlockState(helper.absolutePos(pos))
                    .is(crimson ? Blocks.CRIMSON_SIGN : Blocks.OAK_SIGN)) {
                return;
            }
        }
        check(helper, false, "a sign must stand near the player");
    }

    /** Runs the event in its scene, and checks that only its own type may roll it. */
    private static void signature(
            GameTestHelper helper, String id, SpiritType own, Consumer<ServerPlayer> scene, Runnable after) {
        runs(
                helper,
                id,
                player -> {
                    scene.accept(player);
                    HauntEvent event = HauntEvents.byId(Identifier.parse(id)).orElseThrow();
                    for (SpiritType spirit : Spirits.all()) {
                        double weight = Director.weight(event, player, spirit, List.of(), 1.0, false);
                        check(
                                helper,
                                spirit.equals(own) ? weight > 0 : weight == 0,
                                id + " must be rolled by " + own.id() + " only, not " + spirit.id());
                    }
                },
                after);
    }

    private static void filter(GameTestHelper helper) {
        for (HauntEvent event : HauntEvents.all()) {
            String only = event.only();
            check(
                    helper,
                    only == null || Spirits.byId(only).isPresent(),
                    event.id() + " names a spirit type that does not exist: " + only);
        }
        helper.succeed();
    }
}
