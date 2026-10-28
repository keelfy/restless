package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.spirit.SpiritType;
import java.util.Set;

/** Pure progression math: no Minecraft types, so it is unit-testable. */
public final class Danger {
    private Danger() {}

    /** 0 on a fresh world, 1 once the configured days and advancements are reached. */
    public static double of(long days, int advancements) {
        Config.Section config = Config.section("danger");
        double byDays = Math.min(days / config.num("days_to_max"), 1.0);
        double byAdvancements = Math.min(advancements / config.num("advancements_to_max"), 1.0);
        return config.num("days_share") * byDays + config.num("advancements_share") * byAdvancements;
    }

    public static double tierFactor(HauntTier tier, double danger) {
        return switch (tier) {
            case LIGHT -> 1.0 - Config.section("danger").num("light_penalty") * danger;
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

    /** Spirits are quieter in the open: each of day, open sky and no thunderstorm stretches the cooldown by the daylight step. */
    public static double daylightFactor(boolean day, boolean seesSky, boolean noThunder) {
        double step = Config.section("danger").num("daylight_step");
        double factor = 1.0;
        for (boolean calm : new boolean[] {day, seesSky, noThunder}) {
            if (calm) {
                factor *= step;
            }
        }
        return factor;
    }

    /**
     * Ticks until the next event: the calm cooldown shrinks with danger, a roll stretches it up to twofold, then it is scaled
     * by spirit activity and stretched by {@code quiet} (see {@link #daylightFactor}).
     */
    public static long cooldownTicks(double danger, double activity, double roll, double quiet) {
        Config.Section config = Config.section("danger");
        double minutes = (config.num("cooldown_calm_minutes") - config.num("cooldown_drop_minutes") * danger) * (1.0 + roll) * quiet;
        return Math.max(config.integer("min_cooldown_ticks"), (long) (minutes * Ticks.MINUTE / activity));
    }
}
