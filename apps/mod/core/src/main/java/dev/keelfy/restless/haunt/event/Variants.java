package dev.keelfy.restless.haunt.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** For a block, another kind of the same material: dirt for dirt, stone for stone, so a change never reads as damage to a build. */
final class Variants {
    private static final Map<Block, List<Block>> OF = new HashMap<>();

    static {
        both(Blocks.DIRT, Blocks.COARSE_DIRT);
        both(Blocks.DIRT, Blocks.PODZOL);
        both(Blocks.STONE, Blocks.ANDESITE);
        both(Blocks.STONE, Blocks.GRANITE);
        both(Blocks.STONE, Blocks.DIORITE);
        both(Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE);
        both(Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS);
        both(Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS);
        both(Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE);
        both(Blocks.DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS);
        both(Blocks.DEEPSLATE_TILES, Blocks.CRACKED_DEEPSLATE_TILES);
        OF.put(Blocks.GRASS_BLOCK, List.of(Blocks.DIRT, Blocks.COARSE_DIRT));
    }

    private Variants() {}

    private static void both(Block a, Block b) {
        OF.computeIfAbsent(a, k -> new java.util.ArrayList<>()).add(b);
        OF.computeIfAbsent(b, k -> new java.util.ArrayList<>()).add(a);
    }

    static boolean has(BlockState state) {
        return OF.containsKey(state.getBlock());
    }

    /** A random other kind, or null when the block has none. */
    static @Nullable BlockState of(BlockState state, RandomSource random) {
        List<Block> kinds = OF.get(state.getBlock());
        return kinds == null ? null : kinds.get(random.nextInt(kinds.size())).defaultBlockState();
    }
}
