package dev.keelfy.restless.content;

import com.mojang.serialization.Codec;
import dev.keelfy.restless.Restless;
import dev.keelfy.restless.platform.Platform;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Everything the mod adds to the game's registries; the loader fills them through {@link Platform#register}. */
public final class Content {
    public static final Supplier<Block> TEST_BLOCK = Platform.get()
            .register(
                    BuiltInRegistries.BLOCK,
                    "test_block",
                    key -> new Block(BlockBehaviour.Properties.of().setId(key).strength(1.5f)));
    public static final Supplier<Item> TEST_ITEM = blockItem("test_block", TEST_BLOCK);

    public static final Supplier<Block> SALT_LINE = Platform.get()
            .register(
                    BuiltInRegistries.BLOCK,
                    "salt_line",
                    key -> new SaltLineBlock(BlockBehaviour.Properties.of().setId(key)));
    /** Salt is the block item of the salt line, placed like redstone dust. */
    public static final Supplier<Item> SALT = Platform.get()
            .register(
                    BuiltInRegistries.ITEM,
                    "salt",
                    key -> new BlockItem(SALT_LINE.get(), new Item.Properties().setId(key)));

    public static final Supplier<Block> ASH_LINE = Platform.get()
            .register(
                    BuiltInRegistries.BLOCK,
                    "ash_line",
                    key -> new AshBlock(BlockBehaviour.Properties.of().setId(key)));
    public static final Supplier<Item> ASH = Platform.get()
            .register(
                    BuiltInRegistries.ITEM,
                    "ash",
                    key -> new BlockItem(ASH_LINE.get(), new Item.Properties().setId(key)));

    public static final Supplier<EntityType<?>> ASH_FOOTPRINT = Platform.get()
            .register(
                    BuiltInRegistries.ENTITY_TYPE,
                    "ash_footprint",
                    key -> EntityType.Builder.<AshFootprint>of(AshFootprint::new, MobCategory.MISC)
                            .sized(.5f, .02f)
                            .eyeHeight(0)
                            .clientTrackingRange(8)
                            .noUpdateInterval()
                            .fireImmune()
                            .noLootTable()
                            .build(key));

    public static final Supplier<Item> SPIRIT_CANDLE = item("spirit_candle", p -> new Item(p.stacksTo(1)));
    public static final Supplier<Item> SPIRIT_COMPASS = item("spirit_compass", p -> new Item(p.stacksTo(1)));
    public static final Supplier<Item> CAMERA_OBSCURA = item("camera_obscura", p -> new CameraObscura(p.stacksTo(1)));
    public static final Supplier<Item> PHOTO = item("photo", p -> new Photo(p.stacksTo(1)));
    /** Marks a photo whose picture the photographer's client has yet to send; the value pairs the two. */
    public static final Supplier<DataComponentType<Integer>> FILM = component("film", Codec.INT, ByteBufCodecs.VAR_INT);
    /** What a developed photo shows, see {@link Picture}. */
    public static final Supplier<DataComponentType<Picture>> PICTURE =
            component("picture", Picture.CODEC, Picture.STREAM_CODEC);

    public static final Supplier<Block> SPIRIT_BOX = block("spirit_box", SpiritBox::new);
    public static final Supplier<Item> SPIRIT_BOX_ITEM = blockItem("spirit_box", SPIRIT_BOX);
    public static final Supplier<Block> OUIJA_BOARD = block("ouija_board", OuijaBoard::new);
    public static final Supplier<Item> OUIJA_BOARD_ITEM = blockItem("ouija_board", OUIJA_BOARD);

