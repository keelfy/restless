package dev.keelfy.restless.content;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.platform.Platform;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Everything the mod adds to the game's registries; the loader fills them through {@link Platform#register}. */
public final class Content {
    public static final Supplier<Block> TEST_BLOCK = Platform.get().register(BuiltInRegistries.BLOCK, "test_block",
        key -> new Block(BlockBehaviour.Properties.of().setId(key).strength(1.5f)));
    public static final Supplier<Item> TEST_ITEM = Platform.get().register(BuiltInRegistries.ITEM, "test_block",
        key -> new BlockItem(TEST_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    private Content() {
    }

    /** Forces the class to load, which declares every entry. */
    public static void init() {
        Restless.LOGGER.debug("Content declared");
    }
}
