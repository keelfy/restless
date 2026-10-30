package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.Restless;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Said;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jspecify.annotations.Nullable;

/** Meddling with the player's inventory and with chests nearby. */
final class ItemEvents {
    private static final String MARK = Restless.MOD_ID;

    private ItemEvents() {}

    static List<HauntEvent> all() {
        return List.of(
            // 62
            SimpleEvent.light("swap_slots", EventTag.ITEMS, player -> !stacks(player, s -> true).isEmpty(), player -> {
                Inventory inventory = player.getInventory();
                int a = player.getRandom().nextInt(Inventory.INVENTORY_SIZE);
                int b = (a + 1 + player.getRandom().nextInt(Inventory.INVENTORY_SIZE - 1)) % Inventory.INVENTORY_SIZE;
                ItemStack first = inventory.getItem(a);
                inventory.setItem(a, inventory.getItem(b));
                inventory.setItem(b, first);
            }),
            // 63
            SimpleEvent.light("hotbar_shift", EventTag.ITEMS, player -> true, player -> {
                int hotbar = Inventory.getSelectionSize();
                int slot = (player.getInventory().getSelectedSlot() + 1 + player.getRandom().nextInt(hotbar - 1)) % hotbar;
                player.getInventory().setSelectedSlot(slot);
                player.connection.send(new ClientboundSetHeldSlotPacket(slot));
            }),
            // 65: adds a page instead of rewriting, so nothing the player wrote is lost
            SimpleEvent.light("book_page", EventTag.ITEMS,
                player -> !stacks(player, ItemEvents::isBook).isEmpty() && Said.left(player, "book_page", Config.event("book_page").texts("pages")), player -> {
                List<ItemStack> books = stacks(player, ItemEvents::isBook);
                String page = books.isEmpty() ? null : Said.pick(player, "book_page", Config.event("book_page").texts("pages"));
                if (page == null) {
                    return;
                }
                ItemStack book = books.get(player.getRandom().nextInt(books.size()));
                if (book.is(Items.WRITABLE_BOOK)) {
                    List<Filterable<String>> pages = new ArrayList<>(book.getOrDefault(DataComponents.WRITABLE_BOOK_CONTENT, WritableBookContent.EMPTY).pages());
                    pages.add(Filterable.passThrough(page));
                    book.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(pages));
                } else {
                    WrittenBookContent content = book.get(DataComponents.WRITTEN_BOOK_CONTENT);
                    List<Filterable<Component>> pages = new ArrayList<>(content.pages());
                    pages.add(Filterable.passThrough(Component.literal(page)));
                    book.set(DataComponents.WRITTEN_BOOK_CONTENT,
                        new WrittenBookContent(content.title(), content.author(), content.generation(), pages, content.resolved()));
                }
            }),
            // 66
            SimpleEvent.light("shuffle_chest", EventTag.ITEMS, player -> chests(player, "shuffle_chest").stream().anyMatch(c -> !isEmpty(c.container())), player -> {
                List<Located> filled = chests(player, "shuffle_chest").stream().filter(c -> !isEmpty(c.container())).toList();
                Container container = filled.get(player.getRandom().nextInt(filled.size())).container();
                List<ItemStack> contents = new ArrayList<>();
                for (int i = 0; i < container.getContainerSize(); i++) {
                    contents.add(container.getItem(i));
                }
                Collections.shuffle(contents, new Random(player.getRandom().nextLong()));
                for (int i = 0; i < contents.size(); i++) {
                    container.setItem(i, contents.get(i));
                }
                container.setChanged();
            }),
            // 67
            SimpleEvent.harmful("wear", EventTag.ITEMS, player -> !stacks(player, ItemEvents::wearable).isEmpty(), player -> {
                List<ItemStack> tools = stacks(player, ItemEvents::wearable);
                ItemStack tool = tools.get(player.getRandom().nextInt(tools.size()));
                int loss = Math.max(1, (int) (tool.getMaxDamage() * Config.event("wear").num("fraction")));
                tool.setDamageValue(Math.min(tool.getMaxDamage() - 1, tool.getDamageValue() + loss));
            }),
            // 68
            SimpleEvent.harmful("rot", EventTag.ITEMS, player -> !stacks(player, ItemEvents::rottable).isEmpty(), player -> {
                Inventory inventory = player.getInventory();
                List<Integer> slots = new ArrayList<>();
                for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
                    if (rottable(inventory.getItem(i))) {
                        slots.add(i);
                    }
                }
                int slot = slots.get(player.getRandom().nextInt(slots.size()));
                inventory.setItem(slot, new ItemStack(Items.ROTTEN_FLESH, inventory.getItem(slot).getCount()));
            }),
            // takes some of the worn armor off, or all of it; nothing is lost: it goes to the inventory, or to the floor
            SimpleEvent.harmful("armor_off", EventTag.ITEMS, player -> !worn(player).isEmpty(), ItemEvents::takeArmorOff),
            // 69: moves at once; the "turns up later" part needs delayed tasks
            SimpleEvent.harmful("vanishing_item", EventTag.ITEMS, player -> pair(player) != null, player -> {
                Transfer transfer = pair(player);
                if (transfer == null) {
                    return;
                }
                Container from = transfer.from();
                Container to = transfer.to();
                for (int i = 0; i < from.getContainerSize(); i++) {
                    ItemStack stack = from.getItem(i);
                    if (!stack.isEmpty()) {
                        for (int j = 0; j < to.getContainerSize(); j++) {
                            if (to.getItem(j).isEmpty()) {
                                to.setItem(j, stack);
                                from.setItem(i, ItemStack.EMPTY);
                                from.setChanged();
                                to.setChanged();
                                return;
                            }
                        }
                    }
                }
            }),
            // 71: stays on the map for good; the marker lives in the item, so only a new map gets rid of it
            SimpleEvent.light("map_marker", EventTag.ITEMS, player -> !unmarkedMaps(player).isEmpty(), ItemEvents::markMap),
            // 146: the tool is drawn as another one but works the same; under hardcore (or real swaps) it truly becomes the other one
            SimpleEvent.light("tool_look", EventTag.ITEMS, player -> !stacks(player, ItemEvents::lookable).isEmpty(), ItemEvents::changeLook),
            // 70: aims at a point near the player, so it keeps pointing somewhere wrong
            SimpleEvent.harmful("false_compass", EventTag.ITEMS,
                player -> !stacks(player, s -> s.is(Items.COMPASS) && !s.has(DataComponents.LODESTONE_TRACKER)).isEmpty(),
                player -> {
                    List<ItemStack> compasses = stacks(player, s -> s.is(Items.COMPASS) && !s.has(DataComponents.LODESTONE_TRACKER));
                    Config.Section config = Config.event("false_compass");
                    BlockPos target = player.blockPosition().offset(
                        (player.getRandom().nextBoolean() ? 1 : -1) * (config.integer("min_offset") + player.getRandom().nextInt(config.integer("offset_spread"))),
                        0,
                        (player.getRandom().nextBoolean() ? 1 : -1) * (config.integer("min_offset") + player.getRandom().nextInt(config.integer("offset_spread"))));
                    compasses.get(0).set(DataComponents.LODESTONE_TRACKER,
                        new LodestoneTracker(Optional.of(GlobalPos.of(player.level().dimension(), target)), false));
                    Config.Tone tone = config.tone("sound");
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.HOSTILE, tone.volume(), tone.pitch());
                })
        );
    }

    private record Located(BlockPos pos, Container container) {}

    private record Transfer(Container from, Container to) {}

    private static List<ItemStack> stacks(ServerPlayer player, Predicate<ItemStack> match) {
        List<ItemStack> found = new ArrayList<>();
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && match.test(stack)) {
                found.add(stack);
            }
        }
        return found;
    }

    private static final String LOOK = MARK + "_look";

    private static boolean lookable(ItemStack stack) {
        return Config.event("tool_look").texts("models").contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
            && !disguised(stack);
    }

    private static boolean disguised(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains(LOOK);
    }

    private static void changeLook(ServerPlayer player) {
        Config.Section config = Config.event("tool_look");
        List<ItemStack> tools = stacks(player, ItemEvents::lookable);
        ItemStack tool = tools.get(player.getRandom().nextInt(tools.size()));
        Identifier own = BuiltInRegistries.ITEM.getKey(tool.getItem());
        List<String> others = config.texts("models").stream().filter(id -> !id.equals(own.toString())).toList();
        Identifier model = Identifier.parse(others.get(player.getRandom().nextInt(others.size())));
        if (Swaps.real(player.level())) {
            player.getInventory().setItem(player.getInventory().findSlotMatchingItem(tool), new ItemStack(BuiltInRegistries.ITEM.getValue(model), tool.getCount()));
            return;
        }
        tool.set(DataComponents.ITEM_MODEL, model);
        CustomData.update(DataComponents.CUSTOM_DATA, tool, tag -> tag.putBoolean(LOOK, true));
        UUID id = player.getUUID();
        Restore.revert(player.level(), config.integer("restore_ticks"), force -> {
            ServerPlayer owner = player.level().getServer().getPlayerList().getPlayer(id);
            if (owner == null) {
                return force;
            }
            Inventory inventory = owner.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (disguised(stack)) {
                    stack.set(DataComponents.ITEM_MODEL, stack.getItem().components().get(DataComponents.ITEM_MODEL));
                    CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(LOOK));
                }
            }
            return true;
        });
    }

    private static List<ItemStack> unmarkedMaps(ServerPlayer player) {
        return stacks(player, s -> s.is(Items.FILLED_MAP) && MapItem.getSavedData(s, player.level()) != null
            && s.getOrDefault(DataComponents.MAP_DECORATIONS, MapDecorations.EMPTY).decorations().keySet().stream().noneMatch(k -> k.startsWith(MARK)));
    }

    /** An explorer's red X somewhere on the map's area. */
    private static void markMap(ServerPlayer player) {
        List<ItemStack> maps = unmarkedMaps(player);
        if (maps.isEmpty()) {
            return;
        }
        ItemStack map = maps.get(player.getRandom().nextInt(maps.size()));
        MapItemSavedData data = MapItem.getSavedData(map, player.level());
        if (data == null) {
            return;
        }
        int blocksPerPixel = 1 << data.scale;
        Config.Section config = Config.event("map_marker");
        int spread = config.integer("spread_pixels");
        BlockPos at = new BlockPos(
            data.centerX + (player.getRandom().nextInt(2 * spread + 1) - spread) * blocksPerPixel, 0,
            data.centerZ + (player.getRandom().nextInt(2 * spread + 1) - spread) * blocksPerPixel);
        MapItemSavedData.addTargetDecoration(map, at, MARK, MapDecorationTypes.RED_X);
        Config.Tone tone = config.tone("sound");
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.HOSTILE, tone.volume(), tone.pitch());
    }

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static List<EquipmentSlot> worn(ServerPlayer player) {
        return java.util.Arrays.stream(ARMOR).filter(slot -> !player.getItemBySlot(slot).isEmpty()).toList();
    }

    private static void takeArmorOff(ServerPlayer player) {
        List<EquipmentSlot> slots = new ArrayList<>(worn(player));
        Collections.shuffle(slots, new Random(player.getRandom().nextLong()));
        Config.Section config = Config.event("armor_off");
        int span = config.integer("max_pieces") - config.integer("min_pieces") + 1;
        int count = Math.min(slots.size(), config.integer("min_pieces") + player.getRandom().nextInt(span));
        for (EquipmentSlot slot : slots.subList(0, count)) {
            ItemStack piece = player.getItemBySlot(slot);
            player.setItemSlot(slot, ItemStack.EMPTY);
            if (!player.getInventory().add(piece)) {
                player.spawnAtLocation(player.level(), piece);
            }
        }
        Config.Tone tone = config.tone("sound");
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.HOSTILE, tone.volume(), tone.pitch());
    }

    private static boolean isBook(ItemStack stack) {
        return stack.is(Items.WRITABLE_BOOK) || stack.is(Items.WRITTEN_BOOK) && stack.has(DataComponents.WRITTEN_BOOK_CONTENT);
    }

    private static boolean wearable(ItemStack stack) {
        return stack.isDamageableItem() && stack.getDamageValue() < stack.getMaxDamage() - 1;
    }

    private static boolean rottable(ItemStack stack) {
        return stack.has(DataComponents.FOOD) && !stack.is(Items.ROTTEN_FLESH);
    }

    private static boolean isEmpty(Container container) {
        return container.isEmpty();
    }

    /** One entry per chest block; a double chest shows up twice, once per half. */
    private static List<Located> chests(ServerPlayer player, String event) {
        List<Located> found = new ArrayList<>();
        for (BlockPos pos : Nearby.all(player, event, s -> s.getBlock() instanceof ChestBlock)) {
            BlockState state = player.level().getBlockState(pos);
            Container container = ChestBlock.getContainer((ChestBlock) state.getBlock(), state, player.level(), pos, true);
            if (container != null) {
                found.add(new Located(pos, container));
            }
        }
        return found;
    }

    /** A chest with something in it and a different chest (not its other half) with a free slot. */
    private static @Nullable Transfer pair(ServerPlayer player) {
        List<Located> chests = chests(player, "vanishing_item");
        int apart = Config.event("vanishing_item").integer("min_distance");
        for (Located from : chests) {
            if (from.container().isEmpty()) {
                continue;
            }
            for (Located to : chests) {
                if (from.pos().distManhattan(to.pos()) > apart && hasFreeSlot(to.container())) {
                    return new Transfer(from.container(), to.container());
                }
            }
        }
        return null;
    }

    private static boolean hasFreeSlot(Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
