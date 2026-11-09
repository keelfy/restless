package dev.keelfy.restless.haunt.event;

import com.mojang.authlib.GameProfile;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntTier;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Said;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.Ticks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Events only one spirit type rolls (see {@code docs/signature-events.md}): each has {@code spirit} in its config, and
 * the director leaves it out for every other type.
 */
final class Signatures {
    private Signatures() {}

    /** Every signature event: the ones here, and each group's own class. */
    static List<HauntEvent> all() {
        return java.util.stream.Stream.of(
                        own(),
                        Suitors.all(),
                        Pranks.all(),
                        Dusk.all(),
                        Doubles.all(),
                        Copies.all(),
                        Nights.all(),
                        Territories.all())
                .flatMap(List::stream)
                .toList();
    }

    private static List<HauntEvent> own() {
        return List.of(
                // phantom: the player's own name joins the game
                new SimpleEvent(
                        id("phantom_self_join"),
                        HauntTier.LIGHT,
                        Set.of(EventTag.PRESENCE, EventTag.PLAYER),
                        SimpleEvent.ALWAYS,
                        Signatures::selfJoin),
                // nix: fish in a row on the shore
                SimpleEvent.light("nix_catch", EventTag.WATER, player -> shore(player) != null, Signatures::catchFish),
                // nix: a line of bubbles crosses the water toward the player
                SimpleEvent.light(
                        "nix_call",
                        EventTag.WATER,
                        player -> !bubbleLine(player).isEmpty(),
                        Signatures::callBubbles),
                // mimic: an animal makes another mob's sound
                SimpleEvent.light(
                        "mimic_voice", EventTag.MOBS, player -> voiced(player) != null, Signatures::lendVoice),
                // wraith: an animal is taken in the dark
                SimpleEvent.harmful("wraith_taken", EventTag.MOBS, player -> prey(player) != null, Signatures::take),
                // domovoi: one thing to eat is gone from a chest
                SimpleEvent.light(
                        "domovoi_supper", EventTag.ITEMS, player -> supper(player) != null, Signatures::eatSupper),
                // domovoi: a cold campfire or a fuelless furnace is lit
                SimpleEvent.light("domovoi_hearth", EventTag.LIGHTS, player -> cold(player) != null, Signatures::warm),
                // hoarder: a stack in a chest is swapped for junk
                SimpleEvent.harmful(
                        "hoarder_payment", EventTag.ITEMS, player -> stack(player) != null, Signatures::payInJunk),
                // demon: an offer on a sign
                SimpleEvent.light(
                        "demon_offer",
                        EventTag.BLOCKS,
                        player -> Said.left(player, "demon_offer", texts("demon_offer"), String::valueOf)
                                && signSpot(player, "demon_offer") != null,
                        player -> sign(player, "demon_offer", Blocks.CRIMSON_SIGN)),
                // lost one: a sign that asks the way
                SimpleEvent.light(
                        "lost_one_sign",
                        EventTag.BLOCKS,
                        player -> Said.left(player, "lost_one_sign", texts("lost_one_sign"), String::valueOf)
                                && signSpot(player, "lost_one_sign") != null,
                        player -> sign(player, "lost_one_sign", Blocks.OAK_SIGN)));
    }

    private static Identifier id(String id) {
        return Identifier.parse(id);
    }

    private static List<List<String>> texts(String event) {
        return Config.event(event).lines("texts");
    }

    // ---- phantom

