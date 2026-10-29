package dev.keelfy.restless.content;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

/**
 * A slow, soft wisp of grey mist lying on the ground: the blighted biome spawns it as an ambient particle anywhere around the
 * player, and it sinks to the first solid block a few below, so it is only seen hugging the land. Fades in, creeps, fades out.
 */
public final class MistParticle extends SingleQuadParticle {
    private static final int REACH = 6;
    private static final float PEAK = 0.35f;
    private static final int FADE = 60;

    private MistParticle(ClientLevel level, double x, double y, double z, RandomSource random, SpriteSet sprites) {
        super(level, x, y, z, sprites.get(random));
        this.xd = (random.nextDouble() - 0.5) * 0.01;
        this.yd = 0;
        this.zd = (random.nextDouble() - 0.5) * 0.01;
        this.gravity = 0;
        this.friction = 1.0f;
        this.quadSize = 1.0f + random.nextFloat();
        this.lifetime = 200 + random.nextInt(200);
        float grey = 0.6f + random.nextFloat() * 0.1f;
        setColor(grey, grey, grey * 0.95f);
        this.alpha = 0;
    }

    @Override
    public void tick() {
        super.tick();
        this.alpha = PEAK * Math.min(1.0f, Math.min(age, lifetime - age) / (float) FADE);
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
        public @Nullable MistParticle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z,
                                                     double xAux, double yAux, double zAux, RandomSource random) {
            BlockPos.MutableBlockPos pos = BlockPos.containing(x, y, z).mutable();
            for (int i = 0; i < REACH; i++, pos.move(0, -1, 0)) {
                if (level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                    return new MistParticle(level, x, pos.getY() + 0.3 + random.nextDouble() * 0.9, z, random, sprites);
                }
            }
            return null;
        }
    }
}
