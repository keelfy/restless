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
    public static final SoundEvent CUT_OFF_SCREAM = event("haunt.cut_off_scream");
    public static final SoundEvent LONG_SCREAM = event("haunt.long_scream");
    public static final SoundEvent MOAN = event("haunt.moan");
    public static final SoundEvent WHISPER = event("haunt.whisper");
    public static final SoundEvent LAUGH = event("haunt.laugh");
    public static final SoundEvent BREATH = event("haunt.breath");
    public static final SoundEvent SCREECH = event("haunt.screech");
    public static final SoundEvent MUSIC_BOX = event("haunt.music_box");
    public static final SoundEvent LULLABY = event("haunt.lullaby");
    public static final SoundEvent CLAW_SCRATCH = event("haunt.claw_scratch");
    public static final SoundEvent CHAIR_CREAK = event("haunt.chair_creak");
    public static final SoundEvent MIRROR_RING = event("haunt.mirror_ring");
    public static final SoundEvent MIRROR_CRACK = event("haunt.mirror_crack");
    public static final SoundEvent JOLT = event("haunt.jolt");
    public static final SoundEvent RUSTLE = event("haunt.rustle");
    public static final SoundEvent HEARTBEAT = event("haunt.heartbeat");
    public static final SoundEvent CHAIN_RATTLE = event("haunt.chain_rattle");
    public static final SoundEvent CREATURE_RATTLE = event("haunt.creature_rattle");
    public static final SoundEvent SANITY_BED = event("haunt.sanity_bed");
    public static final SoundEvent EVOLVE = event("haunt.evolve");
    public static final SoundEvent LAID_TO_REST = event("haunt.laid_to_rest");
    public static final SoundEvent REQUIEM = event("haunt.requiem");
    /** Ready for the ritual's fury phase: a 30 s loop, not played yet (the ritual is being rewritten). */
    public static final SoundEvent RITUAL_FURY = event("haunt.ritual_fury");
    /** Ready for the ritual's leaving phase: a sting, not played yet. */
    public static final SoundEvent RITUAL_LEAVING = event("haunt.ritual_leaving");
    /** Ready for a wrong offering at the altar: a sting, not played yet. */
    public static final SoundEvent OFFERING_REFUSED = event("haunt.offering_refused");

    private HauntSounds() {}

    /** A sound named in the config, e.g. {@code restless:haunt.sting_roar}; a bare path is taken as the mod's. */
    public static SoundEvent byId(String id) {
        return SoundEvent.createVariableRangeEvent(
                id.contains(":") ? Identifier.parse(id) : Identifier.fromNamespaceAndPath(Restless.MOD_ID, id));
    }

    private static SoundEvent event(String path) {
        return SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Restless.MOD_ID, path));
    }
}
