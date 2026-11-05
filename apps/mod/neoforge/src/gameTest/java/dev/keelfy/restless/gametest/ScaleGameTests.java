package dev.keelfy.restless.gametest;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.haunt.Births;
import dev.keelfy.restless.haunt.HauntData;
import dev.keelfy.restless.haunt.Sense;
import dev.keelfy.restless.haunt.Wards;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/** A world full of territorial spirits (explored area grows it) must not make every lookup walk them all. */
final class ScaleGameTests {
    static final String MANY = "ten_thousand_territorial_spirits";
    private static final int SPIRITS = 10_000;
    private static final int SIDE = 100;
    private static final int SPACING = 200;
    private static final long BUDGET_MS = 50;

    private ScaleGameTests() {}

    private static HauntData.Territorial spirit(ServerLevel level, BlockPos at, RandomSource random) {
        return new HauntData.Territorial(
                HauntData.Haunting.born("revenant", 0, random).rescheduled(Long.MAX_VALUE / 2),
                level.dimension(),
                at,
                48);
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("territorial_index_follows_changes", helper -> {
            var level = helper.getLevel();
            var data = HauntData.get(level.getServer());
            var player = HauntEventGameTests.connected(helper);
            RandomSource random = RandomSource.create(3);
            BlockPos here = player.blockPosition();
            HauntData.Territorial near = spirit(level, here.offset(10, 0, 0), random);
            HauntData.Territorial edge = spirit(level, here.offset(-48, 0, 0), random);
            HauntData.Territorial far = spirit(level, here.offset(0, 0, 49), random);
            HauntData.Territorial other = new HauntData.Territorial(
                    HauntData.Haunting.born("revenant", 0, random), net.minecraft.world.level.Level.NETHER, here, 48);
            data.putTerritorial(List.of(near, edge, far, other));
            UUID id = near.haunting().uuid();
            data.putSpoken(id, "some_event", new HauntData.Spoken(List.of("a"), 5));
            check(helper, data.territorialReaching(player).size() == 2, "the radius edge counts, the far one does not");
            check(helper, data.territorialWithin(level.dimension(), here, 49).size() == 3, "within is by distance");
            check(helper, data.territorial().size() == 4, "all four stay listed in saved order");
            data.change(id, h -> h.angered(0.5));
            check(
                    helper,
                    data.find(id).orElseThrow().anger() == 0.5
                            && data.territorial().getFirst().haunting().anger() == 0.5,
                    "a change must show in the list and in find, in the same place");
            data.change(id, h -> null);
            check(helper, data.find(id).isEmpty() && data.territorial().size() == 3, "a null change removes it");
            check(helper, data.spoken(id, "some_event").used().isEmpty(), "a removed spirit leaves nothing it said");
            check(helper, data.territorialReaching(player).size() == 1, "the index must forget a removed spirit");
            data.removeTerritorial(edge.haunting().uuid());
            check(helper, data.territorialReaching(player).isEmpty(), "removeTerritorial must drop the index entry");
            data.putTerritorial(List.of());
            check(
                    helper,
                    data.territorialWithin(level.dimension(), here, 500).isEmpty(),
                    "replacing with none empties it");
            player.discard();
            helper.succeed();
        });
        tests.put(MANY, helper -> {
            var level = helper.getLevel();
            var data = HauntData.get(level.getServer());
            var player = HauntEventGameTests.connected(helper);
            RandomSource random = RandomSource.create(7);
            long forever = Long.MAX_VALUE / 2;
            BlockPos origin = helper.absolutePos(BlockPos.ZERO);
            List<HauntData.Territorial> all = new ArrayList<>();
            for (int i = 0; i < SPIRITS; i++) {
                BlockPos anchor = origin.offset(i % SIDE * SPACING, 0, i / SIDE * SPACING);
                all.add(new HauntData.Territorial(
                        HauntData.Haunting.born("revenant", 0, random).rescheduled(forever),
                        level.dimension(),
                        anchor,
                        48));
            }
            data.putTerritorial(all);
            UUID last = all.getLast().haunting().uuid();
            StructureStart start = BirthGameTests.start(helper, "shipwreck", origin.offset(-5000, 70, -5000));

            long began = System.nanoTime();
            for (int i = 0; i < 100; i++) {
                Sense.of(player, data); // what sanity and the graves ask of every player
            }
            long sense = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                data.find(last); // what the blight asks for every blighted land
                data.change(last, h -> h.rescheduled(forever));
            }
            long find = System.nanoTime();
            for (int i = 0; i < 100; i++) {
                Births.generated(level, start, false); // every new structure
            }
            long births = System.nanoTime();
            for (int i = 0; i < 100; i++) {
                Wards.rung(level, origin);
            }
            long bell = System.nanoTime();
            long total = (bell - began) / 1_000_000;
            Restless.LOGGER.info(
                    "10000 spirits: sense {} ms, find+change {} ms, births {} ms, bell {} ms",
                    (sense - began) / 1_000_000,
                    (find - sense) / 1_000_000,
                    (births - find) / 1_000_000,
                    (bell - births) / 1_000_000);
            data.putTerritorial(List.of());
            player.discard();
            HauntEventGameTests.check(
                    helper, total < BUDGET_MS, "lookups among " + SPIRITS + " spirits took " + total + " ms");
            helper.succeed();
        });
    }
}
