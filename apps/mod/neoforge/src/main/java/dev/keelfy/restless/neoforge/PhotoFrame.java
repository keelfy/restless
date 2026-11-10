package dev.keelfy.restless.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.Picture;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * A developed photo as an item frame shows it: the picture on its card, flat on the wall, as a map is. Chosen by the
 * photo's item model only for the frame's display context and only when the stack holds a picture.
 */
final class PhotoFrame implements SpecialModelRenderer<Picture> {
    static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());
    /** Of the picture, in units that are half a block as the frame scales its item: it hangs bare on the wall. */
    private static final float WIDTH = 1.3f;
    /** A pixel of a block, in the same units: the photo is a slab this thick, its back against the wall. */
    private static final float THICKNESS = 1f / 16 * 2;

    @Override
    public void submit(
            @Nullable Picture picture,
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            int overlay,
            boolean foil,
            int outline) {
        if (picture == null) return;
        pose.pushPose();
        // The model's own origin is a corner of its unit cube; its middle is where the frame holds the item.
        // Without its frame the item hangs exactly on the wall block's face; the front of the slab is a pixel out of
        // it.
        pose.translate(0.5f, 0.5f, 0.5f - THICKNESS);
        pose.rotateDegrees(Axis.ZP, 180);
        float unit = PhotoHands.unit(WIDTH);
        pose.scale(unit, unit, unit);
        pose.translate(-Picture.WIDTH / 2f, -Picture.HEIGHT / 2f, 0);
        PhotoHands.card(pose, collector, light, picture, THICKNESS / unit);
        pose.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(0, 0, 0.5f - THICKNESS));
        output.accept(new Vector3f(1, 1, 0.5f));
    }

    @Override
    public @Nullable Picture extractArgument(ItemStack stack) {
        return stack.get(Content.PICTURE.get());
    }

    record Unbaked() implements SpecialModelRenderer.Unbaked<Picture> {
        @Override
        public SpecialModelRenderer<Picture> bake(SpecialModelRenderer.BakingContext context) {
            return new PhotoFrame();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }
    }
}