    public static final Supplier<Block> DOOR_CHARM = block(
            "door_charm",
            p -> new DoorCharm(p.instabreak().sound(SoundType.WOOL).noOcclusion()));
    public static final Supplier<Item> DOOR_CHARM_ITEM = blockItem("door_charm", DOOR_CHARM);
    public static final Supplier<Block> DREAMCATCHER = block(
            "dreamcatcher",
            p -> new Dreamcatcher(p.instabreak().sound(SoundType.WOOL).noOcclusion()));
    public static final Supplier<Item> DREAMCATCHER_ITEM = blockItem("dreamcatcher", DREAMCATCHER);
    public static final Supplier<BlockEntityType<?>> DREAMCATCHER_ENTITY = Platform.get()
            .register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    "dreamcatcher",
                    key -> new BlockEntityType<>(DreamcatcherEntity::new, Set.of(DREAMCATCHER.get())));
    public static final Supplier<Block> HALLOWED_LANTERN = block(
            "hallowed_lantern",
            p -> new WardLantern(p.strength(1.0f)
                    .sound(SoundType.LANTERN)
                    .lightLevel(s -> 12)
                    .noOcclusion()));
    public static final Supplier<Item> HALLOWED_LANTERN_ITEM = blockItem("hallowed_lantern", HALLOWED_LANTERN);
    public static final Supplier<Block> SCARECROW = block(
            "scarecrow",
            p -> new WornWard(
                    p.strength(0.5f).sound(SoundType.GRASS).noOcclusion(),
                    Block.box(1, 0, 6, 15, 16, 10),
                    WardBlock.Kind.SCARECROW,
                    10));
    public static final Supplier<Item> SCARECROW_ITEM = blockItem("scarecrow", SCARECROW);

    public static final Supplier<Block> ALTAR = block(
            "altar", p -> new Altar(p.strength(2.0f).sound(SoundType.STONE).noOcclusion()));
    public static final Supplier<Item> ALTAR_ITEM = blockItem("altar", ALTAR);
    public static final Supplier<BlockEntityType<?>> ALTAR_ENTITY = Platform.get()
            .register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    "altar",
                    key -> new BlockEntityType<>(AltarEntity::new, Set.of(ALTAR.get())));
    public static final Supplier<Item> HOLY_WATER = item("holy_water", p -> new Item(p.stacksTo(16)));
    public static final Supplier<Item> MIRROR = item("mirror", p -> new Item(p.stacksTo(16)));
    /** A banished spirit drops the essence named after its type: {@code <type>_essence}. */
    public static final Supplier<Item> POLTERGEIST_ESSENCE = item("poltergeist_essence", Item::new);

    public static final Supplier<Item> SHADE_ESSENCE = item("shade_essence", Item::new);
    /** The essences of every other type; the charms made of them are {@link Relic#ALL} and the five below. */
    public static final List<Supplier<Item>> ESSENCES = List.of(
                    "nix",
                    "phantom",
                    "mimic",
                    "wraith",
                    "mara",
                    "banshee",
                    "revenant",
                    "lost_one",
                    "domovoi",
                    "hoarder",
                    "demon",
                    "trickster")
            .stream()
            .map(spirit -> item(spirit + "_essence", Item::new))
            .toList();

    public static final List<Supplier<Item>> RELICS = Relic.ALL.stream()
            .map(k -> item(k.id(), p -> new Relic(p.stacksTo(1), k.spec())))
            .toList();

    /** Marks the player; hostile things look past them, see {@link Charms#unseen}. */
    public static final Supplier<MobEffect> BORROWED_FACE = Platform.get()
            .register(
                    BuiltInRegistries.MOB_EFFECT,
                    "borrowed_face",
                    key -> new Pact(MobEffectCategory.BENEFICIAL, 0x9FD3E6));
    /** Two hearts fewer ({@code -4}) while it lasts. */
    public static final Supplier<MobEffect> BARGAIN_EFFECT = Platform.get()
            .register(BuiltInRegistries.MOB_EFFECT, "bargain", key -> {
                MobEffect effect = new Pact(MobEffectCategory.HARMFUL, 0x8A1C1C);
                effect.addAttributeModifier(
                        Attributes.MAX_HEALTH,
                        Identifier.fromNamespaceAndPath(Restless.MOD_ID, "bargain"),
                        -8.0,
                        AttributeModifier.Operation.ADD_VALUE);
                return effect;
            });
    /** What a stash holds. */
    public static final Supplier<DataComponentType<ItemContainerContents>> STASHED =
            component("stashed", ItemContainerContents.CODEC, ItemContainerContents.STREAM_CODEC);
    /** Where a way back leads, and until when. */
    public static final Supplier<DataComponentType<WayBack.Mark>> MARK =
            component("mark", WayBack.Mark.CODEC, WayBack.Mark.STREAM_CODEC);

    public static final Supplier<Item> BORROWED_FACE_ITEM = item("borrowed_face", p -> new BorrowedFace(p.stacksTo(1)));
    public static final Supplier<Item> DEATH_WAIL = item("death_wail", p -> new DeathWail(p.stacksTo(1)));
    public static final Supplier<Item> STASH = item("stash", p -> new Stash(p.stacksTo(1)));
    public static final Supplier<Item> BARGAIN = item("bargain", p -> new Bargain(p.stacksTo(1)));
    public static final Supplier<Item> WAY_BACK = item("way_back", p -> new WayBack(p.stacksTo(1)));
    public static final Supplier<Item> TELEKINESIS_AMULET =
            item("telekinesis_amulet", p -> new TelekinesisAmulet(p.stacksTo(1)));
    public static final Supplier<Item> DARK_EYE = item("dark_eye", p -> new DarkEye(p.stacksTo(1)));

    /** Built by the world, never crafted: it drops nothing. */
    public static final Supplier<Block> GRAVESTONE = block(
            "gravestone",
            p -> new Gravestone(p.strength(2.0f, 6.0f).sound(SoundType.STONE).noOcclusion()));

    public static final Supplier<Item> GRAVESTONE_ITEM = blockItem("gravestone", GRAVESTONE);
    public static final Supplier<Item> JOURNAL = item("journal", p -> new JournalBook(p.stacksTo(1)));
    /** Found in chests of structures; reading one teaches the recipe in {@link #RECIPE}. */
    public static final Supplier<Item> DIARY_PAGE = item("diary_page", p -> new DiaryPage(p.stacksTo(16)));
    /** The recipe a diary page teaches; its result is the item of the same id. */
    public static final Supplier<DataComponentType<Identifier>> RECIPE =
            component("recipe", Identifier.CODEC, Identifier.STREAM_CODEC);
    /** Left by the muddy footprints event, never crafted: no item, it drops nothing. */
    public static final Supplier<Block> FOOTPRINTS = block("footprints", Footprints::new);

    /** The thin pictures the fittings events lay on walls, doors and glass; no items, they drop nothing. */
    public static final Supplier<Block> FROST_PRINT = block("frost_print", Decal::new);

    public static final Supplier<Block> FROST_WRITING = block("frost_writing", Decal.Writing::new);
    public static final Supplier<Block> CLAW_MARKS = block("claw_marks", Decal::new);
    public static final Supplier<Block> CRACKED_MIRROR = block("cracked_mirror", Decal.Mirror::new);
    /** Small things set down by the fittings events; no items, they drop nothing. */
    public static final Supplier<Block> DOLL =
            block("doll", p -> new Prop(p.sound(SoundType.WOOL), Block.box(4, 0, 4, 12, 14, 12)));

    public static final Supplier<Block> MUSIC_BOX =
            block("music_box", p -> new Prop.MusicBox(p.sound(SoundType.WOOD), Block.box(3, 0, 4, 13, 7, 12)));
    public static final Supplier<Block> ROCKING_CHAIR =
            block("rocking_chair", p -> new Prop.Chair(p.sound(SoundType.WOOD), Block.box(2, 0, 2, 14, 15, 14)));
    /** Planks that went soft; the rot event spreads it over a house's planks, then puts them back. */
    public static final Supplier<Block> ROTTEN_PLANKS = Platform.get()
            .register(
                    BuiltInRegistries.BLOCK,
                    "rotten_planks",
                    key -> new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.PALE_OAK_PLANKS)
                            .setId(key)
                            .strength(1.0f)));

    /**
     * A tree that is not there, shown by the ghost tree event: it looks like its wood, has no collision and drops
     * nothing.
     */
    public record GhostWood(Supplier<Block> log, Supplier<Block> leaves) {}

    /** By wood name, as the {@code ghost_tree.woods} config lists them. */
    public static final Map<String, GhostWood> GHOST_WOODS = Map.of(
            "pale_oak", ghostWood("pale_oak", Blocks.PALE_OAK_LOG, Blocks.PALE_OAK_LEAVES),
            "oak", ghostWood("oak", Blocks.OAK_LOG, Blocks.OAK_LEAVES),
            "birch", ghostWood("birch", Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES));

    /** Ground mist of the blighted biome, see {@link MistParticle}. */
    public static final Supplier<ParticleType<?>> MIST =
            Platform.get().register(BuiltInRegistries.PARTICLE_TYPE, "mist", key -> new SimpleParticleType(false) {});

    private Content() {}

    private static Supplier<Item> item(String name, java.util.function.Function<Item.Properties, Item> factory) {
        return Platform.get()
                .register(BuiltInRegistries.ITEM, name, key -> factory.apply(new Item.Properties().setId(key)));
    }

    @SuppressWarnings("unchecked") // the registry is typed DataComponentType<?>; this entry is built for T
    private static <T> Supplier<DataComponentType<T>> component(
            String name, Codec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> stream) {
        Supplier<DataComponentType<?>> entry = Platform.get()
                .register(
                        BuiltInRegistries.DATA_COMPONENT_TYPE,
                        name,
                        key -> DataComponentType.<T>builder()
                                .persistent(codec)
                                .networkSynchronized(stream)
                                .build());
        return () -> (DataComponentType<T>) entry.get();
    }

    private static Supplier<Block> block(
            String name, java.util.function.Function<BlockBehaviour.Properties, Block> factory) {
        return Platform.get()
                .register(
                        BuiltInRegistries.BLOCK,
                        name,
                        key -> factory.apply(BlockBehaviour.Properties.of().setId(key)));
    }

    private static GhostWood ghostWood(String wood, Block log, Block leaves) {
        return new GhostWood(
                Platform.get()
                        .register(
                                BuiltInRegistries.BLOCK,
                                "ghost_" + wood + "_log",
                                key -> new RotatedPillarBlock(BlockBehaviour.Properties.ofLegacyCopy(log)
                                        .setId(key)
                                        .noCollision())),
                Platform.get()
                        .register(
                                BuiltInRegistries.BLOCK,
                                "ghost_" + wood + "_leaves",
                                key -> new Block(BlockBehaviour.Properties.ofLegacyCopy(leaves)
                                        .setId(key)
                                        .noCollision())));
    }

    private static Supplier<Item> blockItem(String name, Supplier<Block> block) {
        return Platform.get()
                .register(
                        BuiltInRegistries.ITEM,
                        name,
                        key -> new BlockItem(
                                block.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));
    }

    /** Forces the class to load, which declares every entry and the creative tab that holds the items. */
    public static void init() {
        RelightingRecipe.init();
        var tabItems = new java.util.ArrayList<Supplier<Item>>(UnlitLights.ITEMS);
        tabItems.addAll(List.of(
                SALT,
                ASH,
                SPIRIT_CANDLE,
                SPIRIT_COMPASS,
                CAMERA_OBSCURA,
                SPIRIT_BOX_ITEM,
                OUIJA_BOARD_ITEM,
                DOOR_CHARM_ITEM,
                DREAMCATCHER_ITEM,
                HALLOWED_LANTERN_ITEM,
                SCARECROW_ITEM,
                ALTAR_ITEM,
                HOLY_WATER,
                MIRROR,
                POLTERGEIST_ESSENCE,
                SHADE_ESSENCE,
                TELEKINESIS_AMULET,
                DARK_EYE,
                JOURNAL,
                DIARY_PAGE,
                GRAVESTONE_ITEM,
                TEST_ITEM));
        tabItems.addAll(ESSENCES);
        tabItems.addAll(RELICS);
        tabItems.addAll(List.of(BORROWED_FACE_ITEM, DEATH_WAIL, STASH, BARGAIN, WAY_BACK));
        Platform.get()
                .registerTab(
                        "restless",
                        Component.translatable("itemGroup.restless"),
                        () -> new ItemStack(SPIRIT_CANDLE.get()),
                        tabItems);
        Restless.LOGGER.debug("Content declared");
    }
}
