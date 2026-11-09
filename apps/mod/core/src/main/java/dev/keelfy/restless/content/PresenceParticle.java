package dev.keelfy.restless.content;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

/**
 * A puff of a spirit's haze, see {@link Presence}: a soft dark-grey cloud hanging in a fixed plane, so faint it only
 * darkens what is behind it. It hangs almost still, drifts and slowly turns, fades in and out; a few together make a
 * shape. The layer it is drawn in (the loader gives the haze one, with a shader that paints a soft, lit, turbulent
 * cloud) runs the quad 0..1 in texture space, so the sprite is not sampled.
 */
public final class PresenceParticle extends SingleQuadParticle {
    private static final float PEAK = 0.4f;
    private static final int FADE = 20;

    private final float spin;
    private final Quaternionf facing;
    private final Layer layer;

    private PresenceParticle(
            ClientLevel level, double x, double y, double z, RandomSource random, SpriteSet sprites, Layer layer) {
        super(level, x, y, z, sprites.get(random));
        this.layer = layer;
        this.xd = (random.nextDouble() - 0.5) * 0.006;
        this.yd = 0.002 + random.nextDouble() * 0.002;
        this.zd = (random.nextDouble() - 0.5) * 0.006;
        this.gravity = 0;
        this.friction = 1.0f;
        this.hasPhysics = false;
        this.quadSize = 1.2f + random.nextFloat() * 0.7f;
        this.lifetime = 60 + random.nextInt(30);
        this.facing = new Quaternionf()
                .rotationYXZ(random.nextFloat() * (float) Math.TAU, (random.nextFloat() - 0.5f) * (float) Math.PI, 0);
        this.roll = random.nextFloat() * (float) Math.TAU;
        this.oRoll = this.roll;
        this.spin = (random.nextFloat() - 0.5f) * 0.012f;
        float grey = 0.42f + random.nextFloat() * 0.1f;
        setColor(grey, grey, grey * 1.05f);
        this.alpha = 0;
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        super.tick();
        this.roll += spin;
        this.alpha = PEAK * Math.max(0, Math.min(1.0f, Math.min(age, lifetime - age) / (float) FADE));
    }

    /** Fixed plane instead of facing the camera, so the layers stay in the world as the player moves around. */
    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTickTime) {
        Quaternionf rotation = new Quaternionf(facing).rotateZ(Mth.lerp(partialTickTime, oRoll, roll));
        extractRotatedQuad(state, camera, rotation, partialTickTime);
    }

    @Override
    protected float getU0() {
        return 0;
    }

    @Override
    protected float getU1() {
        return 1;
    }

    @Override
    protected float getV0() {
        return 0;
    }

    @Override
    protected float getV1() {
        return 1;
    }

    @Override
    protected Layer getLayer() {
        return layer;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Layer layer;

        public Provider(SpriteSet sprites, Layer layer) {
            this.sprites = sprites;
            this.layer = layer;
        }

        @Override
        public @Nullable PresenceParticle createParticle(
                SimpleParticleType options,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xAux,
                double yAux,
                double zAux,
                RandomSource random) {
            return new PresenceParticle(level, x, y, z, random, sprites, layer);
        }
    }
}
