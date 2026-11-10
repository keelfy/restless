package dev.keelfy.restless.content;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Draws a {@link Silhouette} as a few solid black boxes, with no texture of its own: a dark patch of the vanilla
 * enderman skin, tinted black, is all the surface there is. The shape sets how tall and wrong it looks. It twitches in
 * sudden small jerks, its head sways slowly, and two pale eyes on the head are lit the same in the dark as in the
 * light.
 */
public final class SilhouetteRenderer extends EntityRenderer<Silhouette, SilhouetteRenderer.State> {
    /** The enderman's body skin, opaque and near black; every vertex samples this one point of it. */
    private static final Identifier SKIN = Identifier.withDefaultNamespace("textures/entity/enderman/enderman.png");

    private static final float U = 40 / 64f;
    private static final float V = 26 / 32f;
    private static final int BLACK = 0xFF000000;
    /** The eyes: an opaque near-white patch of the snow texture, drawn at full brightness. */
    private static final Identifier PALE = Identifier.withDefaultNamespace("textures/block/snow.png");

    private static final int WHITE = 0xFFFFFFFF;
    private static final int FULL_BRIGHT = 0xF000F0;

    /**
     * A box standing on {@code y}, centred at {@code x}, {@code z}, tipped {@code tiltX} forward and {@code tiltZ}
     * sideways in degrees about its base. Lengths are in blocks; +z is the way the figure faces.
     */
    record Part(float x, float y, float z, float w, float h, float d, float tiltX, float tiltZ) {}

