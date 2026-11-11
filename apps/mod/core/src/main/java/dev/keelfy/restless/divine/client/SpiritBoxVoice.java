package dev.keelfy.restless.divine.client;

import com.mojang.logging.LogUtils;
import dev.keelfy.restless.Restless;
import dev.keelfy.restless.divine.SpiritBoxPayload;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.slf4j.Logger;

/**
 * Speaks what a spirit box answers, one part after another from the box: recorded clips in the game's language, and the
 * asker's own voice played raw through OpenAL, as the sound engine only plays files.
 */
public final class SpiritBoxVoice {
    private static final Logger LOG = LogUtils.getLogger();
    /** Languages the clips are recorded in; any other hears the first. */
    private static final List<String> LANGUAGES = List.of("en_us", "ru_ru");
    /** Ticks of silence between two parts. */
    private static final int PAUSE = 5;
    /** A spirit box is heard this far, as a clip is. */
    private static final float RANGE = 16;

    private record Queued(BlockPos box, SpiritBoxPayload.Part part) {}

    /** A voice playing through its own OpenAL source and buffer. */
    private record Raw(int source, int buffer) {}

    private static final Deque<Queued> QUEUE = new ArrayDeque<>();
    private static final List<Raw> RAW = new ArrayList<>();
    private static @Nullable SoundInstance playing;
    private static int wait;

    private SpiritBoxVoice() {}

    public static void say(SpiritBoxPayload payload) {
        payload.parts().forEach(part -> QUEUE.add(new Queued(payload.box(), part)));
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        RAW.removeIf(SpiritBoxVoice::done);
        if (minecraft.level == null) {
            QUEUE.clear();
            playing = null;
            return;
        }
        if (playing != null && minecraft.getSoundManager().isActive(playing)) {
            return;
        }
        playing = null;
        if (wait > 0) {
            wait--;
            return;
        }
        Queued next = QUEUE.poll();
        if (next == null) {
            return;
        }
        wait = PAUSE;
        if (next.part().pcm().length > 0) {
            wait += raw(next.box(), next.part().pcm());
        } else {
            playing = new SimpleSoundInstance(
                    SoundEvent.createVariableRangeEvent(clip(next.part().voice())),
                    SoundSource.BLOCKS,
                    1f,
                    1f,
                    SoundInstance.createUnseededRandom(),
                    next.box());
            minecraft.getSoundManager().play(playing);
        }
    }

    /** The clip of a voice in the game's language, or static when it was never recorded. */
    private static Identifier clip(String voice) {
        Minecraft minecraft = Minecraft.getInstance();
        String selected = minecraft.getLanguageManager().getSelected();
        String language = LANGUAGES.contains(selected) ? selected : LANGUAGES.getFirst();
        Identifier id = Identifier.fromNamespaceAndPath(Restless.MOD_ID, "spirit_box." + language + "." + voice);
        boolean recorded = minecraft.getSoundManager().getSoundEvent(id) != null;
        return recorded ? id : Identifier.fromNamespaceAndPath(Restless.MOD_ID, "spirit_box.hiss");
    }

    /** Plays raw samples from the box; how many ticks they last, none when the sound is off. */
    private static int raw(BlockPos box, byte[] pcm) {
        float volume = Minecraft.getInstance().options.getFinalSoundSourceVolume(SoundSource.BLOCKS);
        if (volume <= 0) {
            return 0;
        }
        try {
            int buffer = AL10.alGenBuffers();
            ByteBuffer data = BufferUtils.createByteBuffer(pcm.length).order(ByteOrder.LITTLE_ENDIAN);
            AL10.alBufferData(buffer, AL10.AL_FORMAT_MONO16, data.put(pcm).flip(), SpiritBoxPayload.RATE);
            int source = AL10.alGenSources();
            AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
            AL10.alSource3f(source, AL10.AL_POSITION, box.getX() + 0.5f, box.getY() + 0.5f, box.getZ() + 0.5f);
            // As the sound engine places a clip: loudest at the box, gone at its range.
            AL10.alSourcei(source, AL10.AL_DISTANCE_MODEL, AL11.AL_LINEAR_DISTANCE);
            AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, RANGE);
            AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, 1);
            AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, 0);
            AL10.alSourcef(source, AL10.AL_GAIN, volume);
            AL10.alSourcePlay(source);
            RAW.add(new Raw(source, buffer));
            return pcm.length / 2 * 20 / SpiritBoxPayload.RATE;
        } catch (RuntimeException e) {
            // No sound device: the box is only read.
            LOG.debug("Restless could not play a spirit box echo", e);
            return 0;
        }
    }

    private static boolean done(Raw raw) {
        if (AL10.alGetSourcei(raw.source(), AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) {
            return false;
        }
        AL10.alDeleteSources(raw.source());
        AL10.alDeleteBuffers(raw.buffer());
        return true;
    }
}
