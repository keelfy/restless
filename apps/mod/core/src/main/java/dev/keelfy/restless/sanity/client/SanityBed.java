package dev.keelfy.restless.sanity.client;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.HauntSounds;
import dev.keelfy.restless.sanity.SanitySounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import org.jspecify.annotations.Nullable;

/** Low sanity heard: a looping bed that deepens as sanity falls, and a heartbeat when it is nearly gone. */
public final class SanityBed {
    private static @Nullable Bed bed;
    private static int gap;

    private SanityBed() {}

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            bed = null;
            gap = 0;
            return;
        }
        Config.Section config = Config.section("sanity");
        double sanity = SanityOverlay.value();
        if ((bed == null || bed.isStopped())
                && SanitySounds.bedVolume(sanity, config.num("overlay_below"), config.num("bed_volume")) > 0) {
            bed = new Bed();
            minecraft.getSoundManager().play(bed);
        }
        if (gap > 0) {
            gap--;
        }
        if (gap == 0 && SanitySounds.heartbeat(sanity, config.num("heartbeat_below"))) {
            gap = config.integer("heartbeat_gap_ticks");
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(HauntSounds.HEARTBEAT, 1f, (float)
                    config.num("heartbeat_volume")));
        }
    }

    /** The bed itself: heard everywhere at once, stops when sanity recovers or the world closes. */
    private static final class Bed extends AbstractTickableSoundInstance {
        Bed() {
            super(HauntSounds.SANITY_BED, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            looping = true;
            relative = true;
            attenuation = SoundInstance.Attenuation.NONE;
            volume = 0.0f;
        }

        /** Starts at volume 0 and rises on its first tick; the sound engine skips a silent start otherwise. */
        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            Config.Section config = Config.section("sanity");
            double target = Minecraft.getInstance().level == null
                    ? 0
                    : SanitySounds.bedVolume(
                            SanityOverlay.value(), config.num("overlay_below"), config.num("bed_volume"));
            if (target <= 0) {
                stop();
            }
            volume = (float) target;
        }
    }
}
