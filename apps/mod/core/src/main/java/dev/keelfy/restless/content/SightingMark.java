package dev.keelfy.restless.content;

import dev.keelfy.restless.haunt.Spells;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A mark on a spot, drawn on every screen that has it as a waypoint, see {@link SightingMarkHud}: it shows through
 * walls as far as the game sends it. It takes no blows and stands until somebody takes it up again with a use of the
 * hand, whoever that is, and the mark comes back as an item. A player may have only so many.
 */
public final class SightingMark extends Entity implements ItemSupplier {
    private @Nullable UUID owner;

    public SightingMark(EntityType<? extends SightingMark> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /** Sets a mark down for {@code owner} at the entity's place. */
    public void setUp(UUID owner) {
        this.owner = owner;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public ItemStack getItem() {
        return new ItemStack(Content.SIGHTING_MARK.get());
    }

    /** Taking it up: the mark goes to the hand's owner, still wearing its name, and no longer counts. */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!(level() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = getItem();
        if (hasCustomName()) {
            stack.set(DataComponents.CUSTOM_NAME, getCustomName());
        }
        level.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0f, 1.4f);
        discard();
        if (!player.addItem(stack)) {
            Block.popResource(level, blockPosition(), stack);
        }
        return InteractionResult.SUCCESS_SERVER;
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
        return true;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        owner = input.read("Owner", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (owner != null) {
            output.store("Owner", net.minecraft.core.UUIDUtil.CODEC, owner);
        }
    }
}
