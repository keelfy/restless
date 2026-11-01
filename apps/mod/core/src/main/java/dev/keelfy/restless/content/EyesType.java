package dev.keelfy.restless.content;

import dev.keelfy.restless.platform.Platform;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * The particle type of the {@code eyes_in_dark} event: a pair of eyes that blink and go out, see {@link EyesParticle}.
 * Kept apart from the client class so the server can load it.
 */
public final class EyesType {
    public static final Supplier<ParticleType<?>> TYPE =
            Platform.get().register(BuiltInRegistries.PARTICLE_TYPE, "eyes", key -> new SimpleParticleType(false) {});

    private EyesType() {}

    /** Loads the class, so the type is registered with the rest of the mod; call while the mod loads. */
    public static void init() {}

    public static SimpleParticleType type() {
        return (SimpleParticleType) TYPE.get();
    }
}
