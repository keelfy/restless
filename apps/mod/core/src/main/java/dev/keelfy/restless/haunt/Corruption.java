package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * A territorial spirit left alone fouls the land around its anchor while that chunk is loaded: grass dies, flowers and ferns wither to dead bushes.
 * The reach grows with the spirit's age, an evolved one spreads faster. Every spot remembers what stood there, and once the
 * spirit is gone (banished, at rest or removed) the land heals a little every check, wherever its chunk is loaded.
 */
public final class Corruption {
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
            if (level != null && loaded(level, spirit.anchor())) {
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
        HauntData.Blight blight = data.blight().getOrDefault(id, new HauntData.Blight(spirit.dimension(), List.of()));
        List<HauntData.Blight.Spot> spots = new ArrayList<>(blight.spots());
        double reach = Power.blightReach(now - spirit.haunting().bornAt(), spirit.radius());
        int tries = config.integer("tries") * (spirit.haunting().evolved() ? config.integer("evolved_factor") : 1);
        RandomSource random = level.getRandom();
        for (int i = 0; i < tries && spots.size() < config.integer("max_blocks"); i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = Math.sqrt(random.nextDouble()) * reach;
            BlockPos column = spirit.anchor().offset((int) Math.round(Math.cos(angle) * distance), 0, (int) Math.round(Math.sin(angle) * distance));
            if (!loaded(level, column)) {
                continue;
            }
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
        if (spots.size() != blight.spots().size()) {
            data.putBlight(id, new HauntData.Blight(spirit.dimension(), spots));
        }
    }

    /** Puts back up to {@code count} spots whose chunks are loaded; a spot changed since by something else is let go. */
    private static void heal(MinecraftServer server, HauntData data, UUID spirit, HauntData.Blight blight, int count) {
        ServerLevel level = server.getLevel(blight.dimension());
        if (level == null) {
            data.putBlight(spirit, null);
            return;
        }
        List<HauntData.Blight.Spot> left = new ArrayList<>();
        int healed = 0;
        for (HauntData.Blight.Spot spot : blight.spots()) {
            if (healed >= count || !loaded(level, spot.pos())) {
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
        if (healed > 0) {
            data.putBlight(spirit, new HauntData.Blight(blight.dimension(), left));
        }
    }

    /** What the configured {@code blocks} map turns this block into, if anything. */
    private static Optional<Block> into(BlockState state, Config.Section config) {
        Config.Section blocks = config.section("blocks");
        String from = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return blocks.has(from) ? BuiltInRegistries.BLOCK.getOptional(Identifier.parse(blocks.text(from))) : Optional.empty();
    }

    private static boolean loaded(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())) != null;
    }

    /** How many spots of this spirit's land are fouled right now. */
    public static int size(HauntData data, UUID spirit) {
        HauntData.Blight blight = data.blight().get(spirit);
        return blight == null ? 0 : blight.spots().size();
    }
}
