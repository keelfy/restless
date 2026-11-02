package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import java.util.Locale;
import net.minecraft.world.entity.Entity;

/** How far an assault may go: where it must leave the player's health. */
public enum Band {
    /** Leaves {@code assaults.safe_health}. */
    THREAT,
    /** Leaves {@code assaults.brink_health}. */
    BRINK,
    /** Can kill. */
    LETHAL;

    private static final String TAG_PREFIX = "restless_band_";

    /** The event's {@code band} from the config; an event without one is a threat. */
    public static Band of(String eventId) {
        Config.Section config = Config.event(eventId);
        return config.has("band") ? valueOf(config.text("band").toUpperCase(Locale.ROOT)) : THREAT;
    }

    /** Health the band must leave; below zero for the band that may kill. */
    public float floor() {
        return switch (this) {
            case THREAT -> (float) Config.section("assaults").num("safe_health");
            case BRINK -> (float) Config.section("assaults").num("brink_health");
            case LETHAL -> -1.0f;
        };
    }

    /** How much damage the band still allows to a player at this health. */
    public float room(float health) {
        return this == LETHAL ? Float.MAX_VALUE : health - floor();
    }

    /** Marks a mob or projectile a spirit made, so {@link #of(Entity)} can cap what it does. */
    public void mark(Entity maker) {
        maker.addTag(TAG_PREFIX + name().toLowerCase(Locale.ROOT));
    }

    /** The band an entity was marked with by {@link #mark}, or null if no spirit made it. */
    public static Band of(Entity maker) {
        for (Band band : values()) {
            if (maker.entityTags().contains(TAG_PREFIX + band.name().toLowerCase(Locale.ROOT))) {
                return band;
            }
        }
        return null;
    }
}
