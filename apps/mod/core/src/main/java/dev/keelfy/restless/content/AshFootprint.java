package dev.keelfy.restless.content;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** A single, non-colliding impression at world coordinates; it never replaces or changes the ash below it. */
public final class AshFootprint extends Entity {
    private static final EntityDataAccessor<Integer> SHAPE =
            SynchedEntityData.defineId(AshFootprint.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> LEFT =
            SynchedEntityData.defineId(AshFootprint.class, EntityDataSerializers.BOOLEAN);
    public static final int LIFETIME = 2400;
    private int age;
    /** Ticks it lasts; a free print (not on ash) fades sooner than the ones in the powder. */
    private int life = LIFETIME;
    /** Whether it stands on no ash: it holds on its own, for {@link #life}. */
    private boolean free;

    public AshFootprint(EntityType<? extends AshFootprint> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SHAPE, 1);
        builder.define(LEFT, true);
    }

    public int shape() {
        return entityData.get(SHAPE);
    }

    public boolean left() {
        return entityData.get(LEFT);
    }

    /** Walk across the connected powder in half-block strides, revealing alternating feet eight ticks apart. */
    public static boolean leaveTrail(ServerLevel level, BlockPos origin, int shape, double angle) {
        Vec3 forward = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        Vec3 side = new Vec3(-forward.z, 0, forward.x);
        Vec3 center = new Vec3(origin.getX() + .5, origin.getY() + 1.0 / 16 + .002, origin.getZ() + .5);
        int first = 0;
        for (int step = -1; step >= -12; step--) {
            if (!covered(level, center.add(forward.scale(step * .55)), angle)) {
                break;
            }
            first = step;
        }
        int count = 0;
        for (int step = first; step <= 12; step++) {
            boolean left = (count & 1) == 0;
            Vec3 at = center.add(forward.scale(step * .55)).add(side.scale(left ? -.13 : .13));
            if (!covered(level, at, angle)) {
                // A lateral offset can miss the edge even though the centre line is still on ash.
                if (!covered(level, center.add(forward.scale(step * .55)), angle)) {
                    break;
                }
                continue;
            }
            @SuppressWarnings("unchecked")
            var type = (EntityType<AshFootprint>) Content.ASH_FOOTPRINT.get();
            AshFootprint print = new AshFootprint(type, level);
            print.setPos(at);
            // Local -Z points toward the toes of the texture.
            print.setYRot((float) (angle * 180 / Math.PI + 90));
            print.entityData.set(SHAPE, Math.clamp(shape, 1, AshBlock.PRINTS));
            print.entityData.set(LEFT, left);
            print.age = -count * 8;
            print.setInvisible(count > 0);
            if (level.addFreshEntity(print)) {
                count++;
            }
        }
        return count > 0;
    }

    /**
     * Sets down one print on any ground, which fades after {@code ticks}: the trail of Wayfinder's Ash, laid where the
     * dead walked, with no powder under it.
     */
    public static boolean lay(ServerLevel level, Vec3 at, double angle, int shape, boolean left, int ticks) {
        @SuppressWarnings("unchecked")
        var type = (EntityType<AshFootprint>) Content.ASH_FOOTPRINT.get();
        AshFootprint print = new AshFootprint(type, level);
        print.setPos(at);
        print.setYRot((float) (angle * 180 / Math.PI + 90));
        print.entityData.set(SHAPE, Math.clamp(shape, 1, AshBlock.PRINTS));
        print.entityData.set(LEFT, left);
        print.free = true;
        print.life = ticks;
        return level.addFreshEntity(print);
    }

    /** All corners must lie on powder, including when a print crosses a block boundary. */
    private static boolean covered(Level level, Vec3 at, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        for (double along : new double[] {-.25, .25}) {
            for (double across : new double[] {-.25, .25}) {
                BlockPos pos =
                        BlockPos.containing(at.x + cos * along - sin * across, at.y, at.z + sin * along + cos * across);
                if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(Content.ASH_LINE.get())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            age++;
            if (age >= life || (!free && !covered(level(), position(), Math.toRadians(getYRot() - 90)))) {
                discard();
            } else if (age >= 0 && isInvisible()) {
                setInvisible(false);
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        age = input.getIntOr("Age", 0);
        life = input.getIntOr("Life", LIFETIME);
        free = input.getBooleanOr("Free", false);
        entityData.set(SHAPE, Math.clamp(input.getIntOr("Shape", 1), 1, AshBlock.PRINTS));
        entityData.set(LEFT, input.getBooleanOr("Left", true));
        setInvisible(age < 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Age", age);
        output.putInt("Life", life);
        output.putBoolean("Free", free);
        output.putInt("Shape", shape());
        output.putBoolean("Left", left());
    }
}
