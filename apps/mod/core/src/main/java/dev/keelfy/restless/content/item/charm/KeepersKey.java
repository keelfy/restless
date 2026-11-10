package dev.keelfy.restless.content.item.charm;

import net.minecraft.world.item.Item;

/**
 * The house key with the hoarder's lock: bound to the first room it shelters, and there it never wears. Elsewhere it
 * does nothing. Crafted again with a campfire it forgets its room, see {@link Charms#keeps}.
 */
public final class KeepersKey extends Item {
    public KeepersKey(Item.Properties properties) {
        super(properties);
    }
}
