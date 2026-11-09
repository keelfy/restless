package dev.keelfy.restless.haunt;

import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/** Walks the loaded blocks of a cube. */
public final class Scan {
    private Scan() {}

    public static void each(ServerLevel level, BlockPos center, int radius, BiConsumer<BlockPos, BlockState> visit) {
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (level.isLoaded(pos)) {
                visit.accept(pos, level.getBlockState(pos));
            }
        }
    }

    /** Like {@link #each} but only over blocks that take random ticks, passing by the sections that have none. */
    public static void eachRandomTicking(
            ServerLevel level, BlockPos center, int radius, BiConsumer<BlockPos, BlockState> visit) {
        int minX = center.getX() - radius;
        int maxX = center.getX() + radius;
        int minY = Math.max(center.getY() - radius, level.getMinY());
        int maxY = Math.min(center.getY() + radius, level.getMaxY());
        int minZ = center.getZ() - radius;
        int maxZ = center.getZ() + radius;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int cx = SectionPos.blockToSectionCoord(minX); cx <= SectionPos.blockToSectionCoord(maxX); cx++) {
            for (int cz = SectionPos.blockToSectionCoord(minZ); cz <= SectionPos.blockToSectionCoord(maxZ); cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (int sy = SectionPos.blockToSectionCoord(minY); sy <= SectionPos.blockToSectionCoord(maxY); sy++) {
                    LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sy));
                    if (!section.isRandomlyTickingBlocks()) {
                        continue;
                    }
                    for (int x = Math.max(minX, cx << 4); x <= Math.min(maxX, (cx << 4) + 15); x++) {
                        for (int z = Math.max(minZ, cz << 4); z <= Math.min(maxZ, (cz << 4) + 15); z++) {
                            for (int y = Math.max(minY, sy << 4); y <= Math.min(maxY, (sy << 4) + 15); y++) {
                                BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                                if (state.isRandomlyTicking()) {
                                    visit.accept(at.set(x, y, z), state);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
