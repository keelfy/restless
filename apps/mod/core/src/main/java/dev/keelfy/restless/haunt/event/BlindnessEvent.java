package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntTier;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class BlindnessEvent implements HauntEvent {
    @Override
    public String id() {
        return "blindness";
    }

    @Override
    public HauntTier tier() {
        return HauntTier.LIGHT;
    }

    @Override
    public Set<EventTag> tags() {
        return Set.of(EventTag.AFFLICTION);
    }

    @Override
    public double baseWeight() {
        return 0.6;
    }

    @Override
    public boolean canRun(ServerPlayer player) {
        return !player.hasEffect(MobEffects.BLINDNESS);
    }

    @Override
    public void run(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 * 6), null);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 0.8F);
    }
}
