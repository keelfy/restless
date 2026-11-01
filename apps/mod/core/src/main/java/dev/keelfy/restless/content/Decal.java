package dev.keelfy.restless.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A flat picture on the face of a wall, door or window: frost, claw marks, a cracked mirror. It stands in the air block
 * in front of the surface, with {@link #FACING} pointing at the surface and {@link #DEPTH} telling how far behind the
 * edge of its own block that surface lies (a pane or a door is thinner than a block). Left by the haunt events, never
 * crafted: no item, it drops nothing.
 */
public class Decal extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Depth> DEPTH = EnumProperty.create("depth", Depth.class);

    /** How many sixteenths the surface is set back from the edge of the decal's own block. */
    public enum Depth implements StringRepresentable {
        FLUSH("flush", 0),
        PANE("pane", 7),
        DOOR("door", 13);

        private final String name;
        private final int pixels;

        Depth(String name, int pixels) {
            this.name = name;
            this.pixels = pixels;
        }

        public int pixels() {
            return pixels;
        }

        /** The depth closest to a gap measured in blocks. */
        public static Depth nearest(double gap) {
            Depth best = FLUSH;
            for (Depth depth : values()) {
                if (Math.abs(depth.pixels / 16.0 - gap) < Math.abs(best.pixels / 16.0 - gap)) {
                    best = depth;
                }
            }
            return best;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public Decal(BlockBehaviour.Properties properties) {
        super(properties
                .noCollision()
                .noOcclusion()
                .instabreak()
                .replaceable()
                .sound(SoundType.GLASS)
                .pushReaction(PushReaction.POPPED));
        registerDefaultState(
                stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(DEPTH, Depth.FLUSH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, DEPTH);
    }

    /** One sixteenth thick against the surface's side, which is as far as the block itself reaches. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> Block.box(0, 0, 15, 16, 16, 16);
            case EAST -> Block.box(15, 0, 0, 16, 16, 16);
            case WEST -> Block.box(0, 0, 0, 1, 16, 16);
            default -> Block.box(0, 0, 0, 16, 16, 1);
        };
    }

    /** Frost writing: one word per block, set side by side along a window. */
    public static final class Writing extends Decal {
        public static final EnumProperty<Word> WORD = EnumProperty.create("word", Word.class);

        /** The words the glyph textures spell, see {@code frost_writing_<word>.png}. */
        public enum Word implements StringRepresentable {
            LET,
            ME,
            IN,
            COLD,
            HELP,
            OPEN;

            @Override
            public String getSerializedName() {
                return name().toLowerCase(java.util.Locale.ROOT);
            }
        }

        public Writing(BlockBehaviour.Properties properties) {
            super(properties);
            registerDefaultState(defaultBlockState().setValue(WORD, Word.LET));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(WORD);
        }
    }

    /** A cracked mirror; when {@link #FIGURE} is set someone stands in it who is not in the room. */
    public static final class Mirror extends Decal {
        public static final BooleanProperty FIGURE = BooleanProperty.create("figure");

        public Mirror(BlockBehaviour.Properties properties) {
            super(properties);
            registerDefaultState(defaultBlockState().setValue(FIGURE, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(FIGURE);
        }
    }
}
