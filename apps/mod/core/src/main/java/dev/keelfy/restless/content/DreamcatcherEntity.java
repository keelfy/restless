package dev.keelfy.restless.content;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** A damped pendulum. Only contact impulses are sent; motion is transient on both sides. */
public final class DreamcatcherEntity extends BlockEntity {
    private static final int TOUCH_EVENT = 1;
    private final Swing x = new Swing();
    private final Swing z = new Swing();
    private long nextTouchTick;

    public DreamcatcherEntity(BlockPos pos, BlockState state) {
        super(Content.DREAMCATCHER_ENTITY.get(), pos, state);
    }

    public void touch(Vec3 movement) {
        if (level == null || level.isClientSide() || level.getGameTime() < nextTouchTick) return;
        int pushX = Mth.clamp((int) Math.round(movement.x * 20), -7, 7);
        int pushZ = Mth.clamp((int) Math.round(movement.z * 20), -7, 7);
        if (pushX == 0 && pushZ == 0) return;
        nextTouchTick = level.getGameTime() + 6;
        // A block event carries one unsigned byte: two signed components biased by seven.
        level.blockEvent(worldPosition, getBlockState().getBlock(), TOUCH_EVENT, (pushX + 7) | ((pushZ + 7) << 4));
    }

    @Override
    public boolean triggerEvent(int event, int data) {
        if (event != TOUCH_EVENT) return super.triggerEvent(event, data);
        // Below the pivot, positive X rotation moves toward -Z; positive Z moves toward +X.
        x.velocity -= ((data >> 4 & 15) - 7) * .75f;
        z.velocity += ((data & 15) - 7) * .75f;
        return true;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DreamcatcherEntity entity) {
        entity.x.tick();
        entity.z.tick();
    }

    public float swingX(float partialTicks) {
        return Mth.lerp(partialTicks, x.previous, x.angle);
    }

    public float swingZ(float partialTicks) {
        return Mth.lerp(partialTicks, z.previous, z.angle);
    }

    private static final class Swing {
        private float previous;
        private float angle;
        private float velocity;

        private void tick() {
            previous = angle;
            velocity = (velocity - angle * .06f) * .92f;
            angle += velocity;
            if (Math.abs(angle) > 25) {
                angle = Mth.clamp(angle, -25, 25);
                velocity = 0;
            }
            if (Math.abs(angle) < .01f && Math.abs(velocity) < .01f) {
                angle = 0;
                velocity = 0;
            }
        }
    }
}
