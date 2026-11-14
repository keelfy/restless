package dev.keelfy.restless.banish.client;

import dev.keelfy.restless.banish.Phase;
import dev.keelfy.restless.banish.RitualPayload;
import dev.keelfy.restless.config.Config;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/** The client side of a ritual's phases: knows which one runs and marks each new one with a sound. */
public final class RitualCue {
    private static Optional<Phase> phase = Optional.empty();

    private RitualCue() {}

    /** The phase of the ritual this player holds, empty when none runs. */
    public static Optional<Phase> phase() {
        return phase;
    }

    public static void receive(RitualPayload payload) {
        phase = payload.current();
        phase.ifPresent(now -> {
            Config.Tone tone = Config.section("ritual").tone("phase_sound");
            Minecraft.getInstance()
                    .getSoundManager()
                    .play(SimpleSoundInstance.forUI(sound(now), tone.pitch(), tone.volume()));
        });
    }

    private static SoundEvent sound(Phase phase) {
        return switch (phase) {
            case CALL -> SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value();
            case BARGAIN -> SoundEvents.AMBIENT_CAVE.value();
            case FURY -> SoundEvents.WARDEN_HEARTBEAT;
            case LEAVING -> SoundEvents.SOUL_ESCAPE.value();
        };
    }
}
