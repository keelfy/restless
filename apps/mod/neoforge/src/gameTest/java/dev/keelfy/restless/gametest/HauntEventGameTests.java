package dev.keelfy.restless.gametest;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.HauntData;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntEvents;
import dev.keelfy.restless.spirit.Spirits;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.network.Filterable;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.CropBlock;

/** Each packet-lie and sound event runs on a connected player and leaves the world as it was (or, for 110, as it says). */
final class HauntEventGameTests {
    static final BlockPos FEET = new BlockPos(4, 1, 4);
    /** Long enough for every lie to send its truth back. */
    private static final int SETTLE_TICKS = 30;

    /** Tests whose figures stand far off: they get room around them, so no other test's figure is in their reach. */
    static final Map<String, Integer> PADDING = new HashMap<>();
    /** Ticks a test may take, where more than the usual 40. */
    static final Map<String, Integer> TIMEOUT = new HashMap<>();

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        for (String id : List.of("false_wound", "false_hearts", "false_hunger", "guardian_face", "false_totem", "subliminal_title",
            "red_vignette", "own_voice", "false_death", "private_rain", "silent_lightning", "distant_mob", "bottle_shatter",
            "stranger_joins", "stranger_death", "phantom_heartbeat", "fake_subtitle", "blinking", "dimming", "false_night", "false_slot", "turned_head", "dragon_flyover",
            "approaching_steps", "distant_mining", "ladder_steps", "ravenous", "infested", "oozing", "weaving")) {
            tests.put("event_" + id, helper -> runs(helper, id, player -> {}, () -> {}));
        }
        tests.put("event_false_advancement", helper -> runs(helper, "false_advancement", player -> {}, () -> {
            for (String name : List.of("who_are_you", "not_alone", "behind_you")) {
                AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(Identifier.fromNamespaceAndPath("restless", "whisper/" + name));
                check(helper, advancement != null, "advancement whisper/" + name + " must load");
                check(helper, helper.getLevel().players().stream().noneMatch(p -> p.getAdvancements().getOrStartProgress(advancement).isDone()),
                    "false_advancement must take the advancement back");
            }
        }));
        // A field off the test floor, past the event's distance from the player, and room enough that no other test's crops are near.
        PADDING.put("event_harvest", 40);
        PADDING.put("event_ghost_tree", 30);
        PADDING.put("event_hallucination", 50);
        tests.put("event_harvest", helper -> runs(helper, "harvest", player -> {
            BlockPos field = helper.absolutePos(FEET).east(13);
            for (BlockPos pos : BlockPos.betweenClosed(field.offset(-3, -1, -2), field.offset(3, -1, 2))) {
                helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
            }
            for (BlockPos crop : List.of(field, field.south())) {
                helper.getLevel().setBlock(crop.below(), Blocks.FARMLAND.defaultBlockState(), 2);
                helper.getLevel().setBlock(crop, ((CropBlock) Blocks.WHEAT).getStateForAge(CropBlock.MAX_AGE), 2);
            }
        }, () -> {
            BlockPos field = helper.absolutePos(FEET).east(13);
            check(helper, helper.getLevel().isEmptyBlock(field) && helper.getLevel().isEmptyBlock(field.south()), "harvest must take the ripe wheat");
            AABB around = new AABB(field).inflate(8);
            check(helper, helper.getLevel().getEntitiesOfClass(Mannequin.class, around).isEmpty(), "harvest must leave once the field is bare");
        }));
        tests.put("event_torches_behind", helper -> runs(helper, "torches_behind", player -> {
            // A roof over the player, and a torch behind them: a fresh player faces south.
            helper.setBlock(FEET.above(2), Blocks.STONE);
            helper.setBlock(FEET.north(3), Blocks.TORCH);
            helper.setBlock(FEET.south(3), Blocks.TORCH);
        }, () -> {
            check(helper, !helper.getBlockState(FEET.north(3)).is(Blocks.TORCH), "torches_behind must put out the torch behind");
            helper.assertBlockPresent(Blocks.TORCH, FEET.south(3));
        }));
        tests.put("event_false_windows", helper -> runs(helper, "false_windows",
            player -> helper.setBlock(FEET.north(2), Blocks.GLASS), () -> helper.assertBlockPresent(Blocks.GLASS, FEET.north(2))));
        tests.put("event_false_looks", helper -> runs(helper, "false_looks",
            player -> helper.setBlock(FEET.west(2), Blocks.CRAFTING_TABLE), () -> helper.assertBlockPresent(Blocks.CRAFTING_TABLE, FEET.west(2))));
        tests.put("event_ghost_tree", helper -> runs(helper, "ghost_tree", player -> {
            // Ground 12 blocks out on every side; the tree itself is only sent, so the server's air must stay.
            BlockPos centre = helper.absolutePos(FEET);
            for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-14, -1, -14), centre.offset(14, -1, 14))) {
                helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
            }
        }, () -> {
            BlockPos centre = helper.absolutePos(FEET);
            for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-14, 0, -14), centre.offset(14, 6, 14))) {
                check(helper, !ghostLog(helper.getLevel().getBlockState(pos)), "ghost_tree must not be on the server");
            }
        }));
        tests.put("event_hallucination", helper -> runs(helper, "hallucination", player -> {
            BlockPos centre = helper.absolutePos(FEET);
            for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-22, -1, -22), centre.offset(22, -1, 22))) {
                helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
            }
        }, () -> check(helper, helper.getLevel().getEntitiesOfClass(Entity.class, new AABB(helper.absolutePos(FEET)).inflate(30), e -> !(e instanceof ServerPlayer)).isEmpty(),
            "hallucination must leave nothing on the server")));
        PADDING.put("event_moving_tree", 20);
        tests.put("event_moving_tree", helper -> runs(helper, "moving_tree", player -> tree(helper), () -> {
            BlockPos centre = helper.absolutePos(FEET);
            check(helper, !helper.getLevel().getBlockState(centre.east(6)).is(BlockTags.LOGS), "moving_tree must leave the trunk's old place");
            check(helper, BlockPos.betweenClosedStream(centre.offset(-12, 0, -12), centre.offset(12, 6, 12))
                .filter(pos -> helper.getLevel().getBlockState(pos).is(BlockTags.LOGS)).count() == 3, "moving_tree must keep the trunk whole");
        }));
        tests.put("event_drowned_fish", helper -> {
            Entity[] cod = new Entity[1];
            runs(helper, "drowned_fish", player -> cod[0] = EntityTypes.COD.spawn(helper.getLevel(), helper.absolutePos(FEET).east(4), EntitySpawnReason.COMMAND),
                () -> check(helper, !cod[0].isAlive(), "drowned_fish must kill the fish"));
        });
        tests.put("event_broken_boat", helper -> {
            Entity[] boat = new Entity[1];
            runs(helper, "broken_boat", player -> boat[0] = EntityTypes.OAK_BOAT.spawn(helper.getLevel(), helper.absolutePos(FEET).east(4), EntitySpawnReason.COMMAND),
                () -> check(helper, !boat[0].isAlive(), "broken_boat must break the boat"));
        });
        tests.put("event_disguise", helper -> {
            Entity[] cow = new Entity[1];
            runs(helper, "disguise", player -> cow[0] = EntityTypes.COW.spawn(helper.getLevel(), helper.absolutePos(FEET).east(4), EntitySpawnReason.COMMAND),
                () -> check(helper, cow[0].isAlive() && helper.getLevel().getEntitiesOfClass(Entity.class, new AABB(helper.absolutePos(FEET)).inflate(10),
                    e -> e.getType() == EntityTypes.SHEEP).isEmpty(), "disguise must leave the cow a cow on the server"));
        });
        tests.put("event_cracking_floor", helper -> runs(helper, "cracking_floor", player -> {},
            () -> helper.assertBlockPresent(Blocks.STONE, FEET.below())));
        tests.put("event_hostile_animal", helper -> runs(helper, "hostile_animal",
            player -> helper.spawn(EntityTypes.COW, FEET.east(2)), () -> {}));
        tests.put("event_note_block_voice", helper -> runs(helper, "note_block_voice",
            player -> helper.setBlock(FEET.west(2), Blocks.NOTE_BLOCK), () -> helper.assertBlockPresent(Blocks.NOTE_BLOCK, FEET.west(2))));
        tests.put("event_many_breaks", helper -> runs(helper, "many_breaks", player -> {
            for (int i = 0; i < 8; i++) {
                helper.setBlock(FEET.east(2).north(i - 4).above(), Blocks.GLASS);
            }
        }, () -> helper.assertBlockPresent(Blocks.GLASS, FEET.east(2).above())));
        tests.put("event_ladder_on_ladder", helper -> runs(helper, "ladder_steps", player -> {
            for (int i = 1; i <= 3; i++) {
                helper.setBlock(FEET.west(2).above(i), Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, net.minecraft.core.Direction.EAST));
            }
        }, () -> helper.assertBlockPresent(Blocks.LADDER, FEET.west(2).above(2))));
        tests.put("event_drift", helper -> runs(helper, "drift", player -> player.setOnGround(true), () -> {}));
        tests.put("event_infested_stone", helper -> runs(helper, "infested_stone", player -> {
            player.setXRot(0);
            player.setYRot(0);
            helper.setBlock(FEET.south(2).above(), Blocks.STONE);
        }, () -> helper.assertBlockPresent(Blocks.STONE, FEET.south(2).above())));
        tests.put("event_ceiling_crack", helper -> runs(helper, "ceiling_crack",
            player -> helper.setBlock(FEET.above(4), Blocks.STONE), () -> helper.assertBlockPresent(Blocks.STONE, FEET.above(4))));
        tests.put("event_ice_over", helper -> runs(helper, "ice_over",
            player -> helper.setBlock(FEET.east(2), Blocks.WATER), () -> helper.assertBlockPresent(Blocks.WATER, FEET.east(2))));
        tests.put("event_portal_out", helper -> runs(helper, "portal_out",
            player -> helper.setBlock(FEET.east(2), Blocks.NETHER_PORTAL), () -> helper.assertBlockPresent(Blocks.AIR, FEET.east(2))));
        tests.put("event_block_blink", helper -> runs(helper, "block_blink", player -> {
            player.setXRot(0);
            player.setYRot(0);
            helper.setBlock(FEET.south(2).above(), Blocks.STONE);
        }, () -> helper.assertBlockPresent(Blocks.STONE, FEET.south(2).above())));
        tests.put("event_leak", helper -> runs(helper, "leak",
            player -> helper.setBlock(FEET.above(3), Blocks.STONE), () -> helper.assertBlockPresent(Blocks.STONE, FEET.above(3))));
        tests.put("event_lava_leak", helper -> runs(helper, "lava_leak",
            player -> helper.setBlock(FEET.above(3), Blocks.STONE), () -> helper.assertBlockPresent(Blocks.STONE, FEET.above(3))));
        tests.put("event_mock_grave", helper -> runs(helper, "mock_grave", player -> {
            for (BlockPos pos : BlockPos.betweenClosed(-12, 0, -12, 21, 0, 21)) {
                helper.getLevel().setBlockAndUpdate(helper.absolutePos(pos), Blocks.STONE.defaultBlockState());
            }
        }, () -> check(helper, BlockPos.betweenClosedStream(helper.absolutePos(FEET).offset(-10, -2, -10), helper.absolutePos(FEET).offset(10, 3, 10))
            .anyMatch(pos -> helper.getLevel().getBlockState(pos).is(Content.GRAVESTONE.get())), "mock_grave must stand a gravestone")));
        tests.put("event_cave_sign", helper -> runs(helper, "cave_sign", player -> {
            for (BlockPos pos : BlockPos.betweenClosed(FEET.offset(-6, 0, -6), FEET.offset(6, 2, 6))) {
                boolean edge = Math.abs(pos.getX() - FEET.getX()) == 6 || Math.abs(pos.getZ() - FEET.getZ()) == 6;
                helper.getLevel().setBlockAndUpdate(helper.absolutePos(pos), edge ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
            for (BlockPos pos : BlockPos.betweenClosed(FEET.offset(-6, 3, -6), FEET.offset(6, 3, 6))) {
                helper.getLevel().setBlockAndUpdate(helper.absolutePos(pos), Blocks.STONE.defaultBlockState());
            }
        }, () -> check(helper, BlockPos.betweenClosedStream(helper.absolutePos(FEET).offset(-6, 0, -6), helper.absolutePos(FEET).offset(6, 3, 6))
            .anyMatch(pos -> helper.getLevel().getBlockState(pos).is(Blocks.OAK_WALL_SIGN)), "cave_sign must hang a wall sign")));
        // The sign stands up to 16 blocks out, past the usual test floor.
        PADDING.put("event_item_trail", 20);
        tests.put("event_item_trail", helper -> runs(helper, "item_trail", player -> {
            for (BlockPos pos : BlockPos.betweenClosed(-14, 0, -14, 22, 0, 22)) {
                helper.getLevel().setBlockAndUpdate(helper.absolutePos(pos), Blocks.STONE.defaultBlockState());
            }
        }, () -> {
            check(helper, !helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(FEET)).inflate(17)).isEmpty(),
                "item_trail must lay items");
            check(helper, BlockPos.betweenClosedStream(helper.absolutePos(FEET).offset(-17, -2, -17), helper.absolutePos(FEET).offset(17, 3, 17))
                .anyMatch(pos -> helper.getLevel().getBlockState(pos).is(Blocks.OAK_SIGN)), "item_trail must stand a sign");
        }));
        tests.put("event_chest_blast", helper -> runs(helper, "chest_blast",
            player -> helper.setBlock(FEET.west(3), Blocks.CHEST), () -> helper.assertBlockPresent(Blocks.CHEST, FEET.west(3))));
        tests.put("event_wrong_smelt", helper -> runs(helper, "wrong_smelt", player -> {
            helper.setBlock(FEET.west(2), Blocks.FURNACE);
            helper.getBlockEntity(FEET.west(2), AbstractFurnaceBlockEntity.class).setItem(2, new ItemStack(Items.IRON_INGOT, 3));
        }, () -> {
            ItemStack output = helper.getBlockEntity(FEET.west(2), AbstractFurnaceBlockEntity.class).getItem(2);
            check(helper, (output.is(Items.CHARCOAL) || output.is(Items.ROTTEN_FLESH)) && output.getCount() == 3, "wrong_smelt must change the output and keep its count");
        }));
        tests.put("event_furniture_moves", helper -> runs(helper, "furniture_moves", player -> {
            helper.setBlock(FEET.west(2), Blocks.CHEST);
            helper.getBlockEntity(FEET.west(2), ChestBlockEntity.class).setItem(0, new ItemStack(Items.DIAMOND, 5));
        }, () -> {
            helper.assertBlockPresent(Blocks.AIR, FEET.west(2));
            check(helper, BlockPos.betweenClosedStream(helper.absolutePos(FEET).offset(-9, -2, -9), helper.absolutePos(FEET).offset(9, 2, 9))
                .anyMatch(pos -> helper.getLevel().getBlockEntity(pos) instanceof ChestBlockEntity chest && ItemStack.matches(chest.getItem(0), new ItemStack(Items.DIAMOND, 5))),
                "furniture_moves must carry the chest and its item elsewhere");
            check(helper, helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(FEET.west(2))).inflate(2)).isEmpty(), "furniture_moves must drop nothing");
        }));
        tests.put("event_doorstep_gift", helper -> runs(helper, "doorstep_gift", player -> helper.setBlock(FEET.west(3), Blocks.OAK_DOOR),
            () -> check(helper, !helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(FEET)).inflate(6)).isEmpty(),
                "doorstep_gift must leave an item")));
        tests.put("event_muddy_footprints", helper -> runs(helper, "muddy_footprints", player -> {
            for (BlockPos pos : BlockPos.betweenClosed(0, 4, 0, 9, 4, 9)) {
                helper.setBlock(pos, Blocks.STONE);
            }
            helper.setBlock(FEET.west(4), Blocks.CHEST);
        }, () -> {
            for (int k = 1; k <= 3; k++) {
                if (helper.getBlockState(FEET.west(k)).is(Content.FOOTPRINTS.get())) {
                    return;
                }
            }
            check(helper, false, "muddy_footprints must leave prints toward the chest");
        }));
        tests.put("event_spider_overhead", helper -> runs(helper, "spider_overhead",
            player -> helper.setBlock(FEET.above(4), Blocks.STONE), () -> {}));
        tests.put("event_lost_growth", helper -> runs(helper, "lost_growth", player -> {
            helper.setBlock(FEET.south(2).below(), Blocks.FARMLAND);
            helper.setBlock(FEET.south(2), ((CropBlock) Blocks.WHEAT).getStateForAge(7));
            // Random ticks may grow it a stage again before the check, so "lost" is: no longer ripe.
        }, () -> check(helper, helper.getBlockState(FEET.south(2)).getValue(CropBlock.AGE) < CropBlock.MAX_AGE, "lost_growth must take the growth")));
        tests.put("event_lectern_pages", helper -> runs(helper, "lectern_pages", player -> {
            helper.setBlock(FEET.west(2), Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.HAS_BOOK, true));
            ItemStack book = new ItemStack(Items.WRITABLE_BOOK);
            book.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(List.of(Filterable.passThrough("a"), Filterable.passThrough("b"), Filterable.passThrough("c"))));
            ((LecternBlockEntity) helper.getBlockEntity(FEET.west(2), LecternBlockEntity.class)).setBook(book);
        }, () -> check(helper, ((LecternBlockEntity) helper.getBlockEntity(FEET.west(2), LecternBlockEntity.class)).getPage() != 0, "lectern_pages must turn the page")));
        tests.put("event_pot_wobble", helper -> runs(helper, "pot_wobble",
            player -> helper.setBlock(FEET.west(2), Blocks.DECORATED_POT), () -> helper.assertBlockPresent(Blocks.DECORATED_POT, FEET.west(2))));
        tests.put("event_bell_swing", helper -> runs(helper, "bell_swing",
            player -> helper.setBlock(FEET.west(2), Blocks.BELL), () -> helper.assertBlockPresent(Blocks.BELL, FEET.west(2))));
        HardcoreGameTests.hardcore(tests, "event_redstone_drift", helper -> runs(helper, "redstone_drift",
            player -> helper.setBlock(FEET.west(2), Blocks.REPEATER),
            () -> check(helper, helper.getBlockState(FEET.west(2)).getValue(RepeaterBlock.DELAY) != 1, "redstone_drift must change the delay")));
        tests.put("event_occupied_bed", helper -> runs(helper, "occupied_bed", player -> {
            helper.setBlock(FEET.west(3), Blocks.BED.red().defaultBlockState().setValue(BedBlock.PART, BedPart.HEAD).setValue(BedBlock.FACING, Direction.WEST));
            helper.setBlock(FEET.west(2), Blocks.BED.red().defaultBlockState().setValue(BedBlock.PART, BedPart.FOOT).setValue(BedBlock.FACING, Direction.WEST));
        }, () -> {
            check(helper, helper.getBlockState(FEET.west(3)).getValue(BedBlock.OCCUPIED), "occupied_bed must mark the head taken");
            check(helper, helper.getBlockState(FEET.west(2)).getValue(BedBlock.OCCUPIED), "occupied_bed must mark the foot taken");
        }));
        tests.put("event_shuffled_books", helper -> {
            BlockPos shelf = helper.absolutePos(FEET.west(2));
            runs(helper, "shuffled_books", player -> {
                helper.setBlock(FEET.west(2), Blocks.CHISELED_BOOKSHELF);
                ChiseledBookShelfBlockEntity entity = (ChiseledBookShelfBlockEntity) helper.getLevel().getBlockEntity(shelf);
                entity.setItem(0, new ItemStack(Items.BOOK));
                entity.setItem(1, new ItemStack(Items.ENCHANTED_BOOK));
            }, () -> check(helper, ((ChiseledBookShelfBlockEntity) helper.getLevel().getBlockEntity(shelf)).getItem(0).getItem() != Items.BOOK
                || ((ChiseledBookShelfBlockEntity) helper.getLevel().getBlockEntity(shelf)).getItem(1).getItem() != Items.ENCHANTED_BOOK,
                "shuffled_books must move the books"));
        });
        tests.put("event_dispenser_shot", helper -> runs(helper, "dispenser_shot",
            player -> helper.setBlock(FEET.west(3), Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST)),
            () -> check(helper, !helper.getLevel().getEntitiesOfClass(Arrow.class, new AABB(helper.absolutePos(FEET)).inflate(6)).isEmpty(),
                "dispenser_shot must fire an arrow")));
        tests.put("event_villager_refuses", helper -> {
            Villager[] villager = new Villager[1];
            runs(helper, "villager_refuses", player -> villager[0] = EntityTypes.VILLAGER.spawn(helper.getLevel(), helper.absolutePos(FEET).east(4), EntitySpawnReason.COMMAND),
                () -> check(helper, villager[0].getUnhappyCounter() > 0, "villager_refuses must shake the head"));
        });
        tests.put("event_cat_flee", helper -> {
            Cat[] cat = new Cat[1];
            double[] start = new double[1];
            runs(helper, "cat_flee", player -> {
                cat[0] = EntityTypes.CAT.spawn(helper.getLevel(), helper.absolutePos(FEET).east(1), EntitySpawnReason.COMMAND);
                start[0] = cat[0].getX();
            }, () -> check(helper, cat[0].getX() > start[0] + 1.5, "cat_flee must send the cat running away from the player"));
        });
        tests.put("event_golem_flower", helper -> {
            Entity[] golem = new Entity[1];
            runs(helper, "golem_flower", player -> golem[0] = EntityTypes.IRON_GOLEM.spawn(helper.getLevel(), helper.absolutePos(FEET).east(4), EntitySpawnReason.COMMAND),
                () -> check(helper, ((IronGolem) golem[0]).getOfferFlowerTick() > 0, "golem_flower must offer the flower"));
        });
        tests.put("event_dog_growls", helper -> {
            Wolf[] wolf = new Wolf[1];
            runs(helper, "dog_growls", player -> {
                wolf[0] = EntityTypes.WOLF.spawn(helper.getLevel(), helper.absolutePos(FEET).east(4), EntitySpawnReason.COMMAND);
                wolf[0].setTame(true, false);
            }, () -> check(helper, wolf[0].isInSittingPose(), "dog_growls must sit the dog"));
        });
        HardcoreGameTests.hardcore(tests, "event_enderman_takes", helper -> {
            Enderman[] man = new Enderman[1];
            runs(helper, "enderman_takes", player -> {
                helper.setBlock(FEET.west(2), Blocks.OAK_PLANKS);
                man[0] = EntityTypes.ENDERMAN.spawn(helper.getLevel(), helper.absolutePos(FEET).east(4), EntitySpawnReason.COMMAND);
            }, () -> check(helper, man[0].getCarriedBlock() != null && helper.getBlockState(FEET.west(2)).isAir(),
                "enderman_takes must move the block into the enderman's hands"));
        });
        HardcoreGameTests.hardcore(tests, "event_torch_swap", helper -> runs(helper, "torch_swap",
            player -> helper.setBlock(FEET.west(2), Blocks.TORCH),
            () -> check(helper, (helper.getBlockState(FEET.west(2)).is(Blocks.SOUL_TORCH) || helper.getBlockState(FEET.west(2)).is(Blocks.COPPER_TORCH)),
                "torch_swap must turn the torch into another kind")));
        HardcoreGameTests.hardcore(tests, "event_repaint", helper -> runs(helper, "repaint",
            player -> {
                helper.setBlock(FEET.west(2), Blocks.WOOL.red());
                helper.setBlock(FEET.west(3), Blocks.WOOL.red());
            },
            () -> check(helper, !helper.getBlockState(FEET.west(2)).is(Blocks.WOOL.red()) && !helper.getBlockState(FEET.west(3)).is(Blocks.WOOL.red()),
                "repaint must change every block of the kind")));
        tests.put("event_armor_off", helper -> {
            ServerPlayer[] worn = new ServerPlayer[1];
            runs(helper, "armor_off", player -> {
                worn[0] = player;
                player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            }, () -> {
                ServerPlayer player = worn[0];
                boolean head = !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
                check(helper, !head || player.getItemBySlot(EquipmentSlot.CHEST).getCount() == 0,
                    "armor_off must take off at least one piece");
                check(helper, player.getInventory().contains(s -> s.is(Items.IRON_HELMET)) != head,
                    "a piece taken off must be in the inventory, not lost");
            });
        });
        HardcoreGameTests.hardcore(tests, "event_tool_look", helper -> {
            ServerPlayer[] holder = new ServerPlayer[1];
            runs(helper, "tool_look", player -> {
                holder[0] = player;
                player.getInventory().setItem(0, new ItemStack(Items.IRON_PICKAXE));
            }, () -> {
                ItemStack tool = holder[0].getInventory().getItem(0);
                Identifier model = tool.get(DataComponents.ITEM_MODEL);
                check(helper, tool.is(Items.IRON_PICKAXE) ? model != null && !model.equals(Identifier.withDefaultNamespace("iron_pickaxe")) : !tool.isEmpty(),
                    "tool_look must show the tool as another one, or turn it into that one");
            });
        });
        tests.put("event_dream", helper -> {
            ServerPlayer[] dreamer = new ServerPlayer[1];
            HauntData data = HauntData.get(helper.getLevel().getServer());
            runs(helper, "dream", player -> {
                dreamer[0] = player;
                Director.add(player, data, Spirits.personal().get(0), helper.getLevel().getGameTime());
                helper.setBlock(FEET.west(3), Blocks.BED.red().defaultBlockState().setValue(BedBlock.PART, BedPart.HEAD).setValue(BedBlock.FACING, Direction.WEST));
                helper.setBlock(FEET.west(2), Blocks.BED.red().defaultBlockState().setValue(BedBlock.PART, BedPart.FOOT).setValue(BedBlock.FACING, Direction.WEST));
                check(helper, player.startSleeping(helper.absolutePos(FEET.west(3))), "the test player must be able to lie in the bed");
                helper.runAfterDelay(12, player::stopSleeping);
            }, () -> {
                check(helper, !data.journal(dreamer[0].getUUID()).isEmpty(), "dream must write the dream in the journal on waking");
                data.put(dreamer[0].getUUID(), List.of());
            });
        });
        tests.put("event_goat_rams", helper -> {
            Goat[] goat = new Goat[1];
            double[] start = new double[1];
            runs(helper, "goat_rams", player -> {
                goat[0] = EntityTypes.GOAT.spawn(helper.getLevel(), helper.absolutePos(FEET).east(5), EntitySpawnReason.COMMAND);
                start[0] = goat[0].getX();
            }, () -> check(helper, goat[0].getX() < start[0] - 1.0, "goat_rams must send the goat at the player"));
        });
        tests.put("event_boat_drifts", helper -> {
            AbstractBoat[] boat = new AbstractBoat[1];
            double[] start = new double[1];
            runs(helper, "boat_drifts", player -> {
                helper.setBlock(FEET.east(3), Blocks.WATER);
                boat[0] = EntityTypes.OAK_BOAT.spawn(helper.getLevel(), helper.absolutePos(FEET).east(3), EntitySpawnReason.COMMAND);
                start[0] = boat[0].getX();
            }, () -> check(helper, boat[0].getX() > start[0] + 0.1, "boat_drifts must push the boat away"));
        });
        tests.put("event_minecart_rolls", helper -> {
            AbstractMinecart[] cart = new AbstractMinecart[1];
            double[] start = new double[1];
            runs(helper, "minecart_rolls", player -> {
                for (int x = 5; x <= 9; x++) {
                    helper.setBlock(new BlockPos(x, FEET.getY(), FEET.getZ()), Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
                }
                cart[0] = EntityTypes.MINECART.spawn(helper.getLevel(), helper.absolutePos(FEET).east(4), EntitySpawnReason.COMMAND);
                start[0] = cart[0].getX();
            }, () -> check(helper, cart[0].getX() < start[0] - 0.3, "minecart_rolls must roll the cart toward the player"));
        });
        tests.put("event_leash_snaps", helper -> {
            Cow[] cow = new Cow[1];
            runs(helper, "leash_snaps", player -> {
                cow[0] = EntityTypes.COW.spawn(helper.getLevel(), helper.absolutePos(FEET).east(4), EntitySpawnReason.COMMAND);
                cow[0].setLeashedTo(LeashFenceKnotEntity.getOrCreateKnot(helper.getLevel(), helper.absolutePos(FEET).east(3)), true);
            }, () -> check(helper, !cow[0].isLeashed(), "leash_snaps must free the animal"));
        });
        tests.put("event_shrieker", helper -> runs(helper, "shrieker",
            player -> helper.setBlock(FEET.west(2), Blocks.SCULK_SHRIEKER), () -> helper.assertBlockPresent(Blocks.SCULK_SHRIEKER, FEET.west(2))));
        tests.put("event_insomnia", helper -> {
            ServerPlayer[] who = new ServerPlayer[1];
            runs(helper, "insomnia", player -> who[0] = player,
                () -> check(helper, who[0].getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)) >= 72000, "insomnia must count the days without sleep"));
        });
        farOff(tests, "distant_player", 60, Mannequin.class, (helper, figure) -> {
            var look = figure.getProfile().skinPatch();
            check(helper, look.body().isPresent() && look.model().isPresent(), "the stranger must wear one of the default skins");
            check(helper, look.body().get().id().getPath().contains(look.model().get().getSerializedName()), "the skin must match its arm width");
        });
        farOff(tests, "distant_walker", 45, Mannequin.class);
        // A spirit, or a player with none behind the event, says each line once; then the event rests.
        tests.put("event_text_never_repeats", helper -> {
            ServerPlayer player = connected(helper);
            HauntEvent voice = HauntEvents.byId(Identifier.withDefaultNamespace("own_voice")).orElseThrow();
            for (int i = 0; i < Config.event("own_voice").texts("lines").size(); i++) {
                check(helper, voice.canRun(player), "own_voice must have line " + (i + 1) + " left");
                voice.run(player);
            }
            check(helper, !voice.canRun(player), "own_voice must rest once every line is said");
            player.discard();
            helper.succeed();
        });
        PADDING.put("event_double_approaches", 50);
        TIMEOUT.put("event_double_approaches", 120);
        tests.put("event_double_approaches", helper -> {
            // The player on a mound one block high and nine across, which the twin has to jump onto.
            BlockPos centre = helper.absolutePos(FEET);
            forceChunks(helper, centre, 24, true);
            for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-22, -2, -22), centre.offset(22, -1, 22))) {
                boolean mound = pos.getY() == centre.getY() - 1 && Math.hypot(pos.getX() - centre.getX(), pos.getZ() - centre.getZ()) <= 9;
                helper.getLevel().setBlock(pos, pos.getY() == centre.getY() - 2 || mound ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
            }
            // The test's own barrier walls would stand in its way.
            for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-22, -1, -22), centre.offset(22, 2, 22))) {
                if (helper.getLevel().getBlockState(pos).is(Blocks.BARRIER)) {
                    helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                }
            }
            ServerPlayer player = connected(helper);
            player.snapTo(centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5);
            HauntEvent event = HauntEvents.byId(Identifier.withDefaultNamespace("double")).orElseThrow();
            check(helper, event.canRun(player), "double must be able to run in its scene");
            event.run(player);
            List<Mannequin> twins = helper.getLevel().getEntitiesOfClass(Mannequin.class, new AABB(centre).inflate(30));
            check(helper, twins.size() == 1, "double must bring one twin, found " + twins.size());
            Mannequin twin = twins.get(0);
            check(helper, twin.getY() < centre.getY(), "the twin must start below the mound");
            helper.runAfterDelay(100, () -> {
                check(helper, twin.distanceTo(player) < 3, "the twin must come up onto the mound to the player, is " + twin.distanceTo(player) + " away");
                twin.discard();
                forceChunks(helper, centre, 24, false);
                player.discard();
                helper.succeed();
            });
        });
        farOff(tests, "nametag", 30, Display.TextDisplay.class);
        farOff(tests, "pillar", 24, Mannequin.class);
        // The cobblestone is in the world only when swaps are real, which hardcore makes them.
        Map<String, Consumer<GameTestHelper>> real = new HashMap<>();
        farOff(real, "pillar", 24, Mannequin.class, (helper, figure) ->
            check(helper, helper.getLevel().getBlockState(figure.blockPosition().below()).is(Blocks.COBBLESTONE), "pillar must stand on its cobblestone"));
        HardcoreGameTests.hardcore(tests, "hardcore_pillar", real.get("event_pillar"));
        PADDING.put("hardcore_pillar", PADDING.get("event_pillar"));
        tests.put("event_repaint_is_an_illusion", helper -> runs(helper, "repaint",
            player -> {
                helper.setBlock(FEET.west(2), Blocks.WOOL.red());
                helper.setBlock(FEET.west(3), Blocks.WOOL.red());
            },
            () -> check(helper, helper.getBlockState(FEET.west(2)).is(Blocks.WOOL.red()) && helper.getBlockState(FEET.west(3)).is(Blocks.WOOL.red()),
                "an illusion must leave the server's blocks alone")));
    }

    /**
     * An event that puts one {@code kind} up to {@code distance} blocks out, given a ring of standing room there wherever
     * it picks its side; the one is checked and removed.
     */
    private static void farOff(Map<String, Consumer<GameTestHelper>> tests, String id, int distance, Class<? extends Entity> kind) {
        farOff(tests, id, distance, kind, (helper, found) -> {});
    }

    private static <T extends Entity> void farOff(Map<String, Consumer<GameTestHelper>> tests, String id, int distance, Class<T> kind,
                                                  BiConsumer<GameTestHelper, T> also) {
        PADDING.put("event_" + id, 2 * (distance + 10));
        tests.put("event_" + id, helper -> runs(helper, id, player -> {
            BlockPos centre = helper.absolutePos(FEET);
            int reach = distance + 4;
            // A player's view distance keeps the ring loaded in a game; here only the test's own chunks are.
            forceChunks(helper, centre, reach, true);
            for (int x = -reach; x <= reach; x++) {
                for (int z = -reach; z <= reach; z++) {
                    double ring = Math.hypot(x, z);
                    if (ring >= distance - 2 && ring <= distance + 2) {
                        helper.getLevel().setBlock(centre.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
                    }
                }
            }
        }, () -> {
            AABB around = new AABB(helper.absolutePos(FEET)).inflate(distance + 10);
            List<T> found = helper.getLevel().getEntitiesOfClass(kind, around);
            check(helper, found.size() == 1, id + " must leave one " + kind.getSimpleName() + ", found " + found.size());
            also.accept(helper, found.get(0));
            found.get(0).discard();
            forceChunks(helper, helper.absolutePos(FEET), distance + 4, false);
        }));
    }

    private static void forceChunks(GameTestHelper helper, BlockPos centre, int reach, boolean forced) {
        for (int x = (centre.getX() - reach) >> 4; x <= (centre.getX() + reach) >> 4; x++) {
            for (int z = (centre.getZ() - reach) >> 4; z <= (centre.getZ() + reach) >> 4; z++) {
                helper.getLevel().setChunkForced(x, z, forced);
            }
        }
    }

    /** A log of any of the ghost tree's woods. */
    static boolean ghostLog(BlockState state) {
        return Content.GHOST_WOODS.values().stream().anyMatch(wood -> state.is(wood.log().get()));
    }

    /** A three-log oak with a crown, six blocks east of the player, on dirt as far as it can walk. */
    static void tree(GameTestHelper helper) {
        BlockPos centre = helper.absolutePos(FEET);
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-12, -1, -12), centre.offset(12, -1, 12))) {
            helper.getLevel().setBlock(pos, Blocks.DIRT.defaultBlockState(), 2);
        }
        BlockPos trunk = centre.east(6);
        for (int dy = 0; dy < 3; dy++) {
            helper.getLevel().setBlock(trunk.above(dy), Blocks.OAK_LOG.defaultBlockState(), 2);
        }
        for (BlockPos pos : BlockPos.betweenClosed(trunk.offset(-1, 2, -1), trunk.offset(1, 3, 1))) {
            if (!pos.equals(trunk.above(2))) {
                helper.getLevel().setBlock(pos, Blocks.OAK_LEAVES.defaultBlockState(), 2);
            }
        }
    }

    /** Sets the scene, checks the event can run, runs it, lets its scheduled steps finish, then checks the result. */
    static void runs(GameTestHelper helper, String id, Consumer<ServerPlayer> scene, Runnable after) {
        for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 9, 0, 9)) {
            helper.setBlock(pos, Blocks.STONE);
        }
        ServerPlayer player = connected(helper);
        BlockPos feet = helper.absolutePos(FEET);
        player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
        scene.accept(player);
        HauntEvent event = HauntEvents.byId(Identifier.withDefaultNamespace(id)).orElseThrow();
        check(helper, event.canRun(player), id + " must be able to run in its scene");
        event.run(player);
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            after.run();
            check(helper, player.isAlive(), id + " must not kill");
            player.discard();
            helper.succeed();
        });
    }

    /**
     * A survival player with a connection that swallows packets. Kept out of the player list, which would send it the
     * mod's payloads, and a mock connection has no channels for them.
     */
    private static ServerPlayer connected(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
        return player;
    }

    static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) throw helper.assertionException(Component.literal(message));
    }
}
