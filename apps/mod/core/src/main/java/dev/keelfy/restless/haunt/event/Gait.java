package dev.keelfy.restless.haunt.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A figure's legs, a tick at a time: strides over the ground by hand as {@link Walk} does, but jumps up a step and
 * falls off a ledge as a player would instead of snapping, and goes round what is in its way. One per figure, since it
 * remembers whether the figure is in the air.
 */
final class Gait {
    /** A player's jump and fall, per tick, as vanilla moves them. */
    private static final double JUMP = 0.42;

    private static final double GRAVITY = 0.08;
    private static final double DRAG = 0.98;
    /** Ticks in the air before the figure is taken to have fallen somewhere it cannot get out of. */
    private static final int MAX_AIR = 60;
    /** Headings tried, off the one wanted, when that one is blocked. */
    private static final float[] DETOURS = {0.0F, 40.0F, -40.0F, 80.0F, -80.0F};

    private static final double EPSILON = 1.0E-3;

    private final int climb;
    private final int drop;
    private float heading;
    private double rise;
    private boolean air;
    private int airborne;

    Gait(int climb, int drop) {
        this.climb = climb;
        this.drop = drop;
    }

    /** The heading the figure last moved along, in degrees as entities face. */
    float heading() {
        return heading;
    }

    /** One stride toward {@code target}; see {@link #along}. */
    boolean toward(ServerLevel level, Entity figure, Vec3 target, double stride) {
        float yaw = (float) (Mth.atan2(target.z - figure.getZ(), target.x - figure.getX()) * Mth.RAD_TO_DEG) - 90.0F;
        return along(level, figure, yaw, stride);
    }

    /**
     * One tick's stride of {@code stride} blocks along {@code yaw}, or the nearest heading off it that has a way on. In
     * the air the jump or fall goes on along the heading it started on. Returns false, and does not move, when no
     * heading has standing room within {@code climb} blocks up or {@code drop} down.
     */
    boolean along(ServerLevel level, Entity figure, float yaw, double stride) {
        if (!air) {
            Integer ahead = null;
            for (float detour : DETOURS) {
                ahead = ahead(level, figure, yaw + detour, stride);
                if (ahead != null) {
                    heading = yaw + detour;
                    break;
                }
            }
            if (ahead == null) {
                return false;
            }
            if (Math.abs(ahead - figure.getY()) < EPSILON) {
                figure.setPos(x(figure, stride), figure.getY(), z(figure, stride));
                return true;
            }
            air = true;
            rise = ahead > figure.getY() ? JUMP : 0.0;
        }
        return fly(level, figure, stride);
    }

    boolean airborne() {
        return air;
    }

    /** A jump on the spot, if the figure is on the ground; it goes on with the next strides. */
    void hop() {
        if (!air) {
            air = true;
            rise = JUMP;
        }
    }

    private boolean fly(ServerLevel level, Entity figure, double stride) {
        if (++airborne > MAX_AIR) {
            return false;
        }
        double y = figure.getY() + rise;
        rise = (rise - GRAVITY) * DRAG;
        Integer ahead = ahead(level, figure, heading, stride);
        // Not yet over the step: up on the spot first, as a jump against a block goes.
        boolean clear = ahead != null && y >= ahead - EPSILON;
        double x = clear ? x(figure, stride) : figure.getX();
        double z = clear ? z(figure, stride) : figure.getZ();
        Integer under = Walk.ground(level, x, z, Mth.floor(Math.max(y, figure.getY()) + EPSILON), climb + drop);
        if (rise < 0 && under != null && y <= under) {
            y = under;
            air = false;
            airborne = 0;
        }
        figure.setPos(x, y, z);
        return true;
    }

    private @Nullable Integer ahead(ServerLevel level, Entity figure, float yaw, double stride) {
        double radians = Math.toRadians(yaw);
        return Walk.ground(
                level,
                figure.getX() - Math.sin(radians) * stride,
                figure.getZ() + Math.cos(radians) * stride,
                Mth.floor(figure.getY() + EPSILON) + climb,
                climb + drop);
    }

    private double x(Entity figure, double stride) {
        return figure.getX() - Math.sin(Math.toRadians(heading)) * stride;
    }

    private double z(Entity figure, double stride) {
        return figure.getZ() + Math.cos(Math.toRadians(heading)) * stride;
    }
}
