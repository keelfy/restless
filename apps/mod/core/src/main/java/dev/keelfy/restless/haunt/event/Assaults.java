package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import java.util.List;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Direct physical harm. Every event leaves the player alive; lethal ones need a telegraph and come later. */
final class Assaults {
    private Assaults() {}

    static List<HauntEvent> all() {
        return List.of(
            // 53: freeze the legs for a second
            SimpleEvent.harmful("trip", EventTag.ASSAULT, 0.7, player -> !player.hasEffect(MobEffects.SLOWNESS), player -> {
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 6), null);
                player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_SMALL_FALL, SoundSource.HOSTILE, 1.0F, 0.7F);
            }),
            // 54
            SimpleEvent.harmful("scorch", EventTag.ASSAULT, 0.6,
                player -> player.getHealth() > 6.0F && !player.isInWater() && !player.isOnFire() && !player.fireImmune(),
                player -> player.igniteForSeconds(1.0F)),
            // 55: like cold_touch, harder
            SimpleEvent.harmful("wrath", EventTag.ASSAULT, 0.6, player -> player.getHealth() > 8.0F, player -> {
                float damage = Math.min(4.0F + player.getRandom().nextInt(4), player.getHealth() - 1.0F);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WARDEN_NEARBY_CLOSE, SoundSource.HOSTILE, 1.0F, 0.8F);
                player.hurtServer(player.level(), player.damageSources().magic(), damage);
            }),
            // 56
            SimpleEvent.harmful("snatch", EventTag.ASSAULT, 0.7, player -> !player.getMainHandItem().isEmpty(), Assaults::snatch)
        );
    }

    private static void snatch(ServerPlayer player) {
        ItemStack held = player.getMainHandItem().copy();
        player.getInventory().setItem(player.getInventory().getSelectedSlot(), ItemStack.EMPTY);
        double angle = player.getRandom().nextDouble() * Math.PI * 2;
        // Launch speed chosen so the item lands roughly five blocks away.
        Vec3 velocity = new Vec3(Math.cos(angle) * 0.45, 0.3, Math.sin(angle) * 0.45);
        ItemEntity item = new ItemEntity(player.level(), player.getX(), player.getEyeY(), player.getZ(), held, velocity.x, velocity.y, velocity.z);
        item.setDefaultPickUpDelay();
        player.level().addFreshEntity(item);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ITEM_PICKUP, SoundSource.HOSTILE, 1.0F, 0.5F);
    }
}
