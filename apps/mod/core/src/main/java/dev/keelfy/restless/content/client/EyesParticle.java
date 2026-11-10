package dev.keelfy.restless.content.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

/**
 * A pair of eyes hanging in the dark: they open, hold, shut once as a blink, open again and then go out at once. Always
 * fully lit, or the dark they hang in would hide them.
 */
public final class EyesParticle extends SingleQuadParticle {
    private static final int OPEN = 6;
    private static final int BLINK_LENGTH = 3;

    private final int blinkAt;

    private EyesParticle(ClientLevel level, double x, double y, double z, RandomSource random, SpriteSet sprites) {
        super(level, x, y, z, sprites.get(random));
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.gravity = 0;
        this.hasPhysics = false;
        this.quadSize = 0.3f;
        this.lifetime = 40 + random.nextInt(20);
        this.blinkAt = 18 + random.nextInt(10);
        this.alpha = 0;
    }

    @Override
    public void tick() {
        super.tick();
        boolean blinking = age >= blinkAt && age < blinkAt + BLINK_LENGTH;
        this.alpha = blinking ? 0 : Math.min(1.0f, age / (float) OPEN);
    }

    @Override
    protected int getLightCoords(float partialTick) {
        return LightCoordsUtil.FULL_BRIGHT;
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable EyesParticle createParticle(
                SimpleParticleType options,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xAux,
                double yAux,
                double zAux,
                RandomSource random) {
            return new EyesParticle(level, x, y, z, random, sprites);
        }
    }
}
