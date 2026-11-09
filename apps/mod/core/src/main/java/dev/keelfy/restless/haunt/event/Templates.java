package dev.keelfy.restless.haunt.event;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Restore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Small structures built from layers of text in the event's config, put down in the world by an event and taken down
 * again block by block: a block goes back only if it is still what the template put there, so whatever the player has
 * changed, built on or taken away is left alone. A chest that the player has put something into stays.
 */
final class Templates {
    private static final int SPOT_TRIES = 6;
    private static final Rotation[] TURNS = Rotation.values();

    private Templates() {}

    /** One block of a template, relative to its middle; {@code slot} names where an event fills a block entity. */
    record Piece(int x, int y, int z, BlockState state, String slot) {}

    record Template(int width, int depth, List<Piece> pieces) {}

    /** A block of a template laid on the ground; {@code grounded} ones need solid ground under them. */
    record Cell(BlockPos pos, BlockState state, String slot, boolean grounded) {}

    private record Laid(BlockPos pos, BlockState placed, BlockState before) {}

    /**
     * The {@code template} of an event's config: {@code layers}, bottom first, each a list of rows, where a space or
     * dot is nothing and any other character is looked up in {@code legend} (character to block state, as in a command,
     * e.g. {@code minecraft:chest[facing=north]}); the characters in {@code slots} name their block for {@link #put}.
     * Read afresh on each call, so a reloaded config shows at once; a bad template fails naming its event.
     */
    static Template load(String event) {
        try {
            Config.Section section = Config.event(event).section("template");
            Config.Section legend = section.section("legend");
            Map<Character, BlockState> key = new HashMap<>();
            for (String name : legend.keys()) {
                if (name.length() != 1) {
                    throw new IllegalStateException("a legend key is one character, not '" + name + "'");
                }
                key.put(name.charAt(0), state(legend.text(name)));
            }
            List<List<String>> layers = section.lines("layers");
            return parse(
                    key,
                    section.has("slots") ? section.text("slots") : "",
                    layers.stream().map(rows -> rows.toArray(String[]::new)).toArray(String[][]::new));
        } catch (RuntimeException e) {
            throw new IllegalStateException("Bad template for event " + event + ": " + e.getMessage(), e);
        }
    }

    private static BlockState state(String text) {
        try {
            return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, text, false)
                    .blockState();
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException("'" + text + "' is no block state: " + e.getMessage(), e);
        }
    }

    private static Template parse(Map<Character, BlockState> key, String slots, String[]... layers) {
        List<Piece> pieces = new ArrayList<>();
        int width = 0;
        int depth = 0;
        for (int y = 0; y < layers.length; y++) {
            depth = Math.max(depth, layers[y].length);
            for (int z = 0; z < layers[y].length; z++) {
                String row = layers[y][z];
                width = Math.max(width, row.length());
                for (int x = 0; x < row.length(); x++) {
                    char c = row.charAt(x);
                    if (c != ' ' && c != '.') {
                        BlockState state = key.get(c);
                        if (state == null) {
                            throw new IllegalStateException("'" + c + "' is not in the legend");
                        }
                        String slot = slots.indexOf(c) >= 0 ? String.valueOf(c) : "";
                        pieces.add(new Piece(x, y, z, state, slot));
                    }
                }
            }
        }
        return new Template(width, depth, pieces);
    }

    /** The template's blocks with its middle at {@code origin}, turned a quarter at a time. */
    static List<Cell> lay(Template template, BlockPos origin, Rotation turn) {
        List<Cell> cells = new ArrayList<>();
        for (Piece piece : template.pieces()) {
            int x = piece.x() - template.width() / 2;
            int z = piece.z() - template.depth() / 2;
            int dx;
            int dz;
            switch (turn) {
                case CLOCKWISE_90 -> {
                    dx = -z;
                    dz = x;
                }
                case CLOCKWISE_180 -> {
                    dx = -x;
                    dz = -z;
                }
                case COUNTERCLOCKWISE_90 -> {
                    dx = z;
                    dz = -x;
                }
                default -> {
                    dx = x;
                    dz = z;
                }
            }
            cells.add(new Cell(
                    origin.offset(dx, piece.y(), dz), piece.state().rotate(turn), piece.slot(), piece.y() == 0));
        }
        return cells;
    }

    /** Every cell is loaded, free to build in, and the bottom ones stand on solid ground. */
    static boolean fits(ServerLevel level, List<Cell> cells) {
        Set<BlockPos> own = new HashSet<>();
        cells.forEach(cell -> own.add(cell.pos()));
        for (Cell cell : cells) {
            BlockPos pos = cell.pos();
            if (!level.isLoaded(pos) || !Place.open(level, pos)) {
                return false;
            }
            if (cell.grounded()
                    && !own.contains(pos.below())
                    && !level.getBlockState(pos.below()).isSolidRender()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Somewhere {@code min} to {@code max} blocks from the player where the template fits, turned any way; null when a
     * few tries find nothing.
     */
    static @Nullable List<Cell> find(ServerPlayer player, Template template, int min, int max) {
        for (int i = 0; i < SPOT_TRIES; i++) {
            BlockPos spot = Place.spawnSpot(player, min, max);
            if (spot == null) {
                continue;
            }
            List<Cell> cells = lay(template, spot, TURNS[player.getRandom().nextInt(TURNS.length)]);
            if (fits(player.level(), cells)) {
                return cells;
            }
        }
        return null;
    }

    /**
     * Builds the cells, hands each block entity to the filler of its slot, and takes every block down again after
     * {@code delay} ticks (never under hardcore). The blocks are set without neighbour updates, so nothing pops off.
     */
    static void put(ServerLevel level, List<Cell> cells, Map<String, Consumer<BlockEntity>> fills, int delay) {
        List<Laid> laid = new ArrayList<>();
        for (Cell cell : cells) {
            BlockPos pos = cell.pos().immutable();
            laid.add(0, new Laid(pos, cell.state(), level.getBlockState(pos)));
            Restore.quiet(level, pos, cell.state());
            Consumer<BlockEntity> fill = fills.get(cell.slot());
            BlockEntity entity = level.getBlockEntity(pos);
            if (fill != null && entity != null) {
                fill.accept(entity);
                entity.setChanged();
            }
        }
        Restore.revert(level, delay, force -> {
            laid.removeIf(one -> {
                if (!force && !level.isLoaded(one.pos())) {
                    return false;
                }
                if (level.getBlockState(one.pos()).is(one.placed().getBlock()) && !kept(level, one.pos())) {
                    Restore.quiet(level, one.pos(), one.before());
                }
                return true;
            });
            return laid.isEmpty();
        });
    }

    /** A container with something in it other than a book the event wrote is the player's now: it stays. */
    private static boolean kept(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof Container container)) {
            return false;
        }
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty() && !stack.is(Items.WRITTEN_BOOK)) {
                return true;
            }
        }
        container.clearContent();
        return false;
    }
}
