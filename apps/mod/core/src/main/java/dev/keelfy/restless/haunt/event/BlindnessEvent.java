package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntTier;
import dev.keelfy.restless.haunt.Ticks;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class BlindnessEvent implements HauntEvent {
    @Override
    public Identifier id() {
        return Identifier.parse("blindness");
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
    public boolean canRun(ServerPlayer player) {
        return !player.hasEffect(MobEffects.BLINDNESS);
    }

    @Override
    public void run(ServerPlayer player) {
        Config.Section config = Config.event("blindness");
        Config.Tone tone = config.tone("sound");
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, config.integer("seconds") * Ticks.SECOND), null);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, tone.volume(), tone.pitch());
    }
}
