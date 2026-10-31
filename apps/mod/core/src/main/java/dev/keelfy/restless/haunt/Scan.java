package dev.keelfy.restless.haunt;

import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

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
}
