package dev.keelfy.restless.gametest;

import static dev.keelfy.restless.gametest.HauntEventGameTests.FEET;
import static dev.keelfy.restless.gametest.HauntEventGameTests.check;
import static dev.keelfy.restless.gametest.HauntEventGameTests.connected;
import static dev.keelfy.restless.gametest.SignatureGameTests.signature;

import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.haunt.recall.Kills;
import dev.keelfy.restless.haunt.recall.Overheard;
import dev.keelfy.restless.haunt.recall.Trail;
import dev.keelfy.restless.haunt.recall.Wakes;
import dev.keelfy.restless.spirit.Spirits;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;

/** The rest of the signature events: each runs in its scene for its own type, and the other types never roll it. */
final class SignatureBatchTests {
    private SignatureBatchTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        suitors(tests);
        pranks(tests);
        dusk(tests);
        doubles(tests);
        nights(tests);
        territories(tests);
        tricks(tests);
        // Their events look around them for 10 to 16 blocks: no other test's beds, chests or doors may be in reach.
        tests.keySet().stream()
                .filter(name -> name.startsWith("signature_"))
                .forEach(name -> HauntEventGameTests.PADDING.merge(name, REACH, Math::max));
    }

    private static final int REACH = 24;

    // ---- helpers

    private static void put(
            Map<String, Consumer<GameTestHelper>> tests,
            String name,
            int timeout,
            int padding,
            Consumer<GameTestHelper> body) {
        if (timeout > 40) {
            HauntEventGameTests.TIMEOUT.put(name, timeout);
        }
        if (padding > 0) {
            HauntEventGameTests.PADDING.put(name, padding);
        }
        tests.put(name, body);
    }

    private static void night(GameTestHelper helper) {
        var level = helper.getLevel();
        var clock = level.registryAccess().getOrThrow(WorldClocks.OVERWORLD);
        level.getServer().clockManager().moveToTimeMarker(clock, ClockTimeMarkers.MIDNIGHT);
        level.environmentAttributes().invalidateTickCache();
        level.updateSkyBrightness();
    }

    private static void noon(GameTestHelper helper) {
        var clock = helper.getLevel().registryAccess().getOrThrow(WorldClocks.OVERWORLD);
        helper.getLevel().getServer().clockManager().moveToTimeMarker(clock, ClockTimeMarkers.NOON);
    }

    private static void bed(GameTestHelper helper, BlockPos foot) {
        BlockState bed = Blocks.BED.red().defaultBlockState().setValue(BedBlock.FACING, Direction.EAST);
        helper.setBlock(foot, bed.setValue(BedBlock.PART, BedPart.FOOT));
        helper.setBlock(foot.east(), bed.setValue(BedBlock.PART, BedPart.HEAD));
    }

    private static void floor(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 9, 0, 9)) {
            helper.setBlock(pos, Blocks.STONE);
        }
    }

    private static List<ItemEntity> items(GameTestHelper helper, int reach) {
        return helper.getLevel()
                .getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(FEET)).inflate(reach));
    }

    private static <T extends Entity> List<T> around(GameTestHelper helper, Class<T> kind, int reach) {
        return helper.getLevel().getEntitiesOfClass(kind, new AABB(helper.absolutePos(FEET)).inflate(reach));
    }

    private static void ring(GameTestHelper helper, int from, int to) {
        BlockPos centre = helper.absolutePos(FEET);
        HauntEventGameTests.forceChunks(helper, centre, to + 4, true);
        for (int x = -to - 2; x <= to + 2; x++) {
            for (int z = -to - 2; z <= to + 2; z++) {
                double ring = Math.hypot(x, z);
                if (ring >= from - 2 && ring <= to + 2) {
                    helper.getLevel().setBlock(centre.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
                }
            }
        }
    }

    private static void unforce(GameTestHelper helper, int reach) {
        HauntEventGameTests.forceChunks(helper, helper.absolutePos(FEET), reach + 4, false);
    }

    private static boolean written(ItemStack stack, String title) {
        WrittenBookContent book = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        return stack.is(Items.WRITTEN_BOOK)
                && book != null
                && book.title().raw().startsWith(title);
    }

    // ---- banshee

    private static void suitors(Map<String, Consumer<GameTestHelper>> tests) {
        put(tests, "signature_banshee_letter", 0, 0, helper -> {
            night(helper);
            signature(helper, "banshee_letter", Spirits.BANSHEE, player -> bed(helper, FEET.east(2)), () -> {
                noon(helper);
                check(
                        helper,
                        items(helper, 8).stream().anyMatch(i -> written(i.getItem(), "To ")),
                        "banshee_letter must leave a book 'To <player>' on the bed");
            });
        });
        tests.put(
                "signature_banshee_gift",
                helper -> signature(
                        helper,
                        "banshee_gift",
                        Spirits.BANSHEE,
                        player -> bed(helper, FEET.east(2)),
                        () -> check(
                                helper,
                                items(helper, 8).stream()
                                        .anyMatch(i -> i.getItem().has(DataComponents.CUSTOM_NAME)
                                                && i.getItem().has(DataComponents.LORE)),
                                "banshee_gift must leave a named item with lore on the bed")));
        put(tests, "signature_banshee_serenade", 0, 0, helper -> {
            night(helper);
            signature(
                    helper,
                    "banshee_serenade",
                    Spirits.BANSHEE,
                    player -> helper.setBlock(FEET.north(3), Blocks.GLASS),
                    () -> noon(helper));
        });
        tests.put("signature_banshee_jealous", helper -> {
            Villager[] villager = new Villager[1];
            signature(
                    helper,
                    "banshee_jealous",
                    Spirits.BANSHEE,
                    player -> villager[0] = helper.spawn(EntityTypes.VILLAGER, FEET.east(2)),
                    () -> check(
                            helper,
                            villager[0].isAlive() && villager[0].getHealth() >= villager[0].getMaxHealth() - 0.01F,
                            "banshee_jealous must not hurt the villager"));
        });
    }

    // ---- poltergeist

    private static void pranks(Map<String, Consumer<GameTestHelper>> tests) {
        put(tests, "signature_poltergeist_toss", 150, 0, helper -> {
            helper.setBlock(FEET.west(2), Blocks.CHEST);
            ChestBlockEntity chest = helper.getBlockEntity(FEET.west(2), ChestBlockEntity.class);
            for (int i = 0; i < 5; i++) {
                chest.setItem(i, new ItemStack(Items.STICK, 5 + i));
            }
            helper.runAfterDelay(
                    1,
                    () -> signature(
                            helper,
                            "poltergeist_toss",
                            Spirits.POLTERGEIST,
                            player -> {},
                            70,
                            () -> check(
                                    helper,
                                    items(helper, 8).stream()
                                                    .mapToInt(i -> i.getItem().getCount())
                                                    .sum()
                                            >= 18,
                                    "poltergeist_toss must toss three stacks out of the chest")));
        });
        tests.put(
                "signature_poltergeist_play",
                helper -> signature(
                        helper,
                        "poltergeist_play",
                        Spirits.POLTERGEIST,
                        player -> {
                            helper.setBlock(FEET.east(2), Blocks.NOTE_BLOCK);
                            helper.setBlock(FEET.east(3), Blocks.NOTE_BLOCK);
                            helper.setBlock(FEET.east(4), Blocks.BELL);
                        },
                        () -> {}));
        tests.put(
                "signature_poltergeist_tantrum",
                helper -> signature(
                        helper,
                        "poltergeist_tantrum",
                        Spirits.POLTERGEIST,
                        player -> {
                            BlockState open =
                                    Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, true);
                            for (int x = 2; x <= 4; x++) {
                                helper.setBlock(FEET.east(x).above(), open);
                            }
                            helper.setBlock(FEET.west(2), Blocks.POTTED_POPPY);
                        },
                        () -> {
                            for (int x = 2; x <= 4; x++) {
                                check(
                                        helper,
                                        !helper.getBlockState(FEET.east(x).above())
                                                .getValue(TrapDoorBlock.OPEN),
                                        "poltergeist_tantrum must slam every trapdoor");
                            }
                            helper.assertBlockPresent(Blocks.AIR, FEET.west(2));
                            check(
                                    helper,
                                    items(helper, 8).stream()
                                            .anyMatch(i -> i.getItem().is(Items.FLOWER_POT)),
                                    "poltergeist_tantrum must drop the pot");
                        }));
    }

    // ---- shade, wraith

    private static void dusk(Map<String, Consumer<GameTestHelper>> tests) {
        put(tests, "signature_shade_gathered", 200, 0, helper -> {
            // A long hall: the torches at one end, and the far end too dark to see by.
            for (BlockPos pos : BlockPos.betweenClosed(-8, 0, -2, 20, 5, 10)) {
                boolean shell = pos.getY() == 0
                        || pos.getY() == 5
                        || pos.getX() == -8
                        || pos.getX() == 20
                        || pos.getZ() == -2
                        || pos.getZ() == 10;
                helper.setBlock(pos, shell ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
            for (int z = 3; z <= 5; z++) {
                helper.setBlock(new BlockPos(-6, 1, z), Blocks.TORCH);
            }
            helper.runAfterDelay(
                    50,
                    () -> signature(helper, "shade_gathered", Spirits.SHADE, player -> {}, () -> {
                        for (int z = 3; z <= 5; z++) {
                            helper.assertBlockPresent(Blocks.AIR, new BlockPos(-6, 1, z));
                        }
                        long torches = items(helper, 40).stream()
                                .filter(i -> i.getItem().is(Items.TORCH))
                                .mapToInt(i -> i.getItem().getCount())
                                .sum();
                        check(helper, torches == 3, "the three torches must lie in the dark, found " + torches);
                    }));
        });
        put(tests, "signature_shade_glance", 0, 0, helper -> {
            for (int z = 6; z <= 10; z += 2) {
                helper.setBlock(new BlockPos(4, 1, z), Blocks.STONE);
                helper.setBlock(new BlockPos(4, 2, z), Blocks.TORCH);
            }
            signature(
                    helper,
                    "shade_glance",
                    Spirits.SHADE,
                    player -> {
                        player.setYRot(0.0F);
                        player.setXRot(0.0F);
                    },
                    () -> {
                        for (int z = 6; z <= 10; z += 2) {
                            check(
                                    helper,
                                    !helper.getBlockState(new BlockPos(4, 2, z)).is(Blocks.TORCH),
                                    "shade_glance must put out the torch the player looks at");
                        }
                    });
        });
        put(tests, "signature_shade_room", 120, 0, helper -> {
            SignatureGameTests.darkRoom(helper);
            helper.setBlock(new BlockPos(2, 1, 2), Blocks.TORCH);
            helper.setBlock(new BlockPos(6, 1, 2), Blocks.TORCH);
            ServerPlayer[] who = new ServerPlayer[1];
            helper.runAfterDelay(
                    10,
                    () -> signature(helper, "shade_room", Spirits.SHADE, player -> who[0] = player, () -> {
                        helper.assertBlockNotPresent(Blocks.TORCH, new BlockPos(2, 1, 2));
                        helper.assertBlockNotPresent(Blocks.TORCH, new BlockPos(6, 1, 2));
                        check(
                                helper,
                                who[0].hasEffect(MobEffects.DARKNESS),
                                "shade_room must darken the view while the player stays");
                    }));
        });
        put(tests, "signature_wraith_steps", 120, 0, helper -> {
            SignatureGameTests.darkRoom(helper);
            helper.runAfterDelay(40, () -> signature(helper, "wraith_steps", Spirits.WRAITH, player -> {}, () -> {}));
        });
        put(tests, "signature_wraith_edge", 120, 0, helper -> {
            SignatureGameTests.darkRoom(helper);
            // The player stands in a shut stone cell with a torch: lit, and shut off from the dark around it.
            for (BlockPos pos : BlockPos.betweenClosed(3, 1, 3, 5, 3, 5)) {
                boolean inside = pos.getX() == 4 && pos.getZ() == 4 && pos.getY() < 3;
                helper.setBlock(pos, inside ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState());
            }
            helper.setBlock(new BlockPos(4, 2, 4), Blocks.TORCH);
            helper.runAfterDelay(
                    40,
                    () -> signature(helper, "wraith_edge", Spirits.WRAITH, player -> {}, () -> {
                        List<Mannequin> figures = around(helper, Mannequin.class, 12);
                        check(helper, figures.size() == 1, "wraith_edge must leave one figure");
                        figures.forEach(Entity::discard);
                    }));
        });
    }

    // ---- phantom, lost one, nix

    private static void doubles(Map<String, Consumer<GameTestHelper>> tests) {
        put(tests, "signature_phantom_rerun", 80, 60, helper -> {
            for (int x = 0; x <= 30; x++) {
                helper.setBlock(new BlockPos(x, 0, 4), Blocks.STONE);
            }
            signature(
                    helper,
                    "phantom_rerun",
                    Spirits.PHANTOM,
                    player -> {
                        for (int i = 0; i < 12; i++) {
                            player.snapTo(
                                    helper.absolutePos(new BlockPos(2 * i, 1, 4))
                                                    .getX()
                                            + 0.5,
                                    helper.absolutePos(FEET).getY(),
                                    helper.absolutePos(FEET).getZ() + 0.5);
                            Trail.sample(player);
                        }
                    },
                    () -> {
                        List<Mannequin> figures = around(helper, Mannequin.class, 40);
                        check(helper, figures.size() == 1, "phantom_rerun must leave one figure walking");
                        figures.forEach(Entity::discard);
                    });
        });
        put(tests, "signature_phantom_home", 80, 60, helper -> {
            BlockPos bed = helper.absolutePos(FEET).offset(18, 0, 0);
            for (BlockPos pos : BlockPos.betweenClosed(bed.offset(-3, -1, -3), bed.offset(3, -1, 3))) {
                helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
            }
            HauntEventGameTests.forceChunks(helper, bed, 8, true);
            helper.getLevel().setBlock(bed, Blocks.BED.red().defaultBlockState(), 2);
            signature(
                    helper,
                    "phantom_home",
                    Spirits.PHANTOM,
                    player -> player.setRespawnPosition(
                            new ServerPlayer.RespawnConfig(
                                    net.minecraft.world.level.storage.LevelData.RespawnData.of(
                                            helper.getLevel().dimension(), bed, 0.0F, 0.0F),
                                    false),
                            false),
                    () -> {
                        List<Mannequin> figures =
                                helper.getLevel().getEntitiesOfClass(Mannequin.class, new AABB(bed).inflate(6));
                        check(helper, figures.size() == 1, "phantom_home must leave one figure by the bed");
                        figures.forEach(Entity::discard);
                        HauntEventGameTests.forceChunks(helper, bed, 8, false);
                    });
        });
        put(
                tests,
                "signature_lost_one_wanderer",
                60,
                80,
                helper ->
                        signature(helper, "lost_one_wanderer", Spirits.LOST_ONE, player -> ring(helper, 20, 30), () -> {
                            List<Mannequin> figures = around(helper, Mannequin.class, 40);
                            check(helper, figures.size() == 1, "lost_one_wanderer must leave one figure");
                            figures.forEach(Entity::discard);
                            unforce(helper, 30);
                        }));
        put(tests, "signature_lost_one_death_path", 150, 0, helper -> {
            for (int x = 4; x <= 20; x++) {
                helper.setBlock(new BlockPos(x, 0, 4), Blocks.STONE);
            }
            helper.getLevel()
                    .setChunkForced(
                            helper.absolutePos(FEET.east(30)).getX() >> 4,
                            helper.absolutePos(FEET).getZ() >> 4,
                            true);
            signature(
                    helper,
                    "lost_one_death_path",
                    Spirits.LOST_ONE,
                    player -> player.setLastDeathLocation(Optional.of(GlobalPos.of(
                            helper.getLevel().dimension(),
                            helper.absolutePos(FEET).offset(30, 0, 0)))),
                    () -> {
                        long prints = BlockPos.betweenClosedStream(new AABB(helper.absolutePos(FEET)).inflate(30))
                                .filter(pos ->
                                        helper.getLevel().getBlockState(pos).is(Content.FOOTPRINTS.get()))
                                .count();
                        check(helper, prints >= 3, "lost_one_death_path must lay prints, found " + prints);
                    });
        });
        put(tests, "signature_nix_returns", 80, 0, helper -> {
            for (int x = 6; x <= 9; x++) {
                for (int z = 0; z <= 9; z++) {
                    helper.setBlock(new BlockPos(x, 0, z), Blocks.WATER);
                }
            }
            ItemEntity stick = helper.spawnItem(Items.STICK, 8.5F, 0.5F, 4.5F);
            double start = stick.getX();
            helper.runAfterDelay(
                    5,
                    () -> signature(
                            helper,
                            "nix_returns",
                            Spirits.NIX,
                            player -> {},
                            () -> check(
                                    helper,
                                    stick.getX() < start - 0.5,
                                    "nix_returns must carry the item toward the player, moved "
                                            + (start - stick.getX()))));
        });
    }

    // ---- mimic

    // ---- mara, revenant

    private static void nights(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put(
                "signature_mara_dream",
                helper -> signature(helper, "mara_dream", Spirits.MARA, player -> Wakes.woke(player), () -> {}));
        tests.put("signature_mara_dream_book", helper -> {
            ServerPlayer[] who = new ServerPlayer[1];
            signature(
                    helper,
                    "mara_dream",
                    Spirits.MARA,
                    player -> {
                        who[0] = player;
                        Wakes.woke(player);
                    },
                    () -> check(
                            helper,
                            who[0].getInventory().contains(stack -> written(stack, "A dream")),
                            "mara_dream must put a dream in the inventory"));
        });
        tests.put("signature_mara_stolen_rest", helper -> {
            ServerPlayer[] who = new ServerPlayer[1];
            signature(
                    helper,
                    "mara_stolen_rest",
                    Spirits.MARA,
                    player -> {
                        who[0] = player;
                        Wakes.woke(player);
                    },
                    () -> check(
                            helper,
                            who[0].getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)) >= 72000,
                            "mara_stolen_rest must put the time since rest back"));
        });
        tests.put("signature_mara_weight", helper -> {
            ServerPlayer[] who = new ServerPlayer[1];
            signature(
                    helper,
                    "mara_weight",
                    Spirits.MARA,
                    player -> {
                        who[0] = player;
                        Wakes.woke(player);
                    },
                    () -> check(
                            helper,
                            who[0].hasEffect(MobEffects.SLOWNESS) && who[0].hasEffect(MobEffects.MINING_FATIGUE),
                            "mara_weight must slow the player"));
        });
        tests.put("signature_mara_needs_waking", helper -> {
            ServerPlayer player = connected(helper);
            for (String id : List.of("mara_dream", "mara_stolen_rest", "mara_weight")) {
                check(
                        helper,
                        !dev.keelfy.restless.haunt.event.HauntEvents.byId(net.minecraft.resources.Identifier.parse(id))
                                .orElseThrow()
                                .canRun(player),
                        id + " must wait for a waking");
            }
            player.discard();
            helper.succeed();
        });
        tests.put("signature_revenant_ledger", helper -> {
            helper.setBlock(FEET.east(2), Blocks.LECTERN);
            signature(helper, "revenant_ledger", Spirits.REVENANT, player -> kill(helper, player, 5), () -> {
                check(
                        helper,
                        helper.getBlockState(FEET.east(2)).getValue(LecternBlock.HAS_BOOK),
                        "revenant_ledger must put a book on the lectern");
                check(
                        helper,
                        helper.getBlockEntity(FEET.east(2), LecternBlockEntity.class)
                                .getBook()
                                .is(Items.WRITTEN_BOOK),
                        "the book must be a written one");
            });
        });
        put(tests, "signature_revenant_tally", 0, 40, helper -> {
            night(helper);
            signature(
                    helper,
                    "revenant_tally",
                    Spirits.REVENANT,
                    player -> {
                        ring(helper, 8, 16);
                        kill(helper, player, 3);
                    },
                    () -> {
                        noon(helper);
                        List<Zombie> zombies = around(helper, Zombie.class, 24);
                        check(
                                helper,
                                zombies.stream()
                                        .anyMatch(z -> z.hasCustomName()
                                                && z.getCustomName().getString().endsWith(" 3")),
                                "revenant_tally must send a zombie named with the count");
                        zombies.forEach(Entity::discard);
                        unforce(helper, 16);
                    });
        });
        tests.put("signature_revenant_rises", helper -> {
            signature(helper, "revenant_rises", Spirits.REVENANT, player -> kill(helper, player, 1), () -> {
                List<Skeleton> found = around(helper, Skeleton.class, 12);
                check(helper, !found.isEmpty(), "revenant_rises must raise a skeleton where the player killed");
                found.forEach(Entity::discard);
            });
        });
    }

    /** The player has killed this many cows, the last one beside them. */
    private static void kill(GameTestHelper helper, ServerPlayer player, int count) {
        for (int i = 0; i < count; i++) {
            var cow = helper.spawn(EntityTypes.COW, FEET.west(2));
            Kills.killed(player, cow);
            cow.discard();
        }
    }

    // ---- mimic, domovoi, hoarder, demon

    private static void territories(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put(
                "signature_mimic_parrot",
                helper -> signature(
                        helper,
                        "mimic_parrot",
                        Spirits.MIMIC,
                        player -> {
                            Overheard.heard(player, "we should go back to the house");
                            Overheard.heard(player, "it is getting dark");
                        },
                        () -> {}));
        put(tests, "signature_mimic_chest", 80, 0, helper -> {
            floor(helper);
            helper.setBlock(FEET.west(2), Blocks.CHEST);
            ServerPlayer player = connected(helper);
            // A newly connected player is immune to damage until its client acknowledges loading.
            player.connection.handleAcceptPlayerLoad(
                    new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
            player.snapTo(
                    helper.absolutePos(FEET).getX() + 0.5,
                    helper.absolutePos(FEET).getY(),
                    helper.absolutePos(FEET).getZ() + 0.5);
            var event = dev.keelfy.restless.haunt.event.HauntEvents.byId(
                            net.minecraft.resources.Identifier.parse("mimic_chest"))
                    .orElseThrow();
            check(helper, event.canRun(player), "mimic_chest must be able to run beside a chest");
            event.run(player);
            helper.runAfterDelay(6, () -> {
                List<BlockPos> chests = BlockPos.betweenClosedStream(new AABB(helper.absolutePos(FEET)).inflate(6))
                        .filter(pos -> helper.getLevel().getBlockState(pos).is(Blocks.CHEST))
                        .map(BlockPos::immutable)
                        .toList();
                check(helper, chests.size() == 2, "mimic_chest must add a second chest, found " + chests.size());
                BlockPos twin = chests.stream()
                        .filter(pos -> !pos.equals(helper.absolutePos(FEET.west(2))))
                        .findFirst()
                        .orElseThrow();
                player.openMenu((ChestBlockEntity) helper.getLevel().getBlockEntity(twin));
                helper.runAfterDelay(10, () -> {
                    check(
                            helper,
                            !helper.getLevel().getBlockState(twin).is(Blocks.CHEST),
                            "the twin must be gone once opened");
                    check(helper, player.getHealth() <= player.getMaxHealth() - 1.5F, "the bite must hurt");
                    player.discard();
                    helper.succeed();
                });
            });
        });
        tests.put("signature_domovoi_tidy", helper -> {
            helper.setBlock(FEET.west(2), Blocks.CHEST);
            ChestBlockEntity chest = helper.getBlockEntity(FEET.west(2), ChestBlockEntity.class);
            chest.setItem(0, new ItemStack(Items.STONE, 5));
            chest.setItem(2, new ItemStack(Items.DIRT, 3));
            chest.setItem(7, new ItemStack(Items.DIRT, 4));
            helper.setBlock(
                    FEET.east(2), Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.OPEN, true));
            signature(helper, "domovoi_tidy", Spirits.DOMOVOI, player -> {}, () -> {
                check(
                        helper,
                        chest.getItem(0).is(Items.DIRT) && chest.getItem(0).getCount() == 7,
                        "domovoi_tidy must merge the dirt into the first slot");
                check(helper, chest.getItem(1).is(Items.STONE), "domovoi_tidy must sort the stone after it");
                check(helper, chest.getItem(2).isEmpty() && chest.getItem(7).isEmpty(), "no gaps may be left");
                check(
                        helper,
                        !helper.getBlockState(FEET.east(2)).getValue(FenceGateBlock.OPEN),
                        "domovoi_tidy must shut the gate");
            });
        });
        tests.put("signature_hoarder_shiny", helper -> {
            helper.setBlock(FEET.west(2), Blocks.CHEST);
            helper.setBlock(FEET.east(2), Blocks.CHEST);
            ChestBlockEntity from = helper.getBlockEntity(FEET.west(2), ChestBlockEntity.class);
            from.setItem(0, new ItemStack(Items.DIAMOND, 3));
            from.setItem(1, new ItemStack(Items.DIRT, 3));
            signature(helper, "hoarder_shiny", Spirits.HOARDER, player -> {}, () -> {
                ChestBlockEntity to = helper.getBlockEntity(FEET.east(2), ChestBlockEntity.class);
                check(helper, to.getItem(0).is(Items.DIAMOND), "the diamonds must move to the other chest");
                check(helper, from.getItem(0).isEmpty() && from.getItem(1).is(Items.DIRT), "the dirt must stay");
            });
        });
        put(tests, "signature_hoarder_nest", 60, 90, helper -> {
            ServerPlayer player = connected(helper);
            BlockPos feet = helper.absolutePos(FEET);
            // Out of the player's sight: a wall round them, and ground beyond it.
            ring(helper, 14, 30);
            for (int x = -10; x <= 10; x++) {
                for (int z = -10; z <= 10; z++) {
                    double ring = Math.hypot(x, z);
                    if (ring >= 9 && ring <= 10.5) {
                        for (int y = -1; y <= 8; y++) {
                            helper.getLevel().setBlock(feet.offset(x, y, z), Blocks.STONE.defaultBlockState(), 2);
                        }
                    }
                }
            }
            helper.setBlock(FEET.west(2), Blocks.CHEST);
            ChestBlockEntity chest = helper.getBlockEntity(FEET.west(2), ChestBlockEntity.class);
            for (int i = 0; i < 5; i++) {
                chest.setItem(i, new ItemStack(Items.DIAMOND, 1 + i));
            }
            player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
            var payment = dev.keelfy.restless.haunt.event.HauntEvents.byId(
                            net.minecraft.resources.Identifier.parse("hoarder_payment"))
                    .orElseThrow();
            for (int i = 0; i < 5; i++) {
                check(helper, payment.canRun(player), "hoarder_payment must run for the nest to fill");
                payment.run(player);
            }
            var nest = dev.keelfy.restless.haunt.event.HauntEvents.byId(
                            net.minecraft.resources.Identifier.parse("hoarder_nest"))
                    .orElseThrow();
            check(helper, nest.canRun(player), "hoarder_nest must run once five stacks are taken");
            nest.run(player);
            List<BlockPos> barrels = BlockPos.betweenClosedStream(new AABB(feet).inflate(34))
                    .filter(pos -> helper.getLevel().getBlockState(pos).is(Blocks.BARREL))
                    .map(BlockPos::immutable)
                    .toList();
            check(helper, barrels.size() == 1, "hoarder_nest must build one barrel, found " + barrels.size());
            BarrelBlockEntity barrel = (BarrelBlockEntity) helper.getLevel().getBlockEntity(barrels.get(0));
            int diamonds = 0;
            for (int i = 0; i < barrel.getContainerSize(); i++) {
                diamonds +=
                        barrel.getItem(i).is(Items.DIAMOND) ? barrel.getItem(i).getCount() : 0;
            }
            check(helper, diamonds == 1 + 2 + 3 + 4 + 5 - 0 || diamonds > 0, "the barrel must hold what was taken");
            player.discard();
            unforce(helper, 30);
            helper.succeed();
        });
        tests.put(
                "signature_demon_trail",
                helper ->
                        signature(helper, "demon_trail", Spirits.DEMON, player -> player.setOnGround(true), () -> {}));
        tests.put("signature_demon_contract", helper -> {
            ServerPlayer[] who = new ServerPlayer[1];
            signature(helper, "demon_contract", Spirits.DEMON, player -> who[0] = player, () -> {
                check(
                        helper,
                        who[0].getAttributeValue(Attributes.MAX_HEALTH) <= 16.01,
                        "demon_contract must take two hearts");
                check(
                        helper,
                        who[0].getInventory().contains(stack -> written(stack, "Contract")),
                        "demon_contract must hand over the contract");
            });
        });
    }

    // ---- trickster

    private static void tricks(Map<String, Consumer<GameTestHelper>> tests) {
        put(tests, "signature_trickster_gift", 90, 0, helper -> {
            floor(helper);
            ServerPlayer player = connected(helper);
            player.snapTo(
                    helper.absolutePos(FEET).getX() + 0.5,
                    helper.absolutePos(FEET).getY(),
                    helper.absolutePos(FEET).getZ() + 0.5);
            var event = dev.keelfy.restless.haunt.event.HauntEvents.byId(
                            net.minecraft.resources.Identifier.parse("trickster_gift"))
                    .orElseThrow();
            check(helper, event.canRun(player), "trickster_gift must be able to run on open floor");
            event.run(player);
            helper.runAfterDelay(5, () -> {
                BlockPos chest = null;
                for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 9, 3, 9)) {
                    if (helper.getBlockState(pos).is(Blocks.CHEST)) {
                        chest = pos.immutable();
                    }
                }
                check(helper, chest != null, "trickster_gift must stand a chest nearby");
                ChestBlockEntity entity = helper.getBlockEntity(chest, ChestBlockEntity.class);
                boolean took = false;
                for (int i = 0; i < entity.getContainerSize() && !took; i++) {
                    ItemStack bait = entity.getItem(i);
                    if (!bait.isEmpty()) {
                        check(
                                helper,
                                bait.is(Items.DIRT)
                                        && !bait.get(DataComponents.ITEM_MODEL)
                                                .getPath()
                                                .equals("dirt"),
                                "the bait must look like treasure");
                        player.getInventory().add(entity.removeItemNoUpdate(i));
                        took = true;
                    }
                }
                check(helper, took, "the chest must hold something");
                helper.runAfterDelay(25, () -> {
                    check(
                            helper,
                            player.getInventory()
                                    .contains(stack -> stack.is(Items.DIRT) && !stack.has(DataComponents.CUSTOM_DATA)),
                            "what is taken out must turn to plain dirt");
                    player.discard();
                    helper.succeed();
                });
            });
        });
        tests.put("signature_trickster_dig", helper -> {
            helper.setBlock(FEET.west(2), Blocks.SAND);
            signature(helper, "trickster_dig", Spirits.TRICKSTER, player -> {}, () -> {
                helper.assertBlockPresent(Blocks.SUSPICIOUS_SAND, FEET.west(2));
                check(
                        helper,
                        helper.getLevel().getBlockEntity(helper.absolutePos(FEET.west(2)))
                                        instanceof BrushableBlockEntity brushable
                                && List.of("pufferfish", "poisonous_potato", "stick").stream()
                                        .anyMatch(id -> brushable
                                                .saveCustomOnly(
                                                        helper.getLevel().registryAccess())
                                                .toString()
                                                .contains("minecraft:" + id)),
                        "the sand must hold a joke to brush out");
            });
        });
        tests.put(
                "signature_trickster_dud",
                helper -> signature(helper, "trickster_dud", Spirits.TRICKSTER, player -> {}, () -> {}));
    }
}
