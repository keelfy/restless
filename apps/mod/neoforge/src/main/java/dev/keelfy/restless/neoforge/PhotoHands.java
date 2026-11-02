package dev.keelfy.restless.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.keelfy.restless.Restless;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.PhotoTooltip;
import dev.keelfy.restless.content.Picture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * Holds a developed photo in front of the face the way vanilla holds a map: in both hands when the other hand is empty,
 * in one hand otherwise.
 */
@EventBusSubscriber(modid = Restless.MOD_ID, value = Dist.CLIENT)
public final class PhotoHands {
    private static final Identifier CARD = Identifier.withDefaultNamespace("textures/map/map_background.png");
    /** Units of the photo's {@link Picture#WIDTH} by {@link Picture#HEIGHT} grid, as a map's 128 by 128. */
    private static final float UNIT = 1f / Picture.WIDTH;

    private PhotoHands() {}

    @SubscribeEvent
    public static void render(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        Picture picture = event.getItemStack().get(Content.PICTURE.get());
        // The open journal holds both hands itself.
        if (picture == null
                || minecraft.player.getMainHandItem().is(Content.JOURNAL.get())
                || minecraft.player.getOffhandItem().is(Content.JOURNAL.get())) {
            return;
        }
        event.setCanceled(true);
        AvatarRenderState avatar = event.getPlayerState().avatarRenderState;
        if (avatar == null) return;
        boolean main = event.getHand() == InteractionHand.MAIN_HAND;
        PoseStack pose = event.getPoseStack();
        if (main && minecraft.player.getOffhandItem().isEmpty()) {
            twoHanded(event, pose, avatar);
        } else {
            oneHanded(event, pose, avatar, main ? avatar.mainArm : avatar.mainArm.getOpposite());
        }
        photo(pose, event.getSubmitNodeCollector(), event.getPackedLight(), picture);
        pose.popPose();
    }

    /** Vanilla's two-handed map: raised and tilted towards the face as the head looks down. */
    private static void twoHanded(RenderHandEvent event, PoseStack pose, AvatarRenderState avatar) {
        float swing = event.getSwingProgress();
        float swingRoot = Mth.sqrt(swing);
        pose.pushPose();
        pose.translate(0, 0.1f * Mth.sin(swing * Mth.PI), -0.4f * Mth.sin(swingRoot * Mth.PI));
        float tilt = Mth.clamp(1 - event.getInterpolatedPitch() / 45f + 0.1f, 0, 1);
        tilt = -Mth.cos(tilt * Mth.PI) * 0.5f + 0.5f;
        pose.translate(0, 0.04f - event.getEquipProgress() * 1.2f - tilt * 0.5f, -0.72f);
        pose.rotateDegrees(Axis.XP, tilt * -85f);
        if (!avatar.isInvisible) {
            pose.pushPose();
            pose.rotateDegrees(Axis.YP, 90);
            hand(event, pose, avatar, HumanoidArm.RIGHT);
            hand(event, pose, avatar, HumanoidArm.LEFT);
            pose.popPose();
        }
        pose.rotateDegrees(Axis.XP, Mth.sin(swingRoot * Mth.PI) * 20f);
        pose.scale(2, 2, 2);
    }

    /** Vanilla's one-handed map: the arm below, the map held out to the side of the view. */
    private static void oneHanded(RenderHandEvent event, PoseStack pose, AvatarRenderState avatar, HumanoidArm arm) {
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float swing = event.getSwingProgress();
        pose.pushPose();
        pose.translate(side * 0.125f, -0.125f, 0);
        if (!avatar.isInvisible) {
            pose.pushPose();
            pose.rotateDegrees(Axis.ZP, side * 10f);
            arm(event, pose, avatar, arm, side);
            pose.popPose();
        }
        pose.translate(side * 0.51f, -0.08f - event.getEquipProgress() * 1.2f, -0.75f);
        float swingRoot = Mth.sqrt(swing);
        float swingSine = Mth.sin(swingRoot * Mth.PI);
        pose.translate(
                side * -0.5f * swingSine,
                0.4f * Mth.sin(swingRoot * Mth.TWO_PI) - 0.3f * swingSine,
                -0.3f * Mth.sin(swing * Mth.PI));
        pose.rotateDegrees(Axis.XP, swingSine * -45f);
        pose.rotateDegrees(Axis.YP, side * swingSine * -30f);
    }

