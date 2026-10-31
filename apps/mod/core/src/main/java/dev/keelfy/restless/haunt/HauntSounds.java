package dev.keelfy.restless.haunt;

import dev.keelfy.restless.Restless;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * The mod's own recordings, declared in {@code assets/restless/sounds.json}. Not registered: like the "behind you"
 * subtitle, the sound packet carries the whole event, so the client needs only the resource pack entry.
 */
public final class HauntSounds {
    public static final SoundEvent SCREAM = event("haunt.scream");
    public static final SoundEvent MOAN = event("haunt.moan");
    public static final SoundEvent WHISPER = event("haunt.whisper");
    public static final SoundEvent LAUGH = event("haunt.laugh");
    public static final SoundEvent BREATH = event("haunt.breath");
    public static final SoundEvent SCREECH = event("haunt.screech");

    private HauntSounds() {}

    private static SoundEvent event(String path) {
        return SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Restless.MOD_ID, path));
    }
}
