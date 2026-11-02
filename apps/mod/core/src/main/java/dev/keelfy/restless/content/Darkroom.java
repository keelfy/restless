package dev.keelfy.restless.content;

import com.mojang.blaze3d.platform.NativeImage;
import dev.keelfy.restless.haunt.DevelopPayload;
import dev.keelfy.restless.haunt.ShutterPayload;
import java.util.Optional;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
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
 * puts the spirit in, looking as it does, as a pale figure where it stands, and sends it back as the photo's picture.
 * Spirits have no model of their own, so the figure exists only on film.
 */
public final class Darkroom {
    /** How tall a spirit stands, in blocks, measured up from its point. */
    private static final double SPIRIT_HEIGHT = 1.9;

    private static final int FLASH_TICKS = 6;

    private static @Nullable ShutterPayload pending;
    private static @Nullable CameraType viewBeforeShutter;
    private static long flashedAt = Long.MIN_VALUE;

    private Darkroom() {}

    /**
     * The lens sits at the eye, so the frame is always drawn from there: from behind or in front of the player the
     * camera itself, and the player holding it, would be in the photo. The view is put back once the frame is grabbed.
     */
    public static void shutter(ShutterPayload payload) {
        pending = payload;
        Minecraft minecraft = Minecraft.getInstance();
        if (viewBeforeShutter == null && !minecraft.options.getCameraType().isFirstPerson()) {
            viewBeforeShutter = minecraft.options.getCameraType();
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        }
    }

    /** Called once the world of a frame is drawn, before the hand and the HUD. */
    public static void afterLevel() {
        ShutterPayload shot = pending;
        Minecraft minecraft = Minecraft.getInstance();
        if (shot == null) {
            return;
        }
        pending = null;
        restoreView(minecraft);
        if (minecraft.level == null) {
            return;
        }
        flashedAt = minecraft.level.getGameTime();
        Camera camera = minecraft.gameRenderer.mainCamera();
        Matrix4f projection = camera.getViewRotationProjectionMatrix(new Matrix4f());
        Vec3 eye = camera.position();
        Optional<Vec3> at = shot.at();
        Screenshot.takeScreenshot(minecraft.gameRenderer.mainRenderTarget(), frame -> {
            try (frame) {
                byte[] pixels = develop(
                        frame,
                        shot.film(),
                        Apparition.of(shot.spirit(), shot.seed()),
                        at.map(p -> project(projection, eye, p, frame)));
                if (minecraft.getConnection() != null) {
                    minecraft
                            .getConnection()
                            .send(new ServerboundCustomPayloadPacket(
                                    new DevelopPayload(shot.film(), new Picture(pixels))));
                }
            }
        });
    }

    private static void restoreView(Minecraft minecraft) {
        if (viewBeforeShutter != null) {
            minecraft.options.setCameraType(viewBeforeShutter);
            viewBeforeShutter = null;
        }
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
    private static byte[] develop(NativeImage frame, int film, Apparition look, Optional<Figure> seen) {
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
        look.paint(grey, feetX, feetY - tall, tall);

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
}
