package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
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
    /** A sign has four lines; the config's texts are shorter and get centred. */
    private static final int SIGN_LINES = 4;

    private BlockEvents() {}

    static List<HauntEvent> all() {
        return List.of(
            // 72
            SimpleEvent.light("wilted_flowers", EventTag.BLOCKS, player -> Nearby.any(player, "wilted_flowers", BlockEvents::pottedFlower), player -> {
                for (BlockPos pos : Nearby.all(player, "wilted_flowers", BlockEvents::pottedFlower)) {
                    Restore.swap(player.level(), pos, Blocks.POTTED_DEAD_BUSH.defaultBlockState(), restoreTicks("wilted_flowers"));
                }
                Config.Tone tone = Config.event("wilted_flowers").tone("sound");
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GRASS_BREAK, SoundSource.HOSTILE, tone.volume(), tone.pitch());
            }),
            // 73
            SimpleEvent.light("withering_crops", EventTag.BLOCKS,
                player -> Nearby.any(player, "withering_crops", BlockEvents::grown),
                player -> {
                    ServerLevel level = player.level();
                    for (BlockPos pos : Nearby.all(player, "withering_crops", BlockEvents::grown)) {
                        BlockState state = level.getBlockState(pos);
                        CropBlock crop = (CropBlock) state.getBlock();
                        level.setBlockAndUpdate(pos, crop.getStateForAge(crop.getAge(state) - 1));
                    }
                }),
            // 76
            SimpleEvent.light("turning_frame", EventTag.BLOCKS,
                player -> !frames(player).isEmpty(),
                player -> {
                    List<ItemFrame> frames = frames(player);
                    ItemFrame frame = frames.get(player.getRandom().nextInt(frames.size()));
                    Config.Section config = Config.event("turning_frame");
                    frame.setRotation(frame.getRotation() + config.integer("min_turns") + player.getRandom().nextInt(config.integer("turn_spread")));
                }),
            // 74
            SimpleEvent.light("new_sign", EventTag.BLOCKS, player -> Nearby.any(player, "new_sign", s -> s.getBlock() instanceof SignBlock), BlockEvents::rewriteSign),
            // 75: another painting of the same size
            SimpleEvent.light("new_painting", EventTag.BLOCKS, player -> !repaintable(player).isEmpty(), BlockEvents::repaint),
            // 77: a window of glass for a few seconds, someone standing behind it
            SimpleEvent.light("glass_wall", EventTag.BLOCKS,
                player -> player.level().getDifficulty() != Difficulty.PEACEFUL && Place.wall(player) != null, BlockEvents::glassWall),
            // 78
            SimpleEvent.light("dry_cauldron", EventTag.BLOCKS,
                player -> Nearby.any(player, "dry_cauldron", s -> s.is(Blocks.WATER_CAULDRON)),
                player -> {
                    BlockPos pos = Nearby.random(player, "dry_cauldron", s -> s.is(Blocks.WATER_CAULDRON));
                    if (pos != null) {
                        player.level().setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
                    }
                }),
            // 79: a scatter of grass around the player turns to dirt
            SimpleEvent.light("dead_ground", EventTag.BLOCKS,
                player -> player.onGround() && player.level().getBlockState(player.blockPosition().below()).is(Blocks.GRASS_BLOCK),
                player -> {
                    ServerLevel level = player.level();
                    Config.Section config = Config.event("dead_ground");
                    BlockPos centre = player.blockPosition().below();
                    for (BlockPos pos : Place.patch(player, centre, config.integer("patch_radius"), config.num("density"),
                        p -> level.getBlockState(p).is(Blocks.GRASS_BLOCK))) {
                        level.setBlockAndUpdate(pos, Variants.of(level.getBlockState(pos), player.getRandom()));
                    }
                }),
            // 80: the floor flickers into another kind of the same material for a moment
            SimpleEvent.harmful("shifting_floor", EventTag.BLOCKS, player -> player.onGround() && shiftable(player.level(), player.blockPosition().below()), BlockEvents::shiftFloor),
            // 81
            SimpleEvent.harmful("broken_bed", EventTag.BLOCKS,
                player -> Nearby.any(player, "broken_bed", s -> s.getBlock() instanceof BedBlock),
                player -> {
                    BlockPos pos = Nearby.random(player, "broken_bed", s -> s.getBlock() instanceof BedBlock);
                    if (pos != null) {
                        player.level().destroyBlock(pos, true);
                        Config.Tone tone = Config.event("broken_bed").tone("sound");
                        player.level().playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, tone.volume(), tone.pitch());
                    }
                }),
            // 82: two blocks of an outer wall vanish for a while
            SimpleEvent.harmful("wall_hole", EventTag.BLOCKS, player -> Place.wall(player) != null, BlockEvents::wallHole),
            // 83: a plus of bone blocks lies on the ground a few blocks away
            SimpleEvent.light("cross", EventTag.BLOCKS, player -> Place.cross(player, crossMin(), crossMax()) != null, BlockEvents::cross)
        );
    }

    /** How long an event's change lasts before it is put back. */
    private static int restoreTicks(String event) {
        return Config.event(event).integer("restore_ticks");
    }

    private static int crossMin() {
        return Config.event("cross").integer("min_distance");
    }

    private static int crossMax() {
        return Config.event("cross").integer("max_distance");
    }

    private static boolean grown(BlockState state) {
        return state.getBlock() instanceof CropBlock crop && crop.getAge(state) > 0;
    }

    private static List<ItemFrame> frames(ServerPlayer player) {
        return player.level().getEntitiesOfClass(ItemFrame.class, new AABB(player.blockPosition()).inflate(Nearby.radius("turning_frame")), f -> !f.getItem().isEmpty());
    }

    private static boolean pottedFlower(BlockState state) {
        return state.getBlock() instanceof FlowerPotBlock pot && pot.getPotted().defaultBlockState().is(BlockTags.FLOWERS);
    }

    private static void rewriteSign(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, "new_sign", s -> s.getBlock() instanceof SignBlock);
        ServerLevel level = player.level();
        if (pos == null || !(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) {
            return;
        }
        SignText original = sign.getText(SignTextSlot.FRONT);
        SignText.Mutable edit = original.asMutable();
        List<List<String>> signs = Config.event("new_sign").lines("texts");
        List<String> lines = signs.get(player.getRandom().nextInt(signs.size()));
        int first = (SIGN_LINES - lines.size()) / 2;
        for (int i = 0; i < SIGN_LINES; i++) {
            edit.setLine(i, Component.literal(i >= first && i - first < lines.size() ? lines.get(i - first) : ""));
        }
        SignText haunted = edit.asImmutable();
        sign.setText(haunted, SignTextSlot.FRONT);
        Restore.after(level, restoreTicks("new_sign"), force -> {
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
        return player.level().getEntitiesOfClass(Painting.class, new AABB(player.blockPosition()).inflate(Nearby.radius("new_painting")), p -> !others(player.level(), p).isEmpty());
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
        Restore.after(level, restoreTicks("new_painting"), force -> {
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
        Restore.swap(level, wall.low(), Blocks.GLASS.defaultBlockState(), restoreTicks("glass_wall"));
        Restore.swap(level, wall.high(), Blocks.GLASS.defaultBlockState(), restoreTicks("glass_wall"));
        Config.Tone tone = Config.event("glass_wall").tone("sound");
        level.playSound(null, wall.low(), SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, tone.volume(), tone.pitch());
        Husk figure = EntityTypes.HUSK.spawn(level, wall.behind(), EntitySpawnReason.EVENT);
        if (figure != null) {
            figure.setNoAi(true);
            figure.setSilent(true);
            figure.lookAt(EntityAnchorArgument.Anchor.EYES, player.getEyePosition());
            figure.setYHeadRot(figure.getYRot());
            Restore.after(level, restoreTicks("glass_wall"), force -> {
                figure.discard();
                return true;
            });
        }
    }

    /** A ragged patch of the floor around the player whose blocks have another kind to turn into. */
    private static List<BlockPos> floorUnder(ServerPlayer player) {
        ServerLevel level = player.level();
        Config.Section config = Config.event("shifting_floor");
        return Place.patch(player, player.blockPosition().below(), config.integer("patch_radius"), config.num("density"), pos -> shiftable(level, pos));
    }

    private static boolean shiftable(ServerLevel level, BlockPos pos) {
        return Variants.has(level.getBlockState(pos)) && !level.getBlockState(pos).hasBlockEntity();
    }

    private static void shiftFloor(ServerPlayer player) {
        ServerLevel level = player.level();
        List<BlockPos> floor = floorUnder(player);
        for (BlockPos pos : floor) {
            BlockState to = Variants.of(level.getBlockState(pos), player.getRandom());
            if (to != null) {
                Restore.swap(level, pos, to, restoreTicks("shifting_floor"));
            }
        }
        if (!floor.isEmpty()) {
            Config.Tone tone = Config.event("shifting_floor").tone("sound");
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                level.getBlockState(floor.get(0)).getSoundType().getBreakSound(), SoundSource.BLOCKS, tone.volume(), tone.pitch());
        }
    }

    private static void wallHole(ServerPlayer player) {
        Place.Wall wall = Place.wall(player);
        if (wall == null) {
            return;
        }
        ServerLevel level = player.level();
        Config.Tone tone = Config.event("wall_hole").tone("sound");
        level.playSound(null, wall.low(), level.getBlockState(wall.low()).getSoundType().getBreakSound(), SoundSource.BLOCKS, tone.volume(), tone.pitch());
        Restore.swap(level, wall.low(), Blocks.AIR.defaultBlockState(), restoreTicks("wall_hole"));
        Restore.swap(level, wall.high(), Blocks.AIR.defaultBlockState(), restoreTicks("wall_hole"));
    }

    private static void cross(ServerPlayer player) {
        BlockPos centre = Place.cross(player, crossMin(), crossMax());
        if (centre == null) {
            return;
        }
        for (BlockPos pos : Place.crossCells(centre)) {
            Restore.swap(player.level(), pos, Blocks.BONE_BLOCK.defaultBlockState(), restoreTicks("cross"));
        }
        Config.Tone tone = Config.event("cross").tone("sound");
        player.level().playSound(null, centre, SoundEvents.BONE_BLOCK_PLACE, SoundSource.BLOCKS, tone.volume(), tone.pitch());
    }
}
