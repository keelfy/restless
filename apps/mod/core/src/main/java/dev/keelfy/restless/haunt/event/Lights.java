package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
}
