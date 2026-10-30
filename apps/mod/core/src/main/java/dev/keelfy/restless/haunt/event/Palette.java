package dev.keelfy.restless.haunt.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColorCollection;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * For a block, the one next to it in colour: a dyed block (wool, concrete, terracotta, stained glass; stairs and slabs
 * of the first two too) takes a neighbouring dye of the same family, planks turn into the stripped log of their wood,
 * stone bricks into plain stone. The facing, half and shape of the old block carry over to the new one.
 */
final class Palette {
    /** Dyes in a ring from light to dark and round the hues, so a step either way stays close to the original colour. */
    private static final List<DyeColor> DYES = List.of(DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.GRAY, DyeColor.BLACK,
        DyeColor.BROWN, DyeColor.RED, DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.LIME, DyeColor.GREEN, DyeColor.CYAN,
        DyeColor.LIGHT_BLUE, DyeColor.BLUE, DyeColor.PURPLE, DyeColor.MAGENTA, DyeColor.PINK);
    private static final List<ColorCollection<Block>> DYED = List.of(Blocks.WOOL, Blocks.CONCRETE, Blocks.DYED_TERRACOTTA,
        Blocks.STAINED_GLASS, Blocks.WOOL_STAIRS, Blocks.WOOL_SLAB, Blocks.CONCRETE_STAIRS, Blocks.CONCRETE_SLAB);
    private static final Map<Block, List<Block>> NEXT = new HashMap<>();

    static {
        for (ColorCollection<Block> family : DYED) {
            for (int i = 0; i < DYES.size(); i++) {
                Block block = family.pick(DYES.get(i));
                add(block, family.pick(DYES.get(Math.floorMod(i - 1, DYES.size()))));
                add(block, family.pick(DYES.get((i + 1) % DYES.size())));
            }
        }
        add(Blocks.OAK_PLANKS, Blocks.STRIPPED_OAK_LOG);
        add(Blocks.SPRUCE_PLANKS, Blocks.STRIPPED_SPRUCE_LOG);
        add(Blocks.BIRCH_PLANKS, Blocks.STRIPPED_BIRCH_LOG);
        add(Blocks.JUNGLE_PLANKS, Blocks.STRIPPED_JUNGLE_LOG);
        add(Blocks.ACACIA_PLANKS, Blocks.STRIPPED_ACACIA_LOG);
        add(Blocks.DARK_OAK_PLANKS, Blocks.STRIPPED_DARK_OAK_LOG);
        add(Blocks.MANGROVE_PLANKS, Blocks.STRIPPED_MANGROVE_LOG);
        add(Blocks.CHERRY_PLANKS, Blocks.STRIPPED_CHERRY_LOG);
        add(Blocks.PALE_OAK_PLANKS, Blocks.STRIPPED_PALE_OAK_LOG);
        add(Blocks.CRIMSON_PLANKS, Blocks.STRIPPED_CRIMSON_STEM);
        add(Blocks.WARPED_PLANKS, Blocks.STRIPPED_WARPED_STEM);
        add(Blocks.BAMBOO_PLANKS, Blocks.STRIPPED_BAMBOO_BLOCK);
        add(Blocks.STONE_BRICKS, Blocks.STONE);
    }

    private Palette() {}

    private static void add(Block from, Block to) {
        NEXT.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
    }

    static boolean has(BlockState state) {
        return NEXT.containsKey(state.getBlock());
    }

    /** A random neighbouring colour, or null when the block has none. */
    static @Nullable Block of(Block block, RandomSource random) {
        List<Block> kinds = NEXT.get(block);
        return kinds == null ? null : kinds.get(random.nextInt(kinds.size()));
    }
}
