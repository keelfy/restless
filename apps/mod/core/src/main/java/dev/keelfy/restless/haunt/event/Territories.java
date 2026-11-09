package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.Session;
import dev.keelfy.restless.haunt.Ticks;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** What the territorial spirits do to a place: the house is kept, the hoard is gathered, the ground is made hell. */
final class Territories {
    private Territories() {}

    /** What the hoarder has taken from each player and not yet put away. In memory only. */
    private static final Map<UUID, List<ItemStack>> HOARD = Session.SERVER.keep(new HashMap<>());

    static List<HauntEvent> all() {
        return List.of(
                // nix: an item lost in the water is carried back toward the player
                SimpleEvent.light(
                        "nix_returns", EventTag.WATER, player -> drifting(player) != null, Territories::carry),
                // domovoi: a chest sorted, open doors shut
                SimpleEvent.light(
                        "domovoi_tidy",
                        EventTag.DOORS,
                        player -> messy(player) != null || !open(player).isEmpty(),
                        Territories::tidy),
                // hoarder: gold and gems move to another chest
                SimpleEvent.harmful(
                        "hoarder_shiny", EventTag.ITEMS, player -> shiny(player) != null, Territories::gather),
                // hoarder: a barrel somewhere out of sight holds what it took
                SimpleEvent.light("hoarder_nest", EventTag.ITEMS, player -> nest(player) != null, Territories::build),
                // demon: the ground under the player's feet is hell for a while
                SimpleEvent.light("demon_trail", EventTag.BLOCKS, player -> player.onGround(), Territories::scorch),
                // demon: a contract to hold, and a price
                SimpleEvent.harmful(
                        "demon_contract",
                        EventTag.AFFLICTION,
                        player -> player.getInventory().getFreeSlot() != -1 && !signed(player),
                        Territories::contract));
    }

    // ---- nix

    private static @Nullable ItemEntity drifting(ServerPlayer player) {
        List<ItemEntity> items = player.level()
                .getEntitiesOfClass(
                        ItemEntity.class,
                        player.getBoundingBox()
                                .inflate(Config.event("nix_returns").num("radius")),
                        item -> item.isInWater() && !item.getItem().isEmpty());
        return items.isEmpty() ? null : items.get(player.getRandom().nextInt(items.size()));
    }

