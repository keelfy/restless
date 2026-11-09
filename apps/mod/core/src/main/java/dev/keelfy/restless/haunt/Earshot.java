package dev.keelfy.restless.haunt;

import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Who hears a spirit's sounds: those of a personal spirit reach only the player it haunts, a territorial one's carry to
 * everyone near, like any other sound.
 */
public final class Earshot {
    private Earshot() {}

    /**
     * Plays a sound made while a personal spirit's event runs to its player alone, if within the sound's range.
     *
     * @return whether the sound was taken, so the world must not broadcast it
     */
    public static boolean keep(
            Level level, Vec3 at, @Nullable Holder<SoundEvent> sound, SoundSource source, float volume, float pitch) {
        return keep(Said.current(), level, at, sound, source, volume, pitch);
    }

    /** As {@link #keep(Level, Vec3, Holder, SoundSource, float, float)}, for a sound made by {@code spirit}. */
    public static boolean keep(
            @Nullable UUID spirit,
            Level level,
            Vec3 at,
            @Nullable Holder<SoundEvent> sound,
            SoundSource source,
            float volume,
            float pitch) {
        if (spirit == null || sound == null || !(level instanceof ServerLevel server)) {
            return false;
        }
        UUID owner = HauntData.get(server.getServer()).owner(spirit).orElse(null);
        if (owner == null) {
            return false;
        }
        double range = sound.value().getRange(volume);
        if (server.getPlayerByUUID(owner) instanceof ServerPlayer player && player.distanceToSqr(at) < range * range) {
            player.connection.send(new ClientboundSoundPacket(
                    sound,
                    source,
                    at.x,
                    at.y,
                    at.z,
                    volume,
                    pitch,
                    level.getRandom().nextLong()));
        }
        return true;
    }
}
