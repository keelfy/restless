package dev.keelfy.restless.neoforge.compat;

import com.mojang.logging.LogUtils;
import dev.keelfy.restless.Restless;
import dev.keelfy.restless.divine.Divination;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import su.plo.voice.api.addon.AddonLoaderScope;
import su.plo.voice.api.addon.InjectPlasmoVoice;
import su.plo.voice.api.addon.ServerAddonsLoader;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.audio.codec.AudioDecoder;
import su.plo.voice.api.encryption.Encryption;
import su.plo.voice.api.event.EventPriority;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.event.audio.source.PlayerSpeakEndEvent;
import su.plo.voice.api.server.event.audio.source.PlayerSpeakEvent;
import su.plo.voice.proto.packets.udp.serverbound.PlayerAudioPacket;

/**
 * Plasmo Voice add-on: the spirit box hears what a player says on voice chat as it does a chat line. While a player
 * near a spirit box speaks, the phrase is decoded and, when it ends, made out by {@link Transcriber}.
 */
@Addon(id = Restless.MOD_ID, scope = AddonLoaderScope.SERVER, version = "", authors = "keelfy")
public final class VoiceAddon {
    private static final Logger LOG = LogUtils.getLogger();
    /** Vosk models hear 16 kHz; voice chat comes at a multiple of it, so it is averaged down. */
    private static final int RATE = 16_000;
    /** Longer phrases are cut: the box answers a question, not a speech. */
    private static final int MOST_SECONDS = 20;

    @InjectPlasmoVoice
    private PlasmoVoiceServer voice;

    /** The phrase each player is saying right now; written by voice threads. */
    private final Map<UUID, Phrase> phrases = new ConcurrentHashMap<>();

    /** Only called with Plasmo Voice present: this class is not loaded without it. */
    public static void register() {
        ServerAddonsLoader.INSTANCE.load(new VoiceAddon());
    }

    /** One packet of a phrase; comes on a voice thread. */
    @EventSubscribe(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onSpeak(@NotNull PlayerSpeakEvent event) {
        UUID id = event.getPlayer().getInstance().getUuid();
        PlayerAudioPacket packet = event.getPacket();
        phrases.computeIfAbsent(id, k -> start(k, packet.isStereo())).add(packet.getData());
    }

    /** The event comes on a voice thread; the cancelled one is still the phrase's end, so it is not skipped. */
    @EventSubscribe(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onSpeakEnd(@NotNull PlayerSpeakEndEvent event) {
        UUID id = event.getPlayer().getInstance().getUuid();
        Phrase phrase = phrases.remove(id);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (phrase == null || server == null) {
            return;
        }
        short[] samples = phrase.end();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null || !Divination.listening(player)) {
                return;
            }
            Transcriber.transcribe(
                    samples,
                    phrase.rate,
                    player.clientInformation().language(),
                    heard -> server.execute(() -> {
                        ServerPlayer speaker = server.getPlayerList().getPlayer(id);
                        if (speaker != null) {
                            Restless.onVoice(speaker, heard);
                        }
                    }));
        });
    }

    /** A new phrase; the server says on its own thread whether a spirit box hears it, until then it is kept. */
    private Phrase start(UUID id, boolean stereo) {
        int rate = voice.getConfig().voice().sampleRate();
        Phrase phrase = new Phrase(voice.createOpusDecoder(stereo), voice.getDefaultEncryption(), stereo, rate);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                phrase.heard(player != null && Divination.listening(player));
            });
        }
        return phrase;
    }

    /** Decoded mono samples of one phrase, at {@link #rate}. */
    private static final class Phrase {
        private final AudioDecoder decoder;
        private final Encryption encryption;
        private final boolean stereo;
        /** How many samples of the voice chat make one of the phrase. */
        private final int step;

        final int rate;
        private short[] samples;
        private int size;
        /** Null until the server says; a phrase no spirit box hears is not decoded. */
        private Boolean heard;

        Phrase(AudioDecoder decoder, Encryption encryption, boolean stereo, int chatRate) {
            this.decoder = decoder;
            this.encryption = encryption;
            this.stereo = stereo;
            this.step = chatRate % RATE == 0 ? chatRate / RATE : 1;
            this.rate = chatRate / step;
            this.samples = new short[rate];
        }

        synchronized void heard(boolean heard) {
            this.heard = heard;
        }

        synchronized void add(byte[] data) {
            if (Boolean.FALSE.equals(heard) || size >= rate * MOST_SECONDS) {
                return;
            }
            try {
                if (!decoder.isOpen()) {
                    decoder.open();
                }
                short[] pcm = decoder.decode(encryption == null ? data : encryption.decrypt(data));
                int channels = stereo ? 2 : 1;
                for (int i = 0; i + step * channels <= pcm.length; i += step * channels) {
                    int sum = 0;
                    for (int j = 0; j < step * channels; j++) {
                        sum += pcm[i + j];
                    }
                    if (size == samples.length) {
                        samples = Arrays.copyOf(samples, size * 2);
                    }
                    samples[size++] = (short) (sum / (step * channels));
                }
            } catch (Exception e) {
                LOG.debug("Restless voice: a packet could not be decoded", e);
                heard = false;
            }
        }

        synchronized short[] end() {
            decoder.close();
            return Arrays.copyOf(samples, size);
        }
    }
}
