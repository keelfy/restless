package dev.keelfy.restless.content.item;

import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.Picture;
import java.util.Optional;
import java.util.Random;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A camera obscura photograph; once developed, its tooltip shows the picture. */
public final class Photo extends Item {
    public Photo(Item.Properties properties) {
        super(properties);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.ofNullable(stack.get(Content.PICTURE.get()));
    }

    /**
     * The picture with a pale, hollow-eyed figure added somewhere in it, {@code tall} pixels high, at a place the
     * random picks. The same figure the camera paints for a spirit; this one was never there when the photo was taken.
     */
    public static Picture withFigure(Picture picture, Random random, int tall) {
        double cx = 14 + random.nextInt(Picture.WIDTH - 28);
        double top = random.nextInt(Math.max(1, Picture.HEIGHT - tall));
        double phase = random.nextDouble() * Math.PI * 2;
        byte[] pixels = picture.pixels().clone();
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
                double grey = pixels[i] & 0xFF;
                grey += (238 - grey) * Math.min(1, mist);
                grey *= 1 - 0.75 * eyes * head;
                pixels[i] = (byte) Mth.clamp((int) grey, 0, 255);
            }
        }
        return new Picture(pixels);
    }

    /** 0 outside, 1 inside, ramping over {@code edge} across the border of a signed distance. */
    private static double soft(double distance, double edge) {
        return Mth.clamp(distance / edge + 0.5, 0, 1);
    }
}
