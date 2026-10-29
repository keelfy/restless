package dev.keelfy.restless.content;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.platform.Platform;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Everything the mod adds to the game's registries; the loader fills them through {@link Platform#register}. */
public final class Content {
    public static final Supplier<Block> TEST_BLOCK = Platform.get().register(BuiltInRegistries.BLOCK, "test_block",
        key -> new Block(BlockBehaviour.Properties.of().setId(key).strength(1.5f)));
    public static final Supplier<Item> TEST_ITEM = blockItem("test_block", TEST_BLOCK);

    public static final Supplier<Block> SALT_LINE = Platform.get().register(BuiltInRegistries.BLOCK, "salt_line",
        key -> new SaltLineBlock(BlockBehaviour.Properties.of().setId(key)));
    /** Salt is the block item of the salt line, placed like redstone dust. */
    public static final Supplier<Item> SALT = Platform.get().register(BuiltInRegistries.ITEM, "salt",
        key -> new BlockItem(SALT_LINE.get(), new Item.Properties().setId(key)));

    public static final Supplier<Block> ASH_LINE = Platform.get().register(BuiltInRegistries.BLOCK, "ash_line",
        key -> new AshBlock(BlockBehaviour.Properties.of().setId(key)));
    public static final Supplier<Item> ASH = Platform.get().register(BuiltInRegistries.ITEM, "ash",
        key -> new BlockItem(ASH_LINE.get(), new Item.Properties().setId(key)));

    public static final Supplier<Item> SPIRIT_CANDLE = item("spirit_candle", p -> new Item(p.stacksTo(1)));
    public static final Supplier<Item> SPIRIT_COMPASS = item("spirit_compass", p -> new Item(p.stacksTo(1)));
    public static final Supplier<Item> CAMERA_OBSCURA = item("camera_obscura", p -> new CameraObscura(p.stacksTo(1)));
    public static final Supplier<Item> PHOTO = item("photo", p -> new Item(p.stacksTo(1)));

    public static final Supplier<Block> SPIRIT_BOX = block("spirit_box",
        p -> new ShapedBlock(p.strength(1.0f).sound(SoundType.METAL).noOcclusion(), Block.box(3, 0, 3, 13, 6, 13)));
    public static final Supplier<Item> SPIRIT_BOX_ITEM = blockItem("spirit_box", SPIRIT_BOX);
    public static final Supplier<Block> OUIJA_BOARD = block("ouija_board", OuijaBoard::new);
    public static final Supplier<Item> OUIJA_BOARD_ITEM = blockItem("ouija_board", OUIJA_BOARD);

