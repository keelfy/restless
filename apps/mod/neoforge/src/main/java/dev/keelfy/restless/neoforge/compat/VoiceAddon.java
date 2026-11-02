package dev.keelfy.restless.neoforge.compat;

import dev.keelfy.restless.Restless;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import su.plo.voice.api.addon.AddonLoaderScope;
import su.plo.voice.api.addon.ServerAddonsLoader;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.event.EventPriority;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.api.server.event.audio.source.PlayerSpeakEndEvent;

/**
 * Plasmo Voice add-on: when a player stops speaking, the spirit box hears the phrase as it does a chat line. Plasmo
 * Voice gives only that the player spoke, not the words.
 */
@Addon(id = Restless.MOD_ID, scope = AddonLoaderScope.SERVER, version = "", authors = "keelfy")
public final class VoiceAddon {
    /** Only called with Plasmo Voice present: this class is not loaded without it. */
    public static void register() {
        ServerAddonsLoader.INSTANCE.load(new VoiceAddon());
    }

    /** The event comes on a voice thread; the cancelled one is still the phrase's end, so it is not skipped. */
    @EventSubscribe(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onSpeakEnd(@NotNull PlayerSpeakEndEvent event) {
        var server = ServerLifecycleHooks.getCurrentServer();
        var id = event.getPlayer().getInstance().getUuid();
        if (server != null) {
            server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player != null) {
                    Restless.onVoice(player);
                }
            });
        }
    }
}
