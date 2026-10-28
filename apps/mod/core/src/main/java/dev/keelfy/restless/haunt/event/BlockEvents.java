package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Small changes to blocks. Each one is something the player can undo by hand (replant, regrow, replace). */
final class BlockEvents {
    private static final double WEIGHT = 0.6;

    private BlockEvents() {}

    static List<HauntEvent> all() {
        return List.of(
            // 73
            SimpleEvent.light("withering_crops", EventTag.BLOCKS, WEIGHT,
                player -> Nearby.any(player, 6, BlockEvents::grown),
                player -> {
                    ServerLevel level = player.level();
                    for (BlockPos pos : Nearby.all(player, 6, BlockEvents::grown)) {
                        BlockState state = level.getBlockState(pos);
                        CropBlock crop = (CropBlock) state.getBlock();
                        level.setBlockAndUpdate(pos, crop.getStateForAge(crop.getAge(state) - 1));
                    }
                }),
            // 76
            SimpleEvent.light("turning_frame", EventTag.BLOCKS, WEIGHT,
                player -> !frames(player).isEmpty(),
                player -> {
                    List<ItemFrame> frames = frames(player);
                    ItemFrame frame = frames.get(player.getRandom().nextInt(frames.size()));
                    frame.setRotation(frame.getRotation() + 1 + player.getRandom().nextInt(3));
                }),
            // 78
            SimpleEvent.light("dry_cauldron", EventTag.BLOCKS, WEIGHT,
                player -> Nearby.any(player, 10, s -> s.is(Blocks.WATER_CAULDRON)),
                player -> {
                    BlockPos pos = Nearby.random(player, 10, s -> s.is(Blocks.WATER_CAULDRON));
                    if (pos != null) {
                        player.level().setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
                    }
                }),
            // 79: a circle three blocks wide around the player, on the ground only
            SimpleEvent.light("dead_ground", EventTag.BLOCKS, WEIGHT,
                player -> player.onGround() && player.level().getBlockState(player.blockPosition().below()).is(Blocks.GRASS_BLOCK),
                player -> {
                    ServerLevel level = player.level();
                    BlockPos center = player.blockPosition().below();
                    for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, -1, -3), center.offset(3, 1, 3))) {
                        if (pos.distSqr(center) <= 9 && level.getBlockState(pos).is(Blocks.GRASS_BLOCK)) {
                            level.setBlockAndUpdate(pos.immutable(), Blocks.DIRT.defaultBlockState());
                        }
                    }
                }),
            // 81
            SimpleEvent.harmful("broken_bed", EventTag.BLOCKS, WEIGHT,
                player -> Nearby.any(player, 16, s -> s.getBlock() instanceof BedBlock),
                player -> {
                    BlockPos pos = Nearby.random(player, 16, s -> s.getBlock() instanceof BedBlock);
                    if (pos != null) {
                        player.level().destroyBlock(pos, true);
                        player.level().playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);
                    }
                })
        );
    }

    private static boolean grown(BlockState state) {
        return state.getBlock() instanceof CropBlock crop && crop.getAge(state) > 0;
    }

    private static List<ItemFrame> frames(ServerPlayer player) {
        return player.level().getEntitiesOfClass(ItemFrame.class, new AABB(player.blockPosition()).inflate(12), f -> !f.getItem().isEmpty());
    }
}
