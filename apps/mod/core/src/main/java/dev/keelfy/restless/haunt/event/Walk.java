package dev.keelfy.restless.haunt.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Moves a figure over the ground by hand: no AI and no pathfinding, only a stride onto standing room toward the player. */
final class Walk {
    private Walk() {}

    /**
     * Steps {@code figure} up to {@code stride} blocks toward the player, onto the first spot from {@code climb} above
     * to {@code drop} below that has a floor and two free blocks. Returns false, and does not move, if there is none.
     */
    static boolean toward(ServerPlayer player, Entity figure, double stride, int climb, int drop) {
        double dx = player.getX() - figure.getX();
        double dz = player.getZ() - figure.getZ();
        double length = Math.max(Math.hypot(dx, dz), 1.0E-4);
        double step = Math.min(stride, length);
        double x = figure.getX() + dx / length * step;
        double z = figure.getZ() + dz / length * step;
        ServerLevel level = player.level();
        BlockPos column = BlockPos.containing(x, figure.getY(), z);
        for (int dy = climb; dy >= -drop; dy--) {
            BlockPos pos = column.above(dy);
            if (level.isLoaded(pos) && level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above())
                && level.getBlockState(pos.below()).isSolidRender()) {
                figure.setPos(x, pos.getY(), z);
                return true;
            }
        }
        return false;
    }
}
