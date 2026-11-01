package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.Restless;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

/**
 * The mod's own damage types (data {@code restless:damage_type/*}), for harmful events that should leave a death
 * message of their own instead of the plain magic one: "was taken by the restless" and the like. 499
 */
final class Taken {
    /** Any harm with no better name; death message {@code death.attack.restless.taken}. */
    static final String TAKEN = "taken";
    /** Breath cut off; {@code death.attack.restless.smothered}. */
    static final String SMOTHERED = "smothered";
    /** Pulled down or away; {@code death.attack.restless.dragged_under}. */
    static final String DRAGGED_UNDER = "dragged_under";

    private Taken() {}

    /** A damage source of one of the types above; it bypasses armor and knockback like the magic damage it replaces. */
    static DamageSource by(ServerLevel level, String kind) {
        ResourceKey<DamageType> key =
                ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(Restless.MOD_ID, kind));
        return new DamageSource(
                level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key));
    }
}