    /**
     * Tall and thin, arms to the knees; low and wide on all fours; a head hung on one shoulder; a child with arms up;
     * many arms; a stooped giant; bent double; a tall one and a small one; a long neck; hung off the ground. After
     * those ten, which any scare may pick, come the shapes made for one spot: a bare head to look out of a hole in a
     * wall, and a figure lying on its back.
     */
    private static final List<List<Part>> SHAPE_PARTS = List.of(
            List.of(
                    new Part(-.1f, 0, 0, .08f, 1.5f, .08f, 0, 0),
                    new Part(.1f, 0, 0, .08f, 1.5f, .08f, 0, 0),
                    new Part(0, 1.5f, 0, .34f, 1.4f, .2f, 0, 0),
                    new Part(0, 2.9f, .05f, .26f, .34f, .26f, 12, 0),
                    new Part(-.22f, .9f, 0, .08f, 1.95f, .08f, 0, -3),
                    new Part(.22f, .9f, 0, .08f, 1.95f, .08f, 0, 3)),
            List.of(
                    new Part(0, .55f, 0, .9f, .5f, .6f, 25, 0),
                    new Part(0, .55f, .55f, .5f, .45f, .5f, 0, 0),
                    new Part(-.6f, 0, .4f, .12f, .9f, .12f, -50, 8),
                    new Part(.6f, 0, .4f, .12f, .9f, .12f, -50, -8),
                    new Part(-.3f, 0, -.25f, .14f, .55f, .14f, 20, 0),
                    new Part(.3f, 0, -.25f, .14f, .55f, .14f, 20, 0)),
            List.of(
                    new Part(-.15f, 0, 0, .22f, .85f, .22f, 0, 0),
                    new Part(.15f, 0, 0, .22f, .85f, .22f, 0, 0),
                    new Part(0, .8f, 0, .75f, 1.0f, .35f, 0, 0),
                    new Part(.3f, 1.8f, 0, .32f, .38f, .32f, 0, -75),
                    new Part(-.5f, .6f, 0, .1f, 1.3f, .1f, 0, 4),
                    new Part(.5f, .6f, 0, .1f, 1.3f, .1f, 0, -4)),
            List.of(
                    new Part(-.08f, 0, 0, .08f, .4f, .08f, 0, 0),
                    new Part(.08f, 0, 0, .08f, .4f, .08f, 0, 0),
                    new Part(0, .35f, 0, .3f, .45f, .2f, 0, 0),
                    new Part(0, .78f, 0, .5f, .5f, .5f, 0, 0),
                    new Part(-.17f, .7f, 0, .07f, .7f, .07f, 0, 25),
                    new Part(.17f, .7f, 0, .07f, .7f, .07f, 0, -25)),
            List.of(
                    new Part(-.1f, 0, 0, .07f, 1.0f, .07f, 0, 0),
                    new Part(.1f, 0, 0, .07f, 1.0f, .07f, 0, 0),
                    new Part(0, 1.0f, 0, .3f, 1.1f, .2f, 0, 0),
                    new Part(0, 2.1f, 0, .22f, .28f, .22f, 0, 0),
                    new Part(-.15f, 1.9f, 0, .05f, 1.4f, .05f, 0, 50),
                    new Part(.15f, 1.9f, 0, .05f, 1.4f, .05f, 0, -50),
                    new Part(-.15f, 1.6f, 0, .05f, 1.4f, .05f, 0, 95),
                    new Part(.15f, 1.6f, 0, .05f, 1.4f, .05f, 0, -95),
                    new Part(-.15f, 1.3f, 0, .05f, 1.4f, .05f, 0, 140),
                    new Part(.15f, 1.3f, 0, .05f, 1.4f, .05f, 0, -140)),
            // stooped giant, head nearly on its chest, hands at its feet
            List.of(
                    new Part(-.1f, 0, 0, .07f, 1.9f, .07f, 0, 0),
                    new Part(.1f, 0, 0, .07f, 1.9f, .07f, 0, 0),
                    new Part(0, 1.9f, 0, .3f, 1.3f, .2f, 25, 0),
                    new Part(0, 3.0f, .55f, .2f, .26f, .2f, 35, 0),
                    new Part(-.24f, .1f, .25f, .05f, 2.6f, .05f, -8, 0),
                    new Part(.24f, .1f, .25f, .05f, 2.6f, .05f, -8, 0)),
            // bent double at the hip, the head held up on the end of its back
            List.of(
                    new Part(-.1f, 0, 0, .08f, 1.0f, .08f, 0, 0),
                    new Part(.1f, 0, 0, .08f, 1.0f, .08f, 0, 0),
                    new Part(0, 1.0f, 0, .3f, 1.1f, .2f, 85, 0),
                    new Part(0, 1.1f, 1.1f, .26f, .34f, .26f, 0, 0),
                    new Part(-.2f, .05f, .95f, .06f, 1.0f, .06f, 0, 0),
                    new Part(.2f, .05f, .95f, .06f, 1.0f, .06f, 0, 0)),
            // a tall one and a small one, joined by the hands
            List.of(
                    new Part(-.35f, 0, 0, .3f, 1.7f, .2f, 0, 0),
                    new Part(-.35f, 1.7f, 0, .24f, .3f, .24f, 0, 0),
                    new Part(.35f, 0, 0, .22f, .9f, .16f, 0, 0),
                    new Part(.35f, .9f, 0, .2f, .24f, .2f, 0, 0),
                    new Part(0, .85f, 0, .7f, .05f, .05f, 0, 0)),
            // a thick low body on a neck as long as it is
            List.of(
                    new Part(0, 0, 0, .5f, 1.0f, .3f, 0, 0),
                    new Part(0, 1.0f, 0, .06f, 1.8f, .06f, 10, 0),
                    new Part(0, 2.77f, .31f, .3f, .32f, .3f, 20, 0),
                    new Part(-.3f, .2f, 0, .07f, .9f, .07f, 0, 8),
                    new Part(.3f, .2f, 0, .07f, .9f, .07f, 0, -8)),
            // off the ground, limp, the head dropped on one shoulder
            List.of(
                    new Part(-.1f, .3f, 0, .07f, .9f, .07f, 0, 0),
                    new Part(.1f, .3f, 0, .07f, .9f, .07f, 0, 0),
                    new Part(0, 1.2f, 0, .3f, 1.0f, .2f, -8, 0),
                    new Part(0, 2.2f, 0, .26f, .32f, .26f, 0, 55),
                    new Part(-.22f, .7f, 0, .06f, 1.4f, .06f, 0, 6),
                    new Part(.22f, .7f, 0, .06f, 1.4f, .06f, 0, -6)),
            // a bare head, wider than it is tall, for a hole in a wall
            List.of(new Part(0, 0, 0, .7f, .9f, .5f, 0, 0)),
            // on its back, head toward +z, face up
            List.of(
                    new Part(-.1f, .1f, -.9f, .08f, .9f, .08f, 90, 0),
                    new Part(.1f, .1f, -.9f, .08f, .9f, .08f, 90, 0),
                    new Part(0, .1f, 0, .3f, .9f, .2f, 90, 0),
                    new Part(0, .13f, 1.24f, .26f, .34f, .26f, -90, 0),
                    new Part(-.2f, .06f, .1f, .05f, .8f, .05f, 90, 0),
                    new Part(.2f, .06f, .1f, .05f, .8f, .05f, 90, 0)));

    /** Which part of each shape is the head, as an index into its parts. */
    private static final int[] HEAD = {3, 1, 3, 3, 3, 3, 3, 1, 2, 3, 0, 3};

