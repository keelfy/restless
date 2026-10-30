package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Said;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.ArrayList;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

/**
 * Small changes to blocks. The ones that swap a block or an entity put it back after a while through {@link Restore};
 * the rest are something the player can undo by hand (replant, regrow, replace).
 */
final class BlockEvents {
    /** A sign has four lines; the config's texts are shorter and get centred. */
    private static final int SIGN_LINES = 4;
    /** Offset to the player's id for the crack progress packets, so they do not collide with the ones of real miners. */
    private static final int CRACK_ID = 1_000_000;
    private static final Map<Block, Block> INFESTED = Map.of(
        Blocks.STONE, Blocks.INFESTED_STONE,
        Blocks.COBBLESTONE, Blocks.INFESTED_COBBLESTONE,
        Blocks.DEEPSLATE, Blocks.INFESTED_DEEPSLATE,
        Blocks.STONE_BRICKS, Blocks.INFESTED_STONE_BRICKS);

    private BlockEvents() {}

    static List<HauntEvent> all() {
        return List.of(
            // 72
            SimpleEvent.light("wilted_flowers", EventTag.BLOCKS, player -> Nearby.any(player, "wilted_flowers", BlockEvents::pottedFlower), player -> {
                Swaps.show(player, Nearby.all(player, "wilted_flowers", BlockEvents::pottedFlower), Blocks.POTTED_DEAD_BUSH.defaultBlockState(), restoreTicks("wilted_flowers"));
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
            // 110: one plant back to a seedling; 73 takes a stage from every crop
            SimpleEvent.light("lost_growth", EventTag.BLOCKS,
                player -> Nearby.any(player, "lost_growth", BlockEvents::growing),
                player -> {
                    BlockPos pos = Nearby.random(player, "lost_growth", BlockEvents::growing);
                    BlockState state = player.level().getBlockState(pos);
                    player.level().setBlockAndUpdate(pos, state.setValue(age(state), 0));
                    Config.Tone tone = Config.event("lost_growth").tone("sound");
                    player.level().playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.HOSTILE, tone.volume(), tone.pitch());
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
            SimpleEvent.light("new_sign", EventTag.BLOCKS,
                player -> Nearby.any(player, "new_sign", s -> s.getBlock() instanceof SignBlock) && Said.left(player, "new_sign", signTexts("new_sign"), SIGN_KEY),
                BlockEvents::rewriteSign),
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
            // 104: a build changes colour: every block of one kind nearby takes the same neighbouring colour
            SimpleEvent.light("repaint", EventTag.BLOCKS, player -> Nearby.any(player, "repaint", Palette::has), BlockEvents::shiftColours),
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
            SimpleEvent.light("cross", EventTag.BLOCKS, player -> Place.cross(player, crossMin(), crossMax()) != null, BlockEvents::cross),
            // 191: the stone the player looks at turns out to be infested
            SimpleEvent.harmful("infested_stone", EventTag.BLOCKS, player -> infestable(player) != null, BlockEvents::infest),
            // 118: a block overhead cracks as if someone were mining it from above, silently
            SimpleEvent.light("ceiling_crack", EventTag.BLOCKS, player -> ceiling(player, "ceiling_crack") != null, BlockEvents::crackCeiling),
            // 142: water nearby freezes over
            SimpleEvent.light("ice_over", EventTag.BLOCKS, player -> Nearby.any(player, "ice_over", BlockEvents::sourceWater), BlockEvents::freeze),
            // 114: the portal the player is near goes dark, frame and all left standing
            SimpleEvent.harmful("portal_out", EventTag.BLOCKS, player -> Nearby.any(player, "portal_out", s -> s.is(Blocks.NETHER_PORTAL)), BlockEvents::portalOut),
            // 109: the block the player looks at breaks, with its sound and dust, and is back a moment later
            SimpleEvent.light("block_blink", EventTag.BLOCKS, player -> blinking(player) != null, BlockEvents::blink),
            // 183: water drips from the ceiling, only seen and heard, in every mode
            SimpleEvent.light("leak", EventTag.BLOCKS, player -> leaking(player, "leak") != null,
                player -> drip(player, "leak", ParticleTypes.FALLING_WATER, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER)),
            // 183: the same from the ceiling, but it is lava
            SimpleEvent.light("lava_leak", EventTag.BLOCKS, player -> leaking(player, "lava_leak") != null,
                player -> drip(player, "lava_leak", ParticleTypes.FALLING_LAVA, SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA)),
            // 185: a gravestone with the player's name and tomorrow's date stands on open ground nearby
            SimpleEvent.light("mock_grave", EventTag.BLOCKS, player -> marker(player) != null, BlockEvents::markGrave),
            SimpleEvent.light("cave_sign", EventTag.BLOCKS,
                player -> Said.left(player, "cave_sign", signTexts("cave_sign"), SIGN_KEY) && caveWall(player) != null, BlockEvents::caveSign)
        );
    }

    /** The block the player looks at within {@code reach}, if any. */
    private static @Nullable BlockPos looked(ServerPlayer player, double reach) {
        return player.pick(reach, 1.0F, false) instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null;
    }

    private static @Nullable BlockPos infestable(ServerPlayer player) {
        BlockPos pos = looked(player, Config.event("infested_stone").num("reach"));
        return pos != null && INFESTED.containsKey(player.level().getBlockState(pos).getBlock()) ? pos : null;
    }

    private static void infest(ServerPlayer player) {
        BlockPos pos = infestable(player);
        if (pos != null) {
            Block block = player.level().getBlockState(pos).getBlock();
            Swaps.show(player, pos, INFESTED.get(block).defaultBlockState(), restoreTicks("infested_stone"));
        }
    }

    /** The first solid block straight above the player's head within reach of the sight, with air between. */
    private static @Nullable BlockPos ceiling(ServerPlayer player, String event) {
        ServerLevel level = player.level();
        int reach = Config.event(event).integer("reach");
        BlockPos head = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        for (int up = 1; up <= reach; up++) {
            BlockPos pos = head.above(up);
            if (Place.solid(level, pos)) {
                return pos;
            }
        }
        return null;
    }

    private static void crackCeiling(ServerPlayer player) {
        BlockPos pos = ceiling(player, "ceiling_crack");
        if (pos == null) {
            return;
        }
        ServerLevel level = player.level();
        BlockState state = level.getBlockState(pos);
        Config.Section config = Config.event("ceiling_crack");
        int id = player.getId() + CRACK_ID;
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            int stage = n + 1;
            boolean last = stage >= config.integer("stages");
            p.connection.send(new ClientboundBlockDestructionPacket(id, pos, last ? -1 : stage));
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 6, 0.3, 0.0, 0.3, 0.05);
            return !last;
        });
    }

