package dev.keelfy.restless.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.keelfy.restless.Restless;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.haunt.JournalPayload;
import dev.keelfy.restless.haunt.JournalScreen;
import dev.keelfy.restless.haunt.JournalView;
import dev.keelfy.restless.haunt.ReadPayload;
import dev.keelfy.restless.haunt.Session;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * Holds the journal open in both of the reader's hands, with its pages on the book. While the pointer key is held the
 * view freezes, the mouse moves a pointer over the pages instead, a click presses what is under it and the wheel turns
 * the page. The screen is the other way to read it.
 */
@EventBusSubscriber(modid = Restless.MOD_ID, value = Dist.CLIENT)
public final class JournalHands {
    /** Pixels of the book the pages are laid out for; the view is the book's face, not the window. */
    private static final int WIDTH = 400;

    private static final int HEIGHT = 300;
    /** How much of the window's width the book takes up, for a pointer that follows the mouse about as fast. */
    private static final float SPAN = 0.5f;

    static final KeyMapping POINT =
            new KeyMapping("key.restless.journal_point", InputConstants.KEY_R, KeyMapping.Category.MISC);

    private static JournalView view;
    private static JournalPayload shown;
    private static boolean wasHeld;
    /** The hint about the pointer key was shown in this world. */
    private static boolean hinted;
    /** The attack key is still down from a click that was already handled: one press is one click. */
    private static boolean clicked;

    private static float lift;
    private static double pointerX = WIDTH / 2.0;
    private static double pointerY = HEIGHT / 2.0;

    private JournalHands() {}

    private static boolean holding(Minecraft minecraft) {
        return minecraft.player != null
                && (minecraft.player.getMainHandItem().is(Content.JOURNAL.get())
                        || minecraft.player.getOffhandItem().is(Content.JOURNAL.get()));
    }

    /** Pointing at the pages: the key is held over an open book, with no screen in the way. */
    private static boolean pointing(Minecraft minecraft) {
        return view != null && holding(minecraft) && minecraft.gui.screen() == null && POINT.isDown();
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean held = holding(minecraft);
        if (held && !wasHeld && minecraft.getConnection() != null) {
            minecraft.getConnection().send(new ServerboundCustomPayloadPacket(new ReadPayload()));
        }
        if (held && !hinted) {
            hinted = true;
            minecraft.gui.hud.setOverlayMessage(
                    Component.translatable("hud.restless.journal_point", POINT.getTranslatedKeyMessage()), false);
        }
        wasHeld = held;
        if (!minecraft.options.keyAttack.isDown()) {
            clicked = false;
        }
        JournalPayload latest = JournalScreen.latest();
        if (latest == null) {
            view = null;
            shown = null;
        } else if (latest != shown) {
            shown = latest;
            if (view == null) {
                view = new JournalView(minecraft.font, latest, null);
                view.layout(WIDTH, HEIGHT);
            } else {
                view.update(latest);
            }
        }
    }

    @SubscribeEvent
    public static void leave(ClientPlayerNetworkEvent.LoggingOut event) {
        JournalScreen.forget();
        Session.CLIENT.end();
        view = null;
        shown = null;
        wasHeld = false;
        hinted = false;
    }

    /** The camera stays where it is while pointing; the mouse's travel moves the pointer. */
    @SubscribeEvent
    public static void turn(CalculatePlayerTurnEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!pointing(minecraft)) return;
        double pixels = WIDTH / (SPAN * minecraft.getWindow().getScreenWidth());
        pointerX = Mth.clamp(
                pointerX + minecraft.mouseHandler.getXVelocity() * pixels, 0, WIDTH + JournalView.OVERHANG - 1);
        pointerY = Mth.clamp(pointerY + minecraft.mouseHandler.getYVelocity() * pixels, 0, HEIGHT - 1);
        // A sensitivity of -1/3 comes out of the game's own formula as no turn at all.
        event.setMouseSensitivity(-1 / 3.0);
        event.setCinematicCameraEnabled(false);
    }

    @SubscribeEvent
    public static void click(InputEvent.InteractionKeyMappingTriggered event) {
        if (!pointing(Minecraft.getInstance())) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        if (event.isAttack() && !clicked) {
            clicked = true;
            view.click(pointerX, pointerY);
        }
    }

    @SubscribeEvent
    public static void scroll(InputEvent.MouseScrollingEvent event) {
        if (!pointing(Minecraft.getInstance())) return;
        event.setCanceled(true);
        if (event.getScrollDeltaY() != 0) {
            view.turn(event.getScrollDeltaY() < 0 ? 1 : -1);
        }
    }

    @SubscribeEvent
    public static void render(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!holding(minecraft)) return;
        event.setCanceled(true);
        InteractionHand hand = minecraft.player.getMainHandItem().is(Content.JOURNAL.get())
                ? InteractionHand.MAIN_HAND
                : InteractionHand.OFF_HAND;
        if (event.getHand() != hand) return;
        var avatar = event.getPlayerState().avatarRenderState;
        if (avatar == null) return;
        boolean pointing = pointing(minecraft);
        lift += ((pointing ? 1 : 0) - lift) * 0.3f;
        PoseStack pose = event.getPoseStack();
        PhotoHands.twoHanded(event, pose, avatar, lift);
        if (!(minecraft.gui.screen() instanceof JournalScreen)) {
            pose.scale(0.45f, 0.45f * HEIGHT / WIDTH, 0.45f);
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
            if (view != null) {
                HeldCanvas canvas =
                        new HeldCanvas(pose, event.getSubmitNodeCollector(), event.getPackedLight(), WIDTH, HEIGHT);
                view.draw(canvas);
                view.drawControls(canvas, pointing ? pointerX : -1, pointing ? pointerY : -1);
                if (pointing) {
                    canvas.pointer((int) pointerX, (int) pointerY);
                }
            }
        }
        pose.popPose();
    }
}
