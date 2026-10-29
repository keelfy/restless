package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.Restless;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * A territorial spirit left alone fouls the land around its anchor while that chunk is loaded: grass dies, flowers and ferns wither to dead bushes,
 * and the biome turns to {@code restless:blighted} (grey grass, thick fog, ash in the air) a column of cells at a time.
 * The reach grows with the spirit's age, an evolved one spreads faster. Every spot and column remembers what was there, and
 * once the spirit is gone (banished, at rest or removed) the land heals a little every check, wherever its chunk is loaded.
 */
public final class Corruption {
    public static final ResourceKey<Biome> BLIGHTED = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(Restless.MOD_ID, "blighted"));

    private Corruption() {}

    public static void tick(MinecraftServer server) {
        Config.Section config = Config.section("corruption");
        if (server.getTickCount() % config.integer("check_ticks") != 0) {
            return;
        }
        HauntData data = HauntData.get(server);
        long now = server.overworld().getGameTime();
        for (HauntData.Territorial spirit : data.territorial()) {
            ServerLevel level = server.getLevel(spirit.dimension());
            if (level != null && loaded(level, spirit.anchor()) != null) {
                spread(level, data, spirit, now, config);
            }
        }
        for (Map.Entry<UUID, HauntData.Blight> entry : List.copyOf(data.blight().entrySet())) {
            if (data.find(entry.getKey()).isEmpty()) {
                heal(server, data, entry.getKey(), entry.getValue(), config.integer("recede"));
            }
        }
    }

    private static void spread(ServerLevel level, HauntData data, HauntData.Territorial spirit, long now, Config.Section config) {
        UUID id = spirit.haunting().uuid();
        HauntData.Blight blight = data.blight().getOrDefault(id, new HauntData.Blight(spirit.dimension(), List.of(), List.of()));
        List<HauntData.Blight.Spot> spots = new ArrayList<>(blight.spots());
        List<HauntData.Blight.Column> columns = new ArrayList<>(blight.columns());
        Optional<Holder.Reference<Biome>> blighted = level.registryAccess().lookupOrThrow(Registries.BIOME).get(BLIGHTED);
        Set<LevelChunk> touched = new HashSet<>();
        double reach = Power.blightReach(now - spirit.haunting().bornAt(), spirit.radius());
        int tries = config.integer("tries") * (spirit.haunting().evolved() ? config.integer("evolved_factor") : 1);
        RandomSource random = level.getRandom();
        for (int i = 0; i < tries && spots.size() < config.integer("max_blocks"); i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = Math.sqrt(random.nextDouble()) * reach;
            BlockPos column = spirit.anchor().offset((int) Math.round(Math.cos(angle) * distance), 0, (int) Math.round(Math.sin(angle) * distance));
            LevelChunk chunk = loaded(level, column);
            if (chunk == null) {
                continue;
            }
            blighted.flatMap(biome -> turn(level, chunk, column, biome, config)).ifPresent(turned -> {
                columns.add(turned);
                touched.add(chunk);
            });
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            for (BlockPos pos : new BlockPos[] {top, top.below()}) {
                BlockState was = level.getBlockState(pos);
                Optional<Block> into = into(was, config);
                if (into.isPresent()) {
                    level.setBlock(pos, into.get().defaultBlockState(), Block.UPDATE_ALL);
                    level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 3, 0.3, 0.1, 0.3, 0.01);
                    spots.add(new HauntData.Blight.Spot(pos.immutable(), was));
                    break;
                }
            }
        }
        resend(level, touched);
        if (spots.size() != blight.spots().size() || columns.size() != blight.columns().size()) {
            data.putBlight(id, new HauntData.Blight(spirit.dimension(), spots, columns));
        }
    }

    /** Turns the cells around the surface of this block column to the blighted biome, unless some spirit already has. */
    private static Optional<HauntData.Blight.Column> turn(ServerLevel level, LevelChunk chunk, BlockPos column, Holder<Biome> biome, Config.Section config) {
        int x = QuartPos.fromBlock(column.getX());
        int z = QuartPos.fromBlock(column.getZ());
        int surface = QuartPos.fromBlock(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, QuartPos.toBlock(x) + 2, QuartPos.toBlock(z) + 2));
        if (chunk.getNoiseBiome(x, surface, z).is(BLIGHTED)) {
            return Optional.empty();
        }
        int from = Math.max(surface - config.integer("biome_depth"), QuartPos.fromBlock(level.getMinY()));
        int to = Math.min(surface + config.integer("biome_height"), QuartPos.fromBlock(level.getMaxY()));
        List<ResourceKey<Biome>> was = new ArrayList<>();
        for (int y = from; y <= to; y++) {
            Optional<ResourceKey<Biome>> key = chunk.getNoiseBiome(x, y, z).unwrapKey();
            if (key.isEmpty()) {
                return Optional.empty();
            }
            was.add(key.get());
        }
        paint(chunk, x, from, z, Collections.nCopies(was.size(), biome));
        return Optional.of(new HauntData.Blight.Column(x, from, z, was));
    }

    /** Sets the cells of quart column {@code x, z} from {@code from} upward; a null entry leaves that cell as it is. */
    private static void paint(LevelChunk chunk, int x, int from, int z, List<@Nullable Holder<Biome>> biomes) {
        chunk.fillBiomesFromNoise((qx, qy, qz) -> {
            Holder<Biome> now = chunk.getNoiseBiome(qx, qy, qz);
            if (qx != x || qz != z || qy < from || qy >= from + biomes.size()) {
                return now;
            }
            Holder<Biome> set = biomes.get(qy - from);
            return set == null ? now : set;
        });
        chunk.markUnsaved();
    }

    private static void resend(ServerLevel level, Set<LevelChunk> chunks) {
        if (!chunks.isEmpty()) {
            level.getChunkSource().chunkMap.resendBiomesForChunks(List.copyOf(chunks));
        }
    }

    /**
     * Puts back up to {@code count} spots and as many biome columns whose chunks are loaded; a spot or cell changed since by
     * something else is let go.
     */
    private static void heal(MinecraftServer server, HauntData data, UUID spirit, HauntData.Blight blight, int count) {
        ServerLevel level = server.getLevel(blight.dimension());
        if (level == null) {
            data.putBlight(spirit, null);
            return;
        }
        List<HauntData.Blight.Spot> left = new ArrayList<>();
        int healed = 0;
        for (HauntData.Blight.Spot spot : blight.spots()) {
            if (healed >= count || loaded(level, spot.pos()) == null) {
                left.add(spot);
                continue;
            }
            healed++;
            Optional<Block> into = into(spot.was(), Config.section("corruption"));
            if (into.isPresent() && level.getBlockState(spot.pos()).is(into.get())) {
                level.setBlock(spot.pos(), spot.was(), Block.UPDATE_ALL);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, spot.pos().getX() + 0.5, spot.pos().getY() + 1.0, spot.pos().getZ() + 0.5, 1, 0.3, 0.1, 0.3, 0.0);
            }
        }
        List<HauntData.Blight.Column> standing = new ArrayList<>();
        Set<LevelChunk> touched = new HashSet<>();
        Registry<Biome> biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
        int restored = 0;
        for (HauntData.Blight.Column column : blight.columns()) {
            LevelChunk chunk = restored < count ? loaded(level, new BlockPos(QuartPos.toBlock(column.x()), 0, QuartPos.toBlock(column.z()))) : null;
            if (chunk == null) {
                standing.add(column);
                continue;
            }
            restored++;
            List<@Nullable Holder<Biome>> back = new ArrayList<>();
            for (int i = 0; i < column.was().size(); i++) {
                boolean still = chunk.getNoiseBiome(column.x(), column.y() + i, column.z()).is(BLIGHTED);
                back.add(still ? biomes.get(column.was().get(i)).orElse(null) : null);
            }
            paint(chunk, column.x(), column.y(), column.z(), back);
            touched.add(chunk);
        }
        resend(level, touched);
        if (healed > 0 || restored > 0) {
            data.putBlight(spirit, new HauntData.Blight(blight.dimension(), left, standing));
        }
    }

    /** What the configured {@code blocks} map turns this block into, if anything. */
    private static Optional<Block> into(BlockState state, Config.Section config) {
        Config.Section blocks = config.section("blocks");
        String from = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return blocks.has(from) ? BuiltInRegistries.BLOCK.getOptional(Identifier.parse(blocks.text(from))) : Optional.empty();
    }

    private static @Nullable LevelChunk loaded(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    /** How much of this spirit's land is fouled right now, as "spots/biome columns", or empty if none. */
    public static String size(HauntData data, UUID spirit) {
        HauntData.Blight blight = data.blight().get(spirit);
        return blight == null ? "" : blight.spots().size() + "/" + blight.columns().size();
    }
}
