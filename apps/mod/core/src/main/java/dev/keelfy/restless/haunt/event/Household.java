package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.Footprints;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.Hardcore;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Furniture and redstone parts of a house acting on their own. */
final class Household {
    /** A lone book can land in its own slot again; a few shuffles make that vanishingly rare. */
    private static final int SHUFFLE_TRIES = 8;

    private static final int OUTPUT_SLOT = 2;

    /** No drops, no neighbour or shape updates: a moved chest keeps its items and a bed half does not break off. */
    private static final int CARRY = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;

    private Household() {}

    static List<HauntEvent> all() {
        return List.of(
            // 174
            SimpleEvent.light("lectern_pages", EventTag.DOORS, player -> !turnable(player).isEmpty(), Household::turnPage),
            // 175: the block events are the real animations
            SimpleEvent.light("pot_wobble", EventTag.DOORS, player -> Nearby.any(player, "pot_wobble", s -> s.is(Blocks.DECORATED_POT)), Household::wobble),
            SimpleEvent.light("bell_swing", EventTag.DOORS, player -> Nearby.any(player, "bell_swing", s -> s.getBlock() instanceof BellBlock), player -> {
                BlockPos pos = Nearby.random(player, "bell_swing", s -> s.getBlock() instanceof BellBlock);
                if (pos != null) {
                    ((BellBlock) player.level().getBlockState(pos).getBlock()).attemptToRing(player.level(), pos, Direction.Plane.HORIZONTAL.getRandomDirection(player.getRandom()));
                }
            }),
            // 178
            SimpleEvent.harmful("redstone_drift", EventTag.DOORS, player -> Nearby.any(player, "redstone_drift", Household::driftable), Household::drift),
            // 169: vanilla shows its own "This bed is occupied"
            SimpleEvent.light("occupied_bed", EventTag.DOORS, player -> Nearby.any(player, "occupied_bed", Household::freeBedHead), Household::occupy),
            // 176
            SimpleEvent.light("shuffled_books", EventTag.DOORS, player -> Nearby.any(player, "shuffled_books", Household::shelved), Household::shuffleBooks),
            // 177: a click first, then the arrow
            SimpleEvent.harmful("dispenser_shot", EventTag.DOORS, player -> dispenser(player) != null, Household::shoot),
            // 117: a hiss from the chest the player has open, then a blast that breaks no blocks
            SimpleEvent.harmful("chest_blast", EventTag.DOORS,
                player -> player.getHealth() > Config.event("chest_blast").num("min_health") && opened(player) != null,
                Household::blast),
            // 182: what came out of the furnace is not what went in
            SimpleEvent.light("wrong_smelt", EventTag.DOORS, player -> !smelted(player).isEmpty(), Household::swapOutput),
            // 186: something left on the outer side of a door
            SimpleEvent.light("doorstep_gift", EventTag.DOORS, player -> doorstep(player) != null, Household::leaveGift),
            // 103: moved back only if both spots are as it left them
            SimpleEvent.harmful("furniture_moves", EventTag.DOORS, player -> Nearby.any(player, "furniture_moves", Household::furniture), Household::moveFurniture),
            // 184: prints from where the player stands to a bed or a chest, one pair at a time
            SimpleEvent.light("muddy_footprints", EventTag.DOORS, player -> !trail(player).isEmpty(), Household::walkMud)
        );
    }

    /** A chest this player has open right now. */
    private static @Nullable BlockPos opened(ServerPlayer player) {
        return Nearby.all(player, "chest_blast", s -> s.getBlock() instanceof ChestBlock).stream()
            .filter(pos -> player.level().getBlockEntity(pos) instanceof ChestBlockEntity chest && player.containerMenu instanceof ChestMenu menu
                && (menu.getContainer() == chest || menu.getContainer() instanceof CompoundContainer both && both.contains(chest)))
            .findFirst().orElse(null);
    }

