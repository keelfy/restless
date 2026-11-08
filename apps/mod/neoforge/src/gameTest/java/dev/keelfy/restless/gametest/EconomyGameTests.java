package dev.keelfy.restless.gametest;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.Effigy;
import dev.keelfy.restless.haunt.Deeds;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.HauntData;
import dev.keelfy.restless.haunt.Ritual;
import dev.keelfy.restless.spirit.Spirits;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The essence economy: where a spirit is met (deeds, evolved and blighted rewards) and the effigy that calls one. */
final class EconomyGameTests {
    /** Deeds roll a chance below one, so a test repeats the deed until the player has the spirit. */
    private static final int TRIES = 120;

    private EconomyGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("economy_rewards", helper -> {
            check(helper, Ritual.essences(false, 0, 64) == 1, "a plain banishing leaves one essence");
            check(helper, Ritual.essences(true, 0, 64) == 2, "an evolved spirit leaves one more");
            check(helper, Ritual.essences(false, 63, 64) == 1, "a small blight earns nothing");
            check(helper, Ritual.essences(false, 64, 64) == 2, "a blight of 64 spots leaves one more");
            check(helper, Ritual.essences(true, 200, 64) == 3, "both rewards add up");
            helper.succeed();
        });
        tests.put("economy_witness_death_births_banshee", helper -> {
            ServerPlayer player = fresh(helper);
            Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, 2, 1, 2);
            player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(3, 1, 2))));
            boolean born = repeat(player, "banshee", () -> {
                Deeds.killed(null, villager);
                return false;
            });
            check(helper, born, "a villager dying before a player must bring a banshee");
            discard(helper, player);
            helper.succeed();
        });
        tests.put("economy_own_kill_is_not_witnessed", helper -> {
            ServerPlayer player = fresh(helper);
            Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, 2, 1, 2);
            player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(3, 1, 2))));
            for (int i = 0; i < TRIES; i++) {
                Deeds.killed(player, villager);
            }
            check(helper, spirits(player).stream().noneMatch(s -> s.equals("banshee")), "the killer is no witness");
            discard(helper, player);
            helper.succeed();
        });
        tests.put("economy_pets_and_slaughter", helper -> {
            ServerPlayer player = fresh(helper);
            Wolf wolf = helper.spawnWithNoFreeWill(EntityTypes.WOLF, 2, 1, 2);
            wolf.tame(player);
            player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(60, 1, 60))));
            check(
                    helper,
                    repeat(player, "banshee", () -> {
                        Deeds.killed(null, wolf);
                        return false;
                    }),
                    "a pet lost away from its owner must bring a banshee");
            fresh(player);
            check(
                    helper,
                    repeat(player, "revenant", () -> {
                        Deeds.killed(player, wolf);
                        return false;
                    }),
                    "an owner who kills their pet must get a revenant");
            fresh(player);
            Cow cow = helper.spawnWithNoFreeWill(EntityTypes.COW, 4, 1, 4);
            check(
                    helper,
                    repeat(player, "revenant", () -> {
                        for (int i = 0;
                                i
                                        < Config.section("births")
                                                .section("slaughter")
                                                .integer("points");
                                i++) {
                            Deeds.killed(player, cow);
                        }
                        return false;
                    }),
                    "a run of animals killed must bring a revenant");
            discard(helper, player);
            helper.succeed();
        });
        tests.put("economy_far_death_and_long_wake", helper -> {
            ServerPlayer player = fresh(helper);
            player.setPos(player.getX(), -20, player.getZ());
            check(
                    helper,
                    repeat(player, "lost_one", () -> {
                        Deeds.killed(null, player);
                        return false;
                    }),
                    "dying deep below the world must bring a lost one");
            fresh(player);
            var rest = Stats.CUSTOM.get(Stats.TIME_SINCE_REST);
            check(
                    helper,
                    repeat(player, "lost_one", () -> {
                        player.getStats().setValue(player, rest, 0);
                        Deeds.wake(player);
                        player.getStats().setValue(player, rest, 48000);
                        Deeds.wake(player);
                        return false;
                    }),
                    "two days without rest must bring a lost one");
            player.getStats().setValue(player, rest, 0);
            discard(helper, player);
            helper.succeed();
        });
        tests.put("economy_effigy_calls_personal", helper -> {
            ServerPlayer player = fresh(helper);
            player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 2))));
            BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
            helper.getLevel().setBlock(at, Content.EFFIGY.get().defaultBlockState(), 3);
            use(helper, player, at, new ItemStack(Items.FLINT_AND_STEEL));
            check(helper, !lit(helper, at), "an empty effigy must not catch fire");
            use(helper, player, at, new ItemStack(Content.ESSENCE.get("banshee").get()));
            check(
                    helper,
                    helper.getLevel().getBlockState(at).getValue(Effigy.STUFFING) == Effigy.Stuffing.BANSHEE,
                    "an essence must be stuffed into the effigy");
            use(helper, player, at, new ItemStack(Items.FLINT_AND_STEEL));
            check(helper, lit(helper, at), "a stuffed effigy must catch fire");
            helper.getLevel()
                    .getBlockState(at)
                    .tick(helper.getLevel(), at, helper.getLevel().getRandom());
            check(helper, helper.getLevel().getBlockState(at).isAir(), "the effigy must burn away");
            check(helper, spirits(player).equals(List.of("banshee")), "the burnt effigy must call its spirit");
            discard(helper, player);
            helper.succeed();
        });
        tests.put("economy_effigy_respects_the_cap", helper -> {
            ServerPlayer player = fresh(helper);
            var server = helper.getLevel().getServer();
            long now = server.overworld().getGameTime();
            var data = HauntData.get(server);
            for (String own : List.of("shade", "phantom", "mimic")) {
                Director.add(player, data, Spirits.byId(own).orElseThrow(), now - 10 * 24000L);
            }
            BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
            helper.getLevel()
                    .setBlock(
                            at,
                            Content.EFFIGY.get().defaultBlockState().setValue(Effigy.STUFFING, Effigy.Stuffing.WRAITH),
                            3);
            use(helper, player, at, new ItemStack(Items.FLINT_AND_STEEL));
            check(helper, !lit(helper, at), "a call that cannot be answered must not light the effigy");
            check(helper, spirits(player).size() == 3, "nothing must be born at the cap");
            discard(helper, player);
            helper.succeed();
        });
        tests.put("economy_effigy_anchors_territorial", helper -> {
            ServerPlayer player = fresh(helper);
            var level = helper.getLevel();
            var data = HauntData.get(level.getServer());
            data.putTerritorial(List.of());
            BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
            level.setBlock(
                    at,
                    Content.EFFIGY
                            .get()
                            .defaultBlockState()
                            .setValue(Effigy.STUFFING, Effigy.Stuffing.NIX)
                            .setValue(Effigy.LIT, true),
                    3);
            level.getBlockState(at).tick(level, at, level.getRandom());
            var here = Director.territorial(data, level);
            check(
                    helper,
                    here.size() == 1
                            && here.get(0).anchor().equals(at)
                            && here.get(0).haunting().spirit().equals("nix"),
                    "a territorial effigy must anchor its spirit where it stood");
            level.setBlock(
                    at, Content.EFFIGY.get().defaultBlockState().setValue(Effigy.STUFFING, Effigy.Stuffing.DEMON), 3);
            use(helper, player, at, new ItemStack(Items.FLINT_AND_STEEL));
            check(helper, !lit(helper, at), "a spirit too near another one must not be called");
            data.putTerritorial(List.of());
            discard(helper, player);
            helper.succeed();
        });
        tests.put("economy_effigy_covers_every_essence", helper -> {
            List<String> stuffings = new ArrayList<>();
            for (Effigy.Stuffing s : Effigy.Stuffing.values()) {
                if (s != Effigy.Stuffing.EMPTY) {
                    stuffings.add(s.spirit());
                }
            }
            check(
                    helper,
                    stuffings.equals(new ArrayList<>(Content.ESSENCE.keySet())),
                    "the effigy must take exactly the essences that exist");
            helper.succeed();
        });
    }

    /** A connected player, in the level, with no spirits. Tests discard it when done. */
    private static ServerPlayer fresh(GameTestHelper helper) {
        ServerPlayer player = HauntEventGameTests.connected(helper);
        helper.getLevel().addNewPlayer(player); // deeds and effigies look for players in the level
        fresh(player);
        return player;
    }

    private static void discard(GameTestHelper helper, ServerPlayer player) {
        player.discard();
    }

    private static void fresh(ServerPlayer player) {
        HauntData.get(player.level().getServer()).put(player.getUUID(), List.of());
    }

    private static List<String> spirits(ServerPlayer player) {
        return HauntData.get(player.level().getServer()).get(player.getUUID()).stream()
                .map(HauntData.Haunting::spirit)
                .toList();
    }

    /** Runs {@code deed} until the player has a spirit of the type, or the tries run out. */
    private static boolean repeat(ServerPlayer player, String spirit, BooleanSupplier deed) {
        for (int i = 0; i < TRIES; i++) {
            deed.getAsBoolean();
            if (spirits(player).contains(spirit)) {
                return true;
            }
        }
        return false;
    }

    private static boolean lit(GameTestHelper helper, BlockPos at) {
        BlockState state = helper.getLevel().getBlockState(at);
        return state.getBlock() instanceof Effigy && state.getValue(Effigy.LIT);
    }

    private static void use(GameTestHelper helper, ServerPlayer player, BlockPos at, ItemStack stack) {
        helper.getLevel()
                .getBlockState(at)
                .useItemOn(
                        stack,
                        helper.getLevel(),
                        player,
                        InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(at), net.minecraft.core.Direction.UP, at, false));
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }
}
