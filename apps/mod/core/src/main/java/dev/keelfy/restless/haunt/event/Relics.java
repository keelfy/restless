package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntSounds;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Events built on the mod's own data: painting, coat, banner, trim, enchantment and jukebox song entries under
 * {@code data/restless}, and a few sounds made from vanilla and haunt recordings. Everything the game changes is put
 * back, so nothing of the data stays in a world unless hardcore keeps it.
 */
final class Relics {
    /** Where the player is under a roof: the walls, the house. */
    private static final java.util.function.Predicate<ServerPlayer> INDOORS =
            player -> !player.level().canSeeSky(player.blockPosition());

    private Relics() {}

    static List<HauntEvent> all() {
        return List.of(
                // 492: the spirit's portrait, the house at night, eyes
                SimpleEvent.light(
                        "haunted_painting",
                        EventTag.BLOCKS,
                        player -> !paintings(player, "haunted_painting").isEmpty(),
                        Relics::haunt),
                // 493: one painting darkens in steps and a figure comes up the path; put back after
                SimpleEvent.light(
                        "painting_darkens",
                        EventTag.BLOCKS,
                        player -> !darkenable(player).isEmpty(),
                        Relics::darken),
                // 494: a tamed wolf or cat of the player turns pale until dawn
                SimpleEvent.light(
                        "pale_pet",
                        EventTag.MOBS,
                        player -> !player.level().isBrightOutside()
                                && !pets(player).isEmpty(),
                        Relics::palePet),
                // 495: a few cows, pigs and chickens turn pale
                SimpleEvent.light(
                        "pale_herd", EventTag.MOBS, player -> !herd(player).isEmpty(), Relics::paleHerd),
                // 496: a jukebox starts a song nobody put in
                SimpleEvent.light(
                        "spirit_song",
                        EventTag.SOUND,
                        player -> Nearby.any(player, "spirit_song", Relics::emptyJukebox),
                        Relics::singSong),
                // 497: a horn, far off, in the dark
                SimpleEvent.light("spirit_horn", EventTag.SOUND, Where::overworld, Relics::horn),
                // 498: a spirit's sigil on a banner
                SimpleEvent.light(
                        "sigil_banner",
                        EventTag.BLOCKS,
                        player -> Nearby.any(player, "sigil_banner", Relics::sigilable),
                        Relics::sigil),
                // 500: whatever the player holds starts to whisper
                SimpleEvent.light(
                        "whispering_item",
                        EventTag.ITEMS,
                        player -> !player.getMainHandItem().isEmpty()
                                && !cursed(player.level(), player.getMainHandItem()),
                        Relics::whisper),
                // 501: a piece of the worn armor gets a hollow trim
                SimpleEvent.light(
                        "hollow_trim", EventTag.ITEMS, player -> trimmable(player) != null, Relics::hollowTrim),
                // 516: ten counted in a child's voice, then small steps through the rooms
                SimpleEvent.light("counting_children", EventTag.SOUND, INDOORS, Relics::count),
                // 517: scratching in a wall, moving along it
                SimpleEvent.light("wall_scratching", EventTag.SOUND, INDOORS, Relics::scratch),
                // 518: knocks from inside a chest
                SimpleEvent.light(
                        "chest_knocks",
                        EventTag.SOUND,
                        player -> Nearby.any(player, "chest_knocks", Relics::knockable),
                        Relics::knockChest),
                // 519: a lullaby hummed where a bed is
                SimpleEvent.light(
                        "bedroom_lullaby",
                        EventTag.SOUND,
                        player -> Nearby.any(player, "bedroom_lullaby", s -> s.getBlock() instanceof BedBlock),
                        Relics::lullaby));
    }

    private static <T> Optional<Holder.Reference<T>> mine(
            ServerLevel level, ResourceKey<? extends Registry<T>> registry, String name) {
        return level.registryAccess()
                .lookupOrThrow(registry)
                .get(ResourceKey.create(registry, Identifier.fromNamespaceAndPath(Restless.MOD_ID, name)));
    }

    private static boolean ours(Holder<?> holder) {
        return holder.unwrapKey()
                .map(key -> key.identifier().getNamespace().equals(Restless.MOD_ID))
                .orElse(false);
    }

    private static int restoreTicks(String event) {
        return Config.event(event).integer("restore_ticks");
    }

    // ---- paintings

    private static List<Painting> paintings(ServerPlayer player, String event) {
        return player.level()
                .getEntitiesOfClass(
                        Painting.class,
                        new AABB(player.blockPosition()).inflate(Nearby.radius(event)),
                        p -> !ours(p.getVariant())
                                && !hauntings(player.level(), p).isEmpty());
    }