    /** The player's own name and skin join the list under a fresh id, then leave. */
    private static void selfJoin(ServerPlayer player) {
        Config.Section config = Config.event("phantom_self_join");
        GameProfile own = player.getGameProfile();
        GameProfile twin = new GameProfile(
                new UUID(player.getRandom().nextLong(), player.getRandom().nextLong()), own.name(), own.properties());
        player.connection.send(Lies.listed(player, twin, config.integer("latency")));
        player.sendSystemMessage(
                Component.translatable("multiplayer.player.joined", own.name()).withStyle(ChatFormatting.YELLOW));
        Scheduler.later(player, Ticks.SECOND * config.integer("seconds"), p -> {
            p.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(twin.id())));
            p.sendSystemMessage(Component.translatable("multiplayer.player.left", own.name())
                    .withStyle(ChatFormatting.YELLOW));
        });
    }

    // ---- nix

    private static boolean water(BlockState state) {
        return state.getFluidState().is(FluidTags.WATER);
    }

    /** Free ground cells in a row beside the water, at least {@code min_cells} of them. */
    private record Shore(BlockPos water, List<BlockPos> cells) {}

    private static @Nullable Shore shore(ServerPlayer player) {
        ServerLevel level = player.level();
        Config.Section config = Config.event("nix_catch");
        List<BlockPos> waters = new ArrayList<>(Nearby.all(player, config.integer("radius"), Signatures::water));
        Collections.shuffle(waters, new java.util.Random(player.getRandom().nextLong()));
        for (BlockPos w : waters.subList(0, Math.min(waters.size(), config.integer("tries")))) {
            if (!level.isEmptyBlock(w.above())) {
                continue;
            }
            for (Direction d : Direction.Plane.HORIZONTAL) {
                Direction along = d.getClockWise();
                List<BlockPos> row = new ArrayList<>();
                for (int k = 0; k < config.integer("max_cells"); k++) {
                    BlockPos cell = w.above().relative(d).relative(along, k);
                    if (!level.isEmptyBlock(cell) || !Place.solid(level, cell.below())) {
                        break;
                    }
                    row.add(cell);
                }
                if (row.size() >= config.integer("min_cells")) {
                    return new Shore(w, row);
                }
            }
        }
        return null;
    }

    private static void catchFish(ServerPlayer player) {
        Shore shore = shore(player);
        if (shore == null) {
            return;
        }
        ServerLevel level = player.level();
        Config.Section config = Config.event("nix_catch");
        for (BlockPos cell : shore.cells()) {
            Item fish = player.getRandom().nextBoolean() ? Items.COD : Items.SALMON;
            ItemEntity drop =
                    new ItemEntity(level, cell.getX() + 0.5, cell.getY() + 0.2, cell.getZ() + 0.5, new ItemStack(fish));
            drop.setDeltaMovement(Vec3.ZERO);
            level.addFreshEntity(drop);
        }
        Vec3 at = Vec3.atCenterOf(shore.water());
        level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.5, at.z, 20, 0.3, 0.1, 0.3, 0.05);
        Nearby.sound(player, SoundEvents.FISHING_BOBBER_SPLASH, at, config.tone("sound"));
    }

    /** Water cells from far out in the water toward the player, as far as the water goes. */
    private static List<Vec3> bubbleLine(ServerPlayer player) {
        if (player.isInWater()) {
            return List.of();
        }
        ServerLevel level = player.level();
        Config.Section config = Config.event("nix_call");
        List<BlockPos> waters = new ArrayList<>(Nearby.all(player, config.integer("radius"), Signatures::water));
        Collections.shuffle(waters, new java.util.Random(player.getRandom().nextLong()));
        for (BlockPos start : waters.subList(0, Math.min(waters.size(), config.integer("tries")))) {
            double dx = player.getX() - (start.getX() + 0.5);
            double dz = player.getZ() - (start.getZ() + 0.5);
            double length = Math.hypot(dx, dz);
            if (length < config.num("min_distance")) {
                continue;
            }
            List<Vec3> line = new ArrayList<>();
            for (int k = 0; k < config.integer("max_cells") && k < length; k++) {
                BlockPos cell = BlockPos.containing(
                        start.getX() + 0.5 + dx / length * k, start.getY() + 0.5, start.getZ() + 0.5 + dz / length * k);
                if (!water(level.getBlockState(cell))) {
                    break;
                }
                line.add(Vec3.atCenterOf(cell));
            }
            if (line.size() >= config.integer("min_cells")) {
                return line;
            }
        }
        return List.of();
    }

    private static void callBubbles(ServerPlayer player) {
        List<Vec3> line = bubbleLine(player);
        if (line.isEmpty()) {
            return;
        }
        Config.Section config = Config.event("nix_call");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            if (n >= line.size()) {
                Nearby.sound(p, SoundEvents.CONDUIT_AMBIENT, line.getLast(), config.tone("sound"));
                return false;
            }
            Vec3 at = line.get(n);
            p.level().sendParticles(ParticleTypes.BUBBLE, at.x, at.y, at.z, 8, 0.25, 0.25, 0.25, 0.05);
            return true;
        });
    }

    // ---- mimic

    private static @Nullable Animal voiced(ServerPlayer player) {
        List<Animal> found = player.level()
                .getEntitiesOfClass(
                        Animal.class,
                        player.getBoundingBox()
                                .inflate(Config.event("mimic_voice").num("radius")),
                        a -> !a.isBaby());
        return found.isEmpty() ? null : found.get(player.getRandom().nextInt(found.size()));
    }

    /** Another mob's sound: a cow says the villager's "hmm", a sheep groans, a chicken mews. */
    private static SoundEvent borrowed(Animal animal, RandomSource random) {
        EntityType<?> type = animal.getType();
        if (type == EntityTypes.COW || type == EntityTypes.MOOSHROOM) {
            return SoundEvents.VILLAGER_AMBIENT;
        }
        if (type == EntityTypes.SHEEP) {
            return SoundEvents.ZOMBIE_AMBIENT;
        }
        if (type == EntityTypes.CHICKEN) {
            return SoundEvents.OCELOT_AMBIENT;
        }
        List<SoundEvent> any =
                List.of(SoundEvents.VILLAGER_AMBIENT, SoundEvents.ZOMBIE_AMBIENT, SoundEvents.OCELOT_AMBIENT);
        return any.get(random.nextInt(any.size()));
    }

    private static void lendVoice(ServerPlayer player) {
        Animal animal = voiced(player);
        if (animal == null) {
            return;
        }
        Config.Section config = Config.event("mimic_voice");
        SoundEvent sound = borrowed(animal, player.getRandom());
        Config.Tone tone = config.tone("sound");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            if (animal.isRemoved()) {
                return false;
            }
            p.level()
                    .playSound(
                            null,
                            animal.getX(),
                            animal.getY(),
                            animal.getZ(),
                            sound,
                            SoundSource.NEUTRAL,
                            tone.volume(),
                            tone.pitch());
            return n + 1 < config.integer("times");
        });
    }

    // ---- wraith

    /** A passive, unnamed, untamed animal in the dark, within sight of nobody's light: a wild one. */
    private static @Nullable Animal prey(ServerPlayer player) {
        ServerLevel level = player.level();
        Config.Section config = Config.event("wraith_taken");
        if (level.getMaxLocalRawBrightness(player.blockPosition()) >= config.integer("dark_level")) {
            return null;
        }
        List<Animal> found = level.getEntitiesOfClass(
                Animal.class,
                player.getBoundingBox().inflate(config.num("radius")),
                a -> !a.hasCustomName()
                        && !(a instanceof TamableAnimal tame && tame.isTame())
                        && !a.isLeashed()
                        && !a.isPassenger()
                        && !a.isVehicle());
        return found.isEmpty() ? null : found.get(player.getRandom().nextInt(found.size()));
    }

    private static void take(ServerPlayer player) {
        Animal animal = prey(player);
        if (animal == null) {
            return;
        }
        player.level()
                .sendParticles(
                        ParticleTypes.LARGE_SMOKE,
                        animal.getX(),
                        animal.getY() + 0.5,
                        animal.getZ(),
                        20,
                        0.3,
                        0.4,
                        0.3,
                        0.02);
        animal.discard();
    }

    // ---- domovoi

    private static boolean food(ItemStack stack) {
        return stack.is(RestlessTags.HOUSEHOLD_FOOD);
    }

    /** What is left once it is eaten: nothing for bread and cookies, the empty vessel for the rest. */
    private static ItemStack leftover(ItemStack eaten) {
        if (eaten.is(Items.MILK_BUCKET)) {
            return new ItemStack(Items.BUCKET);
        }
        if (eaten.is(Items.BREAD) || eaten.is(Items.COOKIE)) {
            ItemStack rest = eaten.copy();
            rest.shrink(1);
            return rest;
        }
        return new ItemStack(Items.BOWL);
    }

    record Slot(Container chest, BlockPos pos, int slot) {}

    static List<Slot> chests(ServerPlayer player, String event, Predicate<ItemStack> match) {
        ServerLevel level = player.level();
        List<Slot> slots = new ArrayList<>();
        for (BlockPos pos : Nearby.all(player, event, s -> s.is(Blocks.CHEST))) {
            if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    if (match.test(chest.getItem(i))) {
                        slots.add(new Slot(chest, pos, i));
                    }
                }
            }
        }
        return slots;
    }

    private static @Nullable Slot supper(ServerPlayer player) {
        List<Slot> slots = chests(player, "domovoi_supper", Signatures::food);
        return slots.isEmpty() ? null : slots.get(player.getRandom().nextInt(slots.size()));
    }

    private static void eatSupper(ServerPlayer player) {
        Slot found = supper(player);
        if (found == null) {
            return;
        }
        found.chest().setItem(found.slot(), leftover(found.chest().getItem(found.slot())));
        found.chest().setChanged();
        Config.Tone tone = Config.event("domovoi_supper").tone("sound");
        player.level()
                .playSound(
                        null,
                        found.pos(),
                        SoundEvents.GENERIC_EAT.value(),
                        SoundSource.BLOCKS,
                        tone.volume(),
                        tone.pitch());
    }

    private static boolean coldCampfire(BlockState state) {
        return state.is(BlockTags.CAMPFIRES)
                && !state.getValue(CampfireBlock.LIT)
                && !state.getValue(BlockStateProperties.WATERLOGGED);
    }

    private static boolean furnace(BlockState state) {
        return state.is(RestlessTags.FURNACES);
    }

    private static boolean unfuelled(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity f
                && !f.getItem(0).isEmpty()
                && f.getItem(1).isEmpty()
                && !level.getBlockState(pos).getValue(BlockStateProperties.LIT);
    }

    private static @Nullable BlockPos cold(ServerPlayer player) {
        ServerLevel level = player.level();
        List<BlockPos> cold = Nearby.all(
                player, "domovoi_hearth", s -> coldCampfire(s) || furnace(s) && !s.getValue(BlockStateProperties.LIT));
        cold = cold.stream()
                .filter(pos -> coldCampfire(level.getBlockState(pos)) || unfuelled(level, pos))
                .toList();
        return cold.isEmpty() ? null : cold.get(player.getRandom().nextInt(cold.size()));
    }

    private static void warm(ServerPlayer player) {
        BlockPos pos = cold(player);
        if (pos == null) {
            return;
        }
        ServerLevel level = player.level();
        if (coldCampfire(level.getBlockState(pos))) {
            level.setBlockAndUpdate(pos, level.getBlockState(pos).setValue(CampfireBlock.LIT, true));
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.6F, 1.0F);
        } else if (level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity furnace) {
            furnace.setItem(1, new ItemStack(Items.COAL));
            furnace.setChanged();
            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.6F, 1.0F);
        }
    }

    // ---- hoarder

    private static @Nullable Slot stack(ServerPlayer player) {
        List<Slot> slots = chests(player, "hoarder_payment", s -> !s.isEmpty() && !isJunk(s));
        return slots.isEmpty() ? null : slots.get(player.getRandom().nextInt(slots.size()));
    }

    private static boolean isJunk(ItemStack stack) {
        return Config.event("hoarder_payment")
                .texts("junk")
                .contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    private static void payInJunk(ServerPlayer player) {
        Slot found = stack(player);
        if (found == null) {
            return;
        }
        List<String> junk = Config.event("hoarder_payment").texts("junk");
        Item item = BuiltInRegistries.ITEM.getValue(
                Identifier.parse(junk.get(player.getRandom().nextInt(junk.size()))));
        Territories.hoard(player, found.chest().getItem(found.slot()).copy());
        int count = Math.min(found.chest().getItem(found.slot()).getCount(), new ItemStack(item).getMaxStackSize());
        found.chest().setItem(found.slot(), new ItemStack(item, count));
        found.chest().setChanged();
        player.level().playSound(null, found.pos(), SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, 1.2F);
    }

    // ---- signs

    private static @Nullable BlockPos signSpot(ServerPlayer player, String event) {
        Config.Section config = Config.event(event);
        return Place.spawnSpot(player, config.integer("min_distance"), config.integer("radius"));
    }

    /** A standing sign turned to the player; it is taken down after {@code restore_ticks}. */
    private static void sign(ServerPlayer player, String event, Block block) {
        BlockPos pos = signSpot(player, event);
        List<String> lines = pos == null ? null : Said.pick(player, event, texts(event), String::valueOf);
        if (lines == null) {
            return;
        }
        ServerLevel level = player.level();
        level.setBlockAndUpdate(
                pos,
                block.defaultBlockState().setValue(StandingSignBlock.ROTATION, RotationSegment.convertToSegment((float)
                        (Mth.atan2(pos.getX() + 0.5 - player.getX(), player.getZ() - pos.getZ() - 0.5)
                                * Mth.RAD_TO_DEG))));
        if (level.getBlockEntity(pos) instanceof SignBlockEntity entity) {
            SignText.Mutable text = entity.getText(SignTextSlot.FRONT).asMutable();
            BlockEvents.centre(text, lines);
            entity.setText(text.asImmutable(), SignTextSlot.FRONT);
        }
        Restore.revert(level, Config.event(event).integer("restore_ticks"), force -> {
            if (!force && !level.isLoaded(pos)) {
                return false;
            }
            if (level.getBlockState(pos).is(block)) {
                level.removeBlock(pos, false);
            }
            return true;
        });
    }
}
