package dev.keelfy.restless.content.entity;

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
 * A spirit's own body: pale, see-through, shaped by its type and seed as its photograph is (see
 * {@code photo.Apparition}). Like {@link Silhouette} it exists only on the client of a player shown it (see
 * {@code divine.Body}); the server never adds one to a world.
 */
public final class SpiritBody extends Entity {
    private static final EntityDataAccessor<String> KIND =
            SynchedEntityData.defineId(SpiritBody.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Long> SEED =
            SynchedEntityData.defineId(SpiritBody.class, EntityDataSerializers.LONG);

    public SpiritBody(EntityType<? extends SpiritBody> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, "");
        builder.define(SEED, 0L);
    }

    /** The spirit's type id. */
    public String kind() {
        return entityData.get(KIND);
    }

    public long seed() {
        return entityData.get(SEED);
    }

    public void setSpirit(String kind, long seed) {
        entityData.set(KIND, kind);
        entityData.set(SEED, seed);
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