    /** The mod's variants of this painting's size. */
    private static List<Holder.Reference<PaintingVariant>> hauntings(ServerLevel level, Painting painting) {
        PaintingVariant now = painting.getVariant().value();
        return level.registryAccess()
                .lookupOrThrow(Registries.PAINTING_VARIANT)
                .listElements()
                .filter(v ->
                        ours(v) && v.value().width() == now.width() && v.value().height() == now.height())
                .toList();
    }

    private static Painting replace(ServerLevel level, Painting old, Holder<PaintingVariant> variant) {
        Painting fresh = new Painting(level, old.getPos(), old.getDirection(), variant);
        old.discard();
        level.addFreshEntity(fresh);
        return fresh;
    }

    private static void haunt(ServerPlayer player) {
        List<Painting> paintings = paintings(player, "haunted_painting");
        if (paintings.isEmpty()) {
            return;
        }
        ServerLevel level = player.level();
        Painting old = paintings.get(player.getRandom().nextInt(paintings.size()));
        Holder<PaintingVariant> original = old.getVariant();
        List<Holder.Reference<PaintingVariant>> options = hauntings(level, old);
        Painting[] now = {replace(level, old, options.get(player.getRandom().nextInt(options.size())))};
        Restore.revert(level, restoreTicks("haunted_painting"), force -> {
            if (!now[0].isRemoved()) {
                replace(level, now[0], original);
            }
            return true;
        });
    }

    /** A painting of the chain's size (2 by 2) that is not part of the chain yet. */
    private static List<Painting> darkenable(ServerPlayer player) {
        return player.level()
                .getEntitiesOfClass(
                        Painting.class,
                        new AABB(player.blockPosition()).inflate(Nearby.radius("painting_darkens")),
                        p -> !ours(p.getVariant())
                                && p.getVariant().value().width() == 2
                                && p.getVariant().value().height() == 2
                                && mine(player.level(), Registries.PAINTING_VARIANT, "dusk_1")
                                        .isPresent());
    }

    private static void darken(ServerPlayer player) {
        List<Painting> paintings = darkenable(player);
        if (paintings.isEmpty()) {
            return;
        }
        Config.Section config = Config.event("painting_darkens");
        ServerLevel level = player.level();
        Painting first = paintings.get(player.getRandom().nextInt(paintings.size()));
        Holder<PaintingVariant> original = first.getVariant();
        Painting[] now = {first};
        int stages = config.integer("stages");
        Restore.revert(level, restoreTicks("painting_darkens"), force -> {
            if (!now[0].isRemoved()) {
                replace(level, now[0], original);
            }
            return true;
        });
        Scheduler.repeat(player, 0, config.integer("step_ticks"), (p, n) -> {
            Optional<Holder.Reference<PaintingVariant>> stage =
                    mine(level, Registries.PAINTING_VARIANT, "dusk_" + (n + 1));
            if (now[0].isRemoved() || stage.isEmpty()) {
                return false;
            }
            now[0] = replace(level, now[0], stage.get());
            return n + 1 < stages;
        });
    }

    // ---- coats

    /**
     * One animal kind with a pale variant of the mod's: which component holds its variant, and where they are listed.
     */
    private record Coat<T>(
            Class<? extends Entity> kind, DataComponentType<Holder<T>> component, ResourceKey<Registry<T>> registry) {
        boolean fits(Entity entity) {
            Holder<T> now = entity.get(component);
            return kind.isInstance(entity) && now != null && !ours(now);
        }

        /** Paints it pale now and puts its own variant back after {@code ticks}, at dawn at the earliest if asked. */
        void dye(ServerLevel level, Entity entity, int ticks, boolean untilDawn) {
            Holder<T> before = entity.get(component);
            Optional<Holder.Reference<T>> pale = mine(level, registry, "pale");
            if (before == null || pale.isEmpty()) {
                return;
            }
            entity.setComponent(component, pale.get());
            UUID id = entity.getUUID();
            Restore.revert(level, ticks, force -> {
                Entity now = level.getEntity(id);
                if (now == null) {
                    // Unloaded: try again when it is back. Dead: nothing left to put back.
                    Entity.RemovalReason reason = entity.getRemovalReason();
                    return force || (reason != null && reason.shouldDestroy());
                }
                if (!force && untilDawn && !level.isBrightOutside()) {
                    return false;
                }
                Holder<T> current = now.get(component);
                if (current != null && ours(current)) {
                    now.setComponent(component, before);
                }
                return true;
            });
        }
    }

