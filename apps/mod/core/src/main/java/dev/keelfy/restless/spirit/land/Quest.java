package dev.keelfy.restless.spirit.land;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.spirit.Story;
import java.util.Locale;
import java.util.Random;

/**
 * The kind of unfinished business a grave asks for, the same for the spirit's whole life: how it died may decide it
 * ({@code grave.cause_quests}, taken with {@code grave.cause_chance}), otherwise the seed draws one by
 * {@code grave.quests} weight.
 */
public enum Quest {
    /** Bring back the thing it lost, see {@link Graves#wanted}. */
    ITEM,
    /** Find its bones somewhere off the grave and bury them there; it acts more often while they are carried. */
    REMAINS,
    /** Take its last letter to a villager. */
    LETTER,
    /** Kill the mob that killed it, which waits near the grave under the name from the story. */
    KILLER,
    /** Another of the player's spirits killed it: it rests only once that one is gone. */
    RIVAL;

    // Keeps the quest roll apart from the story and the wanted item, which use the same seed.
    private static final long SALT = 0x9E57L;

    // Keeps the bones spot apart from the other rolls on the same seed.
    private static final long BONES_SALT = 0xB04E5L;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    @SuppressWarnings("java:S2245") // Seeded so the quest is reproducible; not security-sensitive.
    public static Quest of(long seed) {
        Config.Section config = Config.section("grave");
        Random random = new Random(seed ^ SALT);
        Config.Section causes = config.section("cause_quests");
        String cause = Story.of(seed).cause();
        if (causes.has(cause) && random.nextDouble() < config.num("cause_chance")) {
            return valueOf(causes.text(cause).toUpperCase(Locale.ROOT));
        }
        Config.Section weights = config.section("quests");
        double total = 0;
        for (Quest quest : values()) {
            total += weights.has(quest.id()) ? weights.num(quest.id()) : 0;
        }
        double roll = random.nextDouble() * total;
        for (Quest quest : values()) {
            roll -= weights.has(quest.id()) ? weights.num(quest.id()) : 0;
            if (roll < 0) {
                return quest;
            }
        }
        return ITEM;
    }

    /** The turn from the grave to the bones, as x and z. */
    @SuppressWarnings("java:S2245") // Seeded so the spot is reproducible; not security-sensitive.
    static int[] bonesOffset(long seed, int graveX, int graveZ) {
        Config.Section config = Config.section("grave");
        Random random = new Random(seed ^ BONES_SALT ^ (graveX * 31L + graveZ));
        double angle = random.nextDouble() * Math.PI * 2;
        double min = config.num("remains_min_distance");
        double distance = min + random.nextDouble() * (config.num("remains_max_distance") - min);
        return new int[] {(int) Math.round(Math.cos(angle) * distance), (int) Math.round(Math.sin(angle) * distance)};
    }
}
