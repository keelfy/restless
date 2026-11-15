package dev.keelfy.restless.gametest;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.block.Altar;
import dev.keelfy.restless.haunt.event.EventTag;
import dev.keelfy.restless.haunt.event.HauntEvent;
import dev.keelfy.restless.haunt.event.HauntEvents;
import dev.keelfy.restless.state.HauntData;
import dev.keelfy.restless.ward.Blessing;
import dev.keelfy.restless.ward.Spells;
import dev.keelfy.restless.ward.Wards;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Blessing a house: the water that begins it, the quiet week around the altar and the spirits' return. */
final class BlessingGameTests {
    private static final BlockPos CENTER = AltarGameTests.CENTER;
    private static final List<BlockPos> CANDLES = List.of(
            CENTER.offset(-3, 1, -3),
            CENTER.offset(3, 1, -3),
            CENTER.offset(-3, 1, 3),
            CENTER.offset(3, 1, 3),
            CENTER.offset(-3, 1, 0),
            CENTER.offset(3, 1, 0),
            CENTER.offset(0, 1, -3),
            CENTER.offset(0, 1, 3));

    private BlessingGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("blessing_keeps_events_away_within_its_radius", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            openEnclosure(helper);
            BlockPos at = helper.absolutePos(CENTER);
            player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            Spells spells = Spells.get(helper.getLevel().getServer());
            long now = helper.getLevel().getGameTime();
            int radius = Config.section("blessing").integer("radius");
            Spells.Spell far = spell(helper, at.offset(radius + 4, 0, 0), now + 1000);
            Spells.Spell near = spell(helper, at.offset(radius - 4, 0, 0), now + 1000);
            Spells.Spell ended = spell(helper, at, now - 1);
            spells.add(far);
            spells.add(ended);
            HauntEvent any = HauntEvents.all().stream()
                    .filter(e -> e.tags().contains(EventTag.LIGHTS))
                    .findFirst()
                    .orElseThrow();
            check(helper, !Wards.stops(player, any), "a blessing beyond the radius or ended must stop nothing");
            spells.add(near);
            check(helper, Wards.stops(player, any), "a blessing within the radius must stop the event");
            spells.remove(near);
            spells.remove(far);
            spells.remove(ended);
            player.discard();
            helper.succeed();
        });
        tests.put("blessing_end_returns_every_spirit_angrier", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            MinecraftServer server = helper.getLevel().getServer();
            HauntData data = HauntData.get(server);
            HauntData.Haunting first = spirit(0.1);
            HauntData.Haunting second = spirit(0.5);
            data.put(player.getUUID(), List.of(first, second));
            Spells spells = Spells.get(server);
            long now = server.overworld().getGameTime();
            Spells.Spell blessing = new Spells.Spell(
                    Spells.Kind.BLESSING,
                    helper.getLevel().dimension(),
                    helper.absolutePos(CENTER),
                    now - 1,
                    Optional.of(player.getUUID()));
            Spells.Spell live = new Spells.Spell(
                    Spells.Kind.BLESSING,
                    helper.getLevel().dimension(),
                    helper.absolutePos(CENTER),
                    now + 1000,
                    Optional.of(UUID.randomUUID()));
            spells.add(blessing);
            spells.add(live);
            spells.prune(now);
            check(helper, spells.all(Spells.Kind.BLESSING).contains(blessing), "pruning must not drop a blessing");
            Blessing.release(server);
            check(helper, !spells.all(Spells.Kind.BLESSING).contains(blessing), "an ended blessing must be removed");
            check(helper, spells.all(Spells.Kind.BLESSING).contains(live), "a live blessing must stay");
            double returned = Config.section("blessing").num("return_anger");
            check(
                    helper,
                    Math.abs(data.find(first.uuid()).orElseThrow().anger() - (0.1 + returned)) < 1e-9
                            && Math.abs(data.find(second.uuid()).orElseThrow().anger() - (0.5 + returned)) < 1e-9,
                    "each spirit must return angrier by " + returned);
            spells.remove(live);
            data.change(first.uuid(), h -> null);
            data.change(second.uuid(), h -> null);
            player.discard();
            helper.succeed();
        });
        tests.put("blessing_begins_with_eight_candles", helper -> {
            ServerPlayer player = ready(helper, 8);
            BlockPos altar = helper.absolutePos(CENTER);
            water(helper, player);
            check(helper, Blessing.running(helper.getLevel(), altar), "eight candles must begin a blessing");
            helper.assertBlockProperty(CENTER, Altar.HELD, Altar.Held.EMPTY);
            Blessing.forget(altar);
            player.discard();
            helper.succeed();
        });
        tests.put("blessing_water_with_four_candles_is_holy_water", helper -> {
            BlockPos altar = helper.absolutePos(CENTER);
            ServerPlayer other = ready(helper, 4);
            water(helper, other);
            check(helper, !Blessing.running(helper.getLevel(), altar), "four candles must not begin a blessing");
            helper.assertBlockProperty(CENTER, Altar.HELD, Altar.Held.WATER);
            helper.getLevel()
                    .getBlockTicks()
                    .clearArea(new net.minecraft.world.level.levelgen.structure.BoundingBox(altar));
            other.discard();
            helper.succeed();
        });
        tests.put("blessing_waves_end_in_a_spell_owned_by_the_player", helper -> {
            ServerPlayer player = ready(helper, 8);
            BlockPos altar = helper.absolutePos(CENTER);
            check(helper, Blessing.begin(player, helper.getLevel(), altar, 1), "the blessing must begin");
            check(helper, !Blessing.begin(player, helper.getLevel(), altar, 1), "one blessing per altar at a time");
            int waves = Config.section("blessing").integer("waves");
            helper.runAfterDelay(waves + 10L, () -> {
                Spells spells = Spells.get(helper.getLevel().getServer());
                List<Spells.Spell> found = spells.near(
                        Spells.Kind.BLESSING,
                        helper.getLevel().dimension(),
                        altar,
                        1,
                        helper.getLevel().getGameTime());
                check(
                        helper,
                        found.size() == 1 && found.get(0).owner().equals(Optional.of(player.getUUID())),
                        "a finished blessing must leave a spell owned by the player");
                long left = found.get(0).until() - helper.getLevel().getGameTime();
                check(
                        helper,
                        left > Config.section("blessing").integer("ticks") - 100,
                        "the blessing must last its week");
                found.forEach(spells::remove);
                player.discard();
                helper.succeed();
            });
        });
        tests.put("blessing_fails_when_the_ring_opens", helper -> {
            ServerPlayer player = ready(helper, 8);
            openEnclosure(helper);
            BlockPos altar = helper.absolutePos(CENTER);
            check(helper, Blessing.begin(player, helper.getLevel(), altar, 2), "the blessing must begin");
            // Not beside a candle: a candle above a cell makes it a wall for the ring.
            helper.setBlock(CENTER.offset(2, 0, 1), Blocks.AIR);
            helper.runAfterDelay(15, () -> {
                Spells spells = Spells.get(helper.getLevel().getServer());
                check(
                        helper,
                        spells.near(
                                        Spells.Kind.BLESSING,
                                        helper.getLevel().dimension(),
                                        altar,
                                        1,
                                        helper.getLevel().getGameTime())
                                .isEmpty(),
                        "an open ring must fail the blessing");
                check(helper, !Blessing.running(helper.getLevel(), altar), "a failed blessing must free the altar");
                player.discard();
                helper.succeed();
            });
        });
    }

    /** A complete altar in a closed salt ring with {@code candles} lit candles, and a player beside it. */
    private static ServerPlayer ready(GameTestHelper helper, int candles) {
        AltarGameTests.place(helper);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    helper.setBlock(CENTER.offset(x, 0, z), Content.SALT_LINE.get());
                }
            }
        }
        for (int i = 0; i < CANDLES.size(); i++) {
            helper.setBlock(
                    CANDLES.get(i),
                    i < candles
                            ? Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true)
                            : Blocks.AIR.defaultBlockState());
        }
        ServerPlayer player = HauntEventGameTests.connected(helper);
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(CENTER.above())));
        return player;
    }

    private static void water(GameTestHelper helper, ServerPlayer player) {
        ItemStack water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        BlockPos corner = helper.absolutePos(CENTER.offset(-1, 0, -1));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(corner), Direction.UP, corner, false);
        helper.getLevel()
                .getBlockState(corner)
                .useItemOn(water, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
    }

    /** The test enclosure's own barrier walls would count as a salt ring. */
    private static void openEnclosure(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 9, 0, 9)) {
            helper.setBlock(pos, Blocks.STONE);
        }
        for (int x = 9; x <= 11; x++) {
            helper.setBlock(new BlockPos(x, 1, 4), Blocks.AIR);
            helper.setBlock(new BlockPos(x, 2, 4), Blocks.AIR);
        }
    }

    private static Spells.Spell spell(GameTestHelper helper, BlockPos pos, long until) {
        return new Spells.Spell(Spells.Kind.BLESSING, helper.getLevel().dimension(), pos, until);
    }

    private static HauntData.Haunting spirit(double anger) {
        return new HauntData.Haunting(
                UUID.randomUUID(), "poltergeist", 7L, List.of(), 0L, anger, Long.MAX_VALUE / 2, false, 0, 0L);
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }
}
