package dev.keelfy.restless.haunt.event.kit;

import dev.keelfy.restless.Restless;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** The kinds of blocks and items haunt events look for; members live in {@code data/restless/tags}. */
public final class RestlessTags {
    private RestlessTags() {}

    /** Glass blocks and panes, plain, stained or tinted. */
    static final TagKey<Block> GLASS = block("glass");
    /** Torches and lanterns that can burst. */
    public static final TagKey<Block> BURSTABLE_LIGHTS = block("burstable_lights");
    /** Containers that sound hollow when knocked. */
    public static final TagKey<Block> KNOCKABLE = block("knockable");
    /** Loose ground a trickster can bury a prize in. */
    public static final TagKey<Block> DIGGABLE = block("diggable");

    public static final TagKey<Block> FURNACES = block("furnaces");
    /** Exposed blocks that look placed by hand, not part of the land. */
    public static final TagKey<Block> HAND_BUILT = block("hand_built");

    public static final TagKey<Item> STEWS = item("stews");
    /** What a household spirit is left to eat. */
    public static final TagKey<Item> HOUSEHOLD_FOOD = item("household_food");

    public static final TagKey<Item> POTIONS = item("potions");
    /** Torches and lanterns carried in the inventory. */
    public static final TagKey<Item> HELD_LIGHTS = item("held_lights");

    private static TagKey<Block> block(String name) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Restless.MOD_ID, name));
    }

    private static TagKey<Item> item(String name) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Restless.MOD_ID, name));
    }
}
