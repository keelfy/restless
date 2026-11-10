package dev.keelfy.restless.content.entity;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.state.Session;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
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
 * A thrown rag. What it strikes, if a mob that is no boss, is smeared until it dies and every mob of its kind near it
 * goes for it; the rag is spent either way. The smear is kept for the running world only.
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
                && getOwner() instanceof ServerPlayer) {
            smear(level, mob);
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

    /** The mobs a rag has smeared: their kin go for them until they die. */
    private static final List<Mob> SMEARED = Session.SERVER.keep(new ArrayList<>());
    /** How often {@link #tick} wants calling; the glow outlasts a pass. */
    public static final int EVERY = 10;

    /** Marks {@code victim}: every mob of its type within reach targets it, until it dies. */
    private static void smear(ServerLevel level, Mob victim) {
        if (!SMEARED.contains(victim)) {
            SMEARED.add(victim);
        }
        level.playSound(null, victim.blockPosition(), SoundEvents.WARDEN_AGITATED, SoundSource.HOSTILE, 0.8f, 1.6f);
    }

    /** Call every {@link #EVERY} ticks: the smeared glow, and their kin go for them. Dead ones are forgotten. */
    public static void tick(MinecraftServer server) {
        double radius = Config.section("consumables").num("rag_radius");
        SMEARED.removeIf(victim -> {
            if (!victim.isAlive() || !(victim.level() instanceof ServerLevel level)) {
                return true;
            }
            victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, EVERY * 3, 0, true, false));
            for (Mob kin : level.getEntitiesOfClass(
                    Mob.class,
                    victim.getBoundingBox().inflate(radius),
                    m -> m != victim && m.getType() == victim.getType() && m.isAlive())) {
                kin.setTarget(victim);
            }
            return false;
        });
    }
}
