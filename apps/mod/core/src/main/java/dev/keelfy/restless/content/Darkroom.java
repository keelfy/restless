package dev.keelfy.restless.content;

import com.mojang.blaze3d.platform.NativeImage;
import dev.keelfy.restless.haunt.DevelopPayload;
import dev.keelfy.restless.haunt.ShutterPayload;
import java.util.Optional;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

/**
 * The client side of the camera obscura: grabs the frame after the world is drawn (so without hand or HUD), greys it,
 * puts the spirit in as a pale figure where it stands, and sends it back as the photo's picture. Spirits have no model
 * of their own, so the figure exists only on film.
 */
public final class Darkroom {
    /** How tall a spirit stands, in blocks, measured up from its point. */
    private static final double SPIRIT_HEIGHT = 1.9;

    private static final int FLASH_TICKS = 6;

    private static @Nullable ShutterPayload pending;
    private static long flashedAt = Long.MIN_VALUE;

    private Darkroom() {}

    public static void shutter(ShutterPayload payload) {
        pending = payload;
    }

    /** Called once the world of a frame is drawn, before the hand and the HUD. */
    public static void afterLevel() {
        ShutterPayload shot = pending;
        Minecraft minecraft = Minecraft.getInstance();
        if (shot == null || minecraft.level == null) {
            return;
        }
        pending = null;
        flashedAt = minecraft.level.getGameTime();
        Camera camera = minecraft.gameRenderer.mainCamera();
        Matrix4f projection = camera.getViewRotationProjectionMatrix(new Matrix4f());
        Vec3 eye = camera.position();
        Optional<Vec3> at = shot.at();
        Screenshot.takeScreenshot(minecraft.gameRenderer.mainRenderTarget(), frame -> {
            try (frame) {
                byte[] pixels = develop(frame, shot.film(), at.map(p -> project(projection, eye, p, frame)));
                if (minecraft.getConnection() != null) {
                    minecraft
                            .getConnection()
                            .send(new ServerboundCustomPayloadPacket(
                                    new DevelopPayload(shot.film(), new Picture(pixels))));
                }
            }
        });
    }

