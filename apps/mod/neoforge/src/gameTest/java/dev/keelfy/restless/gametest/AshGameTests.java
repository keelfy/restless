package dev.keelfy.restless.gametest;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.block.AshBlock;
import dev.keelfy.restless.content.entity.AshFootprint;
import dev.keelfy.restless.state.HauntData;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

final class AshGameTests {
    private static final BlockPos CENTER = new BlockPos(4, 1, 4);

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("ash_free_trail", helper -> {
            patch(helper);
            boolean made = AshFootprint.leaveTrail(helper.getLevel(), helper.absolutePos(CENTER), 1, .3);
            List<AshFootprint> trail = trail(helper);
            check(helper, made && trail.size() >= 4, "A spirit must leave a walking trail across connected ash");
            check(
                    helper,
                    trail.stream().filter(p -> !p.isInvisible()).count() == 1,
                    "Only the first impression may appear immediately");
            check(
                    helper,
                    trail.stream().anyMatch(AshFootprint::left)
                            && trail.stream().anyMatch(p -> !p.left()),
                    "The trail must alternate feet");
            check(
                    helper,
                    trail.stream().anyMatch(p -> Math.abs(p.getX() % 1 - .5) > .1),
                    "Impressions must not snap to block centres");
            check(
                    helper,
                    trail.stream()
                            .anyMatch(p -> p.getBoundingBox().minX < Math.floor(p.getX())
                                    || p.getBoundingBox().maxX > Math.floor(p.getX()) + 1
                                    || p.getBoundingBox().minZ < Math.floor(p.getZ())
                                    || p.getBoundingBox().maxZ > Math.floor(p.getZ()) + 1),
                    "An impression must be able to cross a block boundary");
            for (AshFootprint print : trail) {
                check(
                        helper,
                        print.shape() == 1 && Math.abs(print.getYRot() - (.3 * 180 / Math.PI + 90)) < .001,
                        "Shape and arbitrary heading must reach each impression");
                check(
                        helper,
                        !print.isPickable() && !print.isPushable(),
                        "Decals must not block interaction or movement");
                check(
                        helper,
                        print.getY() > helper.absolutePos(CENTER).getY() + 1.0 / 16,
                        "Decals must sit above the powder without z-fighting");
            }
            helper.runAfterDelay(9, () -> {
                check(
                        helper,
                        trail.stream().filter(p -> !p.isInvisible()).count() >= 2,
                        "Later footsteps must appear over time");
                helper.succeed();
            });
        });
        tests.put("ash_cleanup", helper -> {
            patch(helper);
            AshFootprint.leaveTrail(helper.getLevel(), helper.absolutePos(CENTER), 3, 0);
            List<AshFootprint> trail = trail(helper);
            check(helper, !trail.isEmpty(), "Test requires a trail");
            for (BlockPos pos : BlockPos.betweenClosed(1, 1, 1, 7, 1, 7)) {
                helper.setBlock(pos, Blocks.AIR);
            }
            helper.runAfterDelay(2, () -> {
                check(
                        helper,
                        trail.stream().allMatch(AshFootprint::isRemoved),
                        "Removing ash must remove both visible and pending impressions");
                helper.succeed();
            });
        });
        tests.put("ash_expiry", helper -> {
            patch(helper);
            AshFootprint.leaveTrail(helper.getLevel(), helper.absolutePos(CENTER), 4, 0);
            AshFootprint print = trail(helper).stream()
                    .filter(p -> !p.isInvisible())
                    .findFirst()
                    .orElseThrow();
            for (int tick = 0; tick < AshFootprint.LIFETIME; tick++) {
                print.tick();
            }
            check(helper, print.isRemoved(), "Old impressions must expire instead of accumulating entities");
            helper.assertBlockPresent(Content.ASH_LINE.get(), CENTER);
            helper.succeed();
        });
        tests.put("ash_print_follows_type_not_nature", helper -> {
            patch(helper);
            var data = HauntData.get(helper.getLevel().getServer());
            var block = helper.getLevel().getBlockState(helper.absolutePos(CENTER));
            // Same type, opposite natures: iron (Objects) against holy water (Malice).
            int objects = shapeLeftBy(helper, data, block, List.of("restless_hands"));
            int malice = shapeLeftBy(helper, data, block, List.of("vengeful"));
            check(helper, objects == malice, "Two spirits of one type must leave the same print");
            check(helper, objects == AshBlock.print("poltergeist"), "The print must be the one of the type");
            int wet = shapeLeftBy(helper, data, block, List.of("restless_hands", "rain_walker"));
            check(helper, wet == objects, "A condition must not change the print");
            check(helper, trail(helper).stream().allMatch(AshFootprint::wet), "A rain walker must leave damp prints");
            data.putTerritorial(List.of());
            for (String type : List.of(
                    "shade",
                    "phantom",
                    "mimic",
                    "wraith",
                    "mara",
                    "banshee",
                    "revenant",
                    "lost_one",
                    "poltergeist",
                    "nix",
                    "domovoi",
                    "hoarder",
                    "demon",
                    "trickster",
                    "boggart")) {
                check(
                        helper,
                        Config.section("ash").lines("prints").stream().anyMatch(row -> row.contains(type)),
                        "Every type needs a print: " + type);
            }
            helper.succeed();
        });
        tests.put("ash_requires_powder", helper -> {
            helper.setBlock(CENTER.below(), Blocks.STONE);
            check(
                    helper,
                    !AshFootprint.leaveTrail(helper.getLevel(), helper.absolutePos(CENTER), 2, 0),
                    "No impressions may appear without ash");
            check(helper, trail(helper).isEmpty(), "Rejected trails must leave no entities");
            helper.succeed();
        });
    }

    private static int shapeLeftBy(
            GameTestHelper helper,
            HauntData data,
            net.minecraft.world.level.block.state.BlockState block,
            List<String> traits) {
        trail(helper).forEach(AshFootprint::discard);
        var spirit = new HauntData.Haunting(
                java.util.UUID.randomUUID(), "poltergeist", 7L, traits, 0L, 0.0, Long.MAX_VALUE / 2, false, 0, 0L);
        data.putTerritorial(List.of(
                new HauntData.Territorial(spirit, helper.getLevel().dimension(), helper.absolutePos(CENTER), 16)));
        for (int i = 0; i < 40 && trail(helper).isEmpty(); i++) {
            block.tick(
                    helper.getLevel(),
                    helper.absolutePos(CENTER),
                    helper.getLevel().getRandom());
        }
        List<AshFootprint> made = trail(helper);
        check(helper, !made.isEmpty(), "A spirit near ash must leave a trail");
        return made.get(0).shape();
    }

    private static void patch(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(1, 1, 1, 7, 1, 7)) {
            helper.setBlock(pos.below(), Blocks.STONE);
            helper.setBlock(pos, Content.ASH_LINE.get());
        }
    }

    private static List<AshFootprint> trail(GameTestHelper helper) {
        return helper.getLevel()
                .getEntitiesOfClass(AshFootprint.class, new AABB(helper.absolutePos(CENTER)).inflate(7));
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        helper.assertTrue(condition, Component.literal(message));
    }
}
