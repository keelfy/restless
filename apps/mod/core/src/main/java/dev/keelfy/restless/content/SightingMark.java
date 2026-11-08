package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Spells;
import java.util.UUID;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A pale figure that marks a spot. It glows, so it shows through walls as far as the game sends it, it takes no blows
 * and nothing targets it, and it fades after {@code consumables.mark_ticks}. A player may have only so many.
 */
public final class SightingMark extends Entity implements ItemSupplier {
    private @Nullable UUID owner;
    private long until;

    public SightingMark(EntityType<? extends SightingMark> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setGlowingTag(true);
    }

    /** Sets a mark down for {@code owner} at the entity's place; it lasts the config's time from now. */
    public void setUp(UUID owner, long now) {
        this.owner = owner;
        this.until = now + Config.section("consumables").integer("mark_ticks");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public ItemStack getItem() {
        return new ItemStack(Content.SIGHTING_MARK.get());
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel level && level.getGameTime() >= until) {
            discard();
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (owner != null && reason.shouldDestroy() && level() instanceof ServerLevel level) {
            Spells.get(level.getServer()).unmark(owner, getUUID());
        }
        super.remove(reason);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        owner = input.read("Owner", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
        until = input.getLongOr("Until", 0L);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (owner != null) {
            output.store("Owner", net.minecraft.core.UUIDUtil.CODEC, owner);
        }
        output.putLong("Until", until);
    }
}
