package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The last sound each player made (steps, splashes, eating: what the game plays in the {@code PLAYERS} category at
 * their feet), so the {@code echo} event can play it again. In memory only. The replay uses another category, so it is
 * never recorded as a sound of the player's own.
 */
public final class Echoes {
    private record Heard(Holder<SoundEvent> sound, Vec3 at, float volume, float pitch, long time) {}

    private static final Map<UUID, Heard> LAST = new ConcurrentHashMap<>();

    private Echoes() {}

    /** Call for every sound a level plays; keeps only the ones a player makes. */
    public static void hear(Level level, Vec3 at, @Nullable Holder<SoundEvent> sound, SoundSource source, float volume, float pitch) {
        if (!(level instanceof ServerLevel) || sound == null || source != SoundSource.PLAYERS) {
            return;
        }
        Player player = level.getNearestPlayer(at.x, at.y, at.z, Config.event("echo").num("capture_radius"), false);
        if (player != null) {
            LAST.put(player.getUUID(), new Heard(sound, at, volume, pitch, level.getGameTime()));
        }
    }

    public static boolean canReplay(ServerPlayer player) {
        return recent(player) != null;
    }

    /** Plays the player's last sound again after the event's delay, from where it was made. */
    public static void replay(ServerPlayer player) {
        Heard heard = recent(player);
        if (heard == null) {
            return;
        }
        Config.Section config = Config.event("echo");
        Config.Tone tone = config.tone("sound");
        Scheduler.later(player, config.integer("delay_ticks"), p ->
            p.level().playSound(null, heard.at.x, heard.at.y, heard.at.z, heard.sound.value(), SoundSource.HOSTILE,
                heard.volume * tone.volume(), heard.pitch * tone.pitch()));
    }

    private static @Nullable Heard recent(ServerPlayer player) {
        Heard heard = LAST.get(player.getUUID());
        boolean fresh = heard != null && player.level().getGameTime() - heard.time <= Config.event("echo").integer("max_age_ticks");
        return fresh ? heard : null;
    }
}
