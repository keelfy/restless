package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Fire and light sources misbehaving. The older {@code extinguish_light} event lives in its own class. */
final class Lights {
    private static final int RADIUS = 10;

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
}
