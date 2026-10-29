package dev.keelfy.restless.content;

import dev.keelfy.restless.platform.Platform;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Non-emitting counterparts retain placement, support and waterlogging properties. */
public final class UnlitLights {
    private record Entry(Block source, Supplier<Block> unlit) {}
    private static final List<Entry> ENTRIES = new ArrayList<>();
    public static final List<Supplier<Item>> ITEMS = new ArrayList<>();

    static {
        for (String type : List.of("torch", "soul_torch", "copper_torch", "redstone_torch")) {
            String wall = type.replace("torch", "wall_torch");
            Supplier<Block> standing = register(type, false, false);
            Supplier<Block> mounted = register(wall, true, false);
            ITEMS.add(Platform.get().register(BuiltInRegistries.ITEM, "unlit_" + type,
                key -> new StandingAndWallBlockItem(standing.get(), mounted.get(), Direction.DOWN,
                    new Item.Properties().setId(key).useBlockDescriptionPrefix())));
        }
        for (String type : List.of("lantern", "soul_lantern", "copper_lantern", "exposed_copper_lantern",
                "weathered_copper_lantern", "oxidized_copper_lantern", "waxed_copper_lantern",
                "waxed_exposed_copper_lantern", "waxed_weathered_copper_lantern", "waxed_oxidized_copper_lantern")) {
            Supplier<Block> block = register(type, false, true);
            ITEMS.add(Platform.get().register(BuiltInRegistries.ITEM, "unlit_" + type,
                key -> new BlockItem(block.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix())));
        }
    }

    private UnlitLights() {}

    private static Supplier<Block> register(String name, boolean wall, boolean lantern) {
        Block original = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(name));
        Supplier<Block> result = Platform.get().register(BuiltInRegistries.BLOCK, "unlit_" + name, key -> {
            var properties = BlockBehaviour.Properties.ofLegacyCopy(original).setId(key).lightLevel(state -> 0);
            if (lantern) return new LanternBlock(properties);
            return wall ? new Wall(properties) : new Standing(properties);
        });
        ENTRIES.add(new Entry(original, result));
        return result;
    }

    public static boolean isTorch(BlockState state) {
        return ENTRIES.stream().anyMatch(e -> e.source() == state.getBlock() && e.source() instanceof BaseTorchBlock);
    }

    public static boolean isLantern(BlockState state) {
        return ENTRIES.stream().anyMatch(e -> e.source() == state.getBlock() && e.source() instanceof LanternBlock);
    }

    public static BlockState extinguished(BlockState state) {
        for (Entry entry : ENTRIES) {
            if (state.is(entry.source())) return entry.unlit().get().withPropertiesOf(state);
        }
        return state;
    }

    public static void extinguish(Level level, BlockPos pos) {
        level.setBlock(pos, extinguished(level.getBlockState(pos)), Block.UPDATE_ALL);
    }

    private static final class Standing extends TorchBlock {
        Standing(BlockBehaviour.Properties properties) { super(ParticleTypes.FLAME, properties); }
        @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {}
    }

    private static final class Wall extends WallTorchBlock {
        Wall(BlockBehaviour.Properties properties) { super(ParticleTypes.FLAME, properties); }
        @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {}
    }
}
