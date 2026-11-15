package dev.keelfy.restless.gametest;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.banish.Circle;
import dev.keelfy.restless.banish.Natures;
import dev.keelfy.restless.banish.Phase;
import dev.keelfy.restless.banish.Ritual;
import dev.keelfy.restless.banish.Second;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.block.Altar;
import dev.keelfy.restless.content.block.UnlitLights;
import dev.keelfy.restless.sanity.SanityPayload;
import dev.keelfy.restless.spirit.land.Graves;
import dev.keelfy.restless.spirit.life.Echoes;
import dev.keelfy.restless.state.HauntData;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

/** The altar against a spirit of known nature: right offering, right category in another form, and the wrong one. */
final class RitualGameTests {
    /** Iron by nature: two iron traits and one glowstone. */
    private static final List<String> IRON_TRAITS = List.of("door_slammer", "meddler", "light_eater");

    private static final UUID SPIRIT = UUID.fromString("00000000-0000-0000-0000-000000000042");
    private static final List<BlockPos> CANDLES = List.of(
            AltarGameTests.CENTER.offset(-3, 1, -3),
            AltarGameTests.CENTER.offset(3, 1, -3),
            AltarGameTests.CENTER.offset(-3, 1, 3),
            AltarGameTests.CENTER.offset(3, 1, 3));

