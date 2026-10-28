package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
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
    private static final int CHEST_RADIUS = 10;
    private static final double WEIGHT = 0.5;
    private static final List<String> NAMES = List.of("Why are you here", "Not yours", "Behind you", "Leave", "It sees", "Mine now");
    private static final String MARK = "restless";
    private static final List<String> PAGES = List.of("i can hear you breathing", "do not look behind you", "you were never alone here");

    private ItemEvents() {}

    static List<HauntEvent> all() {
        return List.of(
            // 62
            SimpleEvent.light("swap_slots", EventTag.ITEMS, WEIGHT, player -> !stacks(player, s -> true).isEmpty(), player -> {
                Inventory inventory = player.getInventory();
                int a = player.getRandom().nextInt(36);
                int b = (a + 1 + player.getRandom().nextInt(35)) % 36;
                ItemStack first = inventory.getItem(a);
                inventory.setItem(a, inventory.getItem(b));
                inventory.setItem(b, first);
            }),
            // 63
            SimpleEvent.light("hotbar_shift", EventTag.ITEMS, WEIGHT, player -> true, player -> {
                int slot = (player.getInventory().getSelectedSlot() + 1 + player.getRandom().nextInt(8)) % 9;
                player.getInventory().setSelectedSlot(slot);
                player.connection.send(new ClientboundSetHeldSlotPacket(slot));
            }),
            // 64
            SimpleEvent.light("strange_name", EventTag.ITEMS, WEIGHT, player -> !stacks(player, ItemEvents::unnamed).isEmpty(), player -> {
                List<ItemStack> candidates = stacks(player, ItemEvents::unnamed);
                candidates.get(player.getRandom().nextInt(candidates.size()))
                    .set(DataComponents.CUSTOM_NAME, Component.literal(NAMES.get(player.getRandom().nextInt(NAMES.size()))));
            }),
            // 65: adds a page instead of rewriting, so nothing the player wrote is lost
            SimpleEvent.light("book_page", EventTag.ITEMS, WEIGHT, player -> !stacks(player, ItemEvents::isBook).isEmpty(), player -> {
                List<ItemStack> books = stacks(player, ItemEvents::isBook);
                ItemStack book = books.get(player.getRandom().nextInt(books.size()));
                String page = PAGES.get(player.getRandom().nextInt(PAGES.size()));
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
            SimpleEvent.light("shuffle_chest", EventTag.ITEMS, WEIGHT, player -> chests(player).stream().anyMatch(c -> !isEmpty(c.container())), player -> {
                List<Located> filled = chests(player).stream().filter(c -> !isEmpty(c.container())).toList();
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
            SimpleEvent.harmful("wear", EventTag.ITEMS, WEIGHT, player -> !stacks(player, ItemEvents::wearable).isEmpty(), player -> {
                List<ItemStack> tools = stacks(player, ItemEvents::wearable);
                ItemStack tool = tools.get(player.getRandom().nextInt(tools.size()));
                int loss = Math.max(1, tool.getMaxDamage() / 10);
                tool.setDamageValue(Math.min(tool.getMaxDamage() - 1, tool.getDamageValue() + loss));
            }),
            // 68
            SimpleEvent.harmful("rot", EventTag.ITEMS, WEIGHT, player -> !stacks(player, ItemEvents::rottable).isEmpty(), player -> {
                Inventory inventory = player.getInventory();
                List<Integer> slots = new ArrayList<>();
                for (int i = 0; i < 36; i++) {
                    if (rottable(inventory.getItem(i))) {
                        slots.add(i);
                    }
                }
                int slot = slots.get(player.getRandom().nextInt(slots.size()));
                inventory.setItem(slot, new ItemStack(Items.ROTTEN_FLESH, inventory.getItem(slot).getCount()));
            }),
            // 69: moves at once; the "turns up later" part needs delayed tasks
            SimpleEvent.harmful("vanishing_item", EventTag.ITEMS, WEIGHT, player -> pair(player) != null, player -> {
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
            SimpleEvent.light("map_marker", EventTag.ITEMS, WEIGHT, player -> !unmarkedMaps(player).isEmpty(), ItemEvents::markMap),
            // 70: aims at a point near the player, so it keeps pointing somewhere wrong
            SimpleEvent.harmful("false_compass", EventTag.ITEMS, WEIGHT,
                player -> !stacks(player, s -> s.is(Items.COMPASS) && !s.has(DataComponents.LODESTONE_TRACKER)).isEmpty(),
                player -> {
                    List<ItemStack> compasses = stacks(player, s -> s.is(Items.COMPASS) && !s.has(DataComponents.LODESTONE_TRACKER));
                    BlockPos target = player.blockPosition().offset(
                        (player.getRandom().nextBoolean() ? 1 : -1) * (30 + player.getRandom().nextInt(30)),
                        0,
                        (player.getRandom().nextBoolean() ? 1 : -1) * (30 + player.getRandom().nextInt(30)));
                    compasses.get(0).set(DataComponents.LODESTONE_TRACKER,
                        new LodestoneTracker(Optional.of(GlobalPos.of(player.level().dimension(), target)), false));
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.HOSTILE, 1.0F, 0.6F);
                })
        );
    }

    private record Located(BlockPos pos, Container container) {}

    private record Transfer(Container from, Container to) {}

    private static List<ItemStack> stacks(ServerPlayer player, Predicate<ItemStack> match) {
        List<ItemStack> found = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && match.test(stack)) {
                found.add(stack);
            }
        }
        return found;
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
        BlockPos at = new BlockPos(
            data.centerX + (player.getRandom().nextInt(81) - 40) * blocksPerPixel, 0, data.centerZ + (player.getRandom().nextInt(81) - 40) * blocksPerPixel);
        MapItemSavedData.addTargetDecoration(map, at, MARK, MapDecorationTypes.RED_X);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.HOSTILE, 0.8F, 0.6F);
    }

    private static boolean unnamed(ItemStack stack) {
        return !stack.has(DataComponents.CUSTOM_NAME);
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
    private static List<Located> chests(ServerPlayer player) {
        List<Located> found = new ArrayList<>();
        for (BlockPos pos : Nearby.all(player, CHEST_RADIUS, s -> s.getBlock() instanceof ChestBlock)) {
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
        List<Located> chests = chests(player);
        for (Located from : chests) {
            if (from.container().isEmpty()) {
                continue;
            }
            for (Located to : chests) {
                if (from.pos().distManhattan(to.pos()) > 2 && hasFreeSlot(to.container())) {
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
