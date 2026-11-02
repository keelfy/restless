package dev.keelfy.restless.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.keelfy.restless.Restless;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.haunt.JournalScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/** Holds the open journal at the centre of the view, with both of the reader's hands. */
@EventBusSubscriber(modid = Restless.MOD_ID, value = Dist.CLIENT)
public final class JournalHands {
    private JournalHands() {}

    @SubscribeEvent
    public static void render(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        boolean main = minecraft.player.getMainHandItem().is(Content.JOURNAL.get());
        boolean off = minecraft.player.getOffhandItem().is(Content.JOURNAL.get());
        if (!main && !off) return;
        event.setCanceled(true);
        InteractionHand hand = main ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (event.getHand() != hand) return;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(0, -0.28f - event.getEquipProgress() * 0.65f, -0.85f);
        var avatar = event.getPlayerState().avatarRenderState;
        if (avatar != null && !avatar.isInvisible) {
            AvatarRenderer<?> renderer = minecraft.getEntityRenderDispatcher().getRenderer(avatar);
            for (int side : new int[] {-1, 1}) {
                pose.pushPose();
                pose.rotateDegrees(Axis.YP, 182);
                pose.rotateDegrees(Axis.XP, 45);
                pose.rotateDegrees(Axis.ZP, side * -41);
                pose.translate(side * .3f, -1.1f, .45f);
                if (side == 1) {
                    renderer.renderRightHand(
                            pose,
                            event.getSubmitNodeCollector(),
                            event.getPackedLight(),
                            avatar.skin.body().texturePath(),
                            avatar.showRightSleeve);
                } else {
                    renderer.renderLeftHand(
                            pose,
                            event.getSubmitNodeCollector(),
                            event.getPackedLight(),
                            avatar.skin.body().texturePath(),
                            avatar.showLeftSleeve);
                }
                pose.popPose();
            }
        }
        if (!(minecraft.gui.screen() instanceof JournalScreen)) {
            pose.rotateDegrees(Axis.XP, -12);
            ItemStackRenderState model = new ItemStackRenderState();
            minecraft
                    .getItemModelResolver()
                    .updateForTopItem(
                            model,
                            minecraft.player.getItemInHand(hand),
                            ItemDisplayContext.FIXED,
                            minecraft.level,
                            minecraft.player,
                            0);
            model.submit(pose, event.getSubmitNodeCollector(), event.getPackedLight(), OverlayTexture.NO_OVERLAY, 0);
        }
        pose.popPose();
    }
}