    private static void carry(ServerPlayer player) {
        ItemEntity item = drifting(player);
        if (item == null) {
            return;
        }
        Config.Section config = Config.event("nix_returns");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            if (item.isRemoved() || !item.isInWater()) {
                return false;
            }
            Vec3 toward = p.position().subtract(item.position()).multiply(1, 0, 1);
            toward =
                    toward.lengthSqr() < 1.0E-4 ? Vec3.ZERO : toward.normalize().scale(config.num("speed"));
            item.setDeltaMovement(toward.x, config.num("lift"), toward.z);
            ((ServerLevel) item.level())
                    .sendParticles(
                            ParticleTypes.DRIPPING_WATER,
                            item.getX(),
                            item.getY() + 0.3,
                            item.getZ(),
                            3,
                            0.1,
                            0.1,
                            0.1,
                            0.0);
            return n < config.integer("max_steps");
        });
    }

    // ---- domovoi

    /** What a tidy chest holds: like stacks merged, ordered by item, with no gaps. */
    private static List<ItemStack> sorted(Container chest) {
        List<ItemStack> merged = new ArrayList<>();
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack rest = chest.getItem(i).copy();
            for (ItemStack into : merged) {
                if (!rest.isEmpty() && ItemStack.isSameItemSameComponents(into, rest)) {
                    int room = into.getMaxStackSize() - into.getCount();
                    int moved = Math.min(room, rest.getCount());
                    into.grow(moved);
                    rest.shrink(moved);
                }
            }
            if (!rest.isEmpty()) {
                merged.add(rest);
            }
        }
        merged.sort(Comparator.comparing(
                stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
        return merged;
    }

    private static boolean tidy(Container chest) {
        List<ItemStack> sorted = sorted(chest);
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack want = i < sorted.size() ? sorted.get(i) : ItemStack.EMPTY;
            if (!ItemStack.matches(chest.getItem(i), want)) {
                return false;
            }
        }
        return true;
    }

    private static @Nullable ChestBlockEntity messy(ServerPlayer player) {
        List<ChestBlockEntity> chests = Nearby.all(player, "domovoi_tidy", s -> s.is(Blocks.CHEST)).stream()
                .map(pos -> player.level().getBlockEntity(pos))
                .filter(ChestBlockEntity.class::isInstance)
                .map(ChestBlockEntity.class::cast)
                .filter(chest -> !chest.isEmpty() && !tidy(chest))
                .toList();
        return chests.isEmpty() ? null : chests.get(player.getRandom().nextInt(chests.size()));
    }

    private static List<BlockPos> open(ServerPlayer player) {
        ServerLevel level = player.level();
        return Nearby.all(player, "domovoi_tidy", state -> Pranks.slams(state)).stream()
                .filter(pos -> Pranks.isOpen(level.getBlockState(pos)))
                .toList();
    }

    private static void tidy(ServerPlayer player) {
        ServerLevel level = player.level();
        ChestBlockEntity chest = messy(player);
        if (chest != null) {
            List<ItemStack> sorted = sorted(chest);
            for (int i = 0; i < chest.getContainerSize(); i++) {
                chest.setItem(i, i < sorted.size() ? sorted.get(i) : ItemStack.EMPTY);
            }
            chest.setChanged();
            level.playSound(null, chest.getBlockPos(), SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.6F, 1.0F);
        }
        open(player).forEach(pos -> Pranks.shut(level, pos, false));
    }

    // ---- hoarder

    private static boolean shiny(ItemStack stack) {
        return Config.event("hoarder_shiny")
                .texts("items")
                .contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    /** A chest with something shiny in it and another chest, not touching, with a free slot. */
    private record Haul(Container from, List<Integer> slots, Container to) {}

    private static @Nullable Haul shiny(ServerPlayer player) {
        ServerLevel level = player.level();
        List<BlockPos> chests = Nearby.all(player, "hoarder_shiny", s -> s.is(Blocks.CHEST));
        List<Haul> hauls = new ArrayList<>();
        for (BlockPos a : chests) {
            if (!(level.getBlockEntity(a) instanceof ChestBlockEntity from)) {
                continue;
            }
            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i < from.getContainerSize(); i++) {
                if (!from.getItem(i).isEmpty() && shiny(from.getItem(i))) {
                    slots.add(i);
                }
            }
            if (slots.isEmpty()) {
                continue;
            }
            for (BlockPos b : chests) {
                if (b.distManhattan(a) > 1 && level.getBlockEntity(b) instanceof ChestBlockEntity to && free(to) >= 1) {
                    hauls.add(new Haul(from, slots, to));
                }
            }
        }
        return hauls.isEmpty() ? null : hauls.get(player.getRandom().nextInt(hauls.size()));
    }

    private static int free(Container container) {
        int free = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            free += container.getItem(i).isEmpty() ? 1 : 0;
        }
        return free;
    }

    private static void gather(ServerPlayer player) {
        Haul haul = shiny(player);
        if (haul == null) {
            return;
        }
        for (int slot : haul.slots()) {
            for (int i = 0; i < haul.to().getContainerSize(); i++) {
                if (haul.to().getItem(i).isEmpty()) {
                    haul.to().setItem(i, haul.from().removeItemNoUpdate(slot));
                    break;
                }
            }
        }
        haul.from().setChanged();
        haul.to().setChanged();
    }

    /** Remembers a stack the hoarder took, for its nest. */
    static void hoard(ServerPlayer player, ItemStack stack) {
        HOARD.computeIfAbsent(player.getUUID(), id -> new ArrayList<>()).add(stack);
    }

    private static @Nullable BlockPos nest(ServerPlayer player) {
        Config.Section config = Config.event("hoarder_nest");
        List<ItemStack> taken = HOARD.get(player.getUUID());
        if (taken == null || taken.size() < config.integer("min_stacks")) {
            return null;
        }
        ServerLevel level = player.level();
        for (int i = 0; i < config.integer("tries"); i++) {
            BlockPos spot = Place.spawnSpot(player, config.integer("min_distance"), config.integer("radius"));
            if (spot != null && level.getBlockState(spot.below()).isSolidRender() && !seen(player, spot)) {
                return spot;
            }
        }
        return null;
    }

    private static boolean seen(ServerPlayer player, BlockPos pos) {
        var hit = player.level()
                .clip(new ClipContext(
                        player.getEyePosition(),
                        Vec3.atCenterOf(pos),
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        player));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }

    private static void build(ServerPlayer player) {
        BlockPos at = nest(player);
        List<ItemStack> taken = HOARD.get(player.getUUID());
        if (at == null || taken == null) {
            return;
        }
        ServerLevel level = player.level();
        level.setBlockAndUpdate(at, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
        if (level.getBlockEntity(at) instanceof BarrelBlockEntity barrel) {
            int slot = 0;
            while (!taken.isEmpty() && slot < barrel.getContainerSize()) {
                barrel.setItem(slot++, taken.removeFirst());
            }
            barrel.setChanged();
        }
        taken.forEach(rest -> drop(level, at, rest));
        taken.clear();
    }

    private static void drop(ServerLevel level, BlockPos at, ItemStack stack) {
        level.addFreshEntity(new ItemEntity(level, at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, stack));
    }

    // ---- demon

    private static void scorch(ServerPlayer player) {
        Config.Section config = Config.event("demon_trail");
        int interval = config.integer("interval_ticks");
        java.util.Set<BlockPos> shown = new java.util.HashSet<>();
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            BlockPos under = p.blockPosition().below();
            ServerLevel level = p.level();
            BlockState state = level.getBlockState(under);
            if (p.onGround() && Place.solid(level, under) && shown.add(under)) {
                Swaps.show(p, under, Blocks.NETHERRACK.defaultBlockState(), config.integer("stage_ticks"));
                Scheduler.later(p, config.integer("stage_ticks"), q -> {
                    if (q.level() == level) {
                        Swaps.show(q, under, Blocks.SOUL_SOIL.defaultBlockState(), config.integer("stage_ticks"));
                    }
                });
            }
            return n * interval < config.integer("seconds") * 20;
        });
    }

    private static final Identifier PRICE = Identifier.parse("restless:demon_contract");

    private static boolean signed(ServerPlayer player) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        return health != null && health.hasModifier(PRICE);
    }

    private static void contract(ServerPlayer player) {
        Config.Section config = Config.event("demon_contract");
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null || signed(player) || player.getInventory().getFreeSlot() == -1) {
            return;
        }
        double price = config.num("price");
        if (health.getValue() - price < config.num("min_health")) {
            return;
        }
        health.addTransientModifier(new AttributeModifier(PRICE, -price, AttributeModifier.Operation.ADD_VALUE));
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(
                DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(
                        Filterable.passThrough(config.text("title")),
                        config.text("author"),
                        0,
                        List.of(Filterable.passThrough(Component.literal(config.text("page")
                                .formatted(player.getGameProfile().name())))),
                        true));
        player.getInventory().add(book);
        long untilDawn = Ticks.DAY - player.level().getOverworldClockTime() % Ticks.DAY;
        Scheduler.later(player, (int) untilDawn, p -> {
            AttributeInstance now = p.getAttribute(Attributes.MAX_HEALTH);
            if (now != null) {
                now.removeModifier(PRICE);
            }
        });
        player.level()
                .playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.6F);
    }
}
