package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.spirit.SpiritType;
import dev.keelfy.restless.spirit.Trait;
import dev.keelfy.restless.spirit.Traits;
import java.util.List;
import java.util.Set;

/** Pure progression math: no Minecraft types, so it is unit-testable. */
public final class Power {
    private Power() {}

    /** 0 for a spirit just born and calm, 1 once it is fully mature and angry; both come from the spirit alone, not the world. */
    public static double of(long ageTicks, double anger) {
        Config.Section config = Config.section("power");
        double maturity = Math.min(ageTicks / (config.num("maturity_days") * Ticks.DAY), 1.0);
        return Math.min(1.0, config.num("maturity_share") * maturity + config.num("anger_share") * anger);
    }

    public static double tierFactor(HauntTier tier, double power) {
        return switch (tier) {
            case LIGHT -> 1.0 - Config.section("power").num("light_penalty") * power;
            case HARMFUL -> power;
        };
    }

    public static double weight(HauntTier tier, Set<EventTag> tags, double baseWeight, SpiritType spirit, List<Trait> traits, double power) {
        double weight = baseWeight * tierFactor(tier, power);
        for (EventTag tag : tags) {
            weight *= spirit.affinity(tag) * Traits.affinity(traits, tag);
        }
        return weight;
    }

    /** Spirits are quieter in the open: each of day, open sky and no thunderstorm stretches the cooldown by the daylight step. */
    public static double daylightFactor(boolean day, boolean seesSky, boolean noThunder) {
        double step = Config.section("power").num("daylight_step");
        double factor = 1.0;
        for (boolean calm : new boolean[] {day, seesSky, noThunder}) {
            if (calm) {
                factor *= step;
            }
        }
        return factor;
    }

    /**
     * Ticks until the next event: the calm cooldown shrinks with power, a roll stretches it up to twofold, then it is scaled
     * by spirit activity and stretched by {@code quiet} (see {@link #daylightFactor}).
     */
    public static long cooldownTicks(double power, double activity, double roll, double quiet) {
        Config.Section config = Config.section("power");
        double minutes = (config.num("cooldown_calm_minutes") - config.num("cooldown_drop_minutes") * power) * (1.0 + roll) * quiet;
        return Math.max(config.integer("min_cooldown_ticks"), (long) (minutes * Ticks.MINUTE / activity));
    }
}
