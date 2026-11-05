package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.Hardcore;
import dev.keelfy.restless.haunt.HauntData;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Said;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Meddling with the player's inventory and with chests nearby. */
final class ItemEvents {
    private static final String MARK = Restless.MOD_ID;

    private ItemEvents() {}

    static List<HauntEvent> all() {
        return List.of(
                // 62
                SimpleEvent.light(
                        "swap_slots",
                        EventTag.ITEMS,
                        player -> !stacks(player, s -> true).isEmpty(),
                        player -> {
                            Inventory inventory = player.getInventory();
                            int a = player.getRandom().nextInt(Inventory.INVENTORY_SIZE);
                            int b = (a + 1 + player.getRandom().nextInt(Inventory.INVENTORY_SIZE - 1))
                                    % Inventory.INVENTORY_SIZE;
                            ItemStack first = inventory.getItem(a);
                            inventory.setItem(a, inventory.getItem(b));
                            inventory.setItem(b, first);
                        }),
                // 63
                SimpleEvent.light("hotbar_shift", EventTag.ITEMS, SimpleEvent.ALWAYS, player -> {
                    int hotbar = Inventory.getSelectionSize();
                    int slot = (player.getInventory().getSelectedSlot()
                                    + 1
                                    + player.getRandom().nextInt(hotbar - 1))
                            % hotbar;
                    player.getInventory().setSelectedSlot(slot);
                    player.connection.send(new ClientboundSetHeldSlotPacket(slot));
                }),
                // 65: adds a page instead of rewriting, so nothing the player wrote is lost
                SimpleEvent.light(
                        "book_page",
                        EventTag.ITEMS,
                        player -> !stacks(player, ItemEvents::isBook).isEmpty()
                                && Said.left(
                                        player,
                                        "book_page",
                                        Config.event("book_page").texts("pages")),
                        player -> {
                            List<ItemStack> books = stacks(player, ItemEvents::isBook);
                            String page = books.isEmpty()
                                    ? null
                                    : Said.pick(
                                            player,
                                            "book_page",
                                            Config.event("book_page").texts("pages"));
                            if (page == null) {
                                return;
                            }
                            ItemStack book = books.get(player.getRandom().nextInt(books.size()));
                            if (book.is(Items.WRITABLE_BOOK)) {
                                List<Filterable<String>> pages = new ArrayList<>(book.getOrDefault(
                                                DataComponents.WRITABLE_BOOK_CONTENT, WritableBookContent.EMPTY)
                                        .pages());
                                pages.add(Filterable.passThrough(page));
                                book.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(pages));
                            } else {
                                WrittenBookContent content = book.get(DataComponents.WRITTEN_BOOK_CONTENT);
                                List<Filterable<Component>> pages = new ArrayList<>(content.pages());
                                pages.add(Filterable.passThrough(Component.literal(page)));
                                book.set(
                                        DataComponents.WRITTEN_BOOK_CONTENT,
                                        new WrittenBookContent(
                                                content.title(),
                                                content.author(),
                                                content.generation(),
                                                pages,
                                                content.resolved()));
                            }
                        }),
                // 66
                SimpleEvent.light(
                        "shuffle_chest",
                        EventTag.ITEMS,
                        player -> chests(player, "shuffle_chest").stream().anyMatch(c -> !isEmpty(c.container())),
                        player -> {
                            List<Located> filled = chests(player, "shuffle_chest").stream()
                                    .filter(c -> !isEmpty(c.container()))
                                    .toList();
                            Container container = filled.get(player.getRandom().nextInt(filled.size()))
                                    .container();
                            List<ItemStack> contents = new ArrayList<>();
                            for (int i = 0; i < container.getContainerSize(); i++) {
                                contents.add(container.getItem(i));
                            }
                            Collections.shuffle(
                                    contents, new Random(player.getRandom().nextLong()));
                            for (int i = 0; i < contents.size(); i++) {
                                container.setItem(i, contents.get(i));
                            }
                            container.setChanged();
                        }),
                // 67
                SimpleEvent.harmful(
                        "wear",
                        EventTag.ITEMS,
                        player -> !stacks(player, ItemEvents::wearable).isEmpty(),
                        player -> {
                            List<ItemStack> tools = stacks(player, ItemEvents::wearable);
                            ItemStack tool = tools.get(player.getRandom().nextInt(tools.size()));
                            int loss = Math.max(1, (int)
                                    (tool.getMaxDamage() * Config.event("wear").num("fraction")));
                            tool.setDamageValue(Math.min(tool.getMaxDamage() - 1, tool.getDamageValue() + loss));
                        }),
                // 68
                SimpleEvent.harmful(
                        "rot",
                        EventTag.ITEMS,
                        player -> !stacks(player, ItemEvents::rottable).isEmpty(),
                        player -> {
                            Inventory inventory = player.getInventory();
                            List<Integer> slots = new ArrayList<>();
                            for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
                                if (rottable(inventory.getItem(i))) {
                                    slots.add(i);
                                }
                            }
                            int slot = slots.get(player.getRandom().nextInt(slots.size()));
                            inventory.setItem(
                                    slot,
                                    new ItemStack(
                                            Items.ROTTEN_FLESH,
                                            inventory.getItem(slot).getCount()));
                        }),
                // takes some of the worn armor off, or all of it; nothing is lost: it goes to the inventory, or to the
                // floor
                SimpleEvent.harmful(
                        "armor_off", EventTag.ITEMS, player -> !worn(player).isEmpty(), ItemEvents::takeArmorOff),
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
                SimpleEvent.light(
                        "map_marker",
                        EventTag.ITEMS,
                        player -> !unmarkedMaps(player).isEmpty(),
                        ItemEvents::markMap),
                // 146: the tool is drawn as another one but works the same; under hardcore (or real swaps) it truly
                // becomes the other one
                SimpleEvent.light(
                        "tool_look",
                        EventTag.ITEMS,
                        player -> !stacks(player, ItemEvents::lookable).isEmpty(),
                        ItemEvents::changeLook),
                // 70: aims at a point near the player, so it keeps pointing somewhere wrong
                SimpleEvent.harmful(
                        "false_compass",
                        EventTag.ITEMS,
                        player -> !stacks(player, s -> s.is(Items.COMPASS) && !s.has(DataComponents.LODESTONE_TRACKER))
                                .isEmpty(),
                        player -> {
                            List<ItemStack> compasses = stacks(
                                    player, s -> s.is(Items.COMPASS) && !s.has(DataComponents.LODESTONE_TRACKER));
                            Config.Section config = Config.event("false_compass");
                            BlockPos target = player.blockPosition()
                                    .offset(
                                            (player.getRandom().nextBoolean() ? 1 : -1)
                                                    * (config.integer("min_offset")
                                                            + player.getRandom()
                                                                    .nextInt(config.integer("offset_spread"))),
                                            0,
                                            (player.getRandom().nextBoolean() ? 1 : -1)
                                                    * (config.integer("min_offset")
                                                            + player.getRandom()
                                                                    .nextInt(config.integer("offset_spread"))));
                            compasses
                                    .get(0)
                                    .set(
                                            DataComponents.LODESTONE_TRACKER,
                                            new LodestoneTracker(
                                                    Optional.of(GlobalPos.of(
                                                            player.level().dimension(), target)),
                                                    false));
                            Config.Tone tone = config.tone("sound");
                            player.level()
                                    .playSound(
                                            null,
                                            player.getX(),
                                            player.getY(),
                                            player.getZ(),
                                            SoundEvents.LODESTONE_COMPASS_LOCK,
                                            SoundSource.HOSTILE,
                                            tone.volume(),
                                            tone.pitch());
                        }),
                // 421: the worn elytra shows as almost broken for a moment; hardcore wears it for real
                SimpleEvent.light(
                        "elytra_flicker",
                        EventTag.ITEMS,
                        player -> player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)
                                && wearable(player.getItemBySlot(EquipmentSlot.CHEST))
                                && high(player, Config.event("elytra_flicker").integer("min_height")),
                        player -> {
                            Config.Section config = Config.event("elytra_flicker");
                            ItemStack elytra = player.getItemBySlot(EquipmentSlot.CHEST);
                            if (Hardcore.on(player.level())) {
                                wearDown(elytra, config.num("hardcore_fraction"));
                                return;
                            }
                            ItemStack shown = elytra.copy();
                            shown.setDamageValue(shown.getMaxDamage() - 1);
                            lie(player, InventoryMenu.ARMOR_SLOT_START + 1, shown, config.integer("ticks"));
                        }),
                // 424: the stew is gone and the bowl is left
                SimpleEvent.harmful(
                        "stew_bowl",
                        EventTag.ITEMS,
                        player -> !slots(player, ItemEvents::stew).isEmpty(),
                        player -> {
                            List<Integer> found = slots(player, ItemEvents::stew);
                            int slot = found.get(player.getRandom().nextInt(found.size()));
                            Inventory inventory = player.getInventory();
                            inventory.getItem(slot).shrink(1);
                            ItemStack bowl = new ItemStack(Items.BOWL);
                            if (inventory.getItem(slot).isEmpty()) {
                                inventory.setItem(slot, bowl);
                            } else if (!inventory.add(bowl)) {
                                player.spawnAtLocation(player.level(), bowl);
                            }
                            Config.Tone tone = Config.event("stew_bowl").tone("sound");
                            player.level()
                                    .playSound(
                                            null,
                                            player.getX(),
                                            player.getY(),
                                            player.getZ(),
                                            SoundEvents.GENERIC_EAT,
                                            SoundSource.HOSTILE,
                                            tone.volume(),
                                            tone.pitch());
                        }),
                // 427: a water bucket is shown as a pufferfish bucket; hardcore swaps it for real (same count)
                SimpleEvent.light(
                        "bucket_pufferfish",
                        EventTag.ITEMS,
                        player -> !slots(player, s -> s.is(Items.WATER_BUCKET)).isEmpty(),
                        player -> {
                            List<Integer> found = slots(player, s -> s.is(Items.WATER_BUCKET));
                            int slot = found.get(player.getRandom().nextInt(found.size()));
                            ItemStack shown = new ItemStack(
                                    Items.PUFFERFISH_BUCKET,
                                    player.getInventory().getItem(slot).getCount());
                            if (Hardcore.on(player.level())) {
                                player.getInventory().setItem(slot, shown);
                                return;
                            }
                            lie(
                                    player,
                                    menuSlot(slot),
                                    shown,
                                    Config.event("bucket_pufferfish").integer("ticks"));
                        }),
                // 428: part of a stack of torches is gone; at least one is left
                SimpleEvent.harmful(
                        "torch_burn",
                        EventTag.ITEMS,
                        player -> !slots(player, ItemEvents::torches).isEmpty(),
                        player -> {
                            Config.Section config = Config.event("torch_burn");
                            List<Integer> found = slots(player, ItemEvents::torches);
                            ItemStack torches = player.getInventory()
                                    .getItem(found.get(player.getRandom().nextInt(found.size())));
                            torches.shrink(Math.min(torches.getCount() - 1, Math.max(1, (int)
                                    (torches.getCount() * config.num("fraction")))));
                            Config.Tone tone = config.tone("sound");
                            player.level()
                                    .playSound(
                                            null,
                                            player.getX(),
                                            player.getY(),
                                            player.getZ(),
                                            SoundEvents.FIRE_EXTINGUISH,
                                            SoundSource.HOSTILE,
                                            tone.volume(),
                                            tone.pitch());
                        }),
                // 429: a bone turns up in a bundle (nothing is taken out)
                SimpleEvent.light(
                        "bundle_bone",
                        EventTag.ITEMS,
                        player -> !stacks(player, s -> withBone(s) != null).isEmpty(),
                        player -> {
                            List<ItemStack> bundles = stacks(player, s -> withBone(s) != null);
                            ItemStack bundle = bundles.get(player.getRandom().nextInt(bundles.size()));
                            BundleContents next = withBone(bundle);
                            if (next == null) {
                                return;
                            }
                            bundle.set(DataComponents.BUNDLE_CONTENTS, next);
                            Config.Tone tone = Config.event("bundle_bone").tone("sound");
                            player.level()
                                    .playSound(
                                            null,
                                            player.getX(),
                                            player.getY(),
                                            player.getZ(),
                                            SoundEvents.BUNDLE_INSERT,
                                            SoundSource.HOSTILE,
                                            tone.volume(),
                                            tone.pitch());
                        }),
                // 430: a shield in the hand blocks a hit from nothing
                SimpleEvent.light(
                        "shield_ghost",
                        EventTag.ITEMS,
                        player -> player.getMainHandItem().is(Items.SHIELD)
                                || player.getOffhandItem().is(Items.SHIELD),
                        player -> {
                            Config.Tone tone = Config.event("shield_ghost").tone("sound");
                            player.level()
                                    .playSound(
                                            null,
                                            player.getX(),
                                            player.getY(),
                                            player.getZ(),
                                            SoundEvents.SHIELD_BLOCK,
                                            SoundSource.HOSTILE,
                                            tone.volume(),
                                            tone.pitch());
                        }),
                // 433: an item lying nearby flies at the player
                SimpleEvent.light(
                        "item_throwback",
                        EventTag.ITEMS,
                        player -> !lying(player).isEmpty(),
                        player -> {
                            Config.Section config = Config.event("item_throwback");
                            List<ItemEntity> items = lying(player);
                            ItemEntity item = items.get(player.getRandom().nextInt(items.size()));
                            Vec3 aim = player.getEyePosition().subtract(item.position());
                            Vec3 velocity =
                                    aim.normalize().scale(config.num("speed")).add(0, config.num("lift"), 0);
                            item.setDeltaMovement(velocity);
                            player.connection.send(new ClientboundSetEntityMotionPacket(item));
                        }),
                // 436: a goat horn in the inventory sounds by itself, in its own tune
                SimpleEvent.light(
                        "phantom_horn",
                        EventTag.ITEMS,
                        player -> !stacks(player, ItemEvents::horn).isEmpty(),
                        player -> {
                            List<ItemStack> horns = stacks(player, ItemEvents::horn);
                            ItemStack horn = horns.get(player.getRandom().nextInt(horns.size()));
                            Config.Tone tone = Config.event("phantom_horn").tone("sound");
                            player.level()
                                    .playSound(
                                            null,
                                            player.getX(),
                                            player.getY(),
                                            player.getZ(),
                                            horn.get(DataComponents.INSTRUMENT)
                                                    .instrument()
                                                    .value()
                                                    .soundEvent(),
                                            SoundSource.RECORDS,
                                            tone.volume(),
                                            tone.pitch());
                        }),
                // 437: leather armor turns black; put back after a while, but it stays under hardcore
                SimpleEvent.light(
                        "armor_dye",
                        EventTag.ITEMS,
                        player -> !everything(player, ItemEvents::dyeable).isEmpty(),
                        player -> {
                            List<ItemStack> armor = everything(player, ItemEvents::dyeable);
                            ItemStack piece = armor.get(player.getRandom().nextInt(armor.size()));
                            temporarily(
                                    player,
                                    piece,
                                    DYE,
                                    Config.event("armor_dye").integer("restore_ticks"),
                                    true,
                                    stack -> {
                                        DyedItemColor was = stack.get(DataComponents.DYED_COLOR);
                                        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(BLACK));
                                        return was == null ? NO_DYE : was.rgb();
                                    },
                                    (stack, was) -> {
                                        if (was == NO_DYE) {
                                            stack.remove(DataComponents.DYED_COLOR);
                                        } else {
                                            stack.set(DataComponents.DYED_COLOR, new DyedItemColor(was));
                                        }
                                    });
                        }),
                // 438: a plain item glints as if enchanted, then stops
                SimpleEvent.light(
                        "item_glint",
                        EventTag.ITEMS,
                        player -> !stacks(player, ItemEvents::plain).isEmpty(),
                        player -> {
                            List<ItemStack> found = stacks(player, ItemEvents::plain);
                            temporarily(
                                    player,
                                    found.get(player.getRandom().nextInt(found.size())),
                                    GLINT,
                                    Config.event("item_glint").integer("restore_ticks"),
                                    false,
                                    stack -> {
                                        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                                        return 0;
                                    },
                                    (stack, was) -> stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
                        }),
                // 439: a line of lore on an item; taken away after a while, but it stays under hardcore
                SimpleEvent.light(
                        "item_lore",
                        EventTag.ITEMS,
                        player -> !everything(player, s -> !marked(s, LORE)).isEmpty(),
                        player -> {
                            Config.Section config = Config.event("item_lore");
                            List<ItemStack> found = everything(player, s -> !marked(s, LORE));
                            List<String> lines = config.texts("lines");
                            String line = lines.get(player.getRandom().nextInt(lines.size()));
                            temporarily(
                                    player,
                                    found.get(player.getRandom().nextInt(found.size())),
                                    LORE,
                                    config.integer("restore_ticks"),
                                    true,
                                    stack -> {
                                        stack.set(
                                                DataComponents.LORE,
                                                stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY)
                                                        .withLineAdded(Component.literal(line)));
                                        return 0;
                                    },
                                    (stack, was) -> {
                                        List<Component> kept =
                                                stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines().stream()
                                                        .filter(c -> !config.texts("lines")
                                                                .contains(c.getString()))
                                                        .toList();
                                        if (kept.isEmpty()) {
                                            stack.remove(DataComponents.LORE);
                                        } else {
                                            stack.set(DataComponents.LORE, new ItemLore(kept));
                                        }
                                    });
                        }),
                // 440: a tool shows full durability, then almost broken, then the truth; hardcore wears it for real
                SimpleEvent.harmful(
                        "tool_mirage",
                        EventTag.ITEMS,
                        player -> !slots(player, ItemEvents::wearable).isEmpty(),
                        player -> {
                            Config.Section config = Config.event("tool_mirage");
                            List<Integer> found = slots(player, ItemEvents::wearable);
                            int slot = found.get(player.getRandom().nextInt(found.size()));
                            ItemStack tool = player.getInventory().getItem(slot);
                            if (Hardcore.on(player.level())) {
                                wearDown(tool, config.num("hardcore_fraction"));
                                return;
                            }
                            int menuSlot = menuSlot(slot);
                            ItemStack full = tool.copy();
                            full.setDamageValue(0);
                            show(player, menuSlot, full);
                            Scheduler.repeat(player, config.integer("ticks"), config.integer("ticks"), (p, n) -> {
                                ItemStack now =
                                        p.inventoryMenu.getSlot(menuSlot).getItem();
                                if (n == 0 && wearable(now)) {
                                    ItemStack broken = now.copy();
                                    broken.setDamageValue(broken.getMaxDamage() - 1);
                                    show(p, menuSlot, broken);
                                    return true;
                                }
                                show(p, menuSlot, now.copy());
                                return false;
                            });
                        }),
                // 422: a lodestone compass points at the anchor of the spirit around; put back after a while, but it
                // stays under hardcore
                SimpleEvent.light(
                        "lodestone_lure",
                        EventTag.ITEMS,
                        player -> anchor(player) != null
                                && !stacks(player, ItemEvents::lodestoneCompass).isEmpty(),
                        player -> {
                            BlockPos anchor = anchor(player);
                            List<ItemStack> found = stacks(player, ItemEvents::lodestoneCompass);
                            if (anchor == null || found.isEmpty()) {
                                return;
                            }
                            ItemStack compass = found.get(player.getRandom().nextInt(found.size()));
                            LodestoneTracker was = compass.get(DataComponents.LODESTONE_TRACKER);
                            temporarily(
                                    player,
                                    compass,
                                    LURE,
                                    Config.event("lodestone_lure").integer("restore_ticks"),
                                    true,
                                    stack -> {
                                        stack.set(
                                                DataComponents.LODESTONE_TRACKER,
                                                new LodestoneTracker(
                                                        Optional.of(GlobalPos.of(
                                                                player.level().dimension(), anchor)),
                                                        false));
                                        return 0;
                                    },
                                    (stack, ignored) -> stack.set(DataComponents.LODESTONE_TRACKER, was));
                        }),
                // 423: a written book signed with the player's own name turns up in the inventory
                SimpleEvent.light(
                        "ghost_author",
                        EventTag.ITEMS,
                        player -> player.getInventory().getFreeSlot() != -1
                                && stacks(player, s -> signedBy(s, player)).isEmpty()
                                && Said.left(
                                        player,
                                        "ghost_author",
                                        Config.event("ghost_author").texts("pages")),
                        player -> {
                            Config.Section config = Config.event("ghost_author");
                            String page = Said.pick(player, "ghost_author", config.texts("pages"));
                            if (page == null || player.getInventory().getFreeSlot() == -1) {
                                return;
                            }
                            List<String> titles = config.texts("titles");
                            ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
                            book.set(
                                    DataComponents.WRITTEN_BOOK_CONTENT,
                                    new WrittenBookContent(
                                            Filterable.passThrough(titles.get(
                                                    player.getRandom().nextInt(titles.size()))),
                                            player.getGameProfile().name(),
                                            0,
                                            List.of(Filterable.passThrough(Component.literal(page))),
                                            true));
                            player.getInventory().add(book);
                        }),
                // 425: a potion turns into another one; put back after a while, but it stays under hardcore
                SimpleEvent.light(
                        "potion_swap",
                        EventTag.ITEMS,
                        player -> !stacks(player, ItemEvents::swappable).isEmpty(),
                        player -> {
                            Config.Section config = Config.event("potion_swap");
                            List<ItemStack> found = stacks(player, ItemEvents::swappable);
                            ItemStack bottle = found.get(player.getRandom().nextInt(found.size()));
                            PotionContents was = bottle.get(DataComponents.POTION_CONTENTS);
                            List<Holder.Reference<Potion>> others = config.texts("potions").stream()
                                    .map(id -> BuiltInRegistries.POTION.get(Identifier.parse(id)))
                                    .flatMap(Optional::stream)
                                    .filter(potion -> !was.is(potion))
                                    .toList();
                            if (others.isEmpty()) {
                                return;
                            }
                            Holder<Potion> next = others.get(player.getRandom().nextInt(others.size()));
                            temporarily(
                                    player,
                                    bottle,
                                    POTION,
                                    config.integer("restore_ticks"),
                                    true,
                                    stack -> {
                                        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(next));
                                        return 0;
                                    },
                                    (stack, ignored) -> stack.set(DataComponents.POTION_CONTENTS, was));
                        }),
                // 426: arrows leave the inventory one by one and turn up again later; under hardcore they stay gone
                SimpleEvent.harmful(
                        "vanishing_arrows",
                        EventTag.ITEMS,
                        player -> !slots(player, ItemEvents::arrow).isEmpty(),
                        ItemEvents::takeArrows),
                // 431: a loaded crossbow in a hand fires straight up
                SimpleEvent.harmful("crossbow_misfire", EventTag.ITEMS, player -> loadedIn(player) != null, player -> {
                    Config.Section config = Config.event("crossbow_misfire");
                    InteractionHand hand = loadedIn(player);
                    if (hand == null) {
                        return;
                    }
                    ItemStack crossbow = player.getItemInHand(hand);
                    float pitch = player.getXRot();
                    player.setXRot(-90.0F);
                    try {
                        ((CrossbowItem) crossbow.getItem())
                                .performShooting(
                                        player.level(),
                                        player,
                                        hand,
                                        crossbow,
                                        (float) config.num("power"),
                                        (float) config.num("uncertainty"),
                                        null);
                    } finally {
                        player.setXRot(pitch);
                    }
                }),
                // 432: the map in a hand shows the area of the anchor of the spirit around; put back after a while, but
                // it stays under hardcore
                SimpleEvent.light(
                        "map_elsewhere",
                        EventTag.ITEMS,
                        player -> anchor(player) != null && mapIn(player) != null,
                        player -> {
                            BlockPos anchor = anchor(player);
                            ItemStack map = mapIn(player);
                            ServerLevel level = player.level();
                            if (anchor == null || map == null) {
                                return;
                            }
                            MapItemSavedData data = MapItem.getSavedData(map, level);
                            if (data == null) {
                                return;
                            }
                            MapId was = map.get(DataComponents.MAP_ID);
                            temporarily(
                                    player,
                                    map,
                                    ELSEWHERE,
                                    Config.event("map_elsewhere").integer("restore_ticks"),
                                    true,
                                    stack -> {
                                        MapItem.applyNewSavedData(
                                                level, stack, anchor.getX(), anchor.getZ(), data.scale, false, false);
                                        MapItem.renderBiomePreviewMap(level, stack);
                                        return 0;
                                    },
                                    (stack, ignored) -> stack.set(DataComponents.MAP_ID, was));
                        }),
                // 434: an item lying close slides away from the player
                SimpleEvent.light(
                        "shy_item",
                        EventTag.ITEMS,
                        player -> !around(player, ItemEntity.class, "shy_item").isEmpty(),
                        player -> {
                            Config.Section config = Config.event("shy_item");
                            List<ItemEntity> items = around(player, ItemEntity.class, "shy_item");
                            if (!items.isEmpty()) {
                                shove(
                                        player,
                                        items.get(player.getRandom().nextInt(items.size())),
                                        config.num("speed"),
                                        config.num("lift"));
                            }
                        }),
                // 435: experience orbs nearby fly away from the player
                SimpleEvent.light(
                        "orb_scatter",
                        EventTag.ITEMS,
                        player -> !around(player, ExperienceOrb.class, "orb_scatter")
                                .isEmpty(),
                        player -> {
                            Config.Section config = Config.event("orb_scatter");
                            for (ExperienceOrb orb : around(player, ExperienceOrb.class, "orb_scatter")) {
                                shove(player, orb, config.num("speed"), config.num("lift"));
                            }
                        }));
    }

    private static final String DYE = MARK + "_dye";
    private static final String GLINT = MARK + "_glint";
    private static final String LORE = MARK + "_lore";
    private static final String LURE = MARK + "_lure";
    private static final String POTION = MARK + "_potion";
    private static final String ELSEWHERE = MARK + "_elsewhere";
    private static final int BLACK = 0xFF111111;
    private static final int NO_DYE = 0;

    /** Inventory index to the slot of the player's own menu. */
    private static int menuSlot(int inventoryIndex) {
        return inventoryIndex < Inventory.getSelectionSize()
                ? InventoryMenu.USE_ROW_SLOT_START + inventoryIndex
                : inventoryIndex;
    }

    private static List<Integer> slots(ServerPlayer player, Predicate<ItemStack> match) {
        List<Integer> found = new ArrayList<>();
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && match.test(stack)) {
                found.add(i);
            }
        }
        return found;
    }

    /** Like {@link #stacks}, but worn armor and the off hand too. */
    private static List<ItemStack> everything(ServerPlayer player, Predicate<ItemStack> match) {
        List<ItemStack> found = new ArrayList<>();
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && match.test(stack)) {
                found.add(stack);
            }
        }
        return found;
    }

    /** Shows {@code shown} in a slot of the player's menu only; the real item is shown again after {@code ticks}. */
    private static void lie(ServerPlayer player, int menuSlot, ItemStack shown, int ticks) {
        show(player, menuSlot, shown);
        Scheduler.later(
                player,
                ticks,
                p -> show(
                        p, menuSlot, p.inventoryMenu.getSlot(menuSlot).getItem().copy()));
    }

    private static void show(ServerPlayer player, int menuSlot, ItemStack shown) {
        InventoryMenu menu = player.inventoryMenu;
        player.connection.send(
                new ClientboundContainerSetSlotPacket(menu.containerId, menu.getStateId(), menuSlot, shown));
    }

    private static void wearDown(ItemStack tool, double fraction) {
        int loss = Math.max(1, (int) (tool.getMaxDamage() * fraction));
        tool.setDamageValue(Math.min(tool.getMaxDamage() - 1, tool.getDamageValue() + loss));
    }

    private static boolean high(ServerPlayer player, int blocks) {
        return player.getY()
                        - player.level()
                                .getHeight(Heightmap.Types.MOTION_BLOCKING, player.getBlockX(), player.getBlockZ())
                >= blocks;
    }

    private static boolean stew(ItemStack stack) {
        return stack.is(Items.MUSHROOM_STEW)
                || stack.is(Items.BEETROOT_SOUP)
                || stack.is(Items.RABBIT_STEW)
                || stack.is(Items.SUSPICIOUS_STEW);
    }

    private static boolean torches(ItemStack stack) {
        return (stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH)) && stack.getCount() > 1;
    }

    /** The bundle's contents with a bone added; null when the item is no bundle or the bone does not fit. */
    private static @Nullable BundleContents withBone(ItemStack stack) {
        BundleContents old = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (old == null || old.weight().isError()) {
            return null;
        }
        BundleContents.Mutable mutable = old.asMutable();
        if (mutable.tryInsert(new ItemStack(Items.BONE)) == 0) {
            return null;
        }
        BundleContents next = mutable.toImmutable();
        return next.size() >= old.size() ? next : null;
    }

    private static List<ItemEntity> lying(ServerPlayer player) {
        return around(player, ItemEntity.class, "item_throwback");
    }

    /** Entities of a kind between the event's {@code min_distance} and {@code radius} of the player. */
    private static <T extends Entity> List<T> around(ServerPlayer player, Class<T> type, String event) {
        Config.Section config = Config.event(event);
        double near = config.num("min_distance");
        return player.level()
                .getEntitiesOfClass(
                        type,
                        player.getBoundingBox().inflate(config.num("radius")),
                        entity -> entity.isAlive() && entity.distanceToSqr(player) >= near * near);
    }

    /** Pushes the entity away from the player, for the player's client as well. */
    private static void shove(ServerPlayer player, Entity entity, double speed, double lift) {
        Vec3 away = entity.position().subtract(player.position()).multiply(1, 0, 1);
        if (away.lengthSqr() < 1.0E-4) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2;
            away = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        }
        entity.setDeltaMovement(away.normalize().scale(speed).add(0, lift, 0));
        player.connection.send(new ClientboundSetEntityMotionPacket(entity));
    }

    /** The anchor of the nearest territorial spirit whose reach holds the player. */
    private static @Nullable BlockPos anchor(ServerPlayer player) {
        BlockPos nearest = null;
        double best = Double.MAX_VALUE;
        for (HauntData.Territorial spirit :
                HauntData.get(player.level().getServer()).territorialReaching(player)) {
            double distance = spirit.anchor().distSqr(player.blockPosition());
            if (distance < best) {
                nearest = spirit.anchor();
                best = distance;
            }
        }
        return nearest;
    }

    private static boolean lodestoneCompass(ItemStack stack) {
        LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
        return stack.is(Items.COMPASS) && tracker != null && tracker.target().isPresent() && !marked(stack, LURE);
    }

    private static boolean signedBy(ItemStack stack, ServerPlayer player) {
        WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        return content != null
                && content.author().equals(player.getGameProfile().name());
    }

    /** A potion with a plain base potion (no custom effects to lose) that is not mid-change. */
    private static boolean swappable(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION))
                && contents != null
                && contents.potion().isPresent()
                && contents.customEffects().isEmpty()
                && !marked(stack, POTION);
    }

    private static boolean arrow(ItemStack stack) {
        return stack.is(ItemTags.ARROWS);
    }

    /** One arrow at a time leaves the inventory; what was taken is handed back later (or left on the floor). */
    private static void takeArrows(ServerPlayer player) {
        Config.Section config = Config.event("vanishing_arrows");
        ServerLevel level = player.level();
        UUID id = player.getUUID();
        Vec3 spot = player.position();
        List<ItemStack> taken = new ArrayList<>();
        Scheduler.repeat(player, config.integer("interval"), config.integer("interval"), (p, n) -> {
            List<Integer> found = slots(p, ItemEvents::arrow);
            if (found.isEmpty()) {
                return false;
            }
            taken.add(p.getInventory()
                    .getItem(found.get(p.getRandom().nextInt(found.size())))
                    .split(1));
            return taken.size() < config.integer("count");
        });
        Restore.revert(level, config.integer("return_ticks"), force -> {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(id);
            if (owner == null && !force) {
                return false;
            }
            for (ItemStack arrow : taken) {
                if (owner == null) {
                    level.addFreshEntity(new ItemEntity(level, spot.x, spot.y, spot.z, arrow));
                } else if (!owner.getInventory().add(arrow)) {
                    owner.spawnAtLocation(owner.level(), arrow);
                }
            }
            return true;
        });
    }

    private static @Nullable InteractionHand loadedIn(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(stack)) {
                return hand;
            }
        }
        return null;
    }

    private static @Nullable ItemStack mapIn(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(Items.FILLED_MAP)
                    && !marked(stack, ELSEWHERE)
                    && MapItem.getSavedData(stack, player.level()) != null) {
                return stack;
            }
        }
        return null;
    }

    private static boolean horn(ItemStack stack) {
        return stack.is(Items.GOAT_HORN) && stack.has(DataComponents.INSTRUMENT);
    }

    private static boolean dyeable(ItemStack stack) {
        DyedItemColor color = stack.get(DataComponents.DYED_COLOR);
        return !marked(stack, DYE)
                && (color == null || color.rgb() != BLACK)
                && Config.event("armor_dye")
                        .texts("items")
                        .contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    private static boolean plain(ItemStack stack) {
        return !stack.hasFoil() && !stack.has(DataComponents.ENCHANTMENT_GLINT_OVERRIDE) && !marked(stack, GLINT);
    }

    private static boolean marked(ItemStack stack, String key) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .contains(key);
    }

    /**
     * Changes the item now and changes it back after {@code ticks}, wherever the player carries it then (an item given
     * away or dropped meanwhile stays changed). {@code change} returns a number to keep for {@code undo}. With
     * {@code stays} the change is for good under hardcore.
     */
    private static void temporarily(
            ServerPlayer player,
            ItemStack stack,
            String key,
            int ticks,
            boolean stays,
            ToIntFunction<ItemStack> change,
            BiConsumer<ItemStack, Integer> undo) {
        ServerLevel level = player.level();
        if (stays && Hardcore.on(level)) {
            change.applyAsInt(stack);
            return;
        }
        int was = change.applyAsInt(stack);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(key, was));
        UUID id = player.getUUID();
        Restore.after(level, ticks, force -> {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(id);
            if (owner == null) {
                return force;
            }
            List<ItemStack> carried = everything(owner, s -> marked(s, key));
            ItemStack cursor = owner.containerMenu.getCarried();
            if (marked(cursor, key)) {
                carried.add(cursor);
            }
            for (ItemStack held : carried) {
                undo.accept(
                        held,
                        held.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                                .copyTag()
                                .getIntOr(key, 0));
                CustomData.update(DataComponents.CUSTOM_DATA, held, tag -> tag.remove(key));
                if (held.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                        .isEmpty()) {
                    held.remove(DataComponents.CUSTOM_DATA);
                }
            }
            return true;
        });
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
        return Config.event("tool_look")
                        .texts("models")
                        .contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                && !disguised(stack);
    }

    private static boolean disguised(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .contains(LOOK);
    }

    private static void changeLook(ServerPlayer player) {
        Config.Section config = Config.event("tool_look");
        List<ItemStack> tools = stacks(player, ItemEvents::lookable);
        ItemStack tool = tools.get(player.getRandom().nextInt(tools.size()));
        Identifier own = BuiltInRegistries.ITEM.getKey(tool.getItem());
        List<String> others = config.texts("models").stream()
                .filter(id -> !id.equals(own.toString()))
                .toList();
        Identifier model = Identifier.parse(others.get(player.getRandom().nextInt(others.size())));
        if (Swaps.real(player.level())) {
            player.getInventory()
                    .setItem(
                            player.getInventory().findSlotMatchingItem(tool),
                            new ItemStack(BuiltInRegistries.ITEM.getValue(model), tool.getCount()));
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
                    stack.set(
                            DataComponents.ITEM_MODEL,
                            stack.getItem().components().get(DataComponents.ITEM_MODEL));
                    CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(LOOK));
                }
            }
            return true;
        });
    }

    private static List<ItemStack> unmarkedMaps(ServerPlayer player) {
        return stacks(
                player,
                s -> s.is(Items.FILLED_MAP)
                        && MapItem.getSavedData(s, player.level()) != null
                        && s
                                .getOrDefault(DataComponents.MAP_DECORATIONS, MapDecorations.EMPTY)
                                .decorations()
                                .keySet()
                                .stream()
                                .noneMatch(k -> k.startsWith(MARK)));
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
                data.centerX + (player.getRandom().nextInt(2 * spread + 1) - spread) * blocksPerPixel,
                0,
                data.centerZ + (player.getRandom().nextInt(2 * spread + 1) - spread) * blocksPerPixel);
        MapItemSavedData.addTargetDecoration(map, at, MARK, MapDecorationTypes.RED_X);
        Config.Tone tone = config.tone("sound");
        player.level()
                .playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT,
                        SoundSource.HOSTILE,
                        tone.volume(),
                        tone.pitch());
    }

    private static final EquipmentSlot[] ARMOR = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static List<EquipmentSlot> worn(ServerPlayer player) {
        return java.util.Arrays.stream(ARMOR)
                .filter(slot -> !player.getItemBySlot(slot).isEmpty())
                .toList();
    }

    private static void takeArmorOff(ServerPlayer player) {
        List<EquipmentSlot> slots = new ArrayList<>(worn(player));
        Collections.shuffle(slots, new Random(player.getRandom().nextLong()));
        Config.Section config = Config.event("armor_off");
        int span = config.integer("max_pieces") - config.integer("min_pieces") + 1;
        int count = Math.min(
                slots.size(), config.integer("min_pieces") + player.getRandom().nextInt(span));
        for (EquipmentSlot slot : slots.subList(0, count)) {
            ItemStack piece = player.getItemBySlot(slot);
            player.setItemSlot(slot, ItemStack.EMPTY);
            if (!player.getInventory().add(piece)) {
                player.spawnAtLocation(player.level(), piece);
            }
        }
        Config.Tone tone = config.tone("sound");
        player.level()
                .playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        SoundEvents.ARMOR_EQUIP_GENERIC.value(),
                        SoundSource.HOSTILE,
                        tone.volume(),
                        tone.pitch());
    }

    private static boolean isBook(ItemStack stack) {
        return stack.is(Items.WRITABLE_BOOK)
                || stack.is(Items.WRITTEN_BOOK) && stack.has(DataComponents.WRITTEN_BOOK_CONTENT);
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
            Container container =
                    ChestBlock.getContainer((ChestBlock) state.getBlock(), state, player.level(), pos, true);
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
