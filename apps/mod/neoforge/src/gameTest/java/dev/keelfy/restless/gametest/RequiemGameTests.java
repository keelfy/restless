package dev.keelfy.restless.gametest;

import static dev.keelfy.restless.gametest.CharmGameTests.sanityChannel;
import static dev.keelfy.restless.gametest.CharmGameTests.stack;

import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.block.Altar;
import dev.keelfy.restless.content.item.Essences;
import dev.keelfy.restless.content.item.Reliquary;
import dev.keelfy.restless.haunt.BirthRules;
import dev.keelfy.restless.haunt.Births;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.HauntData;
import dev.keelfy.restless.haunt.History;
import dev.keelfy.restless.haunt.Journal;
import dev.keelfy.restless.haunt.Notebook;
import dev.keelfy.restless.haunt.Requiem;
import dev.keelfy.restless.haunt.Sanity;
import dev.keelfy.restless.haunt.requiem.Movement;
import dev.keelfy.restless.haunt.requiem.Movements;
import dev.keelfy.restless.haunt.requiem.Rite;
import dev.keelfy.restless.spirit.Spirits;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The Requiem: the reliquary, the released state and what ends or gives it back. */
final class RequiemGameTests {
    private RequiemGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("requiem_reliquary_takes_only_named_types_once_each", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            ItemStack reliquary = stack("reliquary");
            ItemStack essence = new ItemStack(Content.ESSENCE.get("shade").get(), 2);
            check(
                    helper,
                    Reliquary.insert(player, reliquary, essence) == Reliquary.Result.UNNAMED,
                    "an essence of a type never photographed must be refused");
            check(helper, essence.getCount() == 2, "a refused essence must not be spent");
            name(player, "shade");
            check(
                    helper,
                    Reliquary.insert(player, reliquary, essence) == Reliquary.Result.ADDED,
                    "an essence of a named type must be taken");
            check(helper, essence.getCount() == 1, "a taken essence must be spent");
            check(
                    helper,
                    Reliquary.insert(player, reliquary, essence) == Reliquary.Result.DUPLICATE,
                    "a second essence of one type must be refused");
            check(helper, essence.getCount() == 1, "a duplicate must not be spent");
            check(
                    helper,
                    Reliquary.insert(player, reliquary, new ItemStack(net.minecraft.world.item.Items.BONE))
                            == Reliquary.Result.NOT_ESSENCE,
                    "a bone is no essence");
            check(helper, !reliquary.hasFoil(), "a part-filled reliquary must not glow");
            player.setItemInHand(InteractionHand.MAIN_HAND, reliquary);
            player.setItemInHand(
                    InteractionHand.OFF_HAND,
                    new ItemStack(Content.ESSENCE.get("mara").get()));
            name(player, "mara");
            reliquary.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            check(
                    helper,
                    Reliquary.of(reliquary).held().equals(List.of("shade", "mara")),
                    "using an essence with the reliquary in the other hand must fill it in order");
            for (String type : Content.ESSENCE.keySet()) {
                name(player, type);
                Reliquary.insert(
                        player,
                        reliquary,
                        new ItemStack(Content.ESSENCE.get(type).get()));
            }
            check(helper, Reliquary.of(reliquary).full(), "one of each type must fill it");
            check(helper, reliquary.hasFoil(), "a full reliquary must glow");
            helper.succeed();
        });
        HauntEventGameTests.TIMEOUT.put("requiem_released_player_is_left_alone", 120);
        tests.put("requiem_released_player_is_left_alone", helper -> {
            ServerPlayer player = fresh(helper);
            sanityChannel(player);
            var data = HauntData.get(helper.getLevel().getServer());
            long now = helper.getLevel().getServer().overworld().getGameTime();
            data.putReleased(player.getUUID(), true);
            check(
                    helper,
                    Births.deed(player, Births.Deed.SLAUGHTER, false) == BirthRules.Verdict.NOTHING,
                    "no deed may bring a spirit to a released player");
            check(
                    helper,
                    Births.lure(player, Spirits.byId("shade").orElseThrow()) == BirthRules.Verdict.NOTHING,
                    "an effigy may not call a spirit to a released player");
            check(
                    helper,
                    Director.add(player, data, Spirits.byId("shade").orElseThrow(), now) == null,
                    "no spirit may be bound to a released player");
            check(helper, Director.hauntings(player, data, now).isEmpty(), "a released player has no spirits");
            Sanity.set(player, 0.2);
            check(helper, Sanity.of(player) == 1.0, "a released player's sanity must hold at full");
            // A territorial spirit whose turn is due: once its turn has come and gone, nothing may have run.
            UUID id = UUID.randomUUID();
            ServerLevel level = helper.getLevel();
            var spirit = new HauntData.Haunting(id, "poltergeist", 0x5EED5L, List.of(), 0L, 1.0, 0L, false, 0, 0L);
            var territorial = new HauntData.Territorial(spirit, level.dimension(), player.blockPosition(), 48);
            // Other tests reset the territorial spirits: keep ours due until the director has had its turn at it.
            long until = level.getServer().getTickCount() + 2L * 20 + 5;
            helper.succeedWhen(() -> {
                if (level.getServer().getTickCount() < until) {
                    data.putTerritorial(territorial);
                    check(helper, false, "waiting for the director's turn");
                }
                check(
                        helper,
                        History.last(null, 100).stream()
                                .noneMatch(e -> Director.label(spirit).equals(e.spirit())),
                        "a territorial spirit must not act on a released player");
                data.removeTerritorial(id);
                player.discard();
            });
        });
        tests.put("requiem_release_lays_spirits_to_rest", helper -> {
            ServerPlayer player = fresh(helper);
            sanityChannel(player);
            ServerLevel level = helper.getLevel();
            var data = HauntData.get(level.getServer());
            long now = level.getServer().overworld().getGameTime();
            for (String own : List.of("shade", "mara")) {
                Director.add(player, data, Spirits.byId(own).orElseThrow(), now);
            }
            check(helper, data.get(player.getUUID()).size() == 2, "two spirits must haunt the player");
            player.getInventory().setItem(3, stack("reliquary"));
            Requiem.release(player);
            check(helper, Requiem.released(player), "the player must be released");
            check(helper, data.get(player.getUUID()).isEmpty(), "every personal spirit must be laid to rest");
            check(
                    helper,
                    data.notebook(player.getUUID()).notes().stream()
                                    .filter(n -> n.kind() == Notebook.Kind.LAID)
                                    .count()
                            == 2,
                    "each spirit's dossier must get its last line");
            check(
                    helper,
                    data.notebook(player.getUUID()).has(Notebook.Chapter.REQUIEM),
                    "the closing chapter must open");
            check(
                    helper,
                    player.getInventory().getItem(3).is(Content.REQUIEM_BELL.get()),
                    "the reliquary must become the bell");
            int stones = 0;
            BlockPos at = player.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(
                    at.getX() - 5, level.getMinY(), at.getZ() - 5, at.getX() + 5, level.getMaxY(), at.getZ() + 5)) {
                if (level.getBlockState(pos).is(Content.GRAVESTONE.get())) {
                    stones++;
                }
            }
            check(helper, stones == 2, "each spirit must have a gravestone, found " + stones + " around " + at);
            player.discard();
            helper.succeed();
        });
        tests.put("requiem_bell_gives_three_spirits_back", helper -> {
            ServerPlayer player = fresh(helper);
            sanityChannel(player);
            ServerLevel level = helper.getLevel();
            var data = HauntData.get(level.getServer());
            ItemStack bell = stack("requiem_bell");
            player.setItemInHand(InteractionHand.MAIN_HAND, bell);
            player.setShiftKeyDown(true);
            check(
                    helper,
                    !bell.use(level, player, InteractionHand.MAIN_HAND).consumesAction(),
                    "a bell with nothing to give back must stay silent");
            data.putReleased(player.getUUID(), true);
            player.setShiftKeyDown(false);
            check(
                    helper,
                    !bell.use(level, player, InteractionHand.MAIN_HAND).consumesAction(),
                    "a bell not rung while sneaking must stay silent");
            player.setShiftKeyDown(true);
            check(
                    helper,
                    bell.use(level, player, InteractionHand.MAIN_HAND).consumesAction() && player.isUsingItem(),
                    "sneaking must start the bell's ring");
            bell.finishUsingItem(level, player);
            check(helper, !Requiem.released(player), "the bell must give the release back");
            var spirits = data.get(player.getUUID());
            check(helper, spirits.size() == 3, "three spirits must be born, found " + spirits.size());
            check(
                    helper,
                    spirits.stream().map(HauntData.Haunting::spirit).distinct().count() == 3,
                    "the three must be of different types");
            check(helper, spirits.stream().allMatch(s -> s.anger() > 0.0), "each must be born at the start anger");
            check(helper, player.getMainHandItem().is(Content.REQUIEM_BELL.get()), "the bell must stay");
            player.discard();
            helper.succeed();
        });
        begins(tests);
        endings(tests);
        movements(tests);
    }

    /**
     * What a rite is sung in: an altar ringed with salt, fourteen lit candles, a bell, and a player with a full
     * reliquary.
     */
    private record Hall(ServerPlayer player, BlockPos altar, ItemStack reliquary, List<BlockPos> candles) {}

    /** The movements in the order of the reliquary: the demon last, which passes whatever the player does. */
    private static List<String> order() {
        List<String> order = new ArrayList<>(Content.ESSENCE.keySet());
        order.remove("demon");
        order.add("demon");
        return order;
    }

    private static Hall hall(GameTestHelper helper) {
        AltarGameTests.place(helper);
        BlockPos center = AltarGameTests.CENTER;
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    helper.setBlock(center.offset(x, 0, z), Content.SALT_LINE.get());
                }
            }
        }
        List<BlockPos> candles = new ArrayList<>();
        for (int x = -3; x <= 3 && candles.size() < 14; x++) {
            for (int z = -3; z <= 3 && candles.size() < 14; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) == 3 && !(Math.abs(x) == 3 && Math.abs(z) == 3)) {
                    helper.setBlock(
                            center.offset(x, 0, z),
                            Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true));
                    candles.add(helper.absolutePos(center.offset(x, 0, z)));
                }
            }
        }
        helper.setBlock(center.offset(3, 0, 3), Blocks.BELL);
        ServerPlayer player = fresh(helper);
        sanityChannel(player);
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(center.above())));
        ItemStack reliquary = stack("reliquary");
        reliquary.set(Content.ESSENCES.get(), new Essences(order(), List.of()));
        player.getInventory().setItem(8, reliquary);
        return new Hall(player, helper.absolutePos(center), reliquary, candles);
    }

    private static Rite rite(GameTestHelper helper, Hall hall, int length) {
        return new Rite(hall.player(), helper.getLevel(), hall.altar(), length, order(), hall.candles());
    }

    private static boolean burns(GameTestHelper helper, BlockPos pos) {
        return AbstractCandleBlock.isLit(helper.getLevel().getBlockState(pos));
    }

    private static void begins(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("requiem_begin_refuses_what_is_missing", helper -> {
            Hall hall = hall(helper);
            ServerLevel level = helper.getLevel();
            ServerPlayer player = hall.player();
            var data = HauntData.get(level.getServer());
            BlockPos candle = hall.candles().get(0);
            BlockPos bell = helper.absolutePos(AltarGameTests.CENTER.offset(3, 0, 3));
            ItemStack part = stack("reliquary");
            part.set(Content.ESSENCES.get(), new Essences(List.of("shade"), List.of()));
            check(
                    helper,
                    !Requiem.begin(player, level, hall.altar(), part, 2),
                    "a part-filled reliquary must be refused");
            List<BlockPos> salt = new ArrayList<>();
            for (BlockPos pos : BlockPos.betweenClosed(
                    hall.altar().offset(-2, 0, -2), hall.altar().offset(2, 0, 2))) {
                if (level.getBlockState(pos).is(Content.SALT_LINE.get())) {
                    salt.add(pos.immutable());
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
            check(helper, !Requiem.begin(player, level, hall.altar(), hall.reliquary(), 2), "no salt ring, no Requiem");
            salt.forEach(pos -> level.setBlock(pos, Content.SALT_LINE.get().defaultBlockState(), 3));
            BlockState lit = level.getBlockState(candle);
            level.setBlock(candle, lit.setValue(CandleBlock.LIT, false), 3);
            check(
                    helper,
                    !Requiem.begin(player, level, hall.altar(), hall.reliquary(), 2),
                    "thirteen candles are not enough");
            level.setBlock(candle, lit, 3);
            level.setBlock(bell, Blocks.AIR.defaultBlockState(), 3);
            check(helper, !Requiem.begin(player, level, hall.altar(), hall.reliquary(), 2), "no bell, no Requiem");
            level.setBlock(bell, Blocks.BELL.defaultBlockState(), 3);
            data.putRequiemWary(player.getUUID(), level.getServer().overworld().getGameTime() + 1000);
            check(helper, !Requiem.begin(player, level, hall.altar(), hall.reliquary(), 2), "a wary altar must refuse");
            data.putRequiemWary(player.getUUID(), 0);
            data.putReleased(player.getUUID(), true);
            check(
                    helper,
                    !Requiem.begin(player, level, hall.altar(), hall.reliquary(), 2),
                    "a released player needs no Requiem");
            data.putReleased(player.getUUID(), false);
            check(
                    helper,
                    Requiem.rite(player) == null && !Requiem.running(hall.altar()),
                    "a refusal must start nothing");
            check(
                    helper,
                    Requiem.begin(player, level, hall.altar(), hall.reliquary(), 2),
                    "a full, ringed, lit, belled altar must start the rite");
            check(helper, Requiem.running(hall.altar()), "the altar must be busy");
            check(
                    helper,
                    !Requiem.begin(player, level, hall.altar(), hall.reliquary(), 2),
                    "a rite already under way must not start again");
            check(helper, Reliquary.of(hall.reliquary()).full(), "the reliquary is not given up");
            Requiem.abort(player);
            player.discard();
            helper.succeed();
        });
        tests.put("requiem_altar_takes_a_full_reliquary_and_keeps_it_with_the_player", helper -> {
            Hall hall = hall(helper);
            ServerLevel level = helper.getLevel();
            ServerPlayer player = hall.player();
            player.setItemInHand(InteractionHand.MAIN_HAND, hall.reliquary());
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(hall.altar()), Direction.UP, hall.altar(), false);
            ItemStack part = stack("reliquary");
            player.setItemInHand(InteractionHand.OFF_HAND, part);
            check(
                    helper,
                    !level.getBlockState(hall.altar())
                            .useItemOn(part, level, player, InteractionHand.OFF_HAND, hit)
                            .consumesAction(),
                    "an empty reliquary must not start the rite");
            check(
                    helper,
                    level.getBlockState(hall.altar())
                            .useItemOn(hall.reliquary(), level, player, InteractionHand.MAIN_HAND, hit)
                            .consumesAction(),
                    "the altar must take a full reliquary");
            check(helper, Requiem.rite(player) != null, "the rite must begin");
            check(
                    helper,
                    hall.reliquary().getCount() == 1
                            && Reliquary.of(hall.reliquary()).full(),
                    "the player keeps the reliquary");
            Requiem.abort(player);
            player.discard();
            helper.succeed();
        });
        HauntEventGameTests.TIMEOUT.put("requiem_runs_all_movements_and_cleans_up", 400);
        tests.put("requiem_runs_all_movements_and_cleans_up", helper -> {
            Hall hall = hall(helper);
            ServerLevel level = helper.getLevel();
            Director.add(
                    hall.player(),
                    HauntData.get(level.getServer()),
                    Spirits.byId("shade").orElseThrow(),
                    0L);
            check(
                    helper,
                    Requiem.begin(hall.player(), level, hall.altar(), hall.reliquary(), 3),
                    "the rite must begin");
            boolean[] seen = {false};
            helper.succeedWhen(() -> {
                var rite = Requiem.rite(hall.player());
                seen[0] |= rite != null && rite.index > 0;
                check(helper, Requiem.rite(hall.player()) == null, "the rite is still singing");
                check(helper, seen[0], "the movements must have run");
                var area = new net.minecraft.world.phys.AABB(hall.altar()).inflate(14);
                check(helper, level.getEntitiesOfClass(Mannequin.class, area).isEmpty(), "no stranger may be left");
                check(
                        helper,
                        level.getEntitiesOfClass(Mob.class, area, Mob::hasGlowingTag)
                                .isEmpty(),
                        "no risen mob may be left");
                for (BlockPos pos : BlockPos.betweenClosed(
                        hall.altar().offset(-9, -2, -9), hall.altar().offset(9, 4, 9))) {
                    BlockState state = level.getBlockState(pos);
                    check(
                            helper,
                            !state.is(Blocks.CHEST)
                                    && !state.is(Blocks.WATER)
                                    && !(state.getBlock() instanceof Altar
                                            && !pos.equals(hall.altar())
                                            && state.getValue(Altar.PART) == Altar.Part.CENTER
                                            && Math.max(
                                                            Math.abs(pos.getX()
                                                                    - hall.altar()
                                                                            .getX()),
                                                            Math.abs(pos.getZ()
                                                                    - hall.altar()
                                                                            .getZ()))
                                                    > 1),
                            "a movement left a block behind at " + pos);
                }
                check(
                        helper,
                        Requiem.released(hall.player())
                                || HauntData.get(level.getServer())
                                                .requiemWary(hall.player().getUUID())
                                        > 0,
                        "the rite must end in release or in failure");
                hall.player().discard();
            });
        });
    }

    private static void endings(Map<String, Consumer<GameTestHelper>> tests) {
        for (boolean good : new boolean[] {false, true}) {
            String name = good ? "requiem_success_releases" : "requiem_failure_costs_an_essence_and_anger_and_time";
            HauntEventGameTests.TIMEOUT.put(name, 200);
            tests.put(name, helper -> {
                Hall hall = hall(helper);
                ServerLevel level = helper.getLevel();
                ServerPlayer player = hall.player();
                var data = HauntData.get(level.getServer());
                Director.add(player, data, Spirits.byId("shade").orElseThrow(), 0L);
                long before = level.getServer().overworld().getGameTime();
                check(helper, Requiem.begin(player, level, hall.altar(), hall.reliquary(), 2), "the rite must begin");
                var rite = Requiem.rite(player);
                rite.index = rite.order.size() - 1;
                if (!good) {
                    rite.candles.snuff(8, n -> 0).forEach(rite::put);
                }
                helper.succeedWhen(() -> {
                    check(helper, Requiem.rite(player) == null, "the rite is still singing");
                    if (good) {
                        check(helper, Requiem.released(player), "seven or more candles must release the player");
                        check(
                                helper,
                                player.getInventory().getItem(8).is(Content.REQUIEM_BELL.get()),
                                "the reliquary must become the bell");
                        check(helper, data.requiemWary(player.getUUID()) == 0, "success locks nothing");
                    } else {
                        check(helper, !Requiem.released(player), "six candles must not release");
                        var held =
                                Reliquary.of(player.getInventory().getItem(8)).held();
                        check(
                                helper,
                                held.size() == Content.ESSENCE.size() - 1 && order().containsAll(held),
                                "exactly one essence must be lost, found " + held);
                        check(
                                helper,
                                data.get(player.getUUID()).stream().allMatch(s -> s.anger() >= 0.3),
                                "each spirit must gain 0.3 anger");
                        check(
                                helper,
                                data.requiemWary(player.getUUID()) >= before + 72000,
                                "the altar must turn wary for three days");
                    }
                    player.discard();
                });
            });
        }
        for (String how : List.of("leaves", "dies", "server_stops")) {
            tests.put("requiem_cut_short_loses_nothing_when_player_" + how, helper -> {
                Hall hall = hall(helper);
                ServerLevel level = helper.getLevel();
                ServerPlayer player = hall.player();
                var data = HauntData.get(level.getServer());
                Director.add(player, data, Spirits.byId("shade").orElseThrow(), 0L);
                check(helper, Requiem.begin(player, level, hall.altar(), hall.reliquary(), 50), "must begin");
                var rite = Requiem.rite(player);
                rite.current = Movements.of("poltergeist");
                rite.current.start(rite);
                switch (how) {
                    case "leaves" -> player.discard();
                    case "dies" -> player.setHealth(0.0f);
                    default -> Requiem.abort(player);
                }
                helper.succeedWhen(() -> {
                    check(helper, Requiem.rite(player) == null && rite.current == null, "the rite must end");
                    check(
                            helper,
                            Reliquary.of(hall.reliquary()).held().size() == Content.ESSENCE.size(),
                            "no essence may be lost");
                    check(helper, data.requiemWary(player.getUUID()) == 0, "no cooldown");
                    check(helper, data.get(player.getUUID()).stream().allMatch(s -> s.anger() == 0.0), "no anger");
                    player.discard();
                });
            });
        }
        tests.put("requiem_holy_water_and_bell_and_bargain_reach_the_rite", helper -> {
            Hall hall = hall(helper);
            ServerLevel level = helper.getLevel();
            ServerPlayer player = hall.player();
            check(helper, Requiem.begin(player, level, hall.altar(), hall.reliquary(), 50), "must begin");
            var rite = Requiem.rite(player);
            ItemStack water = new ItemStack(Content.HOLY_WATER.get(), 2);
            check(
                    helper,
                    !Requiem.used(player, hall.altar(), water).consumesAction() && water.getCount() == 2,
                    "holy water means nothing outside the mimic's movement");
            rite.type = "mimic";
            BlockPos fake = hall.altar().offset(5, 0, 0);
            rite.decoys.add(fake);
            check(
                    helper,
                    Requiem.used(player, fake, water).consumesAction()
                            && fake.equals(rite.blessed)
                            && water.getCount() == 1,
                    "holy water on a decoy is spent and noted");
            check(
                    helper,
                    Requiem.used(player, hall.altar(), water).consumesAction()
                            && hall.altar().equals(rite.blessed),
                    "holy water on the altar is noted");
            rite.decoys.clear();
            rite.tick = 7;
            Requiem.rung(level, hall.altar().offset(3, 0, 3));
            Requiem.rung(level, hall.altar().offset(30, 0, 3));
            check(helper, rite.rings.equals(List.of(7)), "only a bell near the altar counts, found " + rite.rings);
            rite.type = "demon";
            Requiem.answer(player, true);
            check(
                    helper,
                    rite.candles.lit().size() == 12 && Boolean.TRUE.equals(rite.bargain),
                    "taking the bargain costs two candles");
            Requiem.abort(player);
            player.discard();
            helper.succeed();
        });
    }

    private static void movements(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("requiem_shade_needs_four_lights_left", helper -> {
            Hall hall = hall(helper);
            ServerLevel level = helper.getLevel();
            Rite rite = rite(helper, hall, 12);
            Movement shade = Movements.of("shade");
            for (int z = 1; z <= 4; z++) {
                helper.setBlock(new BlockPos(0, 1, z), Content.HALLOWED_LANTERN.get());
            }
            for (int z = 5; z <= 9; z++) {
                helper.setBlock(new BlockPos(0, 1, z), Blocks.TORCH);
            }
            shade.start(rite);
            for (int n = 0; n < 12; n++) {
                shade.tick(rite, n);
            }
            check(helper, shade.passed(rite), "four hallowed lanterns must outlast the dark");
            check(helper, burns(helper, hall.candles().get(0)), "the ring's own candles are not the shade's to snuff");
            level.setBlock(helper.absolutePos(new BlockPos(0, 1, 4)), Blocks.AIR.defaultBlockState(), 3);
            check(helper, !shade.passed(rite), "three lights are too few");
            hall.player().discard();
            helper.succeed();
        });
        tests.put("requiem_poltergeist_wants_everything_picked_up", helper -> {
            Hall hall = hall(helper);
            ServerPlayer player = hall.player();
            Rite rite = rite(helper, hall, 10);
            List<net.minecraft.world.item.Item> junk =
                    List.of(Items.COBBLESTONE, Items.DIRT, Items.SAND, Items.GRAVEL, Items.CLAY_BALL);
            for (int slot = 0; slot < 5; slot++) {
                player.getInventory().setItem(slot, new ItemStack(junk.get(slot), 3));
            }
            for (boolean pickUp : new boolean[] {false, true}) {
                Movement poltergeist = Movements.of("poltergeist");
                poltergeist.start(rite);
                var area = new net.minecraft.world.phys.AABB(hall.altar()).inflate(8);
                var flying = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area);
                check(helper, flying.size() == 5, "five stacks must fly, found " + flying.size());
                check(helper, player.getInventory().getItem(0).isEmpty(), "the pockets must be emptied");
                check(helper, player.getInventory().getItem(8).is(Content.RELIQUARY.get()), "the reliquary stays");
                check(helper, !poltergeist.passed(rite), "items on the floor must not pass");
                if (pickUp) {
                    for (ItemEntity item : flying) {
                        player.getInventory().add(item.getItem());
                        item.discard();
                    }
                    check(helper, poltergeist.passed(rite), "everything picked up must pass");
                }
                poltergeist.end(rite);
                int total = 0;
                for (int slot = 0; slot < 36; slot++) {
                    if (junk.contains(player.getInventory().getItem(slot).getItem())) {
                        total += player.getInventory().getItem(slot).getCount();
                    }
                }
                check(helper, total == 15, "nothing may be lost: " + total + " of 15 (picked up: " + pickUp + ")");
                check(
                        helper,
                        helper.getLevel()
                                .getEntitiesOfClass(ItemEntity.class, area)
                                .isEmpty(),
                        "no item may stay on the floor");
            }
            player.discard();
            helper.succeed();
        });
        tests.put("requiem_nix_wants_the_player_to_breathe_and_the_altar_dry", helper -> {
            Hall hall = hall(helper);
            ServerLevel level = helper.getLevel();
            ServerPlayer player = hall.player();
            Rite rite = rite(helper, hall, 8);
            Movement nix = Movements.of("nix");
            nix.start(rite);
            BlockPos pool = hall.altar().offset(3, 0, 0);
            check(helper, level.getFluidState(pool).is(FluidTags.WATER), "water must rise in the circle");
            nix.tick(rite, 2);
            check(helper, level.getFluidState(pool.above()).is(FluidTags.WATER), "it must rise two blocks");
            check(helper, nix.passed(rite), "a player on dry ground breathes");
            player.setPos(Vec3.atBottomCenterOf(pool));
            player.setAirSupply(0);
            nix.tick(rite, 5);
            check(helper, !nix.passed(rite), "out of breath in the water is drowning");
            nix.end(rite);
            check(
                    helper,
                    level.getFluidState(pool).isEmpty()
                            && level.getFluidState(pool.above()).isEmpty(),
                    "the water must go");
            Movement dry = Movements.of("nix");
            dry.start(rite);
            level.setBlock(hall.altar().above(), Blocks.WATER.defaultBlockState(), 3);
            check(helper, !dry.passed(rite), "water over the altar fails the movement");
            level.setBlock(hall.altar().above(), Blocks.AIR.defaultBlockState(), 3);
            dry.end(rite);
            player.discard();
            helper.succeed();
        });
        tests.put("requiem_phantom_must_not_be_looked_at", helper -> {
            Hall hall = hall(helper);
            ServerPlayer player = hall.player();
            Rite rite = rite(helper, hall, 8);
            var area = new net.minecraft.world.phys.AABB(hall.altar()).inflate(9);
            Movement look = Movements.of("phantom");
            look.start(rite);
            var strangers = helper.getLevel().getEntitiesOfClass(Mannequin.class, area);
            check(helper, strangers.size() == 6, "six strangers must stand about, found " + strangers.size());
            // They stand outside the test's walls: bring one close, and look at it.
            strangers.get(0).setPos(player.position().add(3, 0, 0));
            player.lookAt(
                    EntityAnchorArgument.Anchor.EYES,
                    strangers.get(0).position().add(0, 1.0, 0));
            look.tick(rite, 0);
            check(helper, !look.passed(rite), "looking at a stranger must fail");
            look.end(rite);
            check(
                    helper,
                    helper.getLevel().getEntitiesOfClass(Mannequin.class, area).isEmpty(),
                    "the strangers must go");
            Movement away = Movements.of("phantom");
            away.start(rite);
            player.setXRot(-90.0f);
            for (int n = 0; n < 8; n++) {
                away.tick(rite, n);
            }
            check(helper, away.passed(rite), "eyes on the sky must pass");
            away.end(rite);
            player.discard();
            helper.succeed();
        });
        tests.put("requiem_mimic_wants_holy_water_on_the_real_altar", helper -> {
            Hall hall = hall(helper);
            ServerLevel level = helper.getLevel();
            Rite rite = rite(helper, hall, 8);
            Movement mimic = Movements.of("mimic");
            mimic.start(rite);
            check(helper, !rite.decoys.isEmpty(), "false altars must appear");
            check(
                    helper,
                    rite.decoys.stream()
                            .allMatch(pos -> level.getBlockState(pos).getBlock() instanceof Altar),
                    "they must look like altars");
            check(helper, !mimic.passed(rite), "nothing laid yet");
            rite.blessed = rite.decoys.iterator().next();
            check(helper, !mimic.passed(rite), "holy water on a false altar must fail");
            rite.blessed = hall.altar();
            check(helper, mimic.passed(rite), "holy water on the real altar must pass");
            var decoys = List.copyOf(rite.decoys);
            mimic.end(rite);
            check(
                    helper,
                    decoys.stream().allMatch(pos -> level.getBlockState(pos).isAir()) && rite.decoys.isEmpty(),
                    "the false altars must go");
            hall.player().discard();
            helper.succeed();
        });
        tests.put("requiem_wraith_wants_the_player_above_three_hearts", helper -> {
            Hall hall = hall(helper);
            ServerPlayer player = hall.player();
            Rite rite = rite(helper, hall, 10);
            Movement wraith = Movements.of("wraith");
            wraith.start(rite);
            check(helper, player.hasEffect(MobEffects.DARKNESS), "darkness must fall");
            player.setHealth(20.0f);
            check(helper, wraith.passed(rite), "ten hearts must pass");
            player.setHealth(6.0f);
            check(helper, !wraith.passed(rite), "three hearts or fewer must fail");
            for (int n = 0; n < 20; n++) {
                wraith.tick(rite, n);
            }
            check(helper, player.getHealth() > 0, "the wraith never kills");
            wraith.end(rite);
            check(helper, !player.hasEffect(MobEffects.DARKNESS), "the dark must lift");
            player.discard();
            helper.succeed();
        });
        tests.put("requiem_mara_wants_a_steady_mind", helper -> {
            Hall hall = hall(helper);
            ServerPlayer player = hall.player();
            Rite rite = rite(helper, hall, 10);
            Sanity.set(player, 0.8);
            Movement steady = Movements.of("mara");
            steady.start(rite);
            check(helper, steady.passed(rite) && !player.hasEffect(MobEffects.SLOWNESS), "sanity above 0.3 must wake");
            steady.end(rite);
            Sanity.set(player, 0.1);
            Movement shaken = Movements.of("mara");
            shaken.start(rite);
            check(
                    helper,
                    !shaken.passed(rite) && player.hasEffect(MobEffects.SLOWNESS),
                    "sanity below 0.3 must sleep standing");
            shaken.end(rite);
            check(
                    helper,
                    !player.hasEffect(MobEffects.SLOWNESS) && !player.hasEffect(MobEffects.BLINDNESS),
                    "the sleep must lift");
            player.discard();
            helper.succeed();
        });
        tests.put("requiem_banshee_wants_the_bell_in_the_silence", helper -> {
            Hall hall = hall(helper);
            Rite rite = rite(helper, hall, 100);
            Movement banshee = Movements.of("banshee");
            check(helper, !banshee.passed(rite), "no bell, no pass");
            rite.rings.add(10);
            check(helper, !banshee.passed(rite), "a bell during the wail must fail");
            rite.rings.add(85);
            check(helper, !banshee.passed(rite), "a bell after the silence must fail");
            rite.rings.add(50);
            check(helper, banshee.passed(rite), "a bell in the silence must pass");
            hall.player().discard();
            helper.succeed();
        });
        tests.put("requiem_revenant_wants_them_all_dead", helper -> {
            Hall hall = hall(helper);
            ServerLevel level = helper.getLevel();
            Rite rite = rite(helper, hall, 10);
            Movement revenant = Movements.of("revenant");
            revenant.start(rite);
            var area = new net.minecraft.world.phys.AABB(hall.altar()).inflate(10);
            var risen = level.getEntitiesOfClass(Mob.class, area, Mob::hasGlowingTag);
            check(helper, risen.size() == 4, "four must rise, found " + risen.size());
            check(helper, !revenant.passed(rite), "the risen are alive");
            risen.forEach(mob -> mob.kill(level));
            check(helper, revenant.passed(rite), "all dead must pass");
            revenant.end(rite);
            hall.player().discard();
            helper.succeed();
        });
        tests.put("requiem_lost_one_wants_the_way_back", helper -> {
            Hall hall = hall(helper);
            ServerPlayer player = hall.player();
            Rite rite = rite(helper, hall, 10);
            Movement lost = Movements.of("lost_one");
            lost.start(rite);
            double far = player.position().distanceTo(Vec3.atCenterOf(hall.altar()));
            check(helper, far > 25, "the player must be far from the altar, was " + far);
            lost.tick(rite, 1);
            check(helper, !lost.passed(rite), "far away is lost");
            player.setPos(Vec3.atBottomCenterOf(hall.altar().above()));
            lost.tick(rite, 3);
            check(helper, lost.passed(rite), "back in the ring in time must pass");
            Movement late = Movements.of("lost_one");
            late.start(rite);
            late.tick(rite, 9);
            check(helper, !late.passed(rite), "not back at all must fail");
            late.end(rite);
            check(helper, rite.near(5), "a player left out is brought to the altar");
            player.discard();
            helper.succeed();
        });
        tests.put("requiem_domovoi_wants_every_door_shut", helper -> {
            Hall hall = hall(helper);
            ServerLevel level = helper.getLevel();
            Rite rite = rite(helper, hall, 8);
            BlockPos[] doors = {hall.altar().offset(-4, 0, 3), hall.altar().offset(-4, 0, -3)};
            for (BlockPos pos : doors) {
                level.setBlock(pos, Blocks.OAK_DOOR.defaultBlockState(), 3);
                level.setBlock(
                        pos.above(),
                        Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER),
                        3);
            }
            Movement domovoi = Movements.of("domovoi");
            domovoi.start(rite);
            check(
                    helper,
                    level.getBlockState(doors[0]).getValue(DoorBlock.OPEN)
                            && level.getBlockState(doors[1]).getValue(DoorBlock.OPEN),
                    "the doors must slam open");
            check(helper, !domovoi.passed(rite), "open doors must fail");
            BlockState first = level.getBlockState(doors[0]);
            ((DoorBlock) first.getBlock()).setOpen(null, level, first, doors[0], false);
            check(helper, !domovoi.passed(rite), "one open door must fail");
            BlockState second = level.getBlockState(doors[1]);
            ((DoorBlock) second.getBlock()).setOpen(null, level, second, doors[1], false);
            check(helper, domovoi.passed(rite), "every door shut must pass");
            domovoi.end(rite);
            hall.player().discard();
            helper.succeed();
        });
        tests.put("requiem_hoarder_wants_the_offering_found", helper -> {
            Hall hall = hall(helper);
            ServerLevel level = helper.getLevel();
            ServerPlayer player = hall.player();
            Rite rite = rite(helper, hall, 8);
            Movement hoarder = Movements.of("hoarder");
            hoarder.start(rite);
            List<ChestBlockEntity> chests = new ArrayList<>();
            for (BlockPos pos : BlockPos.betweenClosed(
                    hall.altar().offset(-8, -1, -8), hall.altar().offset(8, 1, 8))) {
                if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                    chests.add(chest);
                }
            }
            check(helper, !chests.isEmpty() && chests.size() <= 3, "up to three chests, found " + chests.size());
            check(
                    helper,
                    chests.stream().allMatch(c -> c.getLootTable() != null),
                    "each chest must look unopened to a magpie feather");
            check(helper, !hoarder.passed(rite), "the offering is still hidden");
            ItemStack found = ItemStack.EMPTY;
            for (ChestBlockEntity chest : chests) {
                for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                    ItemStack in = chest.getItem(slot);
                    if (in.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
                        found = in.copy();
                    }
                }
            }
            check(helper, !found.isEmpty(), "one chest must hold the offering");
            player.getInventory().setItem(0, found);
            check(helper, hoarder.passed(rite), "the offering in the pockets must pass");
            hoarder.end(rite);
            check(helper, player.getInventory().getItem(0).isEmpty(), "the offering is not kept");
            check(
                    helper,
                    chests.stream()
                            .allMatch(c -> level.getBlockState(c.getBlockPos()).isAir()),
                    "the chests must go");
            player.discard();
            helper.succeed();
        });
        tests.put("requiem_demon_passes_unless_the_bargain_is_taken", helper -> {
            Hall hall = hall(helper);
            Rite rite = rite(helper, hall, 8);
            Movement demon = Movements.of("demon");
            demon.start(rite);
            check(helper, demon.passed(rite), "no answer is a refusal");
            demon.tick(rite, 0);
            check(helper, !rite.skip, "nothing skips until the bargain is taken");
            rite.bargain = true;
            demon.tick(rite, 1);
            check(helper, rite.skip, "a taken bargain skips the movement");
            hall.player().discard();
            helper.succeed();
        });
        tests.put("requiem_trickster_wants_the_right_candles_relit", helper -> {
            Hall hall = hall(helper);
            ServerLevel level = helper.getLevel();
            Rite rite = rite(helper, hall, 8);
            Movement trickster = Movements.of("trickster");
            trickster.start(rite);
            check(
                    helper,
                    rite.free.size() == 3 && rite.free.stream().noneMatch(pos -> burns(helper, pos)),
                    "three candles must be snuffed");
            check(
                    helper,
                    hall.candles().stream()
                            .anyMatch(pos -> !level.getBlockState(pos).is(Blocks.CANDLE)),
                    "the candles must change colors");
            List<BlockPos> decoys = new ArrayList<>();
            for (BlockPos pos : BlockPos.betweenClosed(
                    hall.altar().offset(-4, 0, -4), hall.altar().offset(4, 0, 4))) {
                if (level.getBlockState(pos).getBlock() instanceof CandleBlock
                        && !hall.candles().contains(pos.immutable())) {
                    decoys.add(pos.immutable());
                }
            }
            check(
                    helper,
                    !decoys.isEmpty() && decoys.stream().noneMatch(pos -> burns(helper, pos)),
                    "false candles must stand, unlit");
            check(helper, !trickster.passed(rite), "nothing relit yet");
            List<BlockPos> snuffed = List.copyOf(rite.free);
            for (BlockPos pos : snuffed) {
                BlockState state = level.getBlockState(pos);
                level.setBlock(pos, state.setValue(CandleBlock.LIT, true), 3);
            }
            check(helper, trickster.passed(rite), "the three right candles relit must pass");
            BlockState bait = level.getBlockState(decoys.get(0));
            level.setBlock(decoys.get(0), bait.setValue(CandleBlock.LIT, true), 3);
            check(helper, !trickster.passed(rite), "a false candle lit must fail");
            level.setBlock(snuffed.get(0), level.getBlockState(snuffed.get(0)).setValue(CandleBlock.LIT, false), 3);
            trickster.end(rite);
            check(
                    helper,
                    decoys.stream().allMatch(pos -> level.getBlockState(pos).isAir()),
                    "the false candles must go");
            check(
                    helper,
                    hall.candles().stream()
                            .allMatch(pos -> level.getBlockState(pos).is(Blocks.CANDLE)),
                    "the colors must come back");
            check(
                    helper,
                    hall.candles().stream().allMatch(pos -> burns(helper, pos)),
                    "the rite relights what the player left out");
            check(helper, rite.free.isEmpty(), "the movement lets go of the candles");
            hall.player().discard();
            helper.succeed();
        });
    }

    /** A photograph that names a type, in the player's journal. */
    private static void name(ServerPlayer player, String type) {
        Journal.tagged(player, Notebook.Kind.PHOTO, null, type, type);
    }

    /** A connected player, in the level, with no spirits. Tests discard it when done. */
    private static ServerPlayer fresh(GameTestHelper helper) {
        ServerPlayer player = HauntEventGameTests.connected(helper);
        helper.getLevel().addNewPlayer(player);
        HauntData.get(player.level().getServer()).put(player.getUUID(), List.of());
        return player;
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }
}
