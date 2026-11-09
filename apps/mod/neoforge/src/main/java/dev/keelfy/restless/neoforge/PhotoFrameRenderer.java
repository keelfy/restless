package dev.keelfy.restless.neoforge;

import dev.keelfy.restless.content.Content;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.world.entity.decoration.ItemFrame;

/**
 * The item frame of vanilla, except that a developed photo hangs on the wall without the frame, as in an invisible one:
 * the photo has its own card.
 */
final class PhotoFrameRenderer<T extends ItemFrame> extends ItemFrameRenderer<T> {
    PhotoFrameRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void extractRenderState(T entity, ItemFrameRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (entity.getItem().has(Content.PICTURE.get())) {
            state.isInvisible = true;
            state.frameModel.clear();
        }
    }
}
