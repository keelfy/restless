package dev.keelfy.restless.banish.client;

import dev.keelfy.restless.banish.Phase;
import dev.keelfy.restless.banish.RitualPayload;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.HauntSounds;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.jspecify.annotations.Nullable;

/**
 * The client side of a ritual's phases: knows which one runs and marks each new one with a sound. The fury is a loop
 * that lasts as long as the phase.
 */
public final class RitualCue {
    private static Optional<Phase> phase = Optional.empty();
    private static @Nullable FuryBed fury;

    private RitualCue() {}

    /** The phase of the ritual this player holds, empty when none runs. */
    public static Optional<Phase> phase() {
        return phase;
    }

    public static void receive(RitualPayload payload) {
        phase = payload.current();
        phase.ifPresent(now -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (now == Phase.FURY) {
                if (fury == null || fury.isStopped()) {
                    fury = new FuryBed();
                    minecraft.getSoundManager().play(fury);
                }
                return;
            }
            Config.Tone tone = Config.section("ritual").tone("phase_sound");
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound(now), tone.pitch(), tone.volume()));
        });
    }

    private static SoundEvent sound(Phase phase) {
        return switch (phase) {
            case CALL -> SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value();
            case BARGAIN -> SoundEvents.AMBIENT_CAVE.value();
            case FURY -> HauntSounds.RITUAL_FURY;
            case LEAVING -> HauntSounds.RITUAL_LEAVING;
        };
    }

    /** The fury's loop, on the player; it stops itself on any other phase or when the ritual ends. */
    private static final class FuryBed extends AbstractTickableSoundInstance {
        FuryBed() {
            super(HauntSounds.RITUAL_FURY, SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
            looping = true;
            relative = true;
            attenuation = SoundInstance.Attenuation.NONE;
            volume = (float) Config.section("ritual").num("fury_bed_volume");
        }

        @Override
        public void tick() {
            if (!Phase.furyHeard(phase) || Minecraft.getInstance().level == null) {
                stop();
            }
        }
    }
}
