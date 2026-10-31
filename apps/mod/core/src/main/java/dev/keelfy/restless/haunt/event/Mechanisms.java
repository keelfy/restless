package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.DoorLocks;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;

/** Doors, gates, buttons and the like acting on their own. */
final class Mechanisms {
    private Mechanisms() {}

    static List<HauntEvent> all() {
        return List.of(
                // 13
                on(
                        "trapdoor",
                        s -> s.getBlock() instanceof TrapDoorBlock && s.is(BlockTags.WOODEN_TRAPDOORS),
                        Mechanisms::slamTrapdoor),
                // 14
                on("fence_gate", s -> s.getBlock() instanceof FenceGateBlock, Mechanisms::toggleGate),
                // 15
                on(
                        "button",
                        s -> s.getBlock() instanceof ButtonBlock && !s.getValue(BlockStateProperties.POWERED),
                        (level, pos, s) -> ((ButtonBlock) s.getBlock()).press(s, level, pos, null)),
                // 16
                on(
                        "lever",
                        s -> s.getBlock() instanceof LeverBlock,
                        (level, pos, s) -> ((LeverBlock) s.getBlock()).pull(s, level, pos, null)),
                // 17
                on(
                        "pressure_plate",
                        s -> s.getBlock() instanceof PressurePlateBlock && !s.getValue(BlockStateProperties.POWERED),
                        Mechanisms::pressPlate),
                // 18
                on(
                        "iron_door",
                        s -> s.getBlock() instanceof DoorBlock && !DoorBlock.isWoodenDoor(s),
                        (level, pos, s) -> {
                            DoorBlock door = (DoorBlock) s.getBlock();
                            door.setOpen(null, level, s, pos, !door.isOpen(s));
                        }),
                // 12: opens and closes in a slow rhythm, ends the way it began
                SimpleEvent.light(
                        "breathing_door",
                        EventTag.DOORS,
                        player -> Nearby.any(player, "breathing_door", Mechanisms::woodenDoor),
                        Mechanisms::breathe),
                // 21: only the lid moves, nothing is opened
                SimpleEvent.light(
                        "chest_lid",
                        EventTag.DOORS,
                        player -> Nearby.any(player, "chest_lid", Mechanisms::chest),
                        Mechanisms::clickLid),
                // 22
                SimpleEvent.light(
                        "barrel_opens",
                        EventTag.DOORS,
                        player -> Nearby.any(player, "barrel_opens", Mechanisms::closedBarrel),
                        Mechanisms::openBarrel),
                // 101: the music stops mid-song and the disc is knocked out
                SimpleEvent.light(
                        "jukebox_pop",
                        EventTag.DOORS,
                        player -> !playingJukeboxes(player).isEmpty(),
                        Mechanisms::popRecord),
                // 19: slams shut and cannot be opened for a while; the cancelling lives in Restless.onUseBlock, on both
                // sides
                SimpleEvent.harmful(
                        "locked_door",
                        EventTag.DOORS,
                        player -> Nearby.any(player, "locked_door", Mechanisms::lockableDoor),
                        Mechanisms::lockDoor),
                // 20
                SimpleEvent.harmful(
                        "open_house",
                        EventTag.DOORS,
                        player -> player.level().isDarkOutside()
                                && Nearby.any(player, "open_house", Mechanisms::closedWoodenDoor),
                        Mechanisms::openHouse));
    }

    private interface Action {
        void apply(ServerLevel level, BlockPos pos, BlockState state);
    }

