package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Scheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A thrown rag. What it strikes, if a mob that is no boss, is smeared for a while and every mob of its kind near it
 * goes for it; the rag is spent either way.
 */
public final class RagEntity extends ThrowableItemProjectile {
    private static final TagKey<EntityType<?>> BOSSES =
            TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("c", "bosses"));

    public RagEntity(EntityType<? extends RagEntity> type, Level level) {
        super(type, level);
    }

    public RagEntity(ServerLevel level, LivingEntity owner, ItemStack stack) {
        super(entityType(), owner, level, stack);
    }

    @SuppressWarnings("unchecked") // registered as this class
    private static EntityType<? extends RagEntity> entityType() {
        return (EntityType<? extends RagEntity>) Content.RAG_ENTITY.get();
    }

    @Override
    protected Item getDefaultItem() {
        return Content.BLOODIED_RAG.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level() instanceof ServerLevel level
                && hit.getEntity() instanceof Mob mob
                && !boss(mob)
                && getOwner() instanceof ServerPlayer thrower) {
            smear(thrower, level, mob);
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide()) {
            level().broadcastEntityEvent(this, EntityEvent.DEATH);
            discard();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EntityEvent.DEATH) {
            for (int i = 0; i < 8; i++) {
                level().addParticle(ParticleTypes.DAMAGE_INDICATOR, getX(), getY(), getZ(), 0, 0, 0);
            }
        }
    }

    private static boolean boss(Mob mob) {
        return mob instanceof WitherBoss
                || mob instanceof EnderDragon
                || BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(mob.getType()).is(BOSSES);
    }

    /** Marks {@code victim}: for a while every mob of its type within reach targets it, until it dies. */
    private static void smear(ServerPlayer thrower, ServerLevel level, Mob victim) {
        Config.Section config = Config.section("consumables");
        int ticks = config.integer("rag_ticks");
        victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, true, false));
        level.playSound(null, victim.blockPosition(), SoundEvents.WARDEN_AGITATED, SoundSource.HOSTILE, 0.8f, 1.6f);
        int every = 10;
        Scheduler.repeat(thrower, 0, every, (p, n) -> {
            if (!victim.isAlive() || victim.level() != level) {
                return false;
            }
            for (Mob kin : level.getEntitiesOfClass(
                    Mob.class,
                    victim.getBoundingBox().inflate(config.num("rag_radius")),
                    m -> m != victim && m.getType() == victim.getType() && m.isAlive())) {
                kin.setTarget(victim);
            }
            return (n + 1) * every < ticks;
        });
    }
}