    /** One hand under a two-handed photo, as vanilla places it under a map. */
    private static void hand(RenderHandEvent event, PoseStack pose, AvatarRenderState avatar, HumanoidArm arm) {
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        pose.pushPose();
        pose.rotateDegrees(Axis.YP, 92);
        pose.rotateDegrees(Axis.XP, 45);
        pose.rotateDegrees(Axis.ZP, side * -41f);
        pose.translate(side * 0.3f, -1.1f, 0.45f);
        skin(event, pose, avatar, arm);
        pose.popPose();
    }

    /** The whole arm of a one-handed photo, reaching up from below the view as an arm does with a held item. */
    private static void arm(
            RenderHandEvent event, PoseStack pose, AvatarRenderState avatar, HumanoidArm arm, float side) {
        float swing = event.getSwingProgress();
        float swingRoot = Mth.sqrt(swing);
        pose.translate(
                side * (-0.3f * Mth.sin(swingRoot * Mth.PI) + 0.64000005f),
                0.4f * Mth.sin(swingRoot * Mth.TWO_PI) - 0.6f - event.getEquipProgress() * 0.6f,
                -0.4f * Mth.sin(swing * Mth.PI) - 0.71999997f);
        pose.rotateDegrees(Axis.YP, side * 45f);
        pose.rotateDegrees(Axis.YP, side * Mth.sin(swingRoot * Mth.PI) * 70f);
        pose.rotateDegrees(Axis.ZP, side * Mth.sin(swing * swing * Mth.PI) * -20f);
        pose.translate(side * -1f, 3.6f, 3.5f);
        pose.rotateDegrees(Axis.ZP, side * 120f);
        pose.rotateDegrees(Axis.XP, 200f);
        pose.rotateDegrees(Axis.YP, side * -135f);
        pose.translate(side * 5.6f, 0, 0);
        skin(event, pose, avatar, arm);
    }

    private static void skin(RenderHandEvent event, PoseStack pose, AvatarRenderState avatar, HumanoidArm arm) {
        AvatarRenderer<?> renderer =
                Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(avatar);
        PlayerRenderState state = event.getPlayerState();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        if (arm == HumanoidArm.RIGHT) {
            renderer.renderRightHand(
                    pose, collector, event.getPackedLight(), avatar.skin.body().texturePath(), avatar.showRightSleeve);
        } else {
            renderer.renderLeftHand(
                    pose, collector, event.getPackedLight(), avatar.skin.body().texturePath(), avatar.showLeftSleeve);
        }
    }

    /** The picture on a card, upright and facing the viewer, as the map is. */
    private static void photo(PoseStack pose, SubmitNodeCollector collector, int light, Picture picture) {
        pose.rotateDegrees(Axis.YP, 180);
        pose.rotateDegrees(Axis.ZP, 180);
        pose.scale(0.38f, 0.38f, 0.38f);
        pose.translate(-0.5f, -0.5f * Picture.HEIGHT / Picture.WIDTH, 0);
        pose.scale(UNIT, UNIT, UNIT);
        int border = PhotoTooltip.BORDER * 2;
        quad(pose, collector, CARD, light, 0, -border, -border, Picture.WIDTH + border, Picture.HEIGHT + border);
        // The picture sits a hair in front of its card, as a map does of its frame.
        quad(pose, collector, PhotoTooltip.texture(picture), light, -0.01f, 0, 0, Picture.WIDTH, Picture.HEIGHT);
    }

    private static void quad(
            PoseStack pose,
            SubmitNodeCollector collector,
            Identifier texture,
            int light,
            float z,
            int x0,
            int y0,
            int x1,
            int y1) {
        collector.submitCustomGeometry(pose, RenderTypes.text(texture), (last, buffer) -> {
            buffer.addVertex(last, x0, y1, z).setColor(-1).setUv(0, 1).setLight(light);
            buffer.addVertex(last, x1, y1, z).setColor(-1).setUv(1, 1).setLight(light);
            buffer.addVertex(last, x1, y0, z).setColor(-1).setUv(1, 0).setLight(light);
            buffer.addVertex(last, x0, y0, z).setColor(-1).setUv(0, 0).setLight(light);
        });
    }
}
