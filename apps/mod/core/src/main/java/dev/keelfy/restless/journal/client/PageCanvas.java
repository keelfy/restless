package dev.keelfy.restless.journal.client;

import dev.keelfy.restless.photo.Picture;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * What a {@link JournalView} draws on, in book pixels with the origin at the top left: the screen, or the book in hand.
 */
@OnlyIn(Dist.CLIENT)
public interface PageCanvas {
    void fill(int x0, int y0, int x1, int y1, int color);

    void text(FormattedCharSequence text, int x, int y, int color);

    /** An item icon, 16 by 16. */
    void item(ItemStack stack, int x, int y);

    /** A developed photo in its pale border, see {@link dev.keelfy.restless.photo.client.PhotoTooltip#draw}. */
    void picture(Picture picture, int x, int y);
}
