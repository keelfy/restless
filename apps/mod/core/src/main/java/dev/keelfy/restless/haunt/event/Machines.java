package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.divine.Candle;
import dev.keelfy.restless.divine.Sense;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.HauntSounds;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.Ticks;
import dev.keelfy.restless.haunt.event.kit.Nearby;
import dev.keelfy.restless.haunt.event.kit.SimpleEvent;
import dev.keelfy.restless.haunt.recall.Said;
import dev.keelfy.restless.haunt.scenario.ScenarioContext;
import dev.keelfy.restless.spirit.Story;
import dev.keelfy.restless.state.HauntData;
import dev.keelfy.restless.state.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CopperChestBlock;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Things with moving parts and things written down: a crafter, a copper golem, a jukebox, scaffolding, a framed map,
 * the spirit candle, holy water, the mirror, and the player's own last chat line. The few hooks the rest of the mod
 * calls ({@link #said}, {@link #cloudy}, {@link #mirrorUsed}) are public; the marks they read live in memory only.
 */
public final class Machines {
    /** The custom data key on holy water: overworld clock time it clears at. */
    private static final String CLOUDY = "restless_cloudy_until";

    private static final String CLOUDY_LORE = "item.restless.holy_water.cloudy";
    private static final int MAP_SIZE = 128;
    private static final int MAP_PROBES = 16;

    /** A chat line and the game time it was said. */
    private record Line(String text, long at) {}

    private static final Map<UUID, Line> SAID = Session.SERVER.keep(new ConcurrentHashMap<>());
    /** Who the mirror is to name on its next use, and until when (game time). */
    /** A mirror a spirit has marked: until when, and the spirit that did. */
    private record Armed(long until, @Nullable UUID spirit) {}

    private static final Map<UUID, Armed> MIRROR = Session.SERVER.keep(new ConcurrentHashMap<>());

    /** Where a golem's item goes. */
    private record Move(ChestBlockEntity from, int slot, ChestBlockEntity to) {}

    private Machines() {}

    static List<HauntEvent> all() {
        return List.of(
                // 216: the crafter's own animation and sound, then an item named after the player from its face
                SimpleEvent.light(
                        "crafter_crafts",
                        EventTag.BLOCKS,
                        player -> Nearby.any(player, "crafter_crafts", s -> s.is(Blocks.CRAFTER)),
                        Machines::craft),
                // 223: a golem takes a stack from a copper chest and drops it into another chest
                SimpleEvent.light(
                        "golem_wrong_chest", EventTag.MOBS, player -> move(player) != null, Machines::misplace),
                // 228: the jukebox goes quiet and its disc turns up in a chest
                SimpleEvent.light(
                        "jukebox_quiet",
                        EventTag.SOUND,
                        player -> !playing(player).isEmpty()
                                && !roomy(player, "jukebox_quiet").isEmpty(),
                        Machines::silence),
                // 232: creaks, then the scaffolding under the player gives way (a fall of under 4 blocks)
                SimpleEvent.harmful(
                        "scaffold_collapses", EventTag.BLOCKS, player -> column(player) != null, Machines::collapse),
                // 238: a framed map shows the spirit's anchor instead; put back after a while
                SimpleEvent.light(
                        "wall_map_shows",
                        EventTag.BLOCKS,
                        player -> !frames(player).isEmpty() && anchor(player) != null,
                        Machines::swapMap),
                // 527: the candle in the player's hand goes out by itself for a while
                SimpleEvent.light(
                        "spirit_candle_out",
                        EventTag.LIGHTS,
                        player -> Candle.holds(player) && !Candle.isOut(player),
                        Machines::snuffCandle),
                // 534: the water in the bottle clouds; the altar will not take it until dawn (stays)
                SimpleEvent.harmful(
                        "holy_water_cloudy",
                        EventTag.ITEMS,
                        player -> !holyWater(player, false).isEmpty(),
                        Machines::cloud),
                // 535: the next time the mirror is used it shows the spirit's name
                SimpleEvent.waiting(
                        "mirror_names",
                        EventTag.ITEMS,
                        player -> !MIRROR.containsKey(player.getUUID())
                                && hasMirror(player)
                                && !Sense.of(
                                                player,
                                                HauntData.get(player.level().getServer()))
                                        .isEmpty(),
                        Machines::armMirror),
                // 354: "???" says the player's own last chat line back, later, as a system message
                SimpleEvent.light("chat_repeats", EventTag.SOUND, Machines::echoable, Machines::echo));
    }

    // 354

    /** Chat hook: remembers what the player last said. */
    public static void said(ServerPlayer player, String line) {
        SAID.put(player.getUUID(), new Line(line, player.level().getGameTime()));
    }

    private static boolean echoable(ServerPlayer player) {
        Line line = SAID.get(player.getUUID());
        Config.Section config = Config.event("chat_repeats");
        if (line == null || line.text().isBlank() || line.text().length() > config.integer("max_length")) {
            return false;
        }
        long age = player.level().getGameTime() - line.at();
        return age >= config.integer("min_age_ticks") && age <= config.integer("max_age_ticks");
    }

    private static void echo(ServerPlayer player) {
        Line line = SAID.remove(player.getUUID());
        if (line == null) {
            return;
        }
        Config.Section config = Config.event("chat_repeats");
        int delay =
                config.integer("delay_ticks") + player.getRandom().nextInt(config.integer("delay_spread_ticks") + 1);
        Scheduler.later(
                player,
                delay,
                p -> p.sendSystemMessage(Component.literal(config.text("format")
                                .replace("???", ScenarioContext.speaker("???"))
                                .formatted(line.text()))
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
    }

    // 216

    private static void craft(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, "crafter_crafts", s -> s.is(Blocks.CRAFTER));
        if (pos == null) {
            return;
        }
        Config.Section config = Config.event("crafter_crafts");
        ServerLevel level = player.level();
        BlockState state = level.getBlockState(pos);
        Direction front = state.getValue(BlockStateProperties.ORIENTATION).front();
        List<String> items = config.texts("items");
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM
                .getOptional(Identifier.parse(items.get(player.getRandom().nextInt(items.size()))))
                .orElse(Items.BONE));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(player.getName().getString()));
        level.setBlock(pos, state.setValue(CrafterBlock.CRAFTING, true), Block.UPDATE_CLIENTS);
        level.levelEvent(1049, pos, 0);
        level.levelEvent(2010, pos, front.get3DDataValue());
        Vec3 at = Vec3.atCenterOf(pos).relative(front, 0.7);
        ItemEntity item =
                new ItemEntity(level, at.x, at.y, at.z, stack, front.getStepX() * 0.2, 0.1, front.getStepZ() * 0.2);
        level.addFreshEntity(item);
        Restore.after(level, config.integer("craft_ticks"), force -> {
            if (!force && !level.isLoaded(pos)) {
                return false;
            }
            BlockState now = level.getBlockState(pos);
            if (now.is(Blocks.CRAFTER) && now.getValue(CrafterBlock.CRAFTING)) {
                level.setBlock(pos, now.setValue(CrafterBlock.CRAFTING, false), Block.UPDATE_CLIENTS);
            }
            return true;
        });
        // What nobody picks up is not meant to stay.
        Restore.after(level, config.integer("vanish_ticks"), force -> {
            item.discard();
            return true;
        });
    }

    // 223, 228

    private static List<ChestBlockEntity> chests(ServerPlayer player, String event) {
        return Nearby.all(player, event, s -> s.getBlock() instanceof ChestBlock).stream()
                .map(pos -> player.level().getBlockEntity(pos))
                .filter(ChestBlockEntity.class::isInstance)
                .map(ChestBlockEntity.class::cast)
                .toList();
    }

    private static int freeSlot(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private static List<ChestBlockEntity> roomy(ServerPlayer player, String event) {
        return chests(player, event).stream()
                .filter(chest -> freeSlot(chest) >= 0)
                .toList();
    }

    private static List<CopperGolem> golems(ServerPlayer player) {
        return player.level()
                .getEntitiesOfClass(
                        CopperGolem.class, player.getBoundingBox().inflate(Nearby.radius("golem_wrong_chest")));
    }

    private static @Nullable Move move(ServerPlayer player) {
        if (golems(player).isEmpty()) {
            return null;
        }
        List<ChestBlockEntity> all = chests(player, "golem_wrong_chest");
        List<Move> moves = new ArrayList<>();
        for (ChestBlockEntity from : all) {
            if (!(from.getBlockState().getBlock() instanceof CopperChestBlock)) {
                continue;
            }
            for (int slot = 0; slot < from.getContainerSize(); slot++) {
                if (from.getItem(slot).isEmpty()) {
                    continue;
                }
                for (ChestBlockEntity to : all) {
                    // Not the other half of the same chest.
                    if (from.getBlockPos().distManhattan(to.getBlockPos()) > 1 && freeSlot(to) >= 0) {
                        moves.add(new Move(from, slot, to));
                    }
                }
            }
        }
        return moves.isEmpty() ? null : moves.get(player.getRandom().nextInt(moves.size()));
    }

    private static void misplace(ServerPlayer player) {
        Move move = move(player);
        if (move == null) {
            return;
        }
        Config.Section config = Config.event("golem_wrong_chest");
        ServerLevel level = player.level();
        List<CopperGolem> golems = golems(player);
        CopperGolem golem = golems.get(player.getRandom().nextInt(golems.size()));
        ItemStack stack = move.from().getItem(move.slot());
        move.from().setItem(move.slot(), ItemStack.EMPTY);
        move.from().setChanged();
        move.to().setItem(freeSlot(move.to()), stack);
        move.to().setChanged();
        Vec3 from = Vec3.atCenterOf(move.from().getBlockPos());
        Vec3 to = Vec3.atCenterOf(move.to().getBlockPos());
        golem.getLookControl().setLookAt(from);
        golem.setState(CopperGolemState.GETTING_ITEM);
        Nearby.sound(player, SoundEvents.COPPER_CHEST_OPEN, from, config.tone("sound"));
        Scheduler.later(player, config.integer("step_ticks"), p -> {
            golem.getLookControl().setLookAt(to);
            golem.setState(CopperGolemState.DROPPING_ITEM);
            Nearby.sound(p, SoundEvents.COPPER_CHEST_OPEN, to, config.tone("sound"));
        });
        Restore.after(level, config.integer("step_ticks") * 3, force -> {
            if (golem.isAlive()) {
                golem.setState(CopperGolemState.IDLE);
            }
            return true;
        });
    }

    // 228

    private static List<JukeboxBlockEntity> playing(ServerPlayer player) {
        return Nearby.all(player, "jukebox_quiet", s -> s.is(Blocks.JUKEBOX) && s.getValue(JukeboxBlock.HAS_RECORD))
                .stream()
                .map(pos -> player.level().getBlockEntity(pos))
                .filter(JukeboxBlockEntity.class::isInstance)
                .map(JukeboxBlockEntity.class::cast)
                .filter(jukebox -> jukebox.getSongPlayer().isPlaying())
                .toList();
    }

    private static void silence(ServerPlayer player) {
        List<JukeboxBlockEntity> jukeboxes = playing(player);
        List<ChestBlockEntity> chests = roomy(player, "jukebox_quiet");
        if (jukeboxes.isEmpty() || chests.isEmpty()) {
            return;
        }
        JukeboxBlockEntity jukebox = jukeboxes.get(player.getRandom().nextInt(jukeboxes.size()));
        ChestBlockEntity chest = chests.get(player.getRandom().nextInt(chests.size()));
        ItemStack disc = jukebox.getTheItem().copy();
        jukebox.setTheItem(ItemStack.EMPTY);
        chest.setItem(freeSlot(chest), disc);
        chest.setChanged();
    }

    // 232

    /**
     * The scaffolding blocks under the player, top first, when they stand on a column that is short enough to fall from
     * without harm, and on solid ground; otherwise null.
     */
    private static @Nullable List<BlockPos> column(ServerPlayer player) {
        if (!player.onGround()) {
            return null;
        }
        Config.Section config = Config.event("scaffold_collapses");
        ServerLevel level = player.level();
        BlockPos feet = player.blockPosition();
        BlockPos.MutableBlockPos cursor =
                (level.getBlockState(feet).is(Blocks.SCAFFOLDING) ? feet : feet.below()).mutable();
        List<BlockPos> blocks = new ArrayList<>();
        while (level.getBlockState(cursor).is(Blocks.SCAFFOLDING) && blocks.size() <= config.integer("max_blocks")) {
            blocks.add(cursor.immutable());
            cursor.move(Direction.DOWN);
        }
        if (blocks.isEmpty() || !level.getBlockState(cursor).isFaceSturdy(level, cursor, Direction.UP)) {
            return null;
        }
        return player.getY() - blocks.getLast().getY() < config.num("max_fall") ? blocks : null;
    }

    private static void collapse(ServerPlayer player) {
        Config.Section config = Config.event("scaffold_collapses");
        int creaks = config.integer("creaks");
        int gap = config.integer("creak_ticks");
        Scheduler.repeat(player, 0, gap, (p, n) -> {
            Nearby.sound(p, SoundEvents.SCAFFOLDING_STEP, p.position(), config.tone("sound"));
            return n + 1 < creaks;
        });
        Scheduler.later(player, creaks * gap, p -> {
            List<BlockPos> blocks = column(p);
            if (blocks != null) {
                blocks.forEach(pos -> p.level().destroyBlock(pos, true));
            }
        });
    }

    // 238

    private static List<ItemFrame> frames(ServerPlayer player) {
        return player.level()
                .getEntitiesOfClass(
                        ItemFrame.class,
                        player.getBoundingBox().inflate(Nearby.radius("wall_map_shows")),
                        frame -> frame.getItem().is(Items.FILLED_MAP));
    }

    /** The anchor of the nearest territorial spirit whose reach holds the player, when the map's area is loaded. */
    private static @Nullable BlockPos anchor(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos nearest = null;
        double best = Double.MAX_VALUE;
        for (HauntData.Territorial spirit : HauntData.get(level.getServer()).territorialReaching(player)) {
            double distance = spirit.anchor().distSqr(player.blockPosition());
            if (distance < best) {
                nearest = spirit.anchor();
                best = distance;
            }
        }
        if (nearest == null) {
            return null;
        }
        int half = MAP_SIZE / 2;
        for (int dx = -half; dx <= half; dx += MAP_SIZE / MAP_PROBES) {
            for (int dz = -half; dz <= half; dz += MAP_SIZE / MAP_PROBES) {
                if (!level.isLoaded(nearest.offset(dx, 0, dz))) {
                    return null;
                }
            }
        }
        return nearest;
    }

    private static void swapMap(ServerPlayer player) {
        BlockPos anchor = anchor(player);
        List<ItemFrame> frames = frames(player);
        if (anchor == null || frames.isEmpty()) {
            return;
        }
        ServerLevel level = player.level();
        ItemFrame frame = frames.get(player.getRandom().nextInt(frames.size()));
        ItemStack before = frame.getItem().copy();
        ItemStack shown = MapItem.create(level, anchor.getX(), anchor.getZ(), (byte) 0, false, false);
        MapItemSavedData data = MapItem.getSavedData(shown, level);
        if (data == null) {
            return;
        }
        paint(level, data);
        frame.setItem(shown);
        UUID id = frame.getUUID();
        BlockPos at = frame.blockPosition();
        Restore.revert(level, Config.event("wall_map_shows").integer("restore_ticks"), force -> {
            Entity now = level.getEntity(id);
            if (now == null) {
                return force || level.isLoaded(at);
            }
            if (now instanceof ItemFrame same
                    && shown.get(DataComponents.MAP_ID) != null
                    && shown.get(DataComponents.MAP_ID).equals(same.getItem().get(DataComponents.MAP_ID))) {
                same.setItem(before);
            }
            return true;
        });
    }

    /** The surface of the area as a map would show it, shaded by the slope toward the north. */
    private static void paint(ServerLevel level, MapItemSavedData data) {
        int[] above = new int[MAP_SIZE];
        for (int z = 0; z < MAP_SIZE; z++) {
            for (int x = 0; x < MAP_SIZE; x++) {
                int worldX = data.centerX - MAP_SIZE / 2 + x;
                int worldZ = data.centerZ - MAP_SIZE / 2 + z;
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ) - 1;
                BlockPos pos = new BlockPos(worldX, y, worldZ);
                MapColor.Brightness brightness = z == 0 || y == above[x]
                        ? MapColor.Brightness.NORMAL
                        : y > above[x] ? MapColor.Brightness.HIGH : MapColor.Brightness.LOW;
                data.setColor(
                        x, z, level.getBlockState(pos).getMapColor(level, pos).getPackedId(brightness));
                above[x] = y;
            }
        }
    }

    // 527

    private static void snuffCandle(ServerPlayer player) {
        Config.Section config = Config.event("spirit_candle_out");
        Candle.snuff(player, config.integer("ticks"));
        Vec3 hand =
                player.getEyePosition().add(player.getLookAngle().scale(0.6)).add(0, -0.4, 0);
        Nearby.sound(player, SoundEvents.CANDLE_EXTINGUISH, hand, config.tone("sound"));
        player.level().sendParticles(ParticleTypes.SMOKE, hand.x, hand.y, hand.z, 6, 0.05, 0.05, 0.05, 0.01);
    }

    // 534

    private static List<ItemStack> holyWater(ServerPlayer player, boolean cloudy) {
        List<ItemStack> found = new ArrayList<>();
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Content.HOLY_WATER.get()) && (cloudy || !marked(stack))) {
                found.add(stack);
            }
        }
        return found;
    }

    private static boolean marked(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .contains(CLOUDY);
    }

    private static void cloud(ServerPlayer player) {
        Config.Section config = Config.event("holy_water_cloudy");
        long clock = player.level().getOverworldClockTime();
        long dawn = (clock / Ticks.DAY + 1) * Ticks.DAY;
        for (ItemStack stack : holyWater(player, false)) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(CLOUDY, dawn));
            stack.set(
                    DataComponents.LORE,
                    stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY)
                            .withLineAdded(Component.translatable(CLOUDY_LORE).withStyle(ChatFormatting.GRAY)));
        }
        Nearby.sound(player, SoundEvents.BOTTLE_EMPTY, player.position(), config.tone("sound"));
    }

    /**
     * Altar hook: whether this holy water is still cloudy. Once dawn has passed the mark and its lore are taken off and
     * the water is good again.
     */
    public static boolean cloudy(ItemStack stack, ServerLevel level) {
        long until = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getLongOr(CLOUDY, 0L);
        if (until == 0L) {
            return false;
        }
        if (level.getOverworldClockTime() < until) {
            return true;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(CLOUDY));
        if (stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        }
        List<Component> kept = stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines().stream()
                .filter(line -> !(line.getContents() instanceof TranslatableContents t
                        && t.getKey().equals(CLOUDY_LORE)))
                .toList();
        if (kept.isEmpty()) {
            stack.remove(DataComponents.LORE);
        } else {
            stack.set(DataComponents.LORE, new ItemLore(kept));
        }
        return false;
    }

    // 535

    private static boolean hasMirror(ServerPlayer player) {
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            if (player.getInventory().getItem(i).is(Content.MIRROR.get())) {
                return true;
            }
        }
        return false;
    }

    private static void armMirror(ServerPlayer player) {
        Config.Section config = Config.event("mirror_names");
        MIRROR.put(player.getUUID(), new Armed(player.level().getGameTime() + config.integer("ticks"), Said.current()));
        Nearby.sound(player, HauntSounds.MIRROR_RING, player.position(), config.tone("sound"));
    }

    /** Altar hook: the mirror was just used. If a spirit has marked it, it shows that spirit's name, once. */
    public static void mirrorUsed(ServerPlayer player) {
        Armed armed = MIRROR.remove(player.getUUID());
        if (armed == null || armed.until() < player.level().getGameTime()) {
            return;
        }
        List<Sense.Spirit> spirits =
                Sense.of(player, HauntData.get(player.level().getServer()));
        if (spirits.isEmpty()) {
            return;
        }
        String name = Story.of(spirits.get(0).haunting().seed()).name();
        player.sendSystemMessage(Component.translatable("message.restless.mirror_name", name)
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        Director.landed(player, "mirror_names", armed.spirit());
    }
}
