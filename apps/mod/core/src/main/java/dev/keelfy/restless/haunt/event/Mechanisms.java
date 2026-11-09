package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.DoorLocks;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

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
                        Mechanisms::openHouse),
                // 219: the click of a tripwire hook that nothing tripped
                SimpleEvent.light(
                        "hook_click",
                        EventTag.DOORS,
                        player -> Nearby.any(player, "hook_click", s -> s.is(Blocks.TRIPWIRE_HOOK)),
                        Mechanisms::clickHook),
                // 221: stays flipped for a while, so lamps on it switch with the daylight the wrong way round
                SimpleEvent.light(
                        "daylight_flip",
                        EventTag.DOORS,
                        player -> Nearby.any(player, "daylight_flip", s -> s.is(Blocks.DAYLIGHT_DETECTOR)),
                        Mechanisms::flipDetector),
                // 224: the vanilla lightning strike on a rod: powered for a moment, with its sparks
                SimpleEvent.light(
                        "rod_sparks",
                        EventTag.DOORS,
                        player -> Nearby.any(player, "rod_sparks", Mechanisms::idleRod),
                        Mechanisms::sparkRod),
                // 240: the vanilla burnout, which relights it by itself after a while
                SimpleEvent.light(
                        "torch_burnout",
                        EventTag.DOORS,
                        player -> Nearby.any(player, "torch_burnout", Mechanisms::litTorch),
                        Mechanisms::burnOut),
                // 243: every lid in the house, at once
                SimpleEvent.light(
                        "chests_open",
                        EventTag.DOORS,
                        player -> Household.indoors(player)
                                && Nearby.all(player, "chests_open", Mechanisms::chest)
                                                .size()
                                        >= Config.event("chests_open").integer("min_chests"),
                        Mechanisms::openChests),
                // 217: only the player's client sees it, which also keeps it from pushing anything
                SimpleEvent.light(
                        "piston_twitch",
                        EventTag.DOORS,
                        player -> !twitchers(player).isEmpty(),
                        Mechanisms::twitchPiston),
                // 218: an item lying beside a hopper slides over it, and the hopper takes it
                SimpleEvent.harmful(
                        "hopper_swallows", EventTag.DOORS, player -> gulp(player) != null, Mechanisms::swallow),
                // 225: toward the player, farthest sensor first
                SimpleEvent.light(
                        "sculk_chain",
                        EventTag.DOORS,
                        player -> Nearby.all(player, "sculk_chain", Mechanisms::idleSensor)
                                        .size()
                                >= Config.event("sculk_chain").integer("min_sensors"),
                        Mechanisms::chainSensors),
                // 241: pulses on and off, and ends off
                SimpleEvent.harmful(
                        "observer_pulse",
                        EventTag.DOORS,
                        player -> Nearby.any(player, "observer_pulse", Mechanisms::idleObserver),
                        Mechanisms::pulseObserver),
                // 215: the vanilla message, the spawn point untouched
                SimpleEvent.harmful(
                        "bed_missing",
                        EventTag.PRESENCE,
                        player -> player.getRespawnConfig() != null,
                        player -> player.connection.send(new ClientboundGameEventPacket(
                                ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE, 0))),
                // 244: shown in an empty slot of the open ender chest; the next click shows the truth
                SimpleEvent.light(
                        "ender_note",
                        EventTag.PRESENCE,
                        player -> !enderSlots(player).isEmpty(),
                        Mechanisms::leaveNote));
    }

    private record Gulp(BlockPos hopper, ItemEntity item) {}

    private static List<BlockPos> twitchers(ServerPlayer player) {
        ServerLevel level = player.level();
        return Nearby.all(
                        player,
                        "piston_twitch",
                        s -> s.getBlock() instanceof PistonBaseBlock && !s.getValue(PistonBaseBlock.EXTENDED))
                .stream()
                .filter(pos -> {
                    Direction facing = level.getBlockState(pos).getValue(PistonBaseBlock.FACING);
                    return level.getBlockState(pos.relative(facing)).isAir()
                            && level.getBlockState(pos.relative(facing, 2)).isAir();
                })
                .toList();
    }

    /** Extend and retract events sent to this client only; the retract is type 2, which pulls nothing back. */
    private static void twitchPiston(ServerPlayer player) {
        List<BlockPos> pistons = twitchers(player);
        if (pistons.isEmpty()) {
            return;
        }
        BlockPos pos = pistons.get(player.getRandom().nextInt(pistons.size()));
        ServerLevel level = player.level();
        Block block = level.getBlockState(pos).getBlock();
        Direction direction = level.getBlockState(pos).getValue(PistonBaseBlock.FACING);
        int facing = direction.get3DDataValue();
        Config.Section config = Config.event("piston_twitch");
        int steps = config.integer("twitches") * 2;
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            p.connection.send(new ClientboundBlockEventPacket(pos, block, n % 2 == 0 ? 0 : 2, facing));
            if (n + 1 < steps) {
                return true;
            }
            // Whatever the client made of it, it is told the real blocks again.
            Scheduler.later(p, config.integer("interval_ticks"), q -> {
                if (q.level() == level) {
                    q.connection.send(new ClientboundBlockUpdatePacket(level, pos));
                    q.connection.send(new ClientboundBlockUpdatePacket(level, pos.relative(direction)));
                }
            });
            return false;
        });
    }

    private static boolean idleObserver(BlockState state) {
        return state.is(Blocks.OBSERVER) && !state.getValue(ObserverBlock.POWERED);
    }

    private static void pulseObserver(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, "observer_pulse", Mechanisms::idleObserver);
        if (pos == null) {
            return;
        }
        Config.Section config = Config.event("observer_pulse");
        int steps = config.integer("pulses") * 2;
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            ServerLevel level = p.level();
            BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.OBSERVER)) {
                return false;
            }
            level.setBlock(pos, state.setValue(ObserverBlock.POWERED, n % 2 == 0), Block.UPDATE_CLIENTS);
            level.updateNeighborsAt(
                    pos.relative(state.getValue(ObserverBlock.FACING).getOpposite()), state.getBlock());
            return n + 1 < steps;
        });
    }

    private static boolean idleSensor(BlockState state) {
        return state.getBlock() instanceof SculkSensorBlock
                && SculkSensorBlock.getPhase(state) == SculkSensorPhase.INACTIVE;
    }

    private static void chainSensors(ServerPlayer player) {
        Config.Section config = Config.event("sculk_chain");
        Vec3 from = player.position();
        List<BlockPos> sensors = Nearby.all(player, "sculk_chain", Mechanisms::idleSensor).stream()
                .sorted(Comparator.comparingDouble((BlockPos pos) -> pos.distToCenterSqr(from))
                        .reversed())
                .limit(config.integer("max_sensors"))
                .toList();
        Scheduler.repeat(player, 0, config.integer("step_ticks"), (p, n) -> {
            ServerLevel level = p.level();
            BlockPos pos = sensors.get(n);
            BlockState state = level.getBlockState(pos);
            if (idleSensor(state)) {
                ((SculkSensorBlock) state.getBlock())
                        .activate(null, level, pos, state, config.integer("power"), config.integer("frequency"));
            }
            return n + 1 < sensors.size();
        });
    }

    private static @Nullable Gulp gulp(ServerPlayer player) {
        ServerLevel level = player.level();
        Config.Section config = Config.event("hopper_swallows");
        double reach = config.num("reach");
        List<BlockPos> hoppers =
                Nearby.all(player, "hopper_swallows", s -> s.is(Blocks.HOPPER) && s.getValue(HopperBlock.ENABLED));
        if (hoppers.isEmpty()) {
            return null;
        }
        List<Gulp> found = new ArrayList<>();
        for (ItemEntity item : level.getEntitiesOfClass(
                ItemEntity.class, player.getBoundingBox().inflate(Nearby.radius("hopper_swallows")), Entity::isAlive)) {
            for (BlockPos hopper : hoppers) {
                double distance = item.position().distanceToSqr(Vec3.atCenterOf(hopper));
                if (distance <= reach * reach && distance > 1.5 * 1.5) {
                    found.add(new Gulp(hopper, item));
                    break;
                }
            }
        }
        return found.isEmpty() ? null : found.get(player.getRandom().nextInt(found.size()));
    }

    private static void swallow(ServerPlayer player) {
        Gulp gulp = gulp(player);
        if (gulp == null) {
            return;
        }
        ItemEntity item = gulp.item();
        BlockPos hopper = gulp.hopper();
        Config.Section config = Config.event("hopper_swallows");
        item.setNoGravity(true);
        Scheduler.repeat(player, 0, 1, (p, n) -> {
            ServerLevel level = p.level();
            Vec3 target = Vec3.atBottomCenterOf(hopper).add(0, config.num("mouth_height"), 0);
            Vec3 left = target.subtract(item.position());
            boolean over = left.length() < config.num("arrive_distance");
            if (item.isAlive() && level.getBlockState(hopper).is(Blocks.HOPPER) && n < config.integer("max_ticks")) {
                Vec3 next = over ? target : item.position().add(left.scale(config.num("speed")));
                item.setPos(next.x, next.y, next.z);
                item.setDeltaMovement(Vec3.ZERO);
            }
            if (over || !item.isAlive() || n >= config.integer("max_ticks")) {
                item.setNoGravity(false);
                return false;
            }
            return true;
        });
    }

    private static List<Integer> enderSlots(ServerPlayer player) {
        if (!(player.containerMenu instanceof ChestMenu menu)
                || menu.getContainer() != player.getEnderChestInventory()) {
            return List.of();
        }
        List<Integer> empty = new ArrayList<>();
        for (int slot = 0; slot < menu.getContainer().getContainerSize(); slot++) {
            if (menu.getContainer().getItem(slot).isEmpty()) {
                empty.add(slot);
            }
        }
        return empty;
    }

    private static void leaveNote(ServerPlayer player) {
        List<Integer> empty = enderSlots(player);
        if (empty.isEmpty()) {
            return;
        }
        ItemStack note = new ItemStack(Items.PAPER);
        List<String> texts = Config.event("ender_note").texts("texts");
        note.set(
                DataComponents.CUSTOM_NAME,
                Component.literal(texts.get(player.getRandom().nextInt(texts.size()))));
        AbstractContainerMenu menu = player.containerMenu;
        player.connection.send(new ClientboundContainerSetSlotPacket(
                menu.containerId,
                menu.getStateId(),
                empty.get(player.getRandom().nextInt(empty.size())),
                note));
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

    private static boolean idleRod(BlockState state) {
        return state.getBlock() instanceof LightningRodBlock && !state.getValue(BlockStateProperties.POWERED);
    }

    private static boolean litTorch(BlockState state) {
        return state.getBlock() instanceof RedstoneTorchBlock && state.getValue(RedstoneTorchBlock.LIT);
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
            Haze.at(player.level(), Vec3.atBottomCenterOf(pos));
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
            Haze.at(level, Vec3.atBottomCenterOf(pos));
            return n + 1 < config.integer("toggles");
        });
    }

    private static void clickLid(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, "chest_lid", Mechanisms::chest);
        if (pos == null) {
            return;
        }
        ServerLevel level = player.level();
        lid("chest_lid", level, pos, true);
        Scheduler.later(
                player,
                Config.event("chest_lid").integer("close_after_ticks"),
                p -> lid("chest_lid", p.level(), pos, false));
    }

    private static void lid(String event, ServerLevel level, BlockPos pos, boolean open) {
        BlockState state = level.getBlockState(pos);
        if (chest(state)) {
            level.blockEvent(pos, state.getBlock(), 1, open ? 1 : 0);
            Config.Tone tone = Config.event(event).tone("sound");
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
        Haze.linger(player, Vec3.atBottomCenterOf(pos), config.integer("lock_ticks"));
    }

    private static void clickHook(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, "hook_click", s -> s.is(Blocks.TRIPWIRE_HOOK));
        if (pos == null) {
            return;
        }
        ServerLevel level = player.level();
        Config.Section config = Config.event("hook_click");
        Config.Tone tone = config.tone("sound");
        level.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, tone.volume(), tone.pitch());
        Scheduler.later(
                player,
                config.integer("release_ticks"),
                p -> level.playSound(
                        null, pos, SoundEvents.TRIPWIRE_CLICK_OFF, SoundSource.BLOCKS, tone.volume(), tone.pitch()));
    }

    /** The detector's own tick works out its power again; set back after a while unless it stays under hardcore. */
    private static void flipDetector(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, "daylight_flip", s -> s.is(Blocks.DAYLIGHT_DETECTOR));
        if (pos == null) {
            return;
        }
        ServerLevel level = player.level();
        BlockState flipped = level.getBlockState(pos).cycle(DaylightDetectorBlock.INVERTED);
        level.setBlockAndUpdate(pos, flipped);
        Restore.revert(level, Config.event("daylight_flip").integer("restore_ticks"), force -> {
            if (!force && !level.isLoaded(pos)) {
                return false;
            }
            BlockState now = level.getBlockState(pos);
            if (now.is(Blocks.DAYLIGHT_DETECTOR)
                    && now.getValue(DaylightDetectorBlock.INVERTED)
                            == flipped.getValue(DaylightDetectorBlock.INVERTED)) {
                level.setBlockAndUpdate(pos, now.cycle(DaylightDetectorBlock.INVERTED));
            }
            return true;
        });
    }

    private static void sparkRod(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, "rod_sparks", Mechanisms::idleRod);
        if (pos != null) {
            BlockState state = player.level().getBlockState(pos);
            ((LightningRodBlock) state.getBlock()).onLightningStrike(state, player.level(), pos);
        }
    }

    /**
     * The torch goes out without a neighbour update, so it does not light itself again at once; the tick scheduled here
     * does it, or any update beside it sooner.
     */
    private static void burnOut(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, "torch_burnout", Mechanisms::litTorch);
        if (pos == null) {
            return;
        }
        ServerLevel level = player.level();
        BlockState state = level.getBlockState(pos);
        Restore.quiet(level, pos, state.setValue(RedstoneTorchBlock.LIT, false));
        level.updateNeighborsAt(pos, state.getBlock());
        level.updateNeighborsAt(pos.above(), state.getBlock());
        level.levelEvent(LevelEvent.REDSTONE_TORCH_BURNOUT, pos, 0);
        level.scheduleTick(pos, state.getBlock(), Config.event("torch_burnout").integer("relight_ticks"));
    }

    private static void openChests(ServerPlayer player) {
        ServerLevel level = player.level();
        List<BlockPos> chests = Nearby.all(player, "chests_open", Mechanisms::chest);
        chests.forEach(pos -> lid("chests_open", level, pos, true));
        Scheduler.later(
                player,
                Config.event("chests_open").integer("close_after_ticks"),
                p -> chests.forEach(pos -> lid("chests_open", p.level(), pos, false)));
    }
}