    private RitualGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("ritual_off_form_runs_longer", helper -> {
            ServerPlayer player = ready(helper);
            HauntData.Haunting spirit = spirit(player);
            // a poltergeist takes iron whole
            check(helper, Ritual.waves(HauntData.get(helper.getLevel().getServer()), spirit, Altar.Held.IRON) == 12, "the form it takes keeps the usual length");
            check(helper, Ritual.waves(HauntData.get(helper.getLevel().getServer()), spirit, Altar.Held.IRON_NUGGET) == 18, "another form must add six waves");
            check(
                    helper,
                    Ritual.waves(HauntData.get(helper.getLevel().getServer()), spirit.evolve(), Altar.Held.IRON_CHAIN) == 22,
                    "grown and off form must add both");
            check(
                    helper,
                    Ritual.begin(
                            player,
                            helper.getLevel(),
                            helper.absolutePos(AltarGameTests.CENTER),
                            Altar.Held.IRON_CHAIN),
                    "the right category in another form must still start the ritual");
            check(
                    helper,
                    lit(helper) == 4 && wary(player) == 0,
                    "an off-form offering must neither snuff nor frighten the spirit");
            helper.succeed();
        });
        tests.put("ritual_wrong_nature_punishes", helper -> {
            ServerPlayer player = ready(helper);
            ServerLevel level = helper.getLevel();
            BlockPos altar = helper.absolutePos(AltarGameTests.CENTER);
            check(helper, Ritual.begin(player, level, altar, Altar.Held.LANTERN), "a wrong offering is still taken");
            check(helper, lit(helper) == 0, "a wrong category must snuff every candle");
            helper.assertBlockProperty(AltarGameTests.CENTER, Altar.HELD, Altar.Held.EMPTY);
            HauntData.Haunting after =
                    HauntData.get(level.getServer()).find(SPIRIT).orElseThrow();
            check(helper, after.anger() == 1.0, "the spirit must peak in anger");
            check(helper, after.waryUntil() > level.getServer().overworld().getGameTime(), "it must turn wary");
            check(helper, after.bornAt() <= -24000, "it must come of age sooner");
            relight(helper);
            check(
                    helper,
                    !Ritual.begin(player, level, altar, Altar.Held.LANTERN),
                    "a wary spirit must refuse another wrong offering");
            check(
                    helper,
                    !Ritual.begin(player, level, altar, Altar.Held.IRON),
                    "a wary spirit must refuse even its own offering");
            helper.assertBlockProperty(AltarGameTests.CENTER, Altar.HELD, Altar.Held.EMPTY);
            helper.succeed();
        });
    }

    /** The phases: one test per edge, stepping the ritual's waves by hand instead of waiting for them. */
    static void registerPhases(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("ritual_call_is_quiet", helper -> {
            ServerPlayer player = ready(helper);
            Circle circle = started(helper, player);
            check(helper, circle.plan().call() == 2, "the call lasts two waves");
            for (int i = 0; i < circle.plan().call(); i++) {
                check(helper, circle.step(), "the call must go on");
                check(helper, circle.phase().orElseThrow() == Phase.CALL, "the first waves are the call");
            }
            check(helper, lit(helper) == 4, "the spirit must not snuff a candle in the call");
            check(helper, spirit(player).anger() == 0.0, "nothing changes in the call");
            helper.succeed();
        });
        tests.put("ritual_broken_circle_fails_in_the_call", helper -> {
            ServerPlayer player = ready(helper);
            Circle circle = started(helper, player);
            check(helper, circle.step(), "the first wave holds");
            helper.setBlock(CANDLES.getFirst(), Blocks.CANDLE.defaultBlockState());
            check(helper, !circle.step(), "a dark candle must break the ritual even in the call");
            check(helper, circle.over(), "the ritual is over");
            check(helper, Circle.at(helper.absolutePos(AltarGameTests.CENTER)).isEmpty(), "nothing runs at the altar");
            check(helper, spirit(player).anger() == 0.3, "a failed ritual angers the spirit");
            helper.assertBlockProperty(AltarGameTests.CENTER, Altar.HELD, Altar.Held.EMPTY);
            helper.succeed();
        });
        tests.put("ritual_bargain_gift_calms_the_spirit", helper -> {
            ServerPlayer player = ready(helper);
            HauntData.get(helper.getLevel().getServer()).change(SPIRIT, h -> h.angered(0.5));
            Circle circle = started(helper, player);
            steps(helper, circle, circle.plan().call());
            drop(helper, new ItemStack(Graves.wanted(7L) == Items.STICK ? Items.BONE : Items.STICK));
            check(helper, circle.step(), "a thing it did not ask for is no gift");
            check(helper, circle.phase().orElseThrow() == Phase.BARGAIN, "the bargain follows the call");
            drop(helper, new ItemStack(Graves.wanted(7L), 2));
            check(helper, !circle.step(), "the gift ends the ritual");
            HauntData.Haunting after = spirit(player);
            check(helper, Math.abs(after.anger() - 0.2) < 1e-9, "the gift lowers anger by 0.3, got " + after.anger());
            check(helper, gifts(helper) == 1, "the spirit takes one gift and leaves the rest");
            check(helper, essences(helper) == 0, "a bargain leaves no essence");
            check(helper, echoes(helper).isEmpty(), "a bargain leaves no echo");
            helper.assertBlockProperty(AltarGameTests.CENTER, Altar.HELD, Altar.Held.EMPTY);
            check(helper, lit(helper) == 4, "the candles still burn");
            helper.succeed();
        });
        tests.put("ritual_unanswered_bargain_turns_to_fury", helper -> {
            ServerPlayer player = ready(helper);
            Circle circle = started(helper, player);
            steps(helper, circle, circle.plan().call() + circle.plan().bargain());
            check(helper, circle.phase().orElseThrow() == Phase.BARGAIN, "the bargain runs its length");
            Config.Section config = Config.section("ritual");
            // snuffs, picks a candle, does not strike: a strike could touch the candles too
            player.getRandom()
                    .setSeed(seed(r -> r.nextDouble() < config.num("snuff_chance")
                            && r.nextInt(CANDLES.size()) >= 0
                            && r.nextDouble() >= config.num("fury_chance")
                            && r.nextDouble() >= config.num("nature_chance")));
            check(helper, circle.step(), "the fury goes on");
            check(helper, circle.phase().orElseThrow() == Phase.FURY, "no gift: the fury follows");
            check(helper, lit(helper) == 3, "the spirit snuffs a candle in the fury");
            helper.succeed();
        });
        tests.put("ritual_leaving_ends_in_banishing", helper -> {
            ServerPlayer player = ready(helper);
            Circle circle = started(helper, player);
            int toLeaving = circle.plan().call()
                    + circle.plan().bargain()
                    + circle.plan().fury();
            steps(helper, circle, toLeaving);
            check(helper, circle.phase().orElseThrow() == Phase.FURY, "the fury runs its length");
            for (int i = 0; i < circle.plan().leaving() - 1; i++) {
                check(helper, circle.step(), "the leaving goes on");
                check(helper, circle.phase().orElseThrow() == Phase.LEAVING, "the leaving follows the fury");
            }
            check(helper, !circle.step(), "the last wave ends the ritual");
            check(
                    helper,
                    HauntData.get(helper.getLevel().getServer()).find(SPIRIT).isEmpty(),
                    "the spirit is banished");
            check(helper, essences(helper) == 1, "it leaves its essence");
            check(helper, echoes(helper).size() == 1, "an altar banishing leaves an echo at the altar");
            echoes(helper)
                    .forEach(t -> Echoes.leave(
                            HauntData.get(helper.getLevel().getServer()),
                            t.haunting().uuid()));
            helper.succeed();
        });
    }

    /** The echoes of the test spirit standing at the altar (see M2). */
    private static List<HauntData.Territorial> echoes(GameTestHelper helper) {
        HauntData data = HauntData.get(helper.getLevel().getServer());
        return data
                .territorialWithin(helper.getLevel().dimension(), helper.absolutePos(AltarGameTests.CENTER), 0)
                .stream()
                .filter(t -> t.haunting().seed() == 7L && data.echo(t.haunting().uuid()) != 0L)
                .toList();
    }

    /** The gaze: the figure walks the ring in the fury and the leaving, and a nature's rule costs a candle. */
    static void registerGaze(Map<String, Consumer<GameTestHelper>> tests) {
        List<String> darkness = List.of("light_eater", "cold_touch", "night_stalker");
        tests.put("ritual_gaze_darkness_punishes_the_stare", helper -> {
            Circle circle = furyOf(helper, darkness, Altar.Held.GLOWSTONE);
            int limit = Config.section("ritual").integer("gaze_ticks");
            gaze(helper, circle, true, limit - 1);
            check(helper, lit(helper) == 4, "a short stare costs nothing");
            gaze(helper, circle, false, 5);
            gaze(helper, circle, true, limit - 1);
            check(helper, lit(helper) == 4, "looking away starts the count over");
            gaze(helper, circle, true, 1);
            check(helper, lit(helper) == 3, "a stare for the limit snuffs one candle");
            helper.succeed();
        });
        tests.put("ritual_gaze_darkness_ignores_looking_away", helper -> {
            Circle circle = furyOf(helper, darkness, Altar.Held.GLOWSTONE);
            gaze(helper, circle, false, Config.section("ritual").integer("gaze_ticks") * 2);
            check(helper, lit(helper) == 4, "looking away from a darkness spirit costs nothing");
            helper.succeed();
        });
        tests.put("ritual_gaze_malice_punishes_the_stare", helper -> {
            Circle circle = furyOf(helper, List.of("vengeful", "rain_walker", "grudge"), Altar.Held.HOLY_WATER);
            gaze(helper, circle, true, Config.section("ritual").integer("gaze_ticks"));
            check(helper, lit(helper) == 3, "a stare snuffs a candle for a malice spirit too");
            helper.succeed();
        });
        tests.put("ritual_gaze_objects_punishes_looking_away", helper -> {
            Circle circle = furyOf(helper, IRON_TRAITS, Altar.Held.IRON);
            int limit = Config.section("ritual").integer("gaze_ticks");
            gaze(helper, circle, true, limit * 2);
            check(helper, lit(helper) == 4, "watching an objects spirit costs nothing");
            gaze(helper, circle, false, limit - 1);
            check(helper, lit(helper) == 4, "a short glance away costs nothing");
            gaze(helper, circle, false, 1);
            check(helper, lit(helper) == 3, "looking away for the limit snuffs one candle");
            helper.succeed();
        });
        tests.put("ritual_gaze_deceit_punishes_looking_away", helper -> {
            Circle circle = furyOf(helper, List.of("whisperer", "liar", "sleep_thief"), Altar.Held.MIRROR);
            gaze(helper, circle, false, Config.section("ritual").integer("gaze_ticks"));
            check(helper, lit(helper) == 3, "looking away snuffs a candle for a deceit spirit too");
            helper.succeed();
        });
        tests.put("ritual_gaze_waits_for_the_fury", helper -> {
            ServerPlayer player = ready(helper, IRON_TRAITS);
            Circle circle = started(helper, player, Altar.Held.IRON);
            int limit = Config.section("ritual").integer("gaze_ticks");
            for (int i = 0; i < limit * 2; i++) {
                circle.watch();
            }
            check(helper, !circle.gaze().shown(), "no figure before any wave");
            steps(helper, circle, circle.plan().call() + circle.plan().bargain());
            for (int i = 0; i < limit * 2; i++) {
                circle.watch();
            }
            check(helper, !circle.gaze().shown(), "no figure in the call and the bargain");
            check(helper, lit(helper) == 4, "no gaze penalty before the fury");
            steps(helper, circle, 1);
            circle.watch();
            check(helper, circle.gaze().shown(), "the figure walks in the fury");
            helper.succeed();
        });
    }

    /** The natures' fury acts, called directly so the roll does not matter. */
    static void registerNatures(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("ritual_nature_objects_throws_the_offering", helper -> {
            Circle circle = furyOf(helper, IRON_TRAITS, Altar.Held.IRON);
            Natures.objects(circle);
            helper.assertBlockProperty(AltarGameTests.CENTER, Altar.HELD, Altar.Held.EMPTY);
            check(helper, count(helper, new ItemStack(Items.IRON_INGOT)) == 1, "the offering lies off the altar");
            check(helper, !circle.step(), "an offering not laid back breaks the ritual");
            helper.succeed();
        });
        tests.put("ritual_nature_objects_offering_can_be_laid_back", helper -> {
            Circle circle = furyOf(helper, IRON_TRAITS, Altar.Held.IRON);
            Natures.objects(circle);
            ItemStack other = new ItemStack(Items.IRON_NUGGET);
            check(helper, !circle.relay(Altar.Held.IRON_NUGGET, other), "another offering is not the one thrown");
            ItemStack back = new ItemStack(Items.IRON_INGOT, 2);
            check(helper, circle.relay(Altar.Held.IRON, back), "the same offering goes back");
            check(helper, back.getCount() == 1, "it is used up");
            helper.assertBlockProperty(AltarGameTests.CENTER, Altar.HELD, Altar.Held.IRON);
            steps(helper, circle, 1);
            check(helper, !circle.over(), "the ritual goes on");
            helper.succeed();
        });
        tests.put("ritual_nature_darkness_puts_out_the_lights", helper -> {
            Circle circle = furyOf(helper, List.of("light_eater", "cold_touch", "night_stalker"), Altar.Held.GLOWSTONE);
            BlockPos torch = AltarGameTests.CENTER.offset(5, 1, 0);
            helper.setBlock(torch, Blocks.TORCH);
            Natures.darkness(circle);
            check(
                    helper,
                    !UnlitLights.isTorch(helper.getLevel().getBlockState(helper.absolutePos(torch))),
                    "the torch is out");
            check(helper, lit(helper) == 4, "the ritual's candles burn on");
            steps(helper, circle, 1);
            helper.succeed();
        });
        tests.put("ritual_nature_malice_strikes_the_player", helper -> {
            Circle circle = furyOf(helper, List.of("vengeful", "rain_walker", "grudge"), Altar.Held.HOLY_WATER);
            // a mock client never reports itself loaded, and until then the player takes no damage
            circle.player().connection.markClientLoaded();
            float before = circle.player().getHealth();
            Natures.malice(circle);
            check(helper, circle.player().getHealth() < before, "the player is hurt");
            helper.succeed();
        });
        tests.put("ritual_nature_deceit_keeps_the_candles_lit", helper -> {
            Circle circle = furyOf(helper, List.of("whisperer", "liar", "sleep_thief"), Altar.Held.MIRROR);
            Natures.deceit(circle);
            check(helper, lit(helper) == 4, "the server candles stay lit");
            steps(helper, circle, 1);
            helper.succeed();
        });
    }

    /** The ring: the spirit wipes a tile once, and the ritual holds only if it is closed again. */
    static void registerRing(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("ritual_ring_wiped_and_closed_holds", helper -> {
            Circle circle = beforeFury(helper);
            check(helper, circle.ring().gap().isEmpty(), "the ring is whole before the spirit wipes it");
            check(helper, circle.ring().wipe(), "the spirit wipes a tile");
            BlockPos gap = circle.ring().gap().orElseThrow();
            check(helper, helper.getLevel().getBlockState(gap).isAir(), "the tile is wiped");
            check(helper, !circle.ring().wipe(), "only once per ritual");
            helper.getLevel().setBlockAndUpdate(gap, Content.SALT_LINE.get().defaultBlockState());
            steps(helper, circle, 1);
            check(helper, !circle.over() && circle.ring().gap().isEmpty(), "a closed ring keeps the ritual going");
            helper.succeed();
        });
        tests.put("ritual_ring_left_open_fails", helper -> {
            Circle circle = beforeFury(helper);
            check(helper, circle.ring().wipe(), "the spirit wipes a tile");
            check(helper, !circle.step(), "an open ring breaks the ritual at the next wave");
            check(
                    helper,
                    Math.abs(spirit(circle.player()).anger() - 0.3) < 1e-9,
                    "as a failed ritual, it angers the spirit");
            helper.succeed();
        });
    }

    /** The name said in chat: right in the fury cuts it short, right in the leaving ends it, wrong angers. */
    static void registerName(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("ritual_name_cuts_the_fury", helper -> {
            Circle circle = furyOf(helper, IRON_TRAITS, Altar.Held.IRON);
            int fury = circle.plan().fury();
            Restless.onChat(circle.player(), " " + name().toUpperCase(java.util.Locale.ROOT) + "!");
            check(helper, circle.plan().fury() == fury - 4, "the right name takes four waves off the fury");
            check(helper, spirit(circle.player()).anger() == 0.0, "the right name does not anger");
            Restless.onChat(circle.player(), name());
            check(helper, circle.plan().fury() == fury - 4, "the name works once a ritual");
            helper.succeed();
        });
        tests.put("ritual_name_ends_the_leaving", helper -> {
            Circle circle = furyOf(helper, IRON_TRAITS, Altar.Held.IRON);
            steps(helper, circle, circle.plan().fury());
            check(helper, circle.phase().orElseThrow() == Phase.LEAVING, "the leaving is on");
            Restless.onChat(circle.player(), name());
            check(helper, circle.over(), "the right name ends the leaving at once");
            check(
                    helper,
                    HauntData.get(helper.getLevel().getServer()).find(SPIRIT).isEmpty(),
                    "the spirit is banished");
            check(helper, essences(helper) == 1, "it leaves its essence");
            helper.succeed();
        });
        tests.put("ritual_wrong_name_angers", helper -> {
            Circle circle = furyOf(helper, IRON_TRAITS, Altar.Held.IRON);
            int fury = circle.plan().fury();
            Restless.onChat(circle.player(), "Nobody Atall");
            check(helper, Math.abs(spirit(circle.player()).anger() - 0.1) < 1e-9, "a wrong name angers the spirit");
            check(helper, circle.plan().fury() == fury && !circle.over(), "a wrong name changes nothing else");
            helper.succeed();
        });
        tests.put("ritual_name_in_the_call_is_not_heard", helper -> {
            Circle circle = started(helper, ready(helper));
            Restless.onChat(circle.player(), "Nobody Atall");
            Restless.onChat(circle.player(), name());
            check(helper, spirit(circle.player()).anger() == 0.0, "the spirit does not listen yet");
            check(helper, circle.plan().fury() == 6, "the fury keeps its length");
            helper.succeed();
        });
    }

    /** Place and hour: a ritual by the spirit's grave runs a longer fury and leaves one more essence. */
    static void registerHallow(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("ritual_by_the_grave_is_hallowed", helper -> {
            ServerPlayer player = ready(helper);
            BlockPos altar = helper.absolutePos(AltarGameTests.CENTER);
            HauntData.get(helper.getLevel().getServer())
                    .putGrave(SPIRIT, new HauntData.Grave(altar.offset(5, 0, 0), true));
            Circle circle = started(helper, player);
            check(helper, circle.plan().fury() == 8, "the grave adds two waves to the fury");
            steps(helper, circle, circle.plan().total() - 1);
            check(helper, circle.phase().orElseThrow() == Phase.LEAVING, "the leaving is on");
            check(helper, !circle.step(), "the last wave ends the ritual");
            check(helper, essences(helper) == 2, "a hallowed banishing leaves one more essence");
            helper.succeed();
        });
        tests.put("ritual_far_from_the_grave_is_plain", helper -> {
            ServerPlayer player = ready(helper);
            BlockPos altar = helper.absolutePos(AltarGameTests.CENTER);
            HauntData.get(helper.getLevel().getServer())
                    .putGrave(SPIRIT, new HauntData.Grave(altar.offset(40, 0, 0), true));
            check(helper, started(helper, player).plan().fury() == 6, "a far grave changes nothing");
            helper.succeed();
        });
    }

    /** Two at the altar: a helper with a bell cuts the fury, and the spirit's strikes land on both. */
    static void registerSecond(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("ritual_second_with_a_bell_cuts_the_fury", helper -> {
            Circle circle = beforeFury(helper);
            ServerPlayer second = HauntEventGameTests.connected(helper);
            second.setPos(Vec3.atBottomCenterOf(helper.absolutePos(AltarGameTests.CENTER.offset(1, 1, 1))));
            helper.getLevel().addNewPlayer(second); // the circle looks for helpers among the level's players
            second.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.BELL));
            check(helper, Second.inside(circle).contains(second), "the helper stands in the circle");
            check(
                    helper,
                    Second.targets(circle).containsAll(List.of(circle.player(), second)),
                    "the spirit's strikes land on both");
            steps(helper, circle, 2);
            check(helper, circle.phase().orElseThrow() == Phase.FURY, "the fury is on");
            check(helper, circle.plan().fury() == 4, "a bell in the circle takes two waves off the fury");
            steps(helper, circle, 1);
            check(helper, circle.plan().fury() == 4, "only once a ritual");
            circle.player().connection.markClientLoaded();
            second.connection.markClientLoaded();
            float before = second.getHealth();
            Natures.malice(circle);
            check(helper, second.getHealth() < before, "the malice strike hurts the helper too");
            second.discard(); // other tests must not find a stray player in the level
            helper.succeed();
        });
        tests.put("ritual_second_empty_handed_changes_nothing", helper -> {
            Circle circle = beforeFury(helper);
            ServerPlayer second = HauntEventGameTests.connected(helper);
            second.setPos(Vec3.atBottomCenterOf(helper.absolutePos(AltarGameTests.CENTER.offset(1, 1, 1))));
            helper.getLevel().addNewPlayer(second); // the circle looks for helpers among the level's players
            steps(helper, circle, 2);
            check(helper, circle.plan().fury() == 6, "a helper with nothing in hand does not cut the fury");
            second.discard(); // other tests must not find a stray player in the level
            helper.succeed();
        });
    }

    /** The test spirit's name, as its gravestone gives it. */
    private static String name() {
        return dev.keelfy.restless.spirit.Story.of(7L).name();
    }

    /** A ritual stepped into the bargain, before the fury could wipe the ring by its own roll. */
    private static Circle beforeFury(GameTestHelper helper) {
        Circle circle = started(helper, ready(helper));
        steps(helper, circle, circle.plan().call() + 1);
        return circle;
    }

    /** A ritual stepped to its first fury wave, with the player looking at nothing yet. */
    private static Circle furyOf(GameTestHelper helper, List<String> traits, Altar.Held held) {
        ServerPlayer player = ready(helper, traits);
        Circle circle = started(helper, player, held);
        steps(helper, circle, circle.plan().call() + circle.plan().bargain() + 1);
        check(helper, circle.phase().orElseThrow() == Phase.FURY, "the fury is on");
        return circle;
    }

    /** Runs {@code n} gaze ticks with the player looking at the figure or straight away from it. */
    private static void gaze(GameTestHelper helper, Circle circle, boolean at, int n) {
        ServerPlayer player = circle.player();
        for (int i = 0; i < n; i++) {
            Vec3 head = circle.gaze().head();
            if (!circle.gaze().shown()) {
                head = Vec3.atBottomCenterOf(helper.absolutePos(AltarGameTests.CENTER))
                        .add(2.5, 1.5, 0);
            }
            Vec3 eye = player.getEyePosition();
            Vec3 target = at ? head : eye.add(eye.subtract(head));
            player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target);
            circle.watch();
        }
    }

    private static Circle started(GameTestHelper helper, ServerPlayer player) {
        return started(helper, player, Altar.Held.IRON);
    }

    /** Starts the ritual with the given offering. */
    private static Circle started(GameTestHelper helper, ServerPlayer player, Altar.Held held) {
        BlockPos altar = helper.absolutePos(AltarGameTests.CENTER);
        check(helper, Ritual.begin(player, helper.getLevel(), altar, held), "the ritual must start");
        return Circle.at(altar).orElseThrow();
    }

    /** The salt ring and the four lit candles around the altar. */
    static void ringAndCandles(GameTestHelper helper) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    helper.setBlock(AltarGameTests.CENTER.offset(x, 0, z), Content.SALT_LINE.get());
                }
            }
        }
        relight(helper);
    }

    /** {@link #steps}, for other test classes. */
    static void calmSteps(GameTestHelper helper, Circle circle, int n) {
        steps(helper, circle, n);
    }

    /** Runs {@code n} waves that go on, with a calm spirit: neither a snuff nor a strike. */
    private static void steps(GameTestHelper helper, Circle circle, int n) {
        Config.Section config = Config.section("ritual");
        long calm = seed(r -> r.nextDouble() >= config.num("snuff_chance")
                && r.nextDouble() >= config.num("fury_chance")
                && r.nextDouble() >= config.num("nature_chance"));
        for (int i = 0; i < n; i++) {
            circle.player().getRandom().setSeed(calm);
            check(helper, circle.step(), "wave " + i + " must go on");
            // the player closes the ring the spirit wipes once in the fury
            circle.ring()
                    .gap()
                    .ifPresent(pos -> helper.getLevel()
                            .setBlockAndUpdate(pos, Content.SALT_LINE.get().defaultBlockState()));
        }
    }

    /** The first seed whose rolls pass {@code test}. */
    private static long seed(java.util.function.Predicate<RandomSource> test) {
        long seed = 0;
        while (!test.test(RandomSource.create(seed))) {
            seed++;
        }
        return seed;
    }

    private static void drop(GameTestHelper helper, ItemStack stack) {
        BlockPos above = helper.absolutePos(AltarGameTests.CENTER.above());
        helper.getLevel()
                .addFreshEntity(
                        new ItemEntity(helper.getLevel(), above.getX() + 0.5, above.getY(), above.getZ() + 0.5, stack));
    }

    private static int gifts(GameTestHelper helper) {
        return count(helper, Graves.wanted(7L).getDefaultInstance());
    }

    private static int essences(GameTestHelper helper) {
        return count(helper, new ItemStack(Content.ESSENCE.get("poltergeist").get()));
    }

    private static int count(GameTestHelper helper, ItemStack kind) {
        BlockPos at = helper.absolutePos(AltarGameTests.CENTER);
        return helper
                .getLevel()
                .getEntitiesOfClass(
                        ItemEntity.class,
                        new net.minecraft.world.phys.AABB(at).inflate(4),
                        e -> e.isAlive() && e.getItem().is(kind.getItem()))
                .stream()
                .mapToInt(e -> e.getItem().getCount())
                .sum();
    }

    /** An altar with its salt ring and four lit candles, and a player haunted by a poltergeist of iron nature. */
    private static ServerPlayer ready(GameTestHelper helper) {
        return ready(helper, IRON_TRAITS);
    }

    private static ServerPlayer ready(GameTestHelper helper, List<String> traits) {
        AltarGameTests.place(helper);
        ringAndCandles(helper);
        ServerPlayer player = HauntEventGameTests.connected(helper);
        ChannelAttributes.getOrCreateCommonChannels(player.connection.getConnection(), ConnectionProtocol.PLAY)
                .add(SanityPayload.TYPE.id());
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(AltarGameTests.CENTER.above())));
        HauntData.get(helper.getLevel().getServer()).put(player.getUUID(), List.of(spirit(null, traits)));
        return player;
    }

    private static HauntData.Haunting spirit(ServerPlayer player) {
        return HauntData.get(player.level().getServer()).find(SPIRIT).orElseThrow();
    }

    private static HauntData.Haunting spirit(ServerPlayer player, List<String> traits) {
        return new HauntData.Haunting(SPIRIT, "poltergeist", 7L, traits, 0L, 0.0, Long.MAX_VALUE / 2, false, 0, 0L);
    }

    private static void relight(GameTestHelper helper) {
        for (BlockPos pos : CANDLES) {
            helper.setBlock(pos, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true));
        }
    }

    private static long lit(GameTestHelper helper) {
        return CANDLES.stream()
                .filter(pos -> AbstractCandleBlock.isLit(helper.getLevel().getBlockState(helper.absolutePos(pos))))
                .count();
    }

    private static long wary(ServerPlayer player) {
        return spirit(player).waryUntil();
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }
}
