package dev.keelfy.restless.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.keelfy.restless.Restless;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.block.Altar;
import dev.keelfy.restless.content.block.AltarEntity;
import dev.keelfy.restless.content.block.AshBlock;
import dev.keelfy.restless.content.block.Decal;
import dev.keelfy.restless.content.block.DoorCharm;
import dev.keelfy.restless.content.block.Dreamcatcher;
import dev.keelfy.restless.content.block.DreamcatcherEntity;
import dev.keelfy.restless.content.block.Effigy;
import dev.keelfy.restless.content.block.Footprints;
import dev.keelfy.restless.content.block.Gravestone;
import dev.keelfy.restless.content.block.HoardersHollow;
import dev.keelfy.restless.content.block.MarasCradle;
import dev.keelfy.restless.content.block.MimicBlock;
import dev.keelfy.restless.content.block.MimicBlockEntity;
import dev.keelfy.restless.content.block.OmenCandle;
import dev.keelfy.restless.content.block.OuijaBoard;
import dev.keelfy.restless.content.block.Prop;
import dev.keelfy.restless.content.block.SaltLineBlock;
import dev.keelfy.restless.content.block.Scarecrow;
import dev.keelfy.restless.content.block.SpiritBox;
import dev.keelfy.restless.content.block.UnlitLights;
import dev.keelfy.restless.content.block.WardBlock;
import dev.keelfy.restless.content.block.WardLantern;
import dev.keelfy.restless.content.client.MistParticle;
import dev.keelfy.restless.content.entity.AshFootprint;
import dev.keelfy.restless.content.entity.RagEntity;
import dev.keelfy.restless.content.entity.SightingMark;
import dev.keelfy.restless.content.entity.Silhouette;
import dev.keelfy.restless.content.item.CameraObscura;
import dev.keelfy.restless.content.item.DiaryPage;
import dev.keelfy.restless.content.item.Essences;
import dev.keelfy.restless.content.item.JournalBook;
import dev.keelfy.restless.content.item.Photo;
import dev.keelfy.restless.content.item.Relics;
import dev.keelfy.restless.content.item.Reliquary;
import dev.keelfy.restless.content.item.RequiemBell;
import dev.keelfy.restless.content.item.charm.Bargain;
import dev.keelfy.restless.content.item.charm.BlackBargain;
import dev.keelfy.restless.content.item.charm.BorrowedDream;
import dev.keelfy.restless.content.item.charm.BorrowedFace;
import dev.keelfy.restless.content.item.charm.ChangelingMask;
import dev.keelfy.restless.content.item.charm.Charges;
import dev.keelfy.restless.content.item.charm.Charms;
import dev.keelfy.restless.content.item.charm.DarkEye;
import dev.keelfy.restless.content.item.charm.DeathWail;
import dev.keelfy.restless.content.item.charm.DevilsCoin;
import dev.keelfy.restless.content.item.charm.FetchPath;
import dev.keelfy.restless.content.item.charm.GraveSatchel;
import dev.keelfy.restless.content.item.charm.HearthDream;
import dev.keelfy.restless.content.item.charm.KeepersKey;
import dev.keelfy.restless.content.item.charm.LostStep;
import dev.keelfy.restless.content.item.charm.Luck;
import dev.keelfy.restless.content.item.charm.MagpiesHook;
import dev.keelfy.restless.content.item.charm.NightwalkShroud;
import dev.keelfy.restless.content.item.charm.PhantomDouble;
import dev.keelfy.restless.content.item.charm.RequitalWail;
import dev.keelfy.restless.content.item.charm.Rest;
import dev.keelfy.restless.content.item.charm.Stash;
import dev.keelfy.restless.content.item.charm.TelekinesisAmulet;
import dev.keelfy.restless.content.item.charm.Tidecaller;
import dev.keelfy.restless.content.item.charm.TricksterCoin;
import dev.keelfy.restless.content.item.charm.TruthseersEye;
import dev.keelfy.restless.content.item.charm.TwinsMirror;
import dev.keelfy.restless.content.item.charm.WraithShroud;
import dev.keelfy.restless.content.item.consumable.BloodiedRag;
import dev.keelfy.restless.content.item.consumable.ChangelingDust;
import dev.keelfy.restless.content.item.consumable.DomovoiPorridge;
import dev.keelfy.restless.content.item.consumable.Draught;
import dev.keelfy.restless.content.item.consumable.Draughts;
import dev.keelfy.restless.content.item.consumable.DuskSalt;
import dev.keelfy.restless.content.item.consumable.InfernalContract;
import dev.keelfy.restless.content.item.consumable.Look;
import dev.keelfy.restless.content.item.consumable.MagpieFeather;
import dev.keelfy.restless.content.item.consumable.MimicClay;
import dev.keelfy.restless.content.item.consumable.RestlessKnot;
import dev.keelfy.restless.content.item.consumable.SightingMarkItem;
import dev.keelfy.restless.content.item.consumable.Wayfinder;
import dev.keelfy.restless.content.item.consumable.WraithOil;
import dev.keelfy.restless.content.recipe.RelightingRecipe;
import dev.keelfy.restless.content.recipe.UpgradeRecipe;
import dev.keelfy.restless.photo.Picture;
import dev.keelfy.restless.platform.Platform;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.component.ItemLore;
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
                    key -> new BlockItem(SALT_LINE.get(), tipped(new Item.Properties().setId(key), "salt")));

    public static final Supplier<Block> ASH_LINE = Platform.get()
            .register(
                    BuiltInRegistries.BLOCK,
                    "ash_line",
                    key -> new AshBlock(BlockBehaviour.Properties.of().setId(key)));
    public static final Supplier<Item> ASH = Platform.get()
            .register(
                    BuiltInRegistries.ITEM,
                    "ash",
                    key -> new BlockItem(ASH_LINE.get(), tipped(new Item.Properties().setId(key), "ash")));

    public static final Supplier<EntityType<?>> SILHOUETTE = Platform.get()
            .register(
                    BuiltInRegistries.ENTITY_TYPE,
                    "silhouette",
                    key -> EntityType.Builder.<Silhouette>of(Silhouette::new, MobCategory.MISC)
                            .sized(.6f, 1.8f)
                            .clientTrackingRange(8)
                            .noUpdateInterval()
                            .fireImmune()
                            .noLootTable()
                            .noSave()
                            .build(key));

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
            p -> new Scarecrow(p.strength(0.5f).sound(SoundType.GRASS).noOcclusion()));
    public static final Supplier<Item> SCARECROW_ITEM = blockItem("scarecrow", SCARECROW);
    public static final Supplier<Block> EFFIGY = block(
            "effigy", p -> new Effigy(p.strength(0.5f).sound(SoundType.GRASS).noOcclusion()));
    public static final Supplier<Item> EFFIGY_ITEM = blockItem("effigy", EFFIGY);

    public static final Supplier<Block> ALTAR = block(
            "altar", p -> new Altar(p.strength(2.0f).sound(SoundType.STONE).noOcclusion()));
    public static final Supplier<Item> ALTAR_ITEM = blockItem("altar", ALTAR);
    public static final Supplier<BlockEntityType<?>> ALTAR_ENTITY = Platform.get()
            .register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    "altar",
                    key -> new BlockEntityType<>(AltarEntity::new, Set.of(ALTAR.get())));
    /** The seal in mid-tier tools and every base charm, see docs/crafting-economy.md. */
    public static final Supplier<Item> HALLOWED_WAX = item("hallowed_wax", p -> new Item(p));

    public static final Supplier<Item> HOLY_WATER = item("holy_water", p -> new Item(p.stacksTo(16)));
    public static final Supplier<Item> MIRROR = item("mirror", p -> new Item(p.stacksTo(16)));
    /** A banished spirit drops the essence named after its type: {@code <type>_essence}. Keyed by spirit id. */
    public static final Map<String, Supplier<Item>> ESSENCE = essences(
            "poltergeist",
            "shade",
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
            "trickster",
            "boggart");

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
    /** Hostiles near the holder look to their double instead, see {@link PhantomDouble}. */
    public static final Supplier<MobEffect> DECOY = Platform.get()
            .register(BuiltInRegistries.MOB_EFFECT, "decoy", key -> new Pact(MobEffectCategory.BENEFICIAL, 0xB08050));
    /** Blows pass through the holder and through what they strike, see {@link Charms#hurt}. */
    public static final Supplier<MobEffect> PHASED = Platform.get()
            .register(BuiltInRegistries.MOB_EFFECT, "phased", key -> new Pact(MobEffectCategory.BENEFICIAL, 0x8FA0B8));
    /** The next kill of a mob is wagered, see {@link TricksterCoin}. */
    public static final Supplier<MobEffect> GAMBLE = Platform.get()
            .register(BuiltInRegistries.MOB_EFFECT, "gamble", key -> new Pact(MobEffectCategory.NEUTRAL, 0xF0BE28));
    /** The game time a charm rests until, see {@link Rest}. */
    public static final Supplier<DataComponentType<Long>> REST_UNTIL =
            component("rest_until", Codec.LONG, ByteBufCodecs.VAR_LONG);
    /** How far the trickster's coin has run its luck, see {@link Luck}. */
    public static final Supplier<DataComponentType<Luck>> LUCK = component(
            "luck",
            RecordCodecBuilder.create(i -> i.group(
                            Codec.INT.fieldOf("streak").forGetter(Luck::streak),
                            Codec.LONG.fieldOf("at").forGetter(Luck::at))
                    .apply(i, Luck::new)),
            StreamCodec.composite(ByteBufCodecs.VAR_INT, Luck::streak, ByteBufCodecs.VAR_LONG, Luck::at, Luck::new));
    /** What a stash holds. */
    public static final Supplier<DataComponentType<ItemContainerContents>> STASHED =
            component("stashed", ItemContainerContents.CODEC, ItemContainerContents.STREAM_CODEC);

    /** The essences a reliquary holds, in the order they went in, see {@link Essences}. */
    public static final Supplier<DataComponentType<Essences>> ESSENCES =
            component("essences", Essences.CODEC, Essences.STREAM_CODEC);

    public static final Supplier<Item> BORROWED_FACE_ITEM = item("borrowed_face", p -> new BorrowedFace(p.stacksTo(1)));
    public static final Supplier<Item> DEATH_WAIL = item("death_wail", p -> new DeathWail(p.stacksTo(1)));
    public static final Supplier<Item> STASH = item("stash", p -> new Stash(p.stacksTo(1)));
    public static final Supplier<Item> BARGAIN = item("bargain", p -> new Bargain(p.stacksTo(1)));
    public static final Supplier<Item> LOST_STEP = item("lost_step", p -> new LostStep(p.stacksTo(1)));
    /** Held in the off hand; what they do is in {@link Charms}. */
    public static final Supplier<Item> DROWNED_STONE = item("drowned_stone", p -> new Item(p.stacksTo(1)));

    public static final Supplier<Item> WRAITH_SHROUD = item("wraith_shroud", p -> new WraithShroud(p.stacksTo(1)));
    public static final Supplier<Item> GRUDGE = item("grudge", p -> new Item(p.stacksTo(1)));
    public static final Supplier<Item> HOUSE_KEY = item(
            "house_key", p -> new Item(p.durability(Config.section("charms").integer("house_uses"))));
    public static final Supplier<Item> PHANTOM_DOUBLE = item("phantom_double", p -> new PhantomDouble(p.stacksTo(1)));
    public static final Supplier<Item> BORROWED_DREAM = item("borrowed_dream", p -> new BorrowedDream(p.stacksTo(1)));
    public static final Supplier<Item> TRICKSTER_COIN = item("trickster_coin", p -> new TricksterCoin(p.stacksTo(1)));
    public static final Supplier<Item> TELEKINESIS_AMULET =
            item("telekinesis_amulet", p -> new TelekinesisAmulet(p.stacksTo(1)));
    public static final Supplier<Item> DARK_EYE = item("dark_eye", p -> new DarkEye(p.stacksTo(1)));

    /** Where a magpie's hook sends what its holder cannot carry. */
    public static final Supplier<DataComponentType<net.minecraft.core.GlobalPos>> HOOK_LINK =
            component("hook_link", net.minecraft.core.GlobalPos.CODEC, net.minecraft.core.GlobalPos.STREAM_CODEC);
    /** The room a keeper's key is bound to, see {@link Charms#keeps}. */
    public static final Supplier<DataComponentType<net.minecraft.core.GlobalPos>> KEY_ROOM =
            component("key_room", net.minecraft.core.GlobalPos.CODEC, net.minecraft.core.GlobalPos.STREAM_CODEC);

    /** The advanced charms: a base charm and one more essence each, see {@link UpgradeRecipe}. */
    public static final Supplier<Item> FETCH_PATH = item("fetch_path", p -> new FetchPath(p.stacksTo(1)));

    public static final Supplier<Item> CHANGELING_MASK =
            item("changeling_mask", p -> new ChangelingMask(p.stacksTo(1)));
    public static final Supplier<Item> NIGHTWALK_SHROUD =
            item("nightwalk_shroud", p -> new NightwalkShroud(p.stacksTo(1)));
    public static final Supplier<Item> BLOOD_DEBT = item("blood_debt", p -> new Item(p.stacksTo(1)));
    public static final Supplier<Item> REQUITAL_WAIL = item("requital_wail", p -> new RequitalWail(p.stacksTo(1)));
    public static final Supplier<Item> TIDECALLER = item("tidecaller", p -> new Tidecaller(p.stacksTo(1)));
    public static final Supplier<Item> MAGPIES_HOOK = item("magpies_hook", p -> new MagpiesHook(p.stacksTo(1)));
    public static final Supplier<Item> TRUTHSEERS_EYE = item("truthseers_eye", p -> new TruthseersEye(p.stacksTo(1)));
    public static final Supplier<Item> HEARTH_DREAM = item("hearth_dream", p -> new HearthDream(p.stacksTo(1)));
    public static final Supplier<Item> KEEPERS_KEY = item("keepers_key", p -> new KeepersKey(p.stacksTo(1)));
    public static final Supplier<Item> GRAVE_SATCHEL = item("grave_satchel", p -> new GraveSatchel(p.stacksTo(1)));
    public static final Supplier<Item> DEVILS_COIN = item("devils_coin", p -> new DevilsCoin(p.stacksTo(1)));
    public static final Supplier<Item> TWINS_MIRROR = item("twins_mirror", p -> new TwinsMirror(p.stacksTo(1)));
    public static final Supplier<Item> BLACK_BARGAIN = item("black_bargain", p -> new BlackBargain(p.stacksTo(1)));

    /** What a drinker of the Dreamless Draught is until they have slept it off, see {@link Draughts}. */
    public static final Supplier<MobEffect> DREAMLESS = Platform.get()
            .register(BuiltInRegistries.MOB_EFFECT, "dreamless", key -> new Pact(MobEffectCategory.NEUTRAL, 0x6C5B9E));
    /** How many hits a weapon coated in wraith oil has left, see {@link WraithOil}. */
    public static final Supplier<DataComponentType<Charges>> CHARGES =
            component("charges", Charges.CODEC, Charges.STREAM_CODEC);
    /** The thrown bloodied rag. */
    public static final Supplier<EntityType<?>> RAG_ENTITY = Platform.get()
            .register(
                    BuiltInRegistries.ENTITY_TYPE,
                    "bloodied_rag",
                    key -> EntityType.Builder.<RagEntity>of(RagEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build(key));

    /** The consumables, one to each spirit that leaves an essence: each is spent as it is used. */
    public static final Supplier<Item> STILL_WATER =
            item("still_water", p -> new Draught(p.stacksTo(16), Draughts::still));

    public static final Supplier<Item> DREAMLESS_DRAUGHT =
            item("dreamless_draught", p -> new Draught(p.stacksTo(16), Draughts::dreamless));
    public static final Supplier<Item> WAYFINDERS_ASH = item("wayfinders_ash", Wayfinder::new);
    public static final Supplier<Item> MAGPIE_FEATHER = item("magpie_feather", MagpieFeather::new);
    public static final Supplier<Item> CHANGELING_DUST = item("changeling_dust", ChangelingDust::new);
    public static final Supplier<Item> BLOODIED_RAG = item("bloodied_rag", p -> new BloodiedRag(p.stacksTo(16)));
    public static final Supplier<Item> WRAITH_OIL = item("wraith_oil", p -> new WraithOil(p.stacksTo(16)));
    public static final Supplier<Item> INFERNAL_CONTRACT = item("infernal_contract", InfernalContract::new);
    public static final Supplier<Item> RESTLESS_KNOT = item("restless_knot", RestlessKnot::new);
    public static final Supplier<Item> DUSK_SALT = item("dusk_salt", DuskSalt::new);
    public static final Supplier<Item> SIGHTING_MARK = item("sighting_mark", SightingMarkItem::new);
    public static final Supplier<Item> MIMIC_CLAY = item("mimic_clay", MimicClay::new);
    /**
     * Holds one essence of each type; full and sung over, it becomes the bell. See
     * {@link dev.keelfy.restless.banish.Requiem}.
     */
    public static final Supplier<Item> RELIQUARY = item("reliquary", p -> new Reliquary(p.stacksTo(1)));

    public static final Supplier<Item> REQUIEM_BELL = item("requiem_bell", p -> new RequiemBell(p.stacksTo(1)));
    /** What a lump of mimic clay shows as, once pressed against a block. */
    public static final Supplier<DataComponentType<Look>> LOOK = component("look", Look.CODEC, Look.STREAM_CODEC);

    public static final Supplier<Block> OMEN_CANDLE = block(
            "omen_candle",
            p -> new OmenCandle(p.instabreak().sound(SoundType.CANDLE).noOcclusion()));
    public static final Supplier<Item> OMEN_CANDLE_ITEM = blockItem("omen_candle", OMEN_CANDLE);
    /** What an eater of the Domovoi's Porridge is until death or milk takes it, see {@link DomovoiPorridge}. */
    public static final Supplier<MobEffect> GROWTH = Platform.get()
            .register(BuiltInRegistries.MOB_EFFECT, "growth", key -> new Pact(MobEffectCategory.BENEFICIAL, 0x7CB342));

    public static final Supplier<Item> DOMOVOIS_PORRIDGE_ITEM =
            item("domovois_porridge", p -> new DomovoiPorridge(p.stacksTo(16)));
    public static final Supplier<Block> MIMIC_BLOCK =
            block("mimic_block", p -> new MimicBlock(p.strength(1.0f).noOcclusion()));
    public static final Supplier<Block> HOARDERS_HOLLOW = block(
            "hoarders_hollow",
            p -> new HoardersHollow(p.strength(2.0f).sound(SoundType.WOOD).noOcclusion()));
    public static final Supplier<Item> HOARDERS_HOLLOW_ITEM = blockItem("hoarders_hollow", HOARDERS_HOLLOW);
    public static final Supplier<BlockEntityType<?>> MIMIC_ENTITY = Platform.get()
            .register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    "mimic_block",
                    key -> new BlockEntityType<>(
                            MimicBlockEntity::new, Set.of(MIMIC_BLOCK.get(), HOARDERS_HOLLOW.get())));
    /** The pale figure of a sighting mark. */
    public static final Supplier<EntityType<?>> MARK_ENTITY = Platform.get()
            .register(
                    BuiltInRegistries.ENTITY_TYPE,
                    "sighting_mark",
                    key -> EntityType.Builder.<SightingMark>of(SightingMark::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(Config.section("consumables").integer("mark_range"))
                            .updateInterval(20)
                            .fireImmune()
                            .noLootTable()
                            .build(key));

    /** What a player is while a ferryman's coin holds them off death, see {@link Relics}. */
    public static final Supplier<MobEffect> FADED = Platform.get()
            .register(BuiltInRegistries.MOB_EFFECT, "faded", key -> new Pact(MobEffectCategory.BENEFICIAL, 0xB9D8D0));
    /** The relics: rare, made of three essences and a rare find, see {@link Relics}. */
    public static final Supplier<Item> FERRYMANS_COIN = item("ferrymans_coin", p -> new Item(p.stacksTo(1)));

    public static final Supplier<Item> EXORCISTS_CENSER = item("exorcists_censer", p -> new Item(p.stacksTo(1)));
    public static final Supplier<Block> HOLLOW_LANTERN = block(
            "hollow_lantern",
            p -> new WardLantern(
                    p.strength(1.0f).sound(SoundType.LANTERN).lightLevel(s -> 8).noOcclusion(), WardBlock.Kind.HOLLOW));
    public static final Supplier<Item> HOLLOW_LANTERN_ITEM = blockItem("hollow_lantern", HOLLOW_LANTERN);
    public static final Supplier<Block> MARAS_CRADLE = block(
            "maras_cradle",
            p -> new MarasCradle(p.strength(0.4f).sound(SoundType.WOOL).noOcclusion()));
    public static final Supplier<Item> MARAS_CRADLE_ITEM = Platform.get()
            .register(
                    BuiltInRegistries.ITEM,
                    "maras_cradle",
                    key -> new BlockItem(
                            MARAS_CRADLE.get(),
                            tipped(new Item.Properties().setId(key), "maras_cradle")
                                    .useBlockDescriptionPrefix()
                                    .stacksTo(1)));

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
    public static final Supplier<Block> DOLL = block("doll", p -> new Prop(p.sound(SoundType.WOOL), Prop.DOLL));

    public static final Supplier<Block> MUSIC_BOX =
            block("music_box", p -> new Prop.MusicBox(p.sound(SoundType.WOOD), Prop.MUSIC_BOX));
    public static final Supplier<Block> ROCKING_CHAIR =
            block("rocking_chair", p -> new Prop.Chair(p.sound(SoundType.WOOD), Prop.CHAIR));
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

    private static Map<String, Supplier<Item>> essences(String... spirits) {
        Map<String, Supplier<Item>> found = new java.util.LinkedHashMap<>();
        for (String spirit : spirits) {
            found.put(spirit, item(spirit + "_essence", "essence", Item::new));
        }
        return java.util.Collections.unmodifiableMap(found);
    }

    private static Supplier<Item> item(String name, java.util.function.Function<Item.Properties, Item> factory) {
        return item(name, name, factory);
    }

    /** {@code tip} names the lang key {@code item.restless.<tip>.tip}, so items may share a line. */
    private static Supplier<Item> item(
            String name, String tip, java.util.function.Function<Item.Properties, Item> factory) {
        return Platform.get()
                .register(
                        BuiltInRegistries.ITEM,
                        name,
                        key -> factory.apply(tipped(new Item.Properties().setId(key), tip)));
    }

    /** Charms worn in an accessory slot (or the off hand without Curios): their reminder is aqua. */
    private static final java.util.Set<String> WORN = java.util.Set.of(
            "death_wail",
            "requital_wail",
            "drowned_stone",
            "tidecaller",
            "grudge",
            "blood_debt",
            "house_key",
            "keepers_key",
            "grave_satchel",
            "magpies_hook",
            "truthseers_eye",
            "nightwalk_shroud",
            "ferrymans_coin");
    /** Tools of the craft, used from the hand: their reminder is gold. */
    private static final java.util.Set<String> TOOLS = java.util.Set.of(
            "dark_eye",
            "telekinesis_amulet",
            "borrowed_face",
            "changeling_mask",
            "wraith_shroud",
            "lost_step",
            "fetch_path",
            "phantom_double",
            "twins_mirror",
            "bargain",
            "black_bargain",
            "borrowed_dream",
            "hearth_dream",
            "trickster_coin",
            "devils_coin",
            "exorcists_censer");

    /**
     * Gives an item its one-line reminder of what it does, shown under its name. Worn charms and tools of the craft
     * read in different colours, so a glance tells what goes in a slot from what is used.
     */
    private static Item.Properties tipped(Item.Properties properties, String tip) {
        ChatFormatting colour = WORN.contains(tip)
                ? ChatFormatting.AQUA
                : TOOLS.contains(tip) ? ChatFormatting.GOLD : ChatFormatting.GRAY;
        return properties.component(
                DataComponents.LORE,
                new ItemLore(List.of(
                        Component.translatable("item.restless." + tip + ".tip").withStyle(colour))));
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
                                block.get(),
                                tipped(new Item.Properties().setId(key), name).useBlockDescriptionPrefix()));
    }

    /** Forces the class to load, which declares every entry and the creative tab that holds the items. */
    public static void init() {
        RelightingRecipe.init();
        UpgradeRecipe.init();
        var tabItems = new java.util.ArrayList<Supplier<Item>>(UnlitLights.ITEMS);
        tabItems.addAll(List.of(
                SALT,
                ASH,
                HALLOWED_WAX,
                SPIRIT_CANDLE,
                CAMERA_OBSCURA,
                SPIRIT_BOX_ITEM,
                OUIJA_BOARD_ITEM,
                DOOR_CHARM_ITEM,
                DREAMCATCHER_ITEM,
                HALLOWED_LANTERN_ITEM,
                SCARECROW_ITEM,
                EFFIGY_ITEM,
                ALTAR_ITEM,
                HOLY_WATER,
                MIRROR,
                TELEKINESIS_AMULET,
                DARK_EYE,
                JOURNAL,
                DIARY_PAGE,
                GRAVESTONE_ITEM,
                TEST_ITEM));
        tabItems.addAll(ESSENCE.values());
        tabItems.addAll(List.of(
                BORROWED_FACE_ITEM,
                DEATH_WAIL,
                STASH,
                BARGAIN,
                LOST_STEP,
                DROWNED_STONE,
                WRAITH_SHROUD,
                GRUDGE,
                HOUSE_KEY,
                PHANTOM_DOUBLE,
                BORROWED_DREAM,
                TRICKSTER_COIN,
                FETCH_PATH,
                CHANGELING_MASK,
                NIGHTWALK_SHROUD,
                BLOOD_DEBT,
                REQUITAL_WAIL,
                TIDECALLER,
                MAGPIES_HOOK,
                TRUTHSEERS_EYE,
                HEARTH_DREAM,
                KEEPERS_KEY,
                GRAVE_SATCHEL,
                DEVILS_COIN,
                TWINS_MIRROR,
                BLACK_BARGAIN,
                STILL_WATER,
                DREAMLESS_DRAUGHT,
                WAYFINDERS_ASH,
                MAGPIE_FEATHER,
                CHANGELING_DUST,
                BLOODIED_RAG,
                WRAITH_OIL,
                INFERNAL_CONTRACT,
                RESTLESS_KNOT,
                DUSK_SALT,
                SIGHTING_MARK,
                MIMIC_CLAY,
                OMEN_CANDLE_ITEM,
                DOMOVOIS_PORRIDGE_ITEM,
                FERRYMANS_COIN,
                EXORCISTS_CENSER,
                HOLLOW_LANTERN_ITEM,
                HOARDERS_HOLLOW_ITEM,
                MARAS_CRADLE_ITEM,
                RELIQUARY,
                REQUIEM_BELL));
        Platform.get()
                .registerTab(
                        "restless",
                        Component.translatable("itemGroup.restless"),
                        () -> new ItemStack(SPIRIT_CANDLE.get()),
                        tabItems);
        Restless.LOGGER.debug("Content declared");
    }
}
