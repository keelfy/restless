package dev.keelfy.restless.haunt;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;

/**
 * The client side of the {@code turned_head} event. The server can only set the view a tick at a time, which steps; here
 * it turns a little every frame, as the mouse turns it, slow at the start and the end. The mouse still works meanwhile.
 */
public final class HeadTurn {
    private static long start;
    private static int total;
    private static float degrees;
    /** The eased share of the turn already made. */
    private static double done;

    private HeadTurn() {}

    public static void start(float yaw, int ticks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && ticks > 0) {
            start = minecraft.level.getGameTime();
            total = ticks;
            degrees = yaw;
            done = 0;
        }
    }

    /** Call once per frame, before it is drawn; does nothing when no turn is running. */
    public static void frame(DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (total == 0 || minecraft.level == null || minecraft.player == null) {
            return;
        }
        double progress = Math.clamp((minecraft.level.getGameTime() - start + delta.getGameTimeDeltaPartialTick(false)) / total, 0.0, 1.0);
        double eased = progress * progress * (3 - 2 * progress);
        float step = (float) (degrees * (eased - done));
        done = eased;
        // Both the rotation and the one the frame interpolates from, as a mouse turn does, so nothing snaps back.
        minecraft.player.setYRot(minecraft.player.getYRot() + step);
        minecraft.player.yRotO += step;
        if (progress >= 1.0) {
            total = 0;
        }
    }
}
