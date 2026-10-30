package dev.keelfy.restless.haunt.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/** Moves a figure over the ground by hand: no AI and no pathfinding, only a stride onto standing room. */
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
        return to(player.level(), figure, figure.getX() + dx / length * step, figure.getZ() + dz / length * step, climb, drop);
    }

    private static boolean to(ServerLevel level, Entity figure, double x, double z, int climb, int drop) {
        Integer ground = ground(level, x, z, Mth.floor(figure.getY()) + climb, climb + drop);
        if (ground == null) {
            return false;
        }
        figure.setPos(x, ground, z);
        return true;
    }

    /**
     * The height of the first spot in the column at {@code x}, {@code z}, from {@code top} down through {@code depth} more
     * blocks, with a floor and two free blocks; null if there is none.
     */
    static @Nullable Integer ground(ServerLevel level, double x, double z, int top, int depth) {
        BlockPos column = BlockPos.containing(x, top, z);
        for (int dy = 0; dy >= -depth; dy--) {
            BlockPos pos = column.above(dy);
            if (level.isLoaded(pos) && Place.open(level, pos) && Place.open(level, pos.above())
                && level.getBlockState(pos.below()).isSolidRender()) {
                return pos.getY();
            }
        }
        return null;
    }
}
