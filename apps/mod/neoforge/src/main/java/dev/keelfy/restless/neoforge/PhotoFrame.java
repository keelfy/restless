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
    /** Half a block across, as the frame's own scale makes a model one unit wide; the card fills most of the frame. */
    private static final float WIDTH = 1.15f;

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
        pose.translate(0.5f, 0.5f, 0.5f);
        pose.rotateDegrees(Axis.ZP, 180);
        float unit = PhotoHands.unit(WIDTH);
        pose.scale(unit, unit, unit);
        pose.translate(-Picture.WIDTH / 2f, -Picture.HEIGHT / 2f, 0);
        PhotoHands.card(pose, collector, light, picture);
        pose.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(0, 0, 0.5f));
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
