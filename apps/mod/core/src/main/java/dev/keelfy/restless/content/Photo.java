package dev.keelfy.restless.content;

import java.util.Optional;
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
}
