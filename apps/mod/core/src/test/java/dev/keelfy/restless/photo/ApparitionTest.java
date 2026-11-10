package dev.keelfy.restless.photo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ApparitionTest {
    private static final List<String> TYPES = List.of(
            "shade",
            "phantom",
            "mimic",
            "wraith",
            "mara",
            "banshee",
            "revenant",
            "lost_one",
            "poltergeist",
            "nix",
            "domovoi",
            "hoarder",
            "demon",
            "trickster",
            "boggart");

    private static double[] paint(String spirit, long seed) {
        double[] grey = new double[Picture.SIZE];
        Arrays.fill(grey, 110);
        Apparition.of(spirit, seed).paint(grey, Picture.WIDTH / 2.0, 8, 80);
        return grey;
    }

    @Test
    void sameSpiritLooksTheSameAndEveryTypeDiffers() {
        List<double[]> seen = new ArrayList<>();
        for (String type : TYPES) {
            assertArrayEquals(paint(type, 7), paint(type, 7));
            double[] figure = paint(type, 7);
            for (double[] other : seen) {
                assertFalse(Arrays.equals(other, figure), type + " looks like another type");
            }
            seen.add(figure);
        }
    }

    @Test
    void spiritsOfOneTypeAreKinButNotTwins() {
        for (String type : TYPES) {
            assertFalse(Arrays.equals(paint(type, 1), paint(type, 2)), type);
        }
    }

    /** Not an assertion: a contact sheet of every type with three seeds each, to look at in build/apparitions.png. */
    @Test
    void contactSheet() throws IOException {
        BufferedImage sheet =
                new BufferedImage(Picture.WIDTH * 3, Picture.HEIGHT * TYPES.size(), BufferedImage.TYPE_INT_RGB);
        for (int row = 0; row < TYPES.size(); row++) {
            for (int column = 0; column < 3; column++) {
                double[] grey = paint(TYPES.get(row), column * 977L + 3);
                for (int i = 0; i < Picture.SIZE; i++) {
                    int v = (int) Math.max(0, Math.min(255, grey[i]));
                    sheet.setRGB(
                            column * Picture.WIDTH + i % Picture.WIDTH,
                            row * Picture.HEIGHT + i / Picture.WIDTH,
                            v * 0x010101);
                }
            }
        }
        File out = new File("build/apparitions.png");
        out.getParentFile().mkdirs();
        ImageIO.write(sheet, "png", out);
    }
}
