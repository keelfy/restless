package dev.keelfy.restless.haunt;

import dev.keelfy.restless.spirit.SpiritType;
import java.util.Set;

/** Pure progression math: no Minecraft types, so it is unit-testable. */
public final class Danger {
    static final double DAYS_TO_MAX = 40;
    static final double ADVANCEMENTS_TO_MAX = 40;

    private Danger() {}

    /** 0 on a fresh world, 1 at ~40 days and ~40 advancements. */
    public static double of(long days, int advancements) {
        double byDays = Math.min(days / DAYS_TO_MAX, 1.0);
        double byAdvancements = Math.min(advancements / ADVANCEMENTS_TO_MAX, 1.0);
        return 0.6 * byDays + 0.4 * byAdvancements;
    }

    public static double tierFactor(HauntTier tier, double danger) {
        return switch (tier) {
            case LIGHT -> 1.0 - 0.7 * danger;
            case HARMFUL -> danger;
        };
    }

    public static double weight(HauntTier tier, Set<EventTag> tags, double baseWeight, SpiritType spirit, double danger) {
        double weight = baseWeight * tierFactor(tier, danger);
        for (EventTag tag : tags) {
            weight *= spirit.affinity(tag);
        }
        return weight;
    }

    /** Ticks until the next event: 4-8 min when calm, 1.5-3 min at max danger, scaled by spirit activity. */
    public static long cooldownTicks(double danger, double activity, double roll) {
        double minutes = (4.0 - 2.5 * danger) * (1.0 + roll);
        return Math.max(20, (long) (minutes * 60 * 20 / activity));
    }
}