    /** The first ten shapes: those a scare picks from at random. */
    public static final int ANY = 10;

    /** The bare head, for looking out of a hole. */
    public static final int FACE = 10;

    /** The figure lying on its back. */
    public static final int LYING = 11;

    /** How many shapes there are. */
    public static final int SHAPES = SHAPE_PARTS.size();

    public static final class State extends EntityRenderState {
        int shape;
        float yaw;
        float age;
    }

    public SilhouetteRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(Silhouette entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.shape = Math.floorMod(entity.shape(), SHAPES);
        state.yaw = entity.getYRot();
        state.age = state.ageInTicks;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.isInvisible) {
            return;
        }
        int light = state.lightCoords;
        // Now and then a sudden small jerk, then still again: 0 most of the time, else -1 to 1.
        int step = (int) (state.age / 2);
        int hash = step * 0x9E3779B1;
        hash ^= hash >>> 15;
        float jerk = (hash & 7) == 0 ? ((hash >>> 3 & 15) - 7.5f) / 7.5f : 0;
        float sway = 18 * Mth.sin(state.age * 0.12f) + 10 * jerk;
        pose.pushPose();
        pose.rotateDegrees(Axis.YP, -state.yaw + 8 * jerk);
        pose.translate(0.06f * jerk, 0, 0);
        List<Part> parts = SHAPE_PARTS.get(state.shape);
        int head = HEAD[state.shape];
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(SKIN), (transform, vertices) -> {
            for (int i = 0; i < parts.size(); i++) {
                cuboid(
                        vertices,
                        place(transform, parts.get(i), i == head ? sway : 0),
                        parts.get(i),
                        light,
                        BLACK,
                        U,
                        V);
            }
        });
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(PALE), (transform, vertices) -> {
            PoseStack stack = new PoseStack();
            stack.last().set(place(transform, parts.get(head), sway));
            Part skull = parts.get(head);
            for (int side = -1; side <= 1; side += 2) {
                stack.pushPose();
                stack.translate(side * skull.w / 4, skull.h * 0.6f, skull.d / 2);
                Part eye = new Part(0, 0, 0, skull.w * 0.22f, skull.w * 0.12f, 0.02f, 0, 0);
                cuboid(vertices, stack.last(), eye, FULL_BRIGHT, WHITE, 0.5f, 0.5f);
                stack.popPose();
            }
        });
        pose.popPose();
    }

    /**
     * Where one part sits: moved to its spot and tipped about the middle of its base, and swayed sideways by
     * {@code extraTiltZ}.
     */
    private static PoseStack.Pose place(PoseStack.Pose base, Part part, float extraTiltZ) {
        PoseStack stack = new PoseStack();
        stack.last().set(base);
        stack.translate(part.x, part.y, part.z);
        stack.rotateDegrees(Axis.XP, part.tiltX);
        stack.rotateDegrees(Axis.ZP, part.tiltZ + extraTiltZ);
        return stack.last();
    }

    /** A box standing on the pose's origin; each face is lit as a flat sheet facing out. */
    private static void cuboid(
            VertexConsumer vertices, PoseStack.Pose pose, Part part, int light, int color, float u, float v) {
        float x = part.w / 2;
        float z = part.d / 2;
        float h = part.h;
        face(vertices, pose, light, color, u, v, 0, 0, 1, -x, 0, z, x, 0, z, x, h, z, -x, h, z);
        face(vertices, pose, light, color, u, v, 0, 0, -1, x, 0, -z, -x, 0, -z, -x, h, -z, x, h, -z);
        face(vertices, pose, light, color, u, v, 1, 0, 0, x, 0, z, x, 0, -z, x, h, -z, x, h, z);
        face(vertices, pose, light, color, u, v, -1, 0, 0, -x, 0, -z, -x, 0, z, -x, h, z, -x, h, -z);
        face(vertices, pose, light, color, u, v, 0, 1, 0, -x, h, z, x, h, z, x, h, -z, -x, h, -z);
        face(vertices, pose, light, color, u, v, 0, -1, 0, -x, 0, -z, x, 0, -z, x, 0, z, -x, 0, z);
    }

    private static void face(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            int light,
            int color,
            float u,
            float v,
            float nx,
            float ny,
            float nz,
            float... corners) {
        for (int i = 0; i < 12; i += 3) {
            vertices.addVertex(pose, corners[i], corners[i + 1], corners[i + 2])
                    .setColor(color)
                    .setUv(u, v)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, nx, ny, nz);
        }
    }
}
