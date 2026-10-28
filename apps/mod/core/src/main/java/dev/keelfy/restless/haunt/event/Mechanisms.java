package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;

/** Doors, gates, buttons and the like acting on their own. */
final class Mechanisms {
    private static final int RADIUS = 12;

    private Mechanisms() {}

    static List<HauntEvent> all() {
        return List.of(
            // 13
            on("trapdoor", s -> s.getBlock() instanceof TrapDoorBlock && s.is(BlockTags.WOODEN_TRAPDOORS), Mechanisms::slamTrapdoor),
            // 14
            on("fence_gate", s -> s.getBlock() instanceof FenceGateBlock, Mechanisms::toggleGate),
            // 15
            on("button", s -> s.getBlock() instanceof ButtonBlock && !s.getValue(BlockStateProperties.POWERED),
                (level, pos, s) -> ((ButtonBlock) s.getBlock()).press(s, level, pos, null)),
            // 16
            on("lever", s -> s.getBlock() instanceof LeverBlock, (level, pos, s) -> ((LeverBlock) s.getBlock()).pull(s, level, pos, null)),
            // 17
            on("pressure_plate", s -> s.getBlock() instanceof PressurePlateBlock && !s.getValue(BlockStateProperties.POWERED), Mechanisms::pressPlate),
            // 18
            on("iron_door", s -> s.getBlock() instanceof DoorBlock && !DoorBlock.isWoodenDoor(s),
                (level, pos, s) -> {
                    DoorBlock door = (DoorBlock) s.getBlock();
                    door.setOpen(null, level, s, pos, !door.isOpen(s));
                }),
            // 12: opens and closes in a slow rhythm, ends the way it began
            SimpleEvent.light("breathing_door", EventTag.DOORS, 0.7,
                player -> Nearby.any(player, RADIUS, Mechanisms::woodenDoor), Mechanisms::breathe),
            // 21: only the lid moves, nothing is opened
            SimpleEvent.light("chest_lid", EventTag.DOORS, 0.7,
                player -> Nearby.any(player, 10, Mechanisms::chest), Mechanisms::clickLid),
            // 22
            SimpleEvent.light("barrel_opens", EventTag.DOORS, 0.7,
                player -> Nearby.any(player, 10, Mechanisms::closedBarrel), Mechanisms::openBarrel),
            // 20
            SimpleEvent.harmful("open_house", EventTag.DOORS, 0.7,
                player -> player.level().isDarkOutside() && Nearby.any(player, 20, Mechanisms::closedWoodenDoor),
                Mechanisms::openHouse)
        );
    }

    private interface Action {
        void apply(ServerLevel level, BlockPos pos, BlockState state);
    }

    private static HauntEvent on(String id, Predicate<BlockState> match, Action action) {
        return SimpleEvent.light(id, EventTag.DOORS, 0.7,
            player -> Nearby.any(player, RADIUS, match),
            player -> {
                BlockPos pos = Nearby.random(player, RADIUS, match);
                if (pos != null) {
                    action.apply(player.level(), pos, player.level().getBlockState(pos));
                }
            });
    }

    private static boolean woodenDoor(BlockState state) {
        return state.getBlock() instanceof DoorBlock && DoorBlock.isWoodenDoor(state);
    }

    private static boolean chest(BlockState state) {
        return state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST);
    }

    private static boolean closedBarrel(BlockState state) {
        return state.getBlock() instanceof BarrelBlock && !state.getValue(BarrelBlock.OPEN);
    }

    private static boolean closedWoodenDoor(BlockState state) {
        return state.getBlock() instanceof DoorBlock door && DoorBlock.isWoodenDoor(state) && !door.isOpen(state);
    }

    private static void openHouse(ServerPlayer player) {
        for (BlockPos pos : Nearby.all(player, 20, Mechanisms::closedWoodenDoor)) {
            BlockState state = player.level().getBlockState(pos);
            ((DoorBlock) state.getBlock()).setOpen(null, player.level(), state, pos, true);
        }
    }

    private static void slamTrapdoor(ServerLevel level, BlockPos pos, BlockState state) {
        boolean open = !state.getValue(BlockStateProperties.OPEN);
        level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.OPEN, open));
        level.playSound(null, pos, open ? SoundEvents.WOODEN_TRAPDOOR_OPEN : SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.9F);
        level.gameEvent(null, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
    }

    private static void toggleGate(ServerLevel level, BlockPos pos, BlockState state) {
        boolean open = !state.getValue(BlockStateProperties.OPEN);
        level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.OPEN, open));
        level.playSound(null, pos, open ? SoundEvents.FENCE_GATE_OPEN : SoundEvents.FENCE_GATE_CLOSE, SoundSource.BLOCKS, 1.0F, 0.9F);
        level.gameEvent(null, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
    }

    /** Powers the plate for a moment; its own tick finds nobody on it and releases it. */
    private static void pressPlate(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(BlockStateProperties.POWERED, true), 3);
        level.updateNeighborsAt(pos, state.getBlock());
        level.updateNeighborsAt(pos.below(), state.getBlock());
        level.scheduleTick(pos, state.getBlock(), 20);
        level.playSound(null, pos,
            state.is(BlockTags.STONE_PRESSURE_PLATES) ? SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON : SoundEvents.WOODEN_PRESSURE_PLATE_CLICK_ON,
            SoundSource.BLOCKS);
        level.gameEvent(null, GameEvent.BLOCK_ACTIVATE, pos);
    }

    /** Six toggles, one a second; each step reads the door afresh, so a door broken or moved meanwhile is left alone. */
    private static void breathe(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, RADIUS, Mechanisms::woodenDoor);
        if (pos == null) {
            return;
        }
        Scheduler.repeat(player, 0, 20, (p, n) -> {
            ServerLevel level = p.level();
            BlockState state = level.getBlockState(pos);
            if (!woodenDoor(state)) {
                return false;
            }
            DoorBlock door = (DoorBlock) state.getBlock();
            door.setOpen(null, level, state, pos, !door.isOpen(state));
            return n < 5;
        });
    }

    private static void clickLid(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, 10, Mechanisms::chest);
        if (pos == null) {
            return;
        }
        ServerLevel level = player.level();
        lid(level, pos, true);
        Scheduler.later(player, 12, p -> lid(p.level(), pos, false));
    }

    private static void lid(ServerLevel level, BlockPos pos, boolean open) {
        BlockState state = level.getBlockState(pos);
        if (chest(state)) {
            level.blockEvent(pos, state.getBlock(), 1, open ? 1 : 0);
            level.playSound(null, pos, open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.8F, 0.9F);
        }
    }

    private static void openBarrel(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, 10, Mechanisms::closedBarrel);
        if (pos == null) {
            return;
        }
        setBarrel(player.level(), pos, true);
        Scheduler.later(player, 30, p -> setBarrel(p.level(), pos, false));
    }

    private static void setBarrel(ServerLevel level, BlockPos pos, boolean open) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BarrelBlock && state.getValue(BarrelBlock.OPEN) != open) {
            level.setBlockAndUpdate(pos, state.setValue(BarrelBlock.OPEN, open));
            level.playSound(null, pos, open ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 0.8F, 0.9F);
        }
    }
}