    /** The camera flash, as a HUD layer: drawn after the frame was grabbed, so never on the photo. */
    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        double elapsed = minecraft.level.getGameTime() - flashedAt + delta.getGameTimeDeltaPartialTick(false);
        if (elapsed < 0 || elapsed >= FLASH_TICKS) {
            return;
        }
        int alpha = (int) (200 * (1 - elapsed / FLASH_TICKS));
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), ARGB.color(alpha, 255, 250, 235));
    }

    /** Where in the frame the spirit stands: its feet and its height, in frame pixels; null when out of sight. */
    private record Figure(double x, double y, double height) {}

    private static @Nullable Figure project(Matrix4f projection, Vec3 eye, Vec3 at, NativeImage frame) {
        Vector4f feet = clip(projection, at.subtract(eye));
        Vector4f head = clip(projection, at.add(0, SPIRIT_HEIGHT, 0).subtract(eye));
        if (feet.w <= 0.05f || head.w <= 0.05f) {
            return null;
        }
        double x = (feet.x / feet.w + 1) / 2 * frame.getWidth();
        double y = (1 - feet.y / feet.w) / 2 * frame.getHeight();
        double top = (1 - head.y / head.w) / 2 * frame.getHeight();
        return new Figure(x, y, y - top);
    }

    private static Vector4f clip(Matrix4f projection, Vec3 offset) {
        return projection.transform(new Vector4f((float) offset.x, (float) offset.y, (float) offset.z, 1));
    }

    /** The frame cropped to the picture's shape, scaled down, greyed and aged, with the spirit in it. */
    private static byte[] develop(NativeImage frame, int film, Optional<Figure> seen) {
        int width = frame.getWidth();
        int height = frame.getHeight();
        double scale = Math.min((double) width / Picture.WIDTH, (double) height / Picture.HEIGHT);
        double left = (width - Picture.WIDTH * scale) / 2;
        double top = (height - Picture.HEIGHT * scale) / 2;
        int step = Math.max(1, (int) (scale / 3));

        double[] grey = new double[Picture.SIZE];
        for (int y = 0; y < Picture.HEIGHT; y++) {
            for (int x = 0; x < Picture.WIDTH; x++) {
                int x0 = (int) (left + x * scale);
                int y0 = (int) (top + y * scale);
                int x1 = Math.max(x0 + 1, Math.min(width, (int) (left + (x + 1) * scale)));
                int y1 = Math.max(y0 + 1, Math.min(height, (int) (top + (y + 1) * scale)));
                double sum = 0;
                int count = 0;
                for (int sy = y0; sy < y1; sy += step) {
                    for (int sx = x0; sx < x1; sx += step) {
                        int argb = frame.getPixel(sx, sy);
                        sum += 0.299 * ARGB.red(argb) + 0.587 * ARGB.green(argb) + 0.114 * ARGB.blue(argb);
                        count++;
                    }
                }
                grey[x + y * Picture.WIDTH] = (sum / count - 128) * 1.15 + 132;
            }
        }

        Random random = new Random(film);
        double feetX = seen.map(f -> (f.x() - left) / scale).orElse(-1.0);
        double feetY = seen.map(f -> (f.y() - top) / scale).orElse(-1.0);
        double tall = seen.map(f -> Mth.clamp(f.height() / scale, 14, Picture.HEIGHT * 1.2))
                .orElse(0.0);
        // Out of the frame, or nowhere known: it shows anyway, somewhere it should not be.
        if (feetX < 0 || feetX > Picture.WIDTH || feetY <= 0 || feetY - tall >= Picture.HEIGHT) {
            tall = 34 + random.nextInt(30);
            feetX = 14 + random.nextInt(Picture.WIDTH - 28);
            feetY = Math.min(Picture.HEIGHT + 4, tall + 6 + random.nextInt(Math.max(1, Picture.HEIGHT - (int) tall)));
        }
        haunt(grey, feetX, feetY - tall, tall, random.nextDouble() * Math.PI * 2);

        byte[] pixels = new byte[Picture.SIZE];
        for (int y = 0; y < Picture.HEIGHT; y++) {
            for (int x = 0; x < Picture.WIDTH; x++) {
                double dx = (x - Picture.WIDTH / 2.0) / (Picture.WIDTH / 2.0);
                double dy = (y - Picture.HEIGHT / 2.0) / (Picture.HEIGHT / 2.0);
                double vignette = 1 - 0.5 * (dx * dx + dy * dy) / 2;
                double value = grey[x + y * Picture.WIDTH] * vignette + random.nextGaussian() * 7;
                pixels[x + y * Picture.WIDTH] = (byte) Mth.clamp((int) value, 0, 255);
            }
        }
        return pixels;
    }

    /**
     * Lays a pale, hollow-eyed figure {@code tall} pixels high, its head at ({@code cx}, {@code top}), over the grey.
     */
    private static void haunt(double[] grey, double cx, double top, double tall, double phase) {
        for (int y = 0; y < Picture.HEIGHT; y++) {
            for (int x = 0; x < Picture.WIDTH; x++) {
                double u = (x - cx) / tall;
                double t = (y - top) / tall;
                if (t < -0.15 || t > 1.05 || Math.abs(u) > 0.5) {
                    continue;
                }
                double head = soft(0.1 - Math.hypot(u, t - 0.12), 0.035);
                double half = 0.07 + 0.2 * Math.max(0, t - 0.2) + 0.025 * Math.sin(t * 14 + phase);
                double body = t < 0.19 ? 0 : soft(half - Math.abs(u), 0.06) * Math.pow(Math.max(0, 1 - t), 0.6);
                double halo = soft(0.22 - Math.hypot(u * 1.4, (t - 0.45) * 0.8), 0.2) * 0.25;
                double eyes = soft(
                        0.022 - Math.min(Math.hypot(u - 0.038, t - 0.115), Math.hypot(u + 0.038, t - 0.115)), 0.012);
                double mist = Math.max(head, body) * 0.72 + halo;
                int i = x + y * Picture.WIDTH;
                grey[i] += (238 - grey[i]) * Math.min(1, mist);
                grey[i] *= 1 - 0.75 * eyes * head;
            }
        }
    }

    /** 0 outside, 1 inside, ramping over {@code edge} across the border of a signed distance. */
    private static double soft(double distance, double edge) {
        return Mth.clamp(distance / edge + 0.5, 0, 1);
    }
}
