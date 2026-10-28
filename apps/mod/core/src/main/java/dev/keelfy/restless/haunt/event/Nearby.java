package dev.keelfy.restless.haunt.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

final class Nearby {
    private Nearby() {}

    /** Random loaded block within a cube of the given radius around the player that matches. */
    static @Nullable BlockPos random(ServerPlayer player, int radius, Predicate<BlockState> match) {
        BlockPos center = player.blockPosition();
        List<BlockPos> hits = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (player.level().isLoaded(pos) && match.test(player.level().getBlockState(pos))) {
                hits.add(pos.immutable());
            }
        }
        return hits.isEmpty() ? null : hits.get(player.getRandom().nextInt(hits.size()));
    }
}
