package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Restore;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

/** Furniture and redstone parts of a house acting on their own. */
final class Household {
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
            SimpleEvent.light("occupied_bed", EventTag.DOORS, player -> Nearby.any(player, "occupied_bed", Household::freeBedHead), Household::occupy)
        );
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
        Restore.swap(level, pos, to, Config.event("redstone_drift").integer("restore_ticks"));
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
}
