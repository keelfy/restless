package dev.keelfy.restless.content.entity;

import dev.keelfy.restless.content.client.SilhouetteRenderer;
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

/**
 * A black figure of no known kind, in one of {@link SilhouetteRenderer#SHAPES} wrong shapes. It exists only on the
 * client of the player a haunt event shows it to (see {@code PacketEntity}); the server never adds one to a world.
 */
public final class Silhouette extends Entity {
    private static final EntityDataAccessor<Integer> SHAPE =
            SynchedEntityData.defineId(Silhouette.class, EntityDataSerializers.INT);

    public Silhouette(EntityType<? extends Silhouette> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SHAPE, 0);
    }

    public int shape() {
        return entityData.get(SHAPE);
    }

    public void setShape(int shape) {
        entityData.set(SHAPE, shape);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}
}