    private static final Coat<?> WOLF = new Coat<>(Wolf.class, DataComponents.WOLF_VARIANT, Registries.WOLF_VARIANT);
    private static final Coat<?> CAT = new Coat<>(Cat.class, DataComponents.CAT_VARIANT, Registries.CAT_VARIANT);
    private static final List<Coat<?>> PETS = List.of(WOLF, CAT);
    private static final List<Coat<?>> FARM = List.of(
            new Coat<>(Cow.class, DataComponents.COW_VARIANT, Registries.COW_VARIANT),
            new Coat<>(Pig.class, DataComponents.PIG_VARIANT, Registries.PIG_VARIANT),
            new Coat<>(Chicken.class, DataComponents.CHICKEN_VARIANT, Registries.CHICKEN_VARIANT));

    private static @Nullable Coat<?> coatOf(List<Coat<?>> coats, Entity entity) {
        for (Coat<?> coat : coats) {
            if (coat.fits(entity)) {
                return coat;
            }
        }
        return null;
    }

    private static List<Entity> near(ServerPlayer player, String event, List<Coat<?>> coats, boolean ownedOnly) {
        return new ArrayList<>(player.level()
                .getEntitiesOfClass(
                        Entity.class,
                        new AABB(player.blockPosition()).inflate(Nearby.radius(event)),
                        e -> coatOf(coats, e) != null
                                && (!ownedOnly
                                        || (e instanceof TamableAnimal pet && pet.isTame() && pet.isOwnedBy(player)))));
    }

    private static List<Entity> pets(ServerPlayer player) {
        return near(player, "pale_pet", PETS, true);
    }

    private static List<Entity> herd(ServerPlayer player) {
        return near(player, "pale_herd", FARM, false);
    }

    private static void palePet(ServerPlayer player) {
        List<Entity> pets = pets(player);
        if (pets.isEmpty()) {
            return;
        }
        Entity pet = pets.get(player.getRandom().nextInt(pets.size()));
        // Held until dawn, and at least as long as restore_ticks says.
        coatOf(PETS, pet).dye(player.level(), pet, restoreTicks("pale_pet"), true);
    }

    private static void paleHerd(ServerPlayer player) {
        List<Entity> animals = herd(player);
        java.util.Collections.shuffle(
                animals, new java.util.Random(player.getRandom().nextLong()));
        Config.Section config = Config.event("pale_herd");
        for (Entity animal : animals.subList(0, Math.min(animals.size(), config.integer("max_animals")))) {
            coatOf(FARM, animal).dye(player.level(), animal, config.integer("restore_ticks"), false);
        }
    }

    // ---- jukebox, horn

    private static boolean emptyJukebox(BlockState state) {
        return state.is(Blocks.JUKEBOX) && !state.getValue(JukeboxBlock.HAS_RECORD);
    }

    private static void singSong(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos pos = Nearby.random(player, "spirit_song", Relics::emptyJukebox);
        Optional<Holder.Reference<JukeboxSong>> song = mine(level, Registries.JUKEBOX_SONG, "spirit_song");
        if (pos == null || song.isEmpty() || !(level.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox)) {
            return;
        }
        // A plain disc whose song is the mod's; the jukebox starts it by itself.
        ItemStack disc = new ItemStack(Items.MUSIC_DISC_13);
        disc.set(DataComponents.JUKEBOX_PLAYABLE, new JukeboxPlayable(song.get()));
        jukebox.setTheItem(disc);
        Restore.revert(level, restoreTicks("spirit_song"), force -> {
            if (!force && !level.isLoaded(pos)) {
                return false;
            }
            // A disc the player has taken out is theirs now.
            if (level.getBlockEntity(pos) instanceof JukeboxBlockEntity now && now.getTheItem() == disc) {
                now.setTheItem(ItemStack.EMPTY);
            }
            return true;
        });
    }

    private static void horn(ServerPlayer player) {
        Config.Section config = Config.event("spirit_horn");
        Optional<Holder.Reference<Instrument>> horn = mine(player.level(), Registries.INSTRUMENT, "spirit_horn");
        if (horn.isEmpty()) {
            return;
        }
        Nearby.sound(
                player,
                horn.get().value().soundEvent().value(),
                Nearby.source(player, config.num("distance")),
                config.tone("sound"));
    }

    // ---- banner