    private static HauntEvent on(String id, Predicate<BlockState> match, Action action) {
        return SimpleEvent.light(id, EventTag.DOORS, player -> Nearby.any(player, id, match), player -> {
            BlockPos pos = Nearby.random(player, id, match);
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

    private static boolean lockableDoor(BlockState state) {
        return woodenDoor(state) && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
    }

    private static boolean closedBarrel(BlockState state) {
        return state.getBlock() instanceof BarrelBlock && !state.getValue(BarrelBlock.OPEN);
    }

    private static boolean closedWoodenDoor(BlockState state) {
        return state.getBlock() instanceof DoorBlock door && DoorBlock.isWoodenDoor(state) && !door.isOpen(state);
    }

    private static List<JukeboxBlockEntity> playingJukeboxes(ServerPlayer player) {
        return Nearby.all(player, "jukebox_pop", s -> s.is(Blocks.JUKEBOX) && s.getValue(JukeboxBlock.HAS_RECORD))
                .stream()
                .map(pos -> player.level().getBlockEntity(pos))
                .filter(entity -> entity instanceof JukeboxBlockEntity jukebox
                        && jukebox.getSongPlayer().isPlaying())
                .map(entity -> (JukeboxBlockEntity) entity)
                .toList();
    }

    private static void popRecord(ServerPlayer player) {
        List<JukeboxBlockEntity> playing = playingJukeboxes(player);
        if (!playing.isEmpty()) {
            JukeboxBlockEntity jukebox = playing.get(player.getRandom().nextInt(playing.size()));
            jukebox.popOutTheItem();
        }
    }

    private static void openHouse(ServerPlayer player) {
        for (BlockPos pos : Nearby.all(player, "open_house", Mechanisms::closedWoodenDoor)) {
            BlockState state = player.level().getBlockState(pos);
            ((DoorBlock) state.getBlock()).setOpen(null, player.level(), state, pos, true);
        }
    }

    private static void slamTrapdoor(ServerLevel level, BlockPos pos, BlockState state) {
        boolean open = !state.getValue(BlockStateProperties.OPEN);
        level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.OPEN, open));
        Config.Tone tone = Config.event("trapdoor").tone("sound");
        level.playSound(
                null,
                pos,
                open ? SoundEvents.WOODEN_TRAPDOOR_OPEN : SoundEvents.WOODEN_TRAPDOOR_CLOSE,
                SoundSource.BLOCKS,
                tone.volume(),
                tone.pitch());
        level.gameEvent(null, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
    }

    private static void toggleGate(ServerLevel level, BlockPos pos, BlockState state) {
        boolean open = !state.getValue(BlockStateProperties.OPEN);
        level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.OPEN, open));
        Config.Tone tone = Config.event("fence_gate").tone("sound");
        level.playSound(
                null,
                pos,
                open ? SoundEvents.FENCE_GATE_OPEN : SoundEvents.FENCE_GATE_CLOSE,
                SoundSource.BLOCKS,
                tone.volume(),
                tone.pitch());
        level.gameEvent(null, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
    }

    /** Powers the plate for a moment; its own tick finds nobody on it and releases it. */
    private static void pressPlate(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(BlockStateProperties.POWERED, true), Block.UPDATE_ALL);
        level.updateNeighborsAt(pos, state.getBlock());
        level.updateNeighborsAt(pos.below(), state.getBlock());
        Config.Section config = Config.event("pressure_plate");
        Config.Tone tone = config.tone("sound");
        level.scheduleTick(pos, state.getBlock(), config.integer("release_ticks"));
        level.playSound(
                null,
                pos,
                state.is(BlockTags.STONE_PRESSURE_PLATES)
                        ? SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON
                        : SoundEvents.WOODEN_PRESSURE_PLATE_CLICK_ON,
                SoundSource.BLOCKS,
                tone.volume(),
                tone.pitch());
        level.gameEvent(null, GameEvent.BLOCK_ACTIVATE, pos);
    }

    /**
     * A run of toggles at a slow rhythm; each step reads the door afresh, so a door broken or moved meanwhile is left
     * alone.
     */
    private static void breathe(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, "breathing_door", Mechanisms::woodenDoor);
        if (pos == null) {
            return;
        }
        Config.Section config = Config.event("breathing_door");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            ServerLevel level = p.level();
            BlockState state = level.getBlockState(pos);
            if (!woodenDoor(state)) {
                return false;
            }
            DoorBlock door = (DoorBlock) state.getBlock();
            door.setOpen(null, level, state, pos, !door.isOpen(state));
            return n + 1 < config.integer("toggles");
        });
    }

    private static void clickLid(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, "chest_lid", Mechanisms::chest);
        if (pos == null) {
            return;
        }
        ServerLevel level = player.level();
        lid(level, pos, true);
        Scheduler.later(
                player, Config.event("chest_lid").integer("close_after_ticks"), p -> lid(p.level(), pos, false));
    }

    private static void lid(ServerLevel level, BlockPos pos, boolean open) {
        BlockState state = level.getBlockState(pos);
        if (chest(state)) {
            level.blockEvent(pos, state.getBlock(), 1, open ? 1 : 0);
            Config.Tone tone = Config.event("chest_lid").tone("sound");
            level.playSound(
                    null,
                    pos,
                    open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE,
                    SoundSource.BLOCKS,
                    tone.volume(),
                    tone.pitch());
        }
    }

    private static void openBarrel(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, "barrel_opens", Mechanisms::closedBarrel);
        if (pos == null) {
            return;
        }
        setBarrel(player.level(), pos, true);
        Scheduler.later(
                player,
                Config.event("barrel_opens").integer("close_after_ticks"),
                p -> setBarrel(p.level(), pos, false));
    }

    private static void setBarrel(ServerLevel level, BlockPos pos, boolean open) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BarrelBlock && state.getValue(BarrelBlock.OPEN) != open) {
            level.setBlockAndUpdate(pos, state.setValue(BarrelBlock.OPEN, open));
            Config.Tone tone = Config.event("barrel_opens").tone("sound");
            level.playSound(
                    null,
                    pos,
                    open ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE,
                    SoundSource.BLOCKS,
                    tone.volume(),
                    tone.pitch());
        }
    }

    private static void lockDoor(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, "locked_door", Mechanisms::lockableDoor);
        if (pos == null) {
            return;
        }
        ServerLevel level = player.level();
        BlockState state = level.getBlockState(pos);
        DoorBlock door = (DoorBlock) state.getBlock();
        if (door.isOpen(state)) {
            door.setOpen(null, level, state, pos, false);
        }
        Config.Section config = Config.event("locked_door");
        Config.Tone tone = config.tone("slam_sound");
        level.playSound(
                null, pos, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.BLOCKS, tone.volume(), tone.pitch());
        DoorLocks.lock(level, pos, config.integer("lock_ticks"));
    }
}
