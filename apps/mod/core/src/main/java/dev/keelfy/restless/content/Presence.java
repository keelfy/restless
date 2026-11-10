package dev.keelfy.restless.content;

import dev.keelfy.restless.content.client.PresenceParticle;
import dev.keelfy.restless.platform.Platform;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * How a spirit looks where it stands: a faint, darkened grey haze about a body's height with a mote or two of ash, easy
 * to miss and plain once looked at, see {@link PresenceParticle}. Kept apart from the client class so the server can
 * load it.
 */
public final class Presence {
    public static final Supplier<ParticleType<?>> TYPE = Platform.get()
            .register(BuiltInRegistries.PARTICLE_TYPE, "presence", key -> new SimpleParticleType(false) {});

    private Presence() {}

    /** Loads the class, so the type is registered with the rest of the mod; call while the mod loads. */
    public static void init() {}

    public static SimpleParticleType type() {
        return (SimpleParticleType) TYPE.get();
    }

    /**
     * One puff of a spirit standing at {@code feet}. A puff lasts a few seconds, so call it about every half second to
     * keep the spirit showing.
     */
    public static void emit(ServerLevel level, Vec3 feet) {
        emit(level, feet, 2);
    }

    /** A puff of {@code count} motes; thicker for a spirit gathering, thinner for one on the move. */
    public static void emit(ServerLevel level, Vec3 feet, int count) {
        level.sendParticles(type(), feet.x, feet.y + 1.0, feet.z, count, 0.3, 0.45, 0.3, 0.0);
        if (level.getRandom().nextInt(3) == 0) {
            level.sendParticles(ParticleTypes.ASH, feet.x, feet.y + 1.0, feet.z, 1, 0.4, 0.6, 0.4, 0.0);
        }
    }
}