    /** The whole portal breaks for good in every mode, with no swap and no restoring; the frame stays and can be lit again. */
    private static void portalOut(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos first = Nearby.random(player, "portal_out", s -> s.is(Blocks.NETHER_PORTAL));
        if (first == null) {
            return;
        }
        List<BlockPos> open = new ArrayList<>(List.of(first));
        Set<BlockPos> portal = new HashSet<>(open);
        while (!open.isEmpty()) {
            BlockPos at = open.removeLast();
            for (Direction side : Direction.values()) {
                BlockPos next = at.relative(side);
                if (level.getBlockState(next).is(Blocks.NETHER_PORTAL) && portal.add(next)) {
                    open.add(next);
                }
            }
        }
        portal.forEach(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
        Config.Tone tone = Config.event("portal_out").tone("sound");
        level.playSound(null, first, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, tone.volume(), tone.pitch());
    }

    /** A plain block at or above the player's feet (so a break under hardcore never drops them), with nothing inside it. */
    private static @Nullable BlockPos blinking(ServerPlayer player) {
        BlockPos pos = looked(player, Config.event("block_blink").num("reach"));
        if (pos == null || pos.getY() < player.getBlockY()) {
            return null;
        }
        ServerLevel level = player.level();
        return level.getBlockEntity(pos) == null && Place.solid(level, pos) ? pos : null;
    }

    private static void blink(ServerPlayer player) {
        BlockPos pos = blinking(player);
        if (pos != null) {
            ServerLevel level = player.level();
            level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, pos, Block.getId(level.getBlockState(pos)));
            Swaps.show(player, pos, Blocks.AIR.defaultBlockState(), restoreTicks("block_blink"));
        }
    }

