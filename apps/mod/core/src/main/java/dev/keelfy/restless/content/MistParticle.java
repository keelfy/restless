package dev.keelfy.restless.content;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

/**
 * A slow, soft wisp of grey mist lying on the ground: the blighted biome spawns it as an ambient particle anywhere around the
 * player, and it sinks to the first solid block a few below, so it is only seen hugging the land. Fades in, creeps, fades out.
 */
public final class MistParticle extends SingleQuadParticle {
    private static final int REACH = 6;
    private static final float PEAK = 0.12f;
    private static final int FADE = 60;
    private static final FacingCameraMode GROUND = (rotation, camera, partialTick) ->
            rotation.rotationX(-(float) Math.PI / 2);

    private MistParticle(ClientLevel level, double x, double y, double z, RandomSource random, SpriteSet sprites) {
        super(level, x, y, z, sprites.get(random));
        this.xd = 0.015 + random.nextDouble() * 0.01;
        this.yd = 0;
        this.zd = 0.005 + random.nextDouble() * 0.005;
        this.gravity = 0;
        this.friction = 1.0f;
        this.hasPhysics = false;
        this.quadSize = 1.0f + random.nextFloat();
        this.lifetime = 200 + random.nextInt(200);
        this.roll = random.nextFloat() * (float) Math.TAU;
        this.oRoll = this.roll;
        float grey = 0.85f + random.nextFloat() * 0.1f;
        setColor(grey, grey, grey);
        this.alpha = 0;
    }

    @Override
    public void tick() {
        super.tick();
        this.alpha = PEAK * Math.max(0, Math.min(1.0f, Math.min(age, lifetime - age) / (float) FADE));
        BlockPos pos = BlockPos.containing(x, y, z);
        if (!canHover(level, pos)) {
            remove();
        }
    }

    @Override
    public FacingCameraMode getFacingCameraMode() {
        return GROUND;
    }

    @Override
    protected int getLightCoords(float partialTick) {
        // Keep the mist readable in shadow without making it fully emissive.
        return LightCoordsUtil.max(super.getLightCoords(partialTick), LightCoordsUtil.pack(8, 0));
    }

    private static boolean canHover(ClientLevel level, BlockPos pos) {
        return level.hasChunkAt(pos)
                && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getFluidState(pos).isEmpty()
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
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
                if (canHover(level, pos)) {
                    return new MistParticle(level, x, pos.getY() + 0.15 + random.nextDouble() * 0.2, z, random, sprites);
                }
            }
            return null;
        }
    }
}