    private static void blast(ServerPlayer player) {
        BlockPos pos = opened(player);
        if (pos == null) {
            return;
        }
        ServerLevel level = player.level();
        Config.Section config = Config.event("chest_blast");
        Config.Tone tone = config.tone("sound");
        level.playSound(null, pos, SoundEvents.CREEPER_PRIMED, SoundSource.BLOCKS, tone.volume(), tone.pitch());
        Scheduler.later(player, config.integer("delay_ticks"), p -> {
            if (level.getBlockState(pos).getBlock() instanceof ChestBlock) {
                level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, (float) config.num("power"), Level.ExplosionInteraction.NONE);
            }
        });
    }

    private static List<BlockPos> smelted(ServerPlayer player) {
        return Nearby.all(player, "wrong_smelt", s -> s.getBlock() instanceof AbstractFurnaceBlock).stream()
            .filter(pos -> player.level().getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity furnace && !furnace.getItem(OUTPUT_SLOT).isEmpty())
            .toList();
    }

    /** The output becomes charcoal or rotten flesh; if nobody took it, the real output comes back (under hardcore it stays). */
    private static void swapOutput(ServerPlayer player) {
        List<BlockPos> furnaces = smelted(player);
        if (furnaces.isEmpty()) {
            return;
        }
        ServerLevel level = player.level();
        BlockPos pos = furnaces.get(player.getRandom().nextInt(furnaces.size()));
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) level.getBlockEntity(pos);
        ItemStack before = furnace.getItem(OUTPUT_SLOT).copy();
        ItemStack after = new ItemStack(player.getRandom().nextBoolean() ? Items.CHARCOAL : Items.ROTTEN_FLESH, before.getCount());
        furnace.setItem(OUTPUT_SLOT, after);
        Restore.revert(level, Config.event("wrong_smelt").integer("restore_ticks"), force -> {
            if (!force && !level.isLoaded(pos)) {
                return false;
            }
            if (level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity now && ItemStack.matches(now.getItem(OUTPUT_SLOT), after)) {
                now.setItem(OUTPUT_SLOT, before);
            }
            return true;
        });
    }

    /** The free spot just outside a door nearby, on the side away from the player. */
    private static @Nullable BlockPos doorstep(ServerPlayer player) {
        ServerLevel level = player.level();
        List<BlockPos> steps = new ArrayList<>();
        for (BlockPos pos : Nearby.all(player, "doorstep_gift", s -> s.getBlock() instanceof DoorBlock && s.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER)) {
            Direction facing = level.getBlockState(pos).getValue(DoorBlock.FACING);
            BlockPos far = pos.relative(facing);
            BlockPos near = pos.relative(facing.getOpposite());
            BlockPos step = far.distToCenterSqr(player.position()) > near.distToCenterSqr(player.position()) ? far : near;
            if (Place.solid(level, step.below()) && !Place.solid(level, step)) {
                steps.add(step);
            }
        }
        return steps.isEmpty() ? null : steps.get(player.getRandom().nextInt(steps.size()));
    }

    private static void leaveGift(ServerPlayer player) {
        BlockPos step = doorstep(player);
        if (step == null) {
            return;
        }
        List<String> gifts = Config.event("doorstep_gift").texts("items");
        Item gift = BuiltInRegistries.ITEM.getValue(Identifier.parse(gifts.get(player.getRandom().nextInt(gifts.size()))));
        player.level().addFreshEntity(new ItemEntity(player.level(), step.getX() + 0.5, step.getY() + 0.2, step.getZ() + 0.5, new ItemStack(gift), 0, 0, 0));
    }

    /** One pair of prints: where it lies and the way it points. */
    private record Print(BlockPos pos, Direction facing) {}

    private static boolean mudTarget(BlockState state) {
        return state.getBlock() instanceof BedBlock || state.getBlock() instanceof ChestBlock;
    }

    /** Indoors only (a roof overhead, read off the heightmap since sky light lags): the floor path from the player up to a bed or chest on their level, or nothing if none is reachable. */
    private static List<Print> trail(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos from = player.blockPosition();
        if (level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, from.getX(), from.getZ()) <= from.getY() + 2) {
            return List.of();
        }
        Config.Section config = Config.event("muddy_footprints");
        List<BlockPos> targets = new ArrayList<>(Nearby.all(player, "muddy_footprints", Household::mudTarget).stream().filter(pos -> pos.getY() == from.getY()).toList());
        Collections.shuffle(targets, new java.util.Random(player.getRandom().nextLong()));
        for (BlockPos target : targets) {
            List<Print> path = walk(level, from, target, config.integer("max_steps"));
            if (path.size() >= config.integer("min_prints")) {
                return path;
            }
        }
        return List.of();
    }

    /** Each step goes to the free floor beside it that is closest to the target; the walk ends next to it, or fails at a wall. */
    private static List<Print> walk(ServerLevel level, BlockPos from, BlockPos target, int maxSteps) {
        List<Print> path = new ArrayList<>();
        BlockPos at = from;
        for (int i = 0; i < maxSteps && steps(at, target) > 1; i++) {
            Print next = null;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos to = at.relative(d);
                if (steps(to, target) < steps(at, target) && level.isEmptyBlock(to) && Place.solid(level, to.below())
                    && (next == null || steps(to, target) < steps(next.pos(), target))) {
                    next = new Print(to, d);
                }
            }
            if (next == null) {
                return List.of();
            }
            path.add(next);
            at = next.pos();
        }
        return steps(at, target) <= 1 ? path : List.of();
    }

    private static int steps(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getZ() - b.getZ());
    }

    /** Every first or second step of the path gets a pair of prints with a squelch; each is wiped later unless hardcore. */
    private static void walkMud(ServerPlayer player) {
        List<Print> path = trail(player);
        ServerLevel level = player.level();
        Config.Section config = Config.event("muddy_footprints");
        Config.Tone tone = config.tone("sound");
        int delay = 0;
        for (int i = 0; i < path.size(); i += 1 + player.getRandom().nextInt(2)) {
            Print print = path.get(i);
            BlockState state = Content.FOOTPRINTS.get().defaultBlockState().setValue(Footprints.FACING, print.facing());
            Scheduler.later(player, delay, p -> {
                if (!level.isEmptyBlock(print.pos())) {
                    return;
                }
                level.setBlock(print.pos(), state, Block.UPDATE_ALL);
                level.playSound(null, print.pos(), SoundEvents.MUD_STEP, SoundSource.BLOCKS, tone.volume(), tone.pitch());
                Restore.revert(level, config.integer("restore_ticks"), force -> {
                    if (!force && !level.isLoaded(print.pos())) {
                        return false;
                    }
                    if (level.getBlockState(print.pos()).is(Content.FOOTPRINTS.get())) {
                        level.removeBlock(print.pos(), false);
                    }
                    return true;
                });
            });
            delay += config.integer("step_ticks");
        }
    }

    private static List<BlockPos> turnable(ServerPlayer player) {
        return Nearby.all(player, "lectern_pages", s -> s.is(Blocks.LECTERN) && s.getValue(LecternBlock.HAS_BOOK)).stream()
            .filter(pos -> player.level().getBlockEntity(pos) instanceof LecternBlockEntity lectern && pages(lectern) > 1)
            .toList();
    }

    private static int pages(LecternBlockEntity lectern) {
        ItemStack book = lectern.getBook();
        if (book.has(DataComponents.WRITTEN_BOOK_CONTENT)) {
            return book.get(DataComponents.WRITTEN_BOOK_CONTENT).pages().size();
        }
        return book.has(DataComponents.WRITABLE_BOOK_CONTENT) ? book.get(DataComponents.WRITABLE_BOOK_CONTENT).pages().size() : 0;
    }

    private static void turnPage(ServerPlayer player) {
        List<BlockPos> lecterns = turnable(player);
        if (lecterns.isEmpty()) {
            return;
        }
        ServerLevel level = player.level();
        BlockPos pos = lecterns.get(player.getRandom().nextInt(lecterns.size()));
        LecternBlockEntity lectern = (LecternBlockEntity) level.getBlockEntity(pos);
        int pages = pages(lectern);
        // Any other page than the one it is on, through the menu a reader would use, which also signals the page change.
        int page = (lectern.getPage() + 1 + player.getRandom().nextInt(pages - 1)) % pages;
        ((LecternMenu) lectern.createMenu(0, player.getInventory(), player)).setData(LecternBlockEntity.DATA_PAGE, page);
        Config.Tone tone = Config.event("lectern_pages").tone("sound");
        level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, tone.volume(), tone.pitch());
    }

    private static void wobble(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, "pot_wobble", s -> s.is(Blocks.DECORATED_POT));
        if (pos != null && player.level().getBlockEntity(pos) instanceof DecoratedPotBlockEntity pot) {
            pot.wobble(DecoratedPotBlockEntity.WobbleStyle.NEGATIVE);
            Config.Tone tone = Config.event("pot_wobble").tone("sound");
            player.level().playSound(null, pos, SoundEvents.DECORATED_POT_INSERT_FAIL, SoundSource.BLOCKS, tone.volume(), tone.pitch());
        }
    }

    private static boolean driftable(BlockState state) {
        return state.getBlock() instanceof RepeaterBlock || state.getBlock() instanceof ComparatorBlock;
    }

    /** A repeater gets another delay, a comparator the other mode; set back after a while unless it stays under hardcore. */
    private static void drift(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, "redstone_drift", Household::driftable);
        if (pos == null) {
            return;
        }
        ServerLevel level = player.level();
        BlockState state = level.getBlockState(pos);
        BlockState to = state.getBlock() instanceof RepeaterBlock
            ? state.setValue(RepeaterBlock.DELAY, 1 + (state.getValue(RepeaterBlock.DELAY) + player.getRandom().nextInt(3)) % 4)
            : state.cycle(ComparatorBlock.MODE);
        Swaps.show(player, pos, to, Config.event("redstone_drift").integer("restore_ticks"));
    }

    private static boolean freeBedHead(BlockState state) {
        return state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.HEAD && !state.getValue(BedBlock.OCCUPIED);
    }

    /** Both halves read as taken until the time is up, whether or not the player is hardcore: nobody can lie in it meanwhile. */
    private static void occupy(ServerPlayer player) {
        BlockPos head = Nearby.random(player, "occupied_bed", Household::freeBedHead);
        if (head == null) {
            return;
        }
        ServerLevel level = player.level();
        BlockPos foot = head.relative(level.getBlockState(head).getValue(BedBlock.FACING).getOpposite());
        for (BlockPos half : List.of(head, foot)) {
            BlockState state = level.getBlockState(half);
            if (state.getBlock() instanceof BedBlock) {
                Restore.quiet(level, half, state.setValue(BedBlock.OCCUPIED, true));
            }
        }
        Restore.after(level, Config.event("occupied_bed").integer("restore_ticks"), force -> {
            if (!force && !level.isLoaded(head)) {
                return false;
            }
            for (BlockPos half : List.of(head, foot)) {
                BlockState state = level.getBlockState(half);
                if (state.getBlock() instanceof BedBlock && state.getValue(BedBlock.OCCUPIED)) {
                    Restore.quiet(level, half, state.setValue(BedBlock.OCCUPIED, false));
                }
            }
            return true;
        });
    }

    private static boolean shelved(BlockState state) {
        return state.is(Blocks.CHISELED_BOOKSHELF) && ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.stream().anyMatch(state::getValue);
    }

    /** The books take other slots; put back after a while unless somebody touched the shelf, and under hardcore they stay. */
    private static void shuffleBooks(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, "shuffled_books", Household::shelved);
        if (pos == null || !(player.level().getBlockEntity(pos) instanceof ChiseledBookShelfBlockEntity shelf)) {
            return;
        }
        ServerLevel level = player.level();
        List<ItemStack> before = copy(shelf);
        List<ItemStack> after = copy(shelf);
        for (int tries = 0; tries < SHUFFLE_TRIES && same(before, after); tries++) {
            Collections.shuffle(after, new java.util.Random(player.getRandom().nextLong()));
        }
        if (same(before, after)) {
            return;
        }
        place(shelf, after);
        Config.Tone tone = Config.event("shuffled_books").tone("sound");
        level.playSound(null, pos, SoundEvents.CHISELED_BOOKSHELF_PICKUP, SoundSource.BLOCKS, tone.volume(), tone.pitch());
        Restore.revert(level, Config.event("shuffled_books").integer("restore_ticks"), force -> {
            if (!force && !level.isLoaded(pos)) {
                return false;
            }
            if (level.getBlockEntity(pos) instanceof ChiseledBookShelfBlockEntity now && same(copy(now), after)) {
                place(now, before);
            }
            return true;
        });
    }

    private static List<ItemStack> copy(ChiseledBookShelfBlockEntity shelf) {
        List<ItemStack> books = new ArrayList<>();
        for (ItemStack book : shelf.getItems()) {
            books.add(book.copy());
        }
        return books;
    }

    private static boolean same(List<ItemStack> a, List<ItemStack> b) {
        for (int i = 0; i < a.size(); i++) {
            if (!ItemStack.matches(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static void place(ChiseledBookShelfBlockEntity shelf, List<ItemStack> books) {
        for (int i = 0; i < books.size(); i++) {
            shelf.setItem(i, books.get(i).copy());
        }
    }

    /** Any dispenser nearby; under hardcore only one loaded with an arrow, which it really spends. */
    private static @Nullable BlockPos dispenser(ServerPlayer player) {
        boolean real = Hardcore.on(player.level());
        List<BlockPos> hits = Nearby.all(player, "dispenser_shot", s -> s.is(Blocks.DISPENSER)).stream()
            .filter(pos -> !real || arrowSlot(player.level(), pos) >= 0)
            .toList();
        return hits.isEmpty() ? null : hits.get(player.getRandom().nextInt(hits.size()));
    }

    private static int arrowSlot(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof DispenserBlockEntity dispenser) {
            for (int slot = 0; slot < dispenser.getContainerSize(); slot++) {
                ItemStack stack = dispenser.getItem(slot);
                if (stack.is(Items.ARROW) || stack.is(Items.TIPPED_ARROW)) {
                    return slot;
                }
            }
        }
        return -1;
    }

    /**
     * The dispenser clicks as if empty, then an arrow leaves its face toward the player; nothing is taken from it. Under
     * hardcore it fires one of its own arrows, which can be picked up.
     */
    private static void shoot(ServerPlayer player) {
        BlockPos pos = dispenser(player);
        if (pos == null) {
            return;
        }
        ServerLevel level = player.level();
        level.levelEvent(LevelEvent.SOUND_DISPENSER_FAIL, pos, 0);
        Config.Section config = Config.event("dispenser_shot");
        Scheduler.later(player, config.integer("delay_ticks"), p -> {
            BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.DISPENSER)) {
                return;
            }
            Direction face = state.getValue(DispenserBlock.FACING);
            Vec3 from = Vec3.atCenterOf(pos).add(face.getStepX() * 0.7, face.getStepY() * 0.7, face.getStepZ() * 0.7);
            Vec3 to = p.getBoundingBox().getCenter().subtract(from);
            boolean real = Hardcore.on(level);
            int slot = real ? arrowSlot(level, pos) : -1;
            if (real && slot < 0) {
                return;
            }
            ItemStack ammo = real && level.getBlockEntity(pos) instanceof DispenserBlockEntity dispenser
                ? dispenser.removeItem(slot, 1) : new ItemStack(Items.ARROW);
            Arrow arrow = Projectile.spawnProjectileUsingShoot(new Arrow(level, from.x, from.y, from.z, ammo, null), level, ammo,
                to.x, to.y, to.z, (float) config.num("speed"), (float) config.num("spread"));
            arrow.pickup = real ? AbstractArrow.Pickup.ALLOWED : AbstractArrow.Pickup.DISALLOWED;
            level.levelEvent(LevelEvent.SOUND_DISPENSER_PROJECTILE_LAUNCH, pos, 0);
            level.levelEvent(LevelEvent.PARTICLES_SHOOT_SMOKE, pos, face.get3DDataValue());
        });
    }

    /** A lone chest, a crafting table, any furnace, or the foot of a bed nobody lies in. */
    private static boolean furniture(BlockState state) {
        return state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) == ChestType.SINGLE
            || state.is(Blocks.CRAFTING_TABLE)
            || state.getBlock() instanceof AbstractFurnaceBlock
            || state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.FOOT && !state.getValue(BedBlock.OCCUPIED);
    }

    /** What it fills from {@code pos}: the block, and for a bed's foot the head too. */
    private static List<BlockPos> cells(BlockState state, BlockPos pos) {
        return state.getBlock() instanceof BedBlock ? List.of(pos, pos.relative(state.getValue(BedBlock.FACING))) : List.of(pos);
    }

    /** It stands somewhere else nearby; moved back after a while if nothing took either spot, and under hardcore it stays. */
    private static void moveFurniture(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, "furniture_moves", Household::furniture);
        if (pos == null) {
            return;
        }
        ServerLevel level = player.level();
        Config.Section config = Config.event("furniture_moves");
        BlockState state = level.getBlockState(pos);
        BlockPos spot = Place.near(player, pos, config.integer("min_distance"), config.integer("max_distance"),
            at -> cells(state, at).stream().allMatch(cell -> Place.floored(level, cell) && !player.getBoundingBox().intersects(new AABB(cell))));
        if (spot == null) {
            return;
        }
        List<BlockPos> from = cells(state, pos);
        List<BlockPos> to = cells(state, spot);
        carry(level, from, to);
        Restore.revert(level, config.integer("restore_ticks"), force -> {
            if (!force && !(level.isLoaded(pos) && level.isLoaded(spot))) {
                return false;
            }
            if (to.stream().allMatch(cell -> level.getBlockState(cell).is(state.getBlock())) && from.stream().allMatch(level::isEmptyBlock)) {
                carry(level, to, from);
            }
            return true;
        });
    }

    /** Each cell's state goes to the matching cell, its block entity through the entity's own save format. */
    private static void carry(ServerLevel level, List<BlockPos> from, List<BlockPos> to) {
        List<BlockState> states = new ArrayList<>();
        List<@Nullable CompoundTag> data = new ArrayList<>();
        for (BlockPos cell : from) {
            states.add(level.getBlockState(cell));
            BlockEntity entity = level.getBlockEntity(cell);
            data.add(entity == null ? null : entity.saveWithoutMetadata(level.registryAccess()));
        }
        for (BlockPos cell : from) {
            level.setBlock(cell, Blocks.AIR.defaultBlockState(), CARRY);
        }
        for (int i = 0; i < to.size(); i++) {
            level.setBlock(to.get(i), states.get(i), CARRY);
            CompoundTag tag = data.get(i);
            BlockEntity entity = level.getBlockEntity(to.get(i));
            if (tag != null && entity != null) {
                entity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
                entity.setChanged();
            }
        }
    }
}