    /** The air just under a ceiling within reach, where the drip would come from. */
    private static @Nullable BlockPos leaking(ServerPlayer player, String event) {
        BlockPos roof = ceiling(player, event);
        return roof != null && player.level().isEmptyBlock(roof.below()) ? roof.below() : null;
    }

    /** Drops fall from the ceiling for a while, with their sound; nothing is placed, in any mode. */
    private static void drip(ServerPlayer player, String event, ParticleOptions drop, SoundEvent sound) {
        BlockPos spot = leaking(player, event);
        if (spot == null) {
            return;
        }
        ServerLevel level = player.level();
        Config.Section config = Config.event(event);
        Config.Tone tone = config.tone("sound");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            level.sendParticles(drop, spot.getX() + 0.5, spot.getY() + 0.9, spot.getZ() + 0.5, 2, 0.1, 0.0, 0.1, 0.0);
            if (n % config.integer("sound_every") == 0) {
                level.playSound(null, spot, sound, SoundSource.BLOCKS, tone.volume(), tone.pitch());
            }
            return (n + 1) * config.integer("interval_ticks") < config.integer("ticks");
        });
    }

    private record Marker(BlockPos stone, BlockPos sign, int rotation) {}

    /** Room for a gravestone with a sign in front of it on open ground, where the player can see it. */
    private static @Nullable Marker marker(ServerPlayer player) {
        ServerLevel level = player.level();
        Config.Section config = Config.event("mock_grave");
        for (int i = 0; i < config.integer("tries"); i++) {
            BlockPos stone = Place.spawnSpot(player, config.integer("min_distance"), config.integer("max_distance"));
            if (stone == null) {
                continue;
            }
            for (Direction front : List.of(Direction.SOUTH, Direction.NORTH)) {
                BlockPos sign = stone.relative(front);
                if (level.isEmptyBlock(sign) && level.getBlockState(sign.below()).isSolidRender()) {
                    return new Marker(stone, sign, front.get2DDataValue() * 4);
                }
            }
        }
        return null;
    }

    private static void markGrave(ServerPlayer player) {
        Marker marker = marker(player);
        if (marker == null) {
            return;
        }
        ServerLevel level = player.level();
        level.setBlockAndUpdate(marker.stone(), Content.GRAVESTONE.get().defaultBlockState());
        level.setBlockAndUpdate(marker.sign(), Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, marker.rotation()));
        if (level.getBlockEntity(marker.sign()) instanceof SignBlockEntity sign) {
            SignText.Mutable text = sign.getText(SignTextSlot.FRONT).asMutable();
            text.setLine(0, Component.literal(Config.event("mock_grave").text("title")));
            text.setLine(1, player.getName());
            text.setLine(2, Component.literal(LocalDate.now().plusDays(1).toString()));
            sign.setText(text.asImmutable(), SignTextSlot.FRONT);
        }
        Restore.revert(level, restoreTicks("mock_grave"), force -> {
            if (!force && !level.isLoaded(marker.stone())) {
                return false;
            }
            if (level.getBlockState(marker.stone()).is(Content.GRAVESTONE.get())) {
                level.removeBlock(marker.stone(), false);
            }
            if (level.getBlockState(marker.sign()).is(Blocks.OAK_SIGN)) {
                level.removeBlock(marker.sign(), false);
            }
            return true;
        });
    }

    private static boolean sourceWater(BlockState state) {
        return state.is(Blocks.WATER) && state.getFluidState().isSource();
    }

    private static void freeze(ServerPlayer player) {
        List<BlockPos> water = new ArrayList<>(Nearby.all(player, "ice_over", BlockEvents::sourceWater));
        Collections.shuffle(water, new java.util.Random(player.getRandom().nextLong()));
        Swaps.show(player, water.subList(0, Math.min(water.size(), Config.event("ice_over").integer("max_blocks"))), Blocks.ICE.defaultBlockState(), restoreTicks("ice_over"));
        Config.Tone tone = Config.event("ice_over").tone("sound");
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE, tone.volume(), tone.pitch());
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

    /** A crop, stem, berry bush or cocoa pod past its first stage. */
    private static boolean growing(BlockState state) {
        Block block = state.getBlock();
        return (block instanceof CropBlock || block instanceof StemBlock || block instanceof SweetBerryBushBlock || block instanceof CocoaBlock)
            && state.getValue(age(state)) > 0;
    }

    private static IntegerProperty age(BlockState state) {
        return (IntegerProperty) state.getBlock().getStateDefinition().getProperty("age");
    }

    private static List<ItemFrame> frames(ServerPlayer player) {
        return player.level().getEntitiesOfClass(ItemFrame.class, new AABB(player.blockPosition()).inflate(Nearby.radius("turning_frame")), f -> !f.getItem().isEmpty());
    }

    private static boolean pottedFlower(BlockState state) {
        return state.getBlock() instanceof FlowerPotBlock pot && pot.getPotted().defaultBlockState().is(BlockTags.FLOWERS);
    }

    private static void centre(SignText.Mutable text, List<String> lines) {
        int first = (SIGN_LINES - lines.size()) / 2;
        for (int i = 0; i < SIGN_LINES; i++) {
            text.setLine(i, Component.literal(i >= first && i - first < lines.size() ? lines.get(i - first) : ""));
        }
    }

    private record WallSign(BlockPos pos, Direction facing) {}

    /** Underground only: the first wall met walking out from the player, 3 to {@code radius} blocks away, at head height. */
    private static @Nullable WallSign caveWall(ServerPlayer player) {
        ServerLevel level = player.level();
        // The heightmap rather than sky light: light is recomputed lazily and a roof placed this tick is not in it yet.
        if (level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, player.getBlockX(), player.getBlockZ()) <= player.getBlockY() + 2) {
            return null;
        }
        int radius = Config.event("cave_sign").integer("radius");
        List<Direction> sides = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
        Collections.shuffle(sides, new java.util.Random(player.getRandom().nextLong()));
        for (Direction side : sides) {
            for (int k = 1; k <= radius; k++) {
                BlockPos pos = player.blockPosition().above().relative(side, k);
                if (level.isEmptyBlock(pos)) {
                    continue;
                }
                if (k > 3 && Place.solid(level, pos) && level.isEmptyBlock(pos.relative(side.getOpposite()))) {
                    return new WallSign(pos.relative(side.getOpposite()), side.getOpposite());
                }
                break;
            }
        }
        return null;
    }

    /** A sign's lines as one text, to tell which a spirit has already written. */
    private static final Function<List<String>, String> SIGN_KEY = lines -> String.join("\n", lines);

    private static List<List<String>> signTexts(String id) {
        return Config.event(id).lines("texts");
    }

    private static void caveSign(ServerPlayer player) {
        WallSign found = caveWall(player);
        List<String> lines = found == null ? null : Said.pick(player, "cave_sign", signTexts("cave_sign"), SIGN_KEY);
        if (lines == null) {
            return;
        }
        ServerLevel level = player.level();
        level.setBlockAndUpdate(found.pos(), Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, found.facing()));
        if (level.getBlockEntity(found.pos()) instanceof SignBlockEntity sign) {
            SignText.Mutable text = sign.getText(SignTextSlot.FRONT).asMutable();
            centre(text, lines);
            sign.setText(text.asImmutable(), SignTextSlot.FRONT);
        }
        Restore.revert(level, restoreTicks("cave_sign"), force -> {
            if (!force && !level.isLoaded(found.pos())) {
                return false;
            }
            if (level.getBlockState(found.pos()).is(Blocks.OAK_WALL_SIGN)) {
                level.removeBlock(found.pos(), false);
            }
            return true;
        });
    }

    private static void rewriteSign(ServerPlayer player) {
        BlockPos pos = Nearby.random(player, "new_sign", s -> s.getBlock() instanceof SignBlock);
        ServerLevel level = player.level();
        if (pos == null || !(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) {
            return;
        }
        List<String> lines = Said.pick(player, "new_sign", signTexts("new_sign"), SIGN_KEY);
        if (lines == null) {
            return;
        }
        SignText original = sign.getText(SignTextSlot.FRONT);
        SignText.Mutable edit = original.asMutable();
        centre(edit, lines);
        SignText haunted = edit.asImmutable();
        sign.setText(haunted, SignTextSlot.FRONT);
        Restore.revert(level, restoreTicks("new_sign"), force -> {
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
        Restore.revert(level, restoreTicks("new_painting"), force -> {
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
        Swaps.show(player, List.of(wall.low(), wall.high()), Blocks.GLASS.defaultBlockState(), restoreTicks("glass_wall"));
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
        Map<BlockPos, BlockState> shown = new LinkedHashMap<>();
        for (BlockPos pos : floor) {
            BlockState to = Variants.of(level.getBlockState(pos), player.getRandom());
            if (to != null) {
                shown.put(pos, to);
            }
        }
        Swaps.show(player, shown, restoreTicks("shifting_floor"));
        if (!floor.isEmpty()) {
            Config.Tone tone = Config.event("shifting_floor").tone("sound");
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                level.getBlockState(floor.get(0)).getSoundType().getBreakSound(), SoundSource.BLOCKS, tone.volume(), tone.pitch());
        }
    }

    private static void shiftColours(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos first = Nearby.random(player, "repaint", Palette::has);
        if (first == null) {
            return;
        }
        Block from = level.getBlockState(first).getBlock();
        Block to = Palette.of(from, player.getRandom());
        Config.Section config = Config.event("repaint");
        List<BlockPos> blocks = Nearby.all(player, "repaint", s -> s.is(from)).stream()
            .sorted(Comparator.comparingDouble((BlockPos pos) -> pos.distToCenterSqr(player.position())))
            .limit(config.integer("max_blocks"))
            .toList();
        Map<BlockPos, BlockState> shown = new LinkedHashMap<>();
        blocks.forEach(pos -> shown.put(pos, to.withPropertiesOf(level.getBlockState(pos))));
        Swaps.show(player, shown, restoreTicks("repaint"));
        Config.Tone tone = config.tone("sound");
        level.playSound(null, first, to.defaultBlockState().getSoundType().getPlaceSound(), SoundSource.BLOCKS, tone.volume(), tone.pitch());
    }

    private static void wallHole(ServerPlayer player) {
        Place.Wall wall = Place.wall(player);
        if (wall == null) {
            return;
        }
        ServerLevel level = player.level();
        Config.Tone tone = Config.event("wall_hole").tone("sound");
        level.playSound(null, wall.low(), level.getBlockState(wall.low()).getSoundType().getBreakSound(), SoundSource.BLOCKS, tone.volume(), tone.pitch());
        Swaps.show(player, List.of(wall.low(), wall.high()), Blocks.AIR.defaultBlockState(), restoreTicks("wall_hole"));
    }

    private static void cross(ServerPlayer player) {
        BlockPos centre = Place.cross(player, crossMin(), crossMax());
        if (centre == null) {
            return;
        }
        Swaps.show(player, Place.crossCells(centre), Blocks.BONE_BLOCK.defaultBlockState(), restoreTicks("cross"));
        Config.Tone tone = Config.event("cross").tone("sound");
        player.level().playSound(null, centre, SoundEvents.BONE_BLOCK_PLACE, SoundSource.BLOCKS, tone.volume(), tone.pitch());
    }
}
