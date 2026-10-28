package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Difficulty;
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

/** Fire and light sources misbehaving. The older {@code extinguish_light} event lives in its own class. */
final class Lights {
    private static final int RADIUS = 10;
    private static final int BLACKOUT_RADIUS = 16;

    private Lights() {}

    static List<HauntEvent> all() {
        return List.of(
            // 1
            SimpleEvent.light("candles_out", EventTag.LIGHTS, 0.8,
                player -> Nearby.any(player, RADIUS, AbstractCandleBlock::isLit),
                player -> {
                    ServerLevel level = player.level();
                    for (BlockPos pos : Nearby.all(player, RADIUS, AbstractCandleBlock::isLit)) {
                        AbstractCandleBlock.extinguish(null, level.getBlockState(pos), level, pos);
                    }
                }),
            // 2: the torches go out one by one, the nearest last
            SimpleEvent.light("torch_chain", EventTag.LIGHTS, 0.8,
                player -> Nearby.any(player, RADIUS, Lights::isTorch), Lights::torchChain),
            // 3: gone and back a few times; Restore finishes it if the player is gone before the last step
            SimpleEvent.light("lantern_flicker", EventTag.LIGHTS, 0.8,
                player -> Nearby.any(player, RADIUS, Lights::isLantern), Lights::flicker),
            // 4: back on after a second
            SimpleEvent.light("lamp_off", EventTag.LIGHTS, 0.8,
                player -> Nearby.any(player, RADIUS, Lights::litLamp), Lights::lampOff),
            // 6
            SimpleEvent.light("campfire_out", EventTag.LIGHTS, 0.8,
                player -> Nearby.any(player, RADIUS, CampfireBlock::isLitCampfire),
                player -> {
                    BlockPos pos = Nearby.random(player, RADIUS, CampfireBlock::isLitCampfire);
                    if (pos != null) {
                        ServerLevel level = player.level();
                        BlockState state = level.getBlockState(pos);
                        level.setBlockAndUpdate(pos, state.setValue(CampfireBlock.LIT, false));
                        level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                }),
            // 5: the fuel is taken away for a while, or the furnace would relight at once
            SimpleEvent.light("furnace_dies", EventTag.LIGHTS, 0.8,
                player -> !fueled(player).isEmpty(), Lights::douse),
            // 8
            SimpleEvent.light("candles_light", EventTag.LIGHTS, 0.8,
                player -> Nearby.any(player, RADIUS, CandleBlock::canLight),
                player -> {
                    BlockPos pos = Nearby.random(player, RADIUS, CandleBlock::canLight);
                    if (pos != null) {
                        ServerLevel level = player.level();
                        level.setBlock(pos, level.getBlockState(pos).setValue(BlockStateProperties.LIT, true), 11);
                        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.6F, 1.2F);
                    }
                }),
            // 9
            SimpleEvent.light("new_face", EventTag.LIGHTS, 0.6,
                player -> Nearby.any(player, RADIUS, s -> s.is(Blocks.JACK_O_LANTERN)),
                player -> {
                    BlockPos pos = Nearby.random(player, RADIUS, s -> s.is(Blocks.JACK_O_LANTERN));
                    if (pos != null) {
                        ServerLevel level = player.level();
                        BlockState state = level.getBlockState(pos);
                        Direction now = state.getValue(CarvedPumpkinBlock.FACING);
                        Direction next = Direction.Plane.HORIZONTAL.stream().filter(d -> d != now).skip(player.getRandom().nextInt(3)).findFirst().orElse(now);
                        level.setBlockAndUpdate(pos, state.setValue(CarvedPumpkinBlock.FACING, next));
                        level.playSound(null, pos, SoundEvents.PUMPKIN_CARVE, SoundSource.BLOCKS, 1.0F, 0.7F);
                    }
                }),
            // 10
            SimpleEvent.harmful("blackout", EventTag.LIGHTS, 0.5,
                player -> player.level().getDifficulty() != Difficulty.PEACEFUL
                    && !(player.level().isBrightOutside() && player.level().canSeeSky(player.blockPosition()))
                    && Nearby.any(player, BLACKOUT_RADIUS, Lights::isLight)
                    && Place.spawnSpot(player, 5, 9) != null,
                Lights::blackout),
            // 11
            SimpleEvent.harmful("burning_hand", EventTag.LIGHTS, 0.6, player -> isHot(player.getMainHandItem()), player -> {
                ItemStack held = player.getMainHandItem().copy();
                player.getInventory().setItem(player.getInventory().getSelectedSlot(), ItemStack.EMPTY);
                ItemEntity item = new ItemEntity(player.level(), player.getX(), player.getEyeY() - 0.3, player.getZ(), held, 0, 0.15, 0);
                item.setDefaultPickUpDelay();
                player.level().addFreshEntity(item);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.HOSTILE, 0.8F, 1.0F);
            })
        );
    }

    private static boolean isHot(ItemStack stack) {
        return stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH) || stack.is(Items.LANTERN) || stack.is(Items.SOUL_LANTERN);
    }

    private static boolean isTorch(BlockState state) {
        return state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH) || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH);
    }

    private static boolean litLamp(BlockState state) {
        return state.is(Blocks.REDSTONE_LAMP) && state.getValue(BlockStateProperties.LIT);
    }

    private static void torchChain(ServerPlayer player) {
        List<BlockPos> torches = Nearby.all(player, RADIUS, Lights::isTorch).stream()
            .sorted(Comparator.comparingDouble((BlockPos pos) -> pos.distToCenterSqr(player.position())).reversed())
            .toList();
        if (torches.isEmpty()) {
            return;
        }
        Scheduler.repeat(player, 0, 6, (p, n) -> {
            ServerLevel level = p.level();
            BlockPos pos = torches.get(n);
            if (isTorch(level.getBlockState(pos))) {
                level.destroyBlock(pos, true);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.4F);
            }
            return n + 1 < Math.min(torches.size(), 8);
        });
    }

    /** The lamp is lit again afterwards only if it is still powered; a lamp left off then is simply a lamp with no power. */
    private static void lampOff(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, RADIUS, Lights::litLamp);
        if (pos == null) {
            return;
        }
        ServerLevel level = player.level();
        level.setBlock(pos, level.getBlockState(pos).setValue(BlockStateProperties.LIT, false), 3);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.6F);
        Scheduler.later(player, 20, p -> {
            BlockState state = p.level().getBlockState(pos);
            if (state.is(Blocks.REDSTONE_LAMP) && !state.getValue(BlockStateProperties.LIT) && p.level().hasNeighborSignal(pos)) {
                p.level().setBlock(pos, state.setValue(BlockStateProperties.LIT, true), 3);
            }
        });
    }

    private static boolean isLantern(BlockState state) {
        return state.is(Blocks.LANTERN) || state.is(Blocks.SOUL_LANTERN);
    }

    private static boolean isLight(BlockState state) {
        return isTorch(state) || AbstractCandleBlock.isLit(state) || CampfireBlock.isLitCampfire(state) || litLamp(state);
    }

    private static void flicker(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, RADIUS, Lights::isLantern);
        if (pos == null) {
            return;
        }
        ServerLevel level = player.level();
        BlockState lantern = level.getBlockState(pos);
        Restore.after(level, 60, force -> {
            if (!force && !level.isLoaded(pos)) {
                return false;
            }
            if (level.isEmptyBlock(pos)) {
                Restore.quiet(level, pos, lantern);
            }
            return true;
        });
        // Out on even steps, back on odd ones; each step reads the world afresh, so a lantern the player replaced is left alone.
        Scheduler.repeat(player, 0, 4, (p, n) -> {
            ServerLevel here = p.level();
            if (n % 2 == 0 && here.getBlockState(pos) == lantern) {
                Restore.quiet(here, pos, Blocks.AIR.defaultBlockState());
                here.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3F, 1.8F);
            } else if (n % 2 == 1 && here.isEmptyBlock(pos)) {
                Restore.quiet(here, pos, lantern);
                here.playSound(null, pos, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 0.6F, 1.0F);
            }
            return n < 5;
        });
    }

    /** Burning furnaces, blast furnaces and smokers that still have fuel in the fuel slot. */
    private static List<BlockPos> fueled(ServerPlayer player) {
        return Nearby.all(player, RADIUS, s -> s.getBlock() instanceof AbstractFurnaceBlock && s.getValue(BlockStateProperties.LIT)).stream()
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
        level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.8F, 0.8F);
        Restore.after(level, 100, force -> {
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

    /** Every light within 16 blocks goes out; a moment later something hostile steps out of the dark. */
    private static void blackout(ServerPlayer player) {
        ServerLevel level = player.level();
        for (BlockPos pos : Nearby.all(player, BLACKOUT_RADIUS, Lights::isLight)) {
            BlockState state = level.getBlockState(pos);
            if (isTorch(state)) {
                level.destroyBlock(pos, true);
            } else if (AbstractCandleBlock.isLit(state)) {
                AbstractCandleBlock.extinguish(null, state, level, pos);
            } else if (CampfireBlock.isLitCampfire(state)) {
                level.setBlockAndUpdate(pos, state.setValue(CampfireBlock.LIT, false));
            } else if (litLamp(state)) {
                level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.LIT, false));
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.0F, 0.6F);
        Scheduler.later(player, 15, p -> {
            BlockPos spot = Place.spawnSpot(p, 5, 9);
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
