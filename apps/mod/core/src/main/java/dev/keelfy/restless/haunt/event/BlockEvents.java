package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Restore;
import java.util.List;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Small changes to blocks. The ones that swap a block or an entity put it back after a while through {@link Restore};
 * the rest are something the player can undo by hand (replant, regrow, replace).
 */
final class BlockEvents {
    private static final double WEIGHT = 0.6;
    private static final int WILT_TICKS = 2400;
    private static final int SIGN_TICKS = 1200;
    private static final int PAINTING_TICKS = 1200;
    private static final int GLASS_TICKS = 80;
    private static final int FLOOR_TICKS = 30;
    private static final int HOLE_TICKS = 600;
    private static final int CROSS_TICKS = 1200;
    private static final List<String> SIGNS = List.of("behind you", "i am still here", "do not sleep", "leave while you can", "you are not alone");

    private BlockEvents() {}

    static List<HauntEvent> all() {
        return List.of(
            // 72
            SimpleEvent.light("wilted_flowers", EventTag.BLOCKS, WEIGHT, player -> Nearby.any(player, 10, BlockEvents::pottedFlower), player -> {
                for (BlockPos pos : Nearby.all(player, 10, BlockEvents::pottedFlower)) {
                    Restore.swap(player.level(), pos, Blocks.POTTED_DEAD_BUSH.defaultBlockState(), WILT_TICKS);
                }
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GRASS_BREAK, SoundSource.HOSTILE, 0.8F, 0.5F);
            }),
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
            // 74
            SimpleEvent.light("new_sign", EventTag.BLOCKS, WEIGHT, player -> Nearby.any(player, 10, s -> s.getBlock() instanceof SignBlock), BlockEvents::rewriteSign),
            // 75: another painting of the same size
            SimpleEvent.light("new_painting", EventTag.BLOCKS, WEIGHT, player -> !repaintable(player).isEmpty(), BlockEvents::repaint),
            // 77: a window of glass for four seconds, someone standing behind it
            SimpleEvent.light("glass_wall", EventTag.BLOCKS, WEIGHT,
                player -> player.level().getDifficulty() != Difficulty.PEACEFUL && Place.wall(player) != null, BlockEvents::glassWall),
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
            // 80: only floor that rests on something, so the sand does not fall; it looks and sounds wrong for a second
            SimpleEvent.harmful("shifting_floor", EventTag.BLOCKS, 0.5, player -> player.onGround() && floorUnder(player).size() > 1, BlockEvents::shiftFloor),
            // 81
            SimpleEvent.harmful("broken_bed", EventTag.BLOCKS, WEIGHT,
                player -> Nearby.any(player, 16, s -> s.getBlock() instanceof BedBlock),
                player -> {
                    BlockPos pos = Nearby.random(player, 16, s -> s.getBlock() instanceof BedBlock);
                    if (pos != null) {
                        player.level().destroyBlock(pos, true);
                        player.level().playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);
                    }
                }),
            // 82: two blocks of an outer wall vanish for half a minute
            SimpleEvent.harmful("wall_hole", EventTag.BLOCKS, WEIGHT, player -> Place.wall(player) != null, BlockEvents::wallHole),
            // 83: a plus of bone blocks lies on the ground a few blocks away
            SimpleEvent.light("cross", EventTag.BLOCKS, WEIGHT, player -> Place.cross(player, 3, 6) != null, BlockEvents::cross)
        );
    }

    private static boolean grown(BlockState state) {
        return state.getBlock() instanceof CropBlock crop && crop.getAge(state) > 0;
    }

    private static List<ItemFrame> frames(ServerPlayer player) {
        return player.level().getEntitiesOfClass(ItemFrame.class, new AABB(player.blockPosition()).inflate(12), f -> !f.getItem().isEmpty());
    }

    private static boolean pottedFlower(BlockState state) {
        return state.getBlock() instanceof FlowerPotBlock pot && pot.getPotted().defaultBlockState().is(BlockTags.FLOWERS);
    }

    private static void rewriteSign(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, 10, s -> s.getBlock() instanceof SignBlock);
        ServerLevel level = player.level();
        if (pos == null || !(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) {
            return;
        }
        SignText original = sign.getText(SignTextSlot.FRONT);
        SignText.Mutable edit = original.asMutable();
        for (int i = 0; i < 4; i++) {
            edit.setLine(i, Component.literal(i == 1 ? SIGNS.get(player.getRandom().nextInt(SIGNS.size())) : ""));
        }
        SignText haunted = edit.asImmutable();
        sign.setText(haunted, SignTextSlot.FRONT);
        Restore.after(level, SIGN_TICKS, force -> {
            if (!force && !level.isLoaded(pos)) {
                return false;
            }
            // A sign the player has since edited no longer reads as ours and is left alone.
            if (level.getBlockEntity(pos) instanceof SignBlockEntity now && now.getText(SignTextSlot.FRONT).equals(haunted)) {
                now.setText(original, SignTextSlot.FRONT);
            }
            return true;
        });
    }

    private static List<Painting> repaintable(ServerPlayer player) {
        return player.level().getEntitiesOfClass(Painting.class, new AABB(player.blockPosition()).inflate(12), p -> !others(player.level(), p).isEmpty());
    }

    private static List<Holder.Reference<PaintingVariant>> others(ServerLevel level, Painting painting) {
        PaintingVariant now = painting.getVariant().value();
        return level.registryAccess().lookupOrThrow(Registries.PAINTING_VARIANT).listElements()
            .filter(v -> v.value().width() == now.width() && v.value().height() == now.height() && !v.equals(painting.getVariant()))
            .toList();
    }

    private static void repaint(ServerPlayer player) {
        List<Painting> paintings = repaintable(player);
        if (paintings.isEmpty()) {
            return;
        }
        ServerLevel level = player.level();
        Painting old = paintings.get(player.getRandom().nextInt(paintings.size()));
        Holder<PaintingVariant> original = old.getVariant();
        List<Holder.Reference<PaintingVariant>> options = others(level, old);
        Painting fresh = replace(level, old, options.get(player.getRandom().nextInt(options.size())));
        Restore.after(level, PAINTING_TICKS, force -> {
            if (!fresh.isRemoved()) {
                replace(level, fresh, original);
            }
            return true;
        });
    }

    private static Painting replace(ServerLevel level, Painting old, Holder<PaintingVariant> variant) {
        Painting fresh = new Painting(level, old.getPos(), old.getDirection(), variant);
        old.discard();
        level.addFreshEntity(fresh);
        return fresh;
    }

    private static void glassWall(ServerPlayer player) {
        Place.Wall wall = Place.wall(player);
        if (wall == null) {
            return;
        }
        ServerLevel level = player.level();
        Restore.swap(level, wall.low(), Blocks.GLASS.defaultBlockState(), GLASS_TICKS);
        Restore.swap(level, wall.high(), Blocks.GLASS.defaultBlockState(), GLASS_TICKS);
        level.playSound(null, wall.low(), SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 0.6F);
        Husk figure = EntityTypes.HUSK.spawn(level, wall.behind(), EntitySpawnReason.EVENT);
        if (figure != null) {
            figure.setNoAi(true);
            figure.setSilent(true);
            figure.lookAt(EntityAnchorArgument.Anchor.EYES, player.getEyePosition());
            figure.setYHeadRot(figure.getYRot());
            Restore.after(level, GLASS_TICKS, force -> {
                figure.discard();
                return true;
            });
        }
    }

    /** The block under the player and the ones around it that can turn to sand without falling. */
    private static List<BlockPos> floorUnder(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos centre = player.blockPosition().below();
        List<BlockPos> floor = new java.util.ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-1, 0, -1), centre.offset(1, 0, 1))) {
            BlockState state = level.getBlockState(pos);
            if (Place.solid(level, pos) && state.isCollisionShapeFullBlock(level, pos) && !state.is(BlockTags.SAND)
                && !state.is(Blocks.GRAVEL) && level.getBlockState(pos.below()).isSolidRender()) {
                floor.add(pos.immutable());
            }
        }
        return floor.contains(centre) ? floor : List.of();
    }

    private static void shiftFloor(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockState to = (player.getRandom().nextBoolean() ? Blocks.SAND : Blocks.GRAVEL).defaultBlockState();
        for (BlockPos pos : floorUnder(player)) {
            Restore.swap(level, pos, to, FLOOR_TICKS);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), to.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 0.7F);
    }

    private static void wallHole(ServerPlayer player) {
        Place.Wall wall = Place.wall(player);
        if (wall == null) {
            return;
        }
        ServerLevel level = player.level();
        level.playSound(null, wall.low(), level.getBlockState(wall.low()).getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 0.6F);
        Restore.swap(level, wall.low(), Blocks.AIR.defaultBlockState(), HOLE_TICKS);
        Restore.swap(level, wall.high(), Blocks.AIR.defaultBlockState(), HOLE_TICKS);
    }

    private static void cross(ServerPlayer player) {
        BlockPos centre = Place.cross(player, 3, 6);
        if (centre == null) {
            return;
        }
        for (BlockPos pos : Place.crossCells(centre)) {
            Restore.swap(player.level(), pos, Blocks.BONE_BLOCK.defaultBlockState(), CROSS_TICKS);
        }
        player.level().playSound(null, centre, SoundEvents.BONE_BLOCK_PLACE, SoundSource.BLOCKS, 1.0F, 0.6F);
    }
}
