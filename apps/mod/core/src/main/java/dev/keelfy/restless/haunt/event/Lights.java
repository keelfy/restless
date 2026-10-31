package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.UnlitLights;
import dev.keelfy.restless.haunt.DimPayload;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.Hardcore;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.platform.Platform;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Fire and light sources misbehaving. The older {@code extinguish_light} event lives in its own class. */
final class Lights {
    private Lights() {}

    static List<HauntEvent> all() {
        return List.of(
            SimpleEvent.light("held_light_out", EventTag.LIGHTS,
                player -> java.util.Arrays.stream(InteractionHand.values())
                    .anyMatch(hand -> UnlitLights.extinguished(player.getItemInHand(hand)) != player.getItemInHand(hand)),
                player -> {
                    var hands = java.util.Arrays.stream(InteractionHand.values())
                        .filter(hand -> UnlitLights.extinguished(player.getItemInHand(hand)) != player.getItemInHand(hand)).toList();
                    if (hands.isEmpty()) return;
                    InteractionHand hand = hands.get(player.getRandom().nextInt(hands.size()));
                    player.setItemInHand(hand, UnlitLights.extinguished(player.getItemInHand(hand)));
                    Config.Tone tone = Config.event("held_light_out").tone("sound");
                    player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.HOSTILE, tone.volume(), tone.pitch());
                }),
            // 1
            SimpleEvent.light("candles_out", EventTag.LIGHTS,
                player -> Nearby.any(player, "candles_out", AbstractCandleBlock::isLit),
                player -> {
                    ServerLevel level = player.level();
                    for (BlockPos pos : Nearby.all(player, "candles_out", AbstractCandleBlock::isLit)) {
                        AbstractCandleBlock.extinguish(null, level.getBlockState(pos), level, pos);
                    }
                }),
            // 2: the torches go out one by one, the nearest last
            SimpleEvent.light("torch_chain", EventTag.LIGHTS,
                player -> Nearby.any(player, "torch_chain", Lights::isTorch), Lights::torchChain),
            // 187: underground, the torches behind the player go out one by one, the farthest first, so the way back is dark
            SimpleEvent.light("torches_behind", EventTag.LIGHTS,
                player -> roofed(player) && !behind(player).isEmpty(), Lights::torchesBehind),
            // 3: gone and back a few times; Restore finishes it if the player is gone before the last step
            SimpleEvent.light("lantern_flicker", EventTag.LIGHTS,
                player -> Nearby.any(player, "lantern_flicker", Lights::isLantern), Lights::flicker),
            // 4: back on after a second
            SimpleEvent.light("lamp_off", EventTag.LIGHTS,
                player -> Nearby.any(player, "lamp_off", Lights::litLamp), Lights::lampOff),
            // 6
            SimpleEvent.light("campfire_out", EventTag.LIGHTS,
                player -> Nearby.any(player, "campfire_out", CampfireBlock::isLitCampfire),
                player -> {
                    BlockPos pos = Nearby.random(player, "campfire_out", CampfireBlock::isLitCampfire);
                    if (pos != null) {
                        ServerLevel level = player.level();
                        BlockState state = level.getBlockState(pos);
                        level.setBlockAndUpdate(pos, state.setValue(CampfireBlock.LIT, false));
                        Config.Tone tone = Config.event("campfire_out").tone("sound");
                        level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, tone.volume(), tone.pitch());
                    }
                }),
            // 5: the fuel is taken away for a while, or the furnace would relight at once
            SimpleEvent.light("furnace_dies", EventTag.LIGHTS,
                player -> !fueled(player).isEmpty(), Lights::douse),
            // 7: the view fades on this player's client only; see Dimming
            SimpleEvent.light("dimming", EventTag.LIGHTS, player -> true, player -> dim(player, "dimming")),
            // 144: the same, quick and over and over, like eyes that will not stay open
            SimpleEvent.light("blinking", EventTag.LIGHTS, player -> true, player -> dim(player, "blinking")),
            // 8
            SimpleEvent.light("candles_light", EventTag.LIGHTS,
                player -> Nearby.any(player, "candles_light", CandleBlock::canLight),
                player -> {
                    BlockPos pos = Nearby.random(player, "candles_light", CandleBlock::canLight);
                    if (pos != null) {
                        ServerLevel level = player.level();
                        level.setBlock(pos, level.getBlockState(pos).setValue(BlockStateProperties.LIT, true), Block.UPDATE_ALL_IMMEDIATE);
                        Config.Tone tone = Config.event("candles_light").tone("sound");
                        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, tone.volume(), tone.pitch());
                    }
                }),
            // 9
            SimpleEvent.light("new_face", EventTag.LIGHTS,
                player -> Nearby.any(player, "new_face", s -> s.is(Blocks.JACK_O_LANTERN)),
                player -> {
                    BlockPos pos = Nearby.random(player, "new_face", s -> s.is(Blocks.JACK_O_LANTERN));
                    if (pos != null) {
                        ServerLevel level = player.level();
                        BlockState state = level.getBlockState(pos);
                        Direction now = state.getValue(CarvedPumpkinBlock.FACING);
                        Direction next = Direction.Plane.HORIZONTAL.stream().filter(d -> d != now).skip(player.getRandom().nextInt(3)).findFirst().orElse(now);
                        level.setBlockAndUpdate(pos, state.setValue(CarvedPumpkinBlock.FACING, next));
                        Config.Tone tone = Config.event("new_face").tone("sound");
                        level.playSound(null, pos, SoundEvents.PUMPKIN_CARVE, SoundSource.BLOCKS, tone.volume(), tone.pitch());
                    }
                }),
            // 112: another kind of torch or lantern in the same place, facing and hanging kept
            SimpleEvent.light("torch_swap", EventTag.LIGHTS, player -> Nearby.any(player, "torch_swap", Lights::swappable), player -> {
                ServerLevel level = player.level();
                BlockPos pos = Nearby.random(player, "torch_swap", Lights::swappable);
                if (pos != null) {
                    BlockState state = level.getBlockState(pos);
                    List<Block> kinds = KINDS.stream().filter(k -> k.contains(state.getBlock())).findFirst().orElseThrow().stream()
                        .filter(b -> b != state.getBlock()).toList();
                    Block to = kinds.get(player.getRandom().nextInt(kinds.size()));
                    Swaps.show(player, pos, to.withPropertiesOf(state), Config.event("torch_swap").integer("restore_ticks"));
                    Config.Tone tone = Config.event("torch_swap").tone("sound");
                    level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, tone.volume(), tone.pitch());
                }
            }),
            // 10
            SimpleEvent.harmful("blackout", EventTag.LIGHTS,
                player -> player.level().getDifficulty() != Difficulty.PEACEFUL
                    && !(player.level().isBrightOutside() && player.level().canSeeSky(player.blockPosition()))
                    && Nearby.any(player, "blackout", Lights::isLight)
                    && Place.spawnSpot(player, spawnMin(), spawnMax()) != null,
                Lights::blackout),
            // 11
            SimpleEvent.harmful("burning_hand", EventTag.LIGHTS, player -> isHot(player.getMainHandItem()), player -> {
                Config.Section config = Config.event("burning_hand");
                Config.Tone tone = config.tone("sound");
                ItemStack held = player.getMainHandItem().copy();
                player.getInventory().setItem(player.getInventory().getSelectedSlot(), ItemStack.EMPTY);
                ItemEntity item = new ItemEntity(player.level(), player.getX(), player.getEyeY() - config.num("eye_drop"), player.getZ(), held, 0, config.num("lift"), 0);
                item.setDefaultPickUpDelay();
                player.level().addFreshEntity(item);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.HOSTILE, tone.volume(), tone.pitch());
            })
        );
    }

    /** Interchangeable kinds; a wall torch only turns into a wall torch, so the properties carry over. */
    private static final List<List<Block>> KINDS = List.of(
        List.of(Blocks.TORCH, Blocks.SOUL_TORCH, Blocks.COPPER_TORCH),
        List.of(Blocks.WALL_TORCH, Blocks.SOUL_WALL_TORCH, Blocks.COPPER_WALL_TORCH),
        List.of(Blocks.LANTERN, Blocks.SOUL_LANTERN));

    private static boolean swappable(BlockState state) {
        return KINDS.stream().anyMatch(k -> k.contains(state.getBlock()));
    }

    private static boolean isHot(ItemStack stack) {
        return stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH) || stack.is(Items.LANTERN) || stack.is(Items.SOUL_LANTERN);
    }

    private static boolean isTorch(BlockState state) {
        return UnlitLights.isTorch(state);
    }

    private static boolean litLamp(BlockState state) {
        return state.is(Blocks.REDSTONE_LAMP) && state.getValue(BlockStateProperties.LIT);
    }

    private static void torchChain(ServerPlayer player) {
        List<BlockPos> torches = Nearby.all(player, "torch_chain", Lights::isTorch).stream()
            .sorted(Comparator.comparingDouble((BlockPos pos) -> pos.distToCenterSqr(player.position())).reversed())
            .toList();
        if (torches.isEmpty()) {
            return;
        }
        Config.Section config = Config.event("torch_chain");
        Config.Tone tone = config.tone("sound");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            ServerLevel level = p.level();
            BlockPos pos = torches.get(n);
            if (isTorch(level.getBlockState(pos))) {
                UnlitLights.extinguish(level, pos);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, tone.volume(), tone.pitch());
            }
            return n + 1 < Math.min(torches.size(), config.integer("max_torches"));
        });
    }

    /** Something solid overhead; sky light would lag behind a roof just built. */
    private static boolean roofed(ServerPlayer player) {
        BlockPos feet = player.blockPosition();
        return player.level().getHeight(Heightmap.Types.MOTION_BLOCKING, feet.getX(), feet.getZ()) > feet.getY() + 1;
    }

    /** Torches in range behind the player's facing, farthest first. */
    private static List<BlockPos> behind(ServerPlayer player) {
        Vec3 look = player.getLookAngle().multiply(1, 0, 1);
        return Nearby.all(player, "torches_behind", Lights::isTorch).stream()
            .filter(pos -> Vec3.atCenterOf(pos).subtract(player.position()).dot(look) < 0)
            .sorted(Comparator.comparingDouble((BlockPos pos) -> pos.distToCenterSqr(player.position())).reversed())
            .toList();
    }

    /** Back on after a while, each torch only if it is still the one put out; under hardcore they stay out. */
    private static void torchesBehind(ServerPlayer player) {
        List<BlockPos> torches = behind(player);
        Config.Section config = Config.event("torches_behind");
        Config.Tone tone = config.tone("sound");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            ServerLevel level = p.level();
            BlockPos pos = torches.get(n);
            BlockState state = level.getBlockState(pos);
            if (isTorch(state)) {
                Restore.swap(level, pos, UnlitLights.extinguished(state), config.integer("restore_ticks"));
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, tone.volume(), tone.pitch());
            }
            return n + 1 < Math.min(torches.size(), config.integer("max_torches"));
        });
    }

    private static void dim(ServerPlayer player, String id) {
        if (!Platform.get().canReceive(player, DimPayload.TYPE)) {
            return;
        }
        Config.Section config = Config.event(id);
        int times = config.integer("times_min") + player.getRandom().nextInt(config.integer("times_max") - config.integer("times_min") + 1);
        player.connection.send(new ClientboundCustomPayloadPacket(new DimPayload(
            config.integer("fall_ticks"), config.integer("hold_ticks"), config.integer("rise_ticks"), times, (float) config.num("peak"))));
    }

    /** The lamp is lit again afterwards only if it is still powered; a lamp left off then is simply a lamp with no power. */
    private static void lampOff(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, "lamp_off", Lights::litLamp);
        if (pos == null) {
            return;
        }
        ServerLevel level = player.level();
        level.setBlock(pos, level.getBlockState(pos).setValue(BlockStateProperties.LIT, false), Block.UPDATE_ALL);
        Config.Section config = Config.event("lamp_off");
        Config.Tone tone = config.tone("sound");
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, tone.volume(), tone.pitch());
        Scheduler.later(player, config.integer("relight_ticks"), p -> {
            BlockState state = p.level().getBlockState(pos);
            if (state.is(Blocks.REDSTONE_LAMP) && !state.getValue(BlockStateProperties.LIT) && p.level().hasNeighborSignal(pos)) {
                p.level().setBlock(pos, state.setValue(BlockStateProperties.LIT, true), Block.UPDATE_ALL);
            }
        });
    }

    private static boolean isLantern(BlockState state) {
        return UnlitLights.isLantern(state);
    }

    private static boolean isLight(BlockState state) {
        return isTorch(state) || AbstractCandleBlock.isLit(state) || CampfireBlock.isLitCampfire(state) || litLamp(state);
    }

    private static void flicker(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, "lantern_flicker", Lights::isLantern);
        if (pos == null) {
            return;
        }
        Config.Section config = Config.event("lantern_flicker");
        Config.Tone off = config.tone("off_sound");
        Config.Tone on = config.tone("on_sound");
        ServerLevel level = player.level();
        BlockState lantern = level.getBlockState(pos);
        BlockState unlit = UnlitLights.extinguished(lantern);
        int interval = config.integer("interval_ticks");
        // Out for one interval on even steps, then back on its own (never in hardcore); each step reads the world afresh, so a lantern the player replaced is left alone.
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            ServerLevel here = p.level();
            if (n % 2 == 0 && here.getBlockState(pos) == lantern) {
                Swaps.show(p, pos, unlit, interval);
                here.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, off.volume(), off.pitch());
            } else if (n % 2 == 1 && !Hardcore.on(here)) {
                here.playSound(null, pos, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, on.volume(), on.pitch());
            }
            return n + 1 < config.integer("steps");
        });
    }

    /** Burning furnaces, blast furnaces and smokers that still have fuel in the fuel slot. */
    private static List<BlockPos> fueled(ServerPlayer player) {
        return Nearby.all(player, "furnace_dies", s -> s.getBlock() instanceof AbstractFurnaceBlock && s.getValue(BlockStateProperties.LIT)).stream()
            .filter(pos -> player.level().getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity furnace && !furnace.getItem(1).isEmpty())
            .toList();
    }

    /**
     * The remaining burn time is zeroed through the entity's own save format (the field is private), the fuel slot
     * is emptied, and the fuel goes back after five seconds, or onto the ground if the furnace is gone or the slot taken.
     */
    private static void douse(ServerPlayer player) {
        List<BlockPos> furnaces = fueled(player);
        if (furnaces.isEmpty()) {
            return;
        }
        ServerLevel level = player.level();
        BlockPos pos = furnaces.get(player.getRandom().nextInt(furnaces.size()));
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) level.getBlockEntity(pos);
        ItemStack fuel = furnace.getItem(1).copy();
        CompoundTag data = furnace.saveCustomOnly(level.registryAccess());
        data.putInt("lit_time_remaining", 0);
        furnace.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data));
        furnace.setItem(1, ItemStack.EMPTY);
        level.setBlock(pos, level.getBlockState(pos).setValue(BlockStateProperties.LIT, false), Block.UPDATE_ALL);
        Config.Section config = Config.event("furnace_dies");
        Config.Tone tone = config.tone("sound");
        level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, tone.volume(), tone.pitch());
        Restore.revert(level, config.integer("return_ticks"), force -> {
            if (!force && !level.isLoaded(pos)) {
                return false;
            }
            if (level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity back && back.getItem(1).isEmpty()) {
                back.setItem(1, fuel);
            } else {
                Block.popResource(level, pos, fuel);
            }
            return true;
        });
    }

    private static int spawnMin() {
        return Config.event("blackout").integer("spawn_min");
    }

    private static int spawnMax() {
        return Config.event("blackout").integer("spawn_max");
    }

    /** Every light in range goes out; a moment later something hostile steps out of the dark. */
    private static void blackout(ServerPlayer player) {
        ServerLevel level = player.level();
        for (BlockPos pos : Nearby.all(player, "blackout", Lights::isLight)) {
            BlockState state = level.getBlockState(pos);
            if (isTorch(state)) {
                UnlitLights.extinguish(level, pos);
            } else if (AbstractCandleBlock.isLit(state)) {
                AbstractCandleBlock.extinguish(null, state, level, pos);
            } else if (CampfireBlock.isLitCampfire(state)) {
                level.setBlockAndUpdate(pos, state.setValue(CampfireBlock.LIT, false));
            } else if (litLamp(state)) {
                level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.LIT, false));
            }
        }
        Config.Tone tone = Config.event("blackout").tone("sound");
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, tone.volume(), tone.pitch());
        Scheduler.later(player, Config.event("blackout").integer("spawn_delay_ticks"), p -> {
            BlockPos spot = Place.spawnSpot(p, spawnMin(), spawnMax());
            if (spot == null) {
                return;
            }
            Mob mob = (p.getRandom().nextBoolean() ? EntityTypes.ZOMBIE : EntityTypes.SKELETON).spawn(p.level(), spot, EntitySpawnReason.EVENT);
            if (mob != null) {
                mob.setTarget(p);
            }
        });
    }
}