    public static final Supplier<Block> DOOR_CHARM = block("door_charm",
        p -> new WornWard(p.instabreak().sound(SoundType.WOOL).noOcclusion(), Block.box(5, 0, 7, 11, 10, 9), WardBlock.Kind.CHARM, 5));
    public static final Supplier<Item> DOOR_CHARM_ITEM = blockItem("door_charm", DOOR_CHARM);
    public static final Supplier<Block> DREAMCATCHER = block("dreamcatcher",
        p -> new Dreamcatcher(p.instabreak().sound(SoundType.WOOL).noOcclusion()));
    public static final Supplier<Item> DREAMCATCHER_ITEM = blockItem("dreamcatcher", DREAMCATCHER);
    public static final Supplier<BlockEntityType<?>> DREAMCATCHER_ENTITY = Platform.get().register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "dreamcatcher",
        key -> new BlockEntityType<>(DreamcatcherEntity::new, Set.of(DREAMCATCHER.get())));
    public static final Supplier<Block> HALLOWED_LANTERN = block("hallowed_lantern",
        p -> new WardLantern(p.strength(1.0f).sound(SoundType.LANTERN).lightLevel(s -> 12).noOcclusion()));
    public static final Supplier<Item> HALLOWED_LANTERN_ITEM = blockItem("hallowed_lantern", HALLOWED_LANTERN);
    public static final Supplier<Block> SCARECROW = block("scarecrow",
        p -> new WornWard(p.strength(0.5f).sound(SoundType.GRASS).noOcclusion(), Block.box(1, 0, 6, 15, 16, 10), WardBlock.Kind.SCARECROW, 10));
    public static final Supplier<Item> SCARECROW_ITEM = blockItem("scarecrow", SCARECROW);

    public static final Supplier<Block> ALTAR = block("altar",
        p -> new Altar(p.strength(2.0f).sound(SoundType.STONE).noOcclusion()));
    public static final Supplier<Item> ALTAR_ITEM = blockItem("altar", ALTAR);
    public static final Supplier<BlockEntityType<?>> ALTAR_ENTITY = Platform.get().register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "altar",
        key -> new BlockEntityType<>(AltarEntity::new, Set.of(ALTAR.get())));
    public static final Supplier<Item> HOLY_WATER = item("holy_water", p -> new Item(p.stacksTo(16)));
    public static final Supplier<Item> MIRROR = item("mirror", p -> new Item(p.stacksTo(16)));
    /** A banished spirit drops the essence named after its type: {@code <type>_essence}. */
    public static final Supplier<Item> POLTERGEIST_ESSENCE = item("poltergeist_essence", Item::new);
    public static final Supplier<Item> SHADE_ESSENCE = item("shade_essence", Item::new);
    public static final Supplier<Item> TELEKINESIS_AMULET = item("telekinesis_amulet", p -> new TelekinesisAmulet(p.stacksTo(1)));
    public static final Supplier<Item> DARK_EYE = item("dark_eye", p -> new DarkEye(p.stacksTo(1)));

    /** Built by the world, never crafted: it drops nothing. */
    public static final Supplier<Block> GRAVESTONE = block("gravestone",
        p -> new Gravestone(p.strength(2.0f, 6.0f).sound(SoundType.STONE).noOcclusion()));
    public static final Supplier<Item> GRAVESTONE_ITEM = blockItem("gravestone", GRAVESTONE);
    public static final Supplier<Item> JOURNAL = item("journal", p -> new JournalBook(p.stacksTo(1)));

    /** Ground mist of the blighted biome, see {@link MistParticle}. */
    public static final Supplier<ParticleType<?>> MIST = Platform.get().register(BuiltInRegistries.PARTICLE_TYPE, "mist", key -> new SimpleParticleType(false) {});

    private Content() {
    }

    private static Supplier<Item> item(String name, java.util.function.Function<Item.Properties, Item> factory) {
        return Platform.get().register(BuiltInRegistries.ITEM, name, key -> factory.apply(new Item.Properties().setId(key)));
    }

    private static Supplier<Block> block(String name, java.util.function.Function<BlockBehaviour.Properties, Block> factory) {
        return Platform.get().register(BuiltInRegistries.BLOCK, name, key -> factory.apply(BlockBehaviour.Properties.of().setId(key)));
    }

    private static Supplier<Item> blockItem(String name, Supplier<Block> block) {
        return Platform.get().register(BuiltInRegistries.ITEM, name,
            key -> new BlockItem(block.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));
    }

    /** Forces the class to load, which declares every entry and the creative tab that holds the items. */
    public static void init() {
        var tabItems = new java.util.ArrayList<Supplier<Item>>(UnlitLights.ITEMS);
        tabItems.addAll(List.of(SALT, ASH, SPIRIT_CANDLE, SPIRIT_COMPASS, CAMERA_OBSCURA, SPIRIT_BOX_ITEM, OUIJA_BOARD_ITEM,
            DOOR_CHARM_ITEM, DREAMCATCHER_ITEM, HALLOWED_LANTERN_ITEM, SCARECROW_ITEM, ALTAR_ITEM, HOLY_WATER, MIRROR,
            POLTERGEIST_ESSENCE, SHADE_ESSENCE, TELEKINESIS_AMULET, DARK_EYE, JOURNAL, GRAVESTONE_ITEM, TEST_ITEM));
        Platform.get().registerTab("restless", Component.translatable("itemGroup.restless"), () -> new ItemStack(SPIRIT_CANDLE.get()),
            tabItems);
        Restless.LOGGER.debug("Content declared");
    }
}