    private static boolean sigilable(BlockState state) {
        return state.getBlock() instanceof AbstractBannerBlock;
    }

    private static void sigil(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, "sigil_banner", Relics::sigilable);
        ServerLevel level = player.level();
        Optional<Holder.Reference<BannerPattern>> pattern = mine(level, Registries.BANNER_PATTERN, "sigil");
        if (pos == null
                || pattern.isEmpty()
                || !(level.getBlockEntity(pos) instanceof BannerBlockEntity banner)
                || banner.getPatterns().layers().size() >= BannerBlockEntity.MAX_PATTERNS) {
            return;
        }
        BannerPatternLayers before = banner.getPatterns();
        List<BannerPatternLayers.Layer> layers = new ArrayList<>(before.layers());
        layers.add(new BannerPatternLayers.Layer(
                pattern.get(), DyeColor.byName(Config.event("sigil_banner").text("colour"), DyeColor.BLACK)));
        BannerPatternLayers after = new BannerPatternLayers(layers);
        setPatterns(level, pos, after);
        Restore.revert(level, restoreTicks("sigil_banner"), force -> {
            if (!force && !level.isLoaded(pos)) {
                return false;
            }
            if (level.getBlockEntity(pos) instanceof BannerBlockEntity now
                    && now.getPatterns().equals(after)) {
                setPatterns(level, pos, before);
            }
            return true;
        });
    }

    private static void setPatterns(ServerLevel level, BlockPos pos, BannerPatternLayers patterns) {
        BannerBlockEntity banner = (BannerBlockEntity) level.getBlockEntity(pos);
        banner.applyComponents(
                DataComponentMap.builder()
                        .addAll(banner.collectComponents())
                        .set(DataComponents.BANNER_PATTERNS, patterns)
                        .build(),
                DataComponentPatch.EMPTY);
        banner.setChanged();
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
    }

    // ---- enchantment, trim

    private static boolean cursed(ServerLevel level, ItemStack stack) {
        return mine(level, Registries.ENCHANTMENT, "curse_of_the_restless")
                .map(curse -> EnchantmentHelper.getItemEnchantmentLevel(curse, stack) > 0)
                .orElse(true);
    }

    /** Runs {@code undo} on every stack the player has, when it is time; waits for a player who has left. */
    private static void restoreInventory(ServerLevel level, ServerPlayer player, int ticks, Consumer<ItemStack> undo) {
        UUID id = player.getUUID();
        Restore.revert(level, ticks, force -> {
            ServerPlayer now = level.getServer().getPlayerList().getPlayer(id);
            if (now == null) {
                return force;
            }
            for (int i = 0; i < now.getInventory().getContainerSize(); i++) {
                undo.accept(now.getInventory().getItem(i));
            }
            return true;
        });
    }

    private static void whisper(ServerPlayer player) {
        ServerLevel level = player.level();
        Optional<Holder.Reference<Enchantment>> curse = mine(level, Registries.ENCHANTMENT, "curse_of_the_restless");
        ItemStack stack = player.getMainHandItem();
        if (curse.isEmpty() || stack.isEmpty()) {
            return;
        }
        EnchantmentHelper.updateEnchantments(stack, enchantments -> enchantments.set(curse.get(), 1));
        restoreInventory(level, player, restoreTicks("whispering_item"), held -> {
            if (!held.isEmpty() && EnchantmentHelper.getItemEnchantmentLevel(curse.get(), held) > 0) {
                EnchantmentHelper.updateEnchantments(held, enchantments -> enchantments.removeIf(curse.get()::equals));
            }
        });
    }

    private static @Nullable EquipmentSlot trimmable(ServerPlayer player) {
        for (EquipmentSlot slot :
                List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.is(ItemTags.TRIMMABLE_ARMOR) && !stack.has(DataComponents.TRIM)) {
                return slot;
            }
        }
        return null;
    }

    private static void hollowTrim(ServerPlayer player) {
        ServerLevel level = player.level();
        EquipmentSlot slot = trimmable(player);
        Optional<Holder.Reference<TrimPattern>> pattern = mine(level, Registries.TRIM_PATTERN, "hollow");
        Optional<Holder.Reference<TrimMaterial>> material = level.registryAccess()
                .lookupOrThrow(Registries.TRIM_MATERIAL)
                .get(ResourceKey.create(
                        Registries.TRIM_MATERIAL,
                        Identifier.parse(Config.event("hollow_trim").text("material"))));
        if (slot == null || pattern.isEmpty() || material.isEmpty()) {
            return;
        }
        player.getItemBySlot(slot).set(DataComponents.TRIM, new ArmorTrim(material.get(), pattern.get()));
        restoreInventory(level, player, restoreTicks("hollow_trim"), worn -> {
            ArmorTrim trim = worn.get(DataComponents.TRIM);
            if (trim != null && ours(trim.pattern())) {
                worn.remove(DataComponents.TRIM);
            }
        });
    }

    // ---- sounds

    private static void count(ServerPlayer player) {
        Config.Section config = Config.event("counting_children");
        double yaw = player.getRandom().nextDouble() * Nearby.FULL_TURN;
        int count = config.integer("count");
        Config.Tone voice = config.tone("sound");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            // Each number a little higher, as a child hurries toward ten.
            Nearby.sound(
                    p,
                    HauntSounds.WHISPER,
                    Nearby.around(p, config.num("distance"), yaw),
                    new Config.Tone(voice.volume(), voice.pitch() + (float) config.num("pitch_step") * n));
            if (n + 1 < count) {
                return true;
            }
            search(p, config, yaw);
            return false;
        });
    }

    /** The counting is over: small steps closing in, a door, and a last laugh. */
    private static void search(ServerPlayer player, Config.Section config, double yaw) {
        int steps = config.integer("steps");
        Config.Tone tone = config.tone("steps_sound");
        double[] bearing = {yaw};
        Scheduler.repeat(player, config.integer("pause_ticks"), config.integer("step_interval_ticks"), (p, n) -> {
            bearing[0] += (p.getRandom().nextDouble() - 0.5) * config.num("wander");
            double distance =
                    config.num("distance") + (config.num("end_distance") - config.num("distance")) * n / steps;
            SoundType ground =
                    p.level().getBlockState(p.blockPosition().below()).getSoundType();
            Nearby.sound(p, ground.getStepSound(), Nearby.around(p, distance, bearing[0]), tone);
            if (n == steps / 2) {
                Nearby.sound(
                        p,
                        SoundEvents.WOODEN_DOOR_OPEN,
                        Nearby.around(p, distance, bearing[0]),
                        config.tone("door_sound"));
            }
            if (n + 1 < steps) {
                return true;
            }
            Nearby.sound(
                    p,
                    HauntSounds.LAUGH,
                    Nearby.around(p, config.num("end_distance"), bearing[0]),
                    config.tone("end_sound"));
            return false;
        });
    }

    private static void scratch(ServerPlayer player) {
        Config.Section config = Config.event("wall_scratching");
        double yaw = player.getRandom().nextDouble() * Nearby.FULL_TURN;
        Vec3 start = Nearby.around(player, config.num("distance"), yaw);
        // Along the wall: sideways to the bearing, one way or the other.
        double side =
                Math.toRadians(player.getYRot() + yaw + (player.getRandom().nextBoolean() ? 90.0 : -90.0));
        Vec3 along = new Vec3(-Math.sin(side), 0.0, Math.cos(side)).scale(config.num("stride"));
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            Nearby.sound(p, SoundType.GRAVEL.getHitSound(), start.add(along.scale(n)), config.tone("sound"));
            return n + 1 < config.integer("scratches");
        });
    }

    private static boolean knockable(BlockState state) {
        return state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST) || state.is(Blocks.BARREL);
    }

    private static void knockChest(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, "chest_knocks", Relics::knockable);
        if (pos == null) {
            return;
        }
        Config.Section config = Config.event("chest_knocks");
        Vec3 at = Vec3.atCenterOf(pos);
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            Nearby.sound(p, SoundType.WOOD.getHitSound(), at, config.tone("sound"));
            return n + 1 < config.integer("knocks");
        });
    }

    private static void lullaby(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, "bedroom_lullaby", s -> s.getBlock() instanceof BedBlock);
        if (pos == null) {
            return;
        }
        Config.Section config = Config.event("bedroom_lullaby");
        String[] melody = config.text("melody").trim().split("\\s+");
        Vec3 at = Vec3.atCenterOf(pos);
        Config.Tone tone = config.tone("sound");
        Scheduler.repeat(player, 0, config.integer("note_ticks"), (p, n) -> {
            // Semitones above the lowest note; the pitch of a note block, two octaves at most.
            float pitch = (float) Math.pow(2.0, (Integer.parseInt(melody[n]) - 12) / 12.0);
            Nearby.sound(p, HauntSounds.LULLABY, at, new Config.Tone(tone.volume(), pitch * tone.pitch()));
            return n + 1 < melody.length;
        });
    }
}
