package dev.keelfy.restless.gametest;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.state.HauntData;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

/**
 * What one spirit's roll costs on generated terrain rather than a test plot: the overworld noise (with its caves and
 * aquifers) is copied around a spot far from the other tests, then the roll is timed on the surface, in a cave and
 * among village houses.
 */
final class TerrainRollGameTests {
    static final String TIMED = "roll_is_cheap_on_generated_terrain";
    private static final int REACH = 40;
    private static final long SEED = 1234L;
    private static final int WARMUP = 30;
    private static final int ROLLS = 100;
    private static final double BUDGET_MS = 2.0;
    private static final List<String> HOUSES = List.of(
            "plains_small_house_1",
            "plains_small_house_2",
            "plains_medium_house_1",
            "plains_big_house_1",
            "plains_butcher_shop_1",
            "plains_library_1",
            "plains_temple_3",
            "plains_tannery_1");

    private TerrainRollGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put(TIMED, helper -> {
            ServerLevel level = helper.getLevel();
            BlockPos centre = helper.absolutePos(BlockPos.ZERO).offset(6000, 0, 6000);
            int[] surface = generate(level, centre);
            ServerPlayer player = HauntEventGameTests.connected(helper);
            long now = level.getGameTime();
            HauntData.Haunting spirit = new HauntData.Haunting(
                    UUID.randomUUID(), "poltergeist", SEED, List.of(), now - 1_000_000_000L, 1.0, 0L, true, 0, 0L);

            int top = surface[index(0, 0)];
            player.setPos(centre.getX() + 0.5, top + 1, centre.getZ() + 0.5);
            double open = time(player, spirit, now);

            BlockPos cave = cave(level, centre, surface);
            HauntEventGameTests.check(helper, cave != null, "the generated terrain must have a cave");
            player.setPos(cave.getX() + 0.5, cave.getY(), cave.getZ() + 0.5);
            double underground = time(player, spirit, now);

            village(level, centre, surface);
            player.setPos(centre.getX() + 0.5, top + 1, centre.getZ() + 0.5);
            double village = time(player, spirit, now);

            Restless.LOGGER.info(
                    "Roll on generated terrain: surface {} ms, cave {} ms, village {} ms",
                    String.format("%.3f", open),
                    String.format("%.3f", underground),
                    String.format("%.3f", village));
            HauntEventGameTests.check(
                    helper,
                    open < BUDGET_MS && underground < BUDGET_MS && village < BUDGET_MS,
                    String.format(
                            "a roll must stay under %.1f ms: surface %.3f, cave %.3f, village %.3f",
                            BUDGET_MS, open, underground, village));
            player.discard();
            helper.succeed();
        });
        HauntEventGameTests.TIMEOUT.put(TIMED, 400);
    }

    /** Median milliseconds of one roll, after a warm-up. */
    private static double time(ServerPlayer player, HauntData.Haunting spirit, long now) {
        for (int i = 0; i < WARMUP; i++) {
            Director.choose(player, spirit, now);
        }
        long[] took = new long[ROLLS];
        for (int i = 0; i < ROLLS; i++) {
            long began = System.nanoTime();
            Director.choose(player, spirit, now);
            took[i] = System.nanoTime() - began;
        }
        Arrays.sort(took);
        return took[ROLLS / 2] / 1_000_000.0;
    }

    /** Copies the overworld's noise terrain around {@code centre}; @return the top solid y of each column */
    private static int[] generate(ServerLevel level, BlockPos centre) {
        var registries = level.registryAccess();
        var settings = registries.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD);
        var generator = new NoiseBasedChunkGenerator(
                new FixedBiomeSource(registries.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS)), settings);
        RandomState random = RandomState.create(registries.lookupOrThrow(Registries.NOISE), SEED, settings.value());
        int side = REACH * 2 + 1;
        int[] surface = new int[side * side];
        int ceiling = settings.value().seaLevel() + 64;
        for (int dx = -REACH; dx <= REACH; dx++) {
            for (int dz = -REACH; dz <= REACH; dz++) {
                int x = centre.getX() + dx;
                int z = centre.getZ() + dz;
                NoiseColumn column = generator.getBaseColumn(x, z, level, random);
                int top = level.getMinY();
                for (int y = level.getMinY(); y < ceiling; y++) {
                    BlockState state = column.getBlock(y);
                    if (!state.isAir()) {
                        top = y;
                    }
                    level.setBlock(new BlockPos(x, y, z), state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
                surface[index(dx, dz)] = top;
            }
        }
        return surface;
    }

    /** An open spot well under the surface with ground below it, the nearest one to the centre. */
    private static BlockPos cave(ServerLevel level, BlockPos centre, int[] surface) {
        for (int ring = 0; ring <= REACH / 2; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    for (int y = surface[index(dx, dz)] - 12; y > level.getMinY() + 8; y--) {
                        BlockPos at = centre.offset(dx, 0, dz).atY(y);
                        if (level.getBlockState(at).isAir()
                                && level.getBlockState(at.above()).isAir()
                                && level.getBlockState(at.below()).isSolid()) {
                            return at;
                        }
                    }
                }
            }
        }
        return null;
    }

    /** Plains houses in a ring around the centre, each on the ground under its corner. */
    private static void village(ServerLevel level, BlockPos centre, int[] surface) {
        var templates = level.getServer().getStructureTemplateManager();
        RandomSource random = RandomSource.create(SEED);
        for (int i = 0; i < HOUSES.size(); i++) {
            double angle = Math.PI * 2 * i / HOUSES.size();
            int dx = (int) Math.round(Math.cos(angle) * 18);
            int dz = (int) Math.round(Math.sin(angle) * 18);
            BlockPos at = centre.offset(dx, 0, dz).atY(surface[index(dx, dz)] + 1);
            templates
                    .get(Identifier.withDefaultNamespace("village/plains/houses/" + HOUSES.get(i)))
                    .orElseThrow()
                    .placeInWorld(level, at, at, new StructurePlaceSettings(), random, Block.UPDATE_CLIENTS);
        }
    }

    private static int index(int dx, int dz) {
        return (dx + REACH) * (REACH * 2 + 1) + dz + REACH;
    }
}
