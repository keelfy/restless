package dev.keelfy.restless.haunt.requiem;

import dev.keelfy.restless.config.Config;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * One Requiem under way: who sings it, where, the candles that are its life bar, and what the player has done that a
 * movement needs to know (bell rung, holy water laid, the demon answered). Lives in memory only, like every rite.
 */
public final class Rite {
    public final ServerPlayer player;
    public final ServerLevel level;
    public final BlockPos altar;
    /** Ticks each movement lasts. */
    public final int length;
    /** The spirit types in the order of the movements. */
    public final List<String> order;

    public final Candles<BlockPos> candles;
    /** Candles a movement lets the player relight or snuff for now; the rite keeps every other one that is out, out. */
    public final Set<BlockPos> free = new HashSet<>();
    /** Ticks into the current movement at which a bell near the altar rang. */
    public final List<Integer> rings = new ArrayList<>();
    /** Where holy water was laid during the current movement. */
    public @Nullable BlockPos blessed;
    /** The demon's answer: true taken, false refused, null none yet. */
    public @Nullable Boolean bargain;
    /** The movement running now, its type, and how far into it the rite is. */
    public @Nullable Movement current;

    public String type = "";
    public int tick;
    /** A movement sets this to end at once, passed. */
    public boolean skip;
    /** Altars standing in for the real one; the real one is {@link #altar}. */
    public final Set<BlockPos> decoys = new HashSet<>();

    /** Which movement the rite is at. */
    public int index;

    public Rite(
            ServerPlayer player,
            ServerLevel level,
            BlockPos altar,
            int length,
            List<String> order,
            List<BlockPos> candles) {
        this.player = player;
        this.level = level;
        this.altar = altar;
        this.length = Math.max(1, length);
        this.order = List.copyOf(order);
        this.candles = new Candles<>(candles, Config.section("requiem").integer("needed"));
    }

    /** How many of the lit candles still burn in the world. */
    public int burning() {
        return (int) candles.lit().stream().filter(this::burns).count();
    }

    public boolean burns(BlockPos pos) {
        return AbstractCandleBlock.isLit(level.getBlockState(pos));
    }

    /** Puts a candle out in the world; true if it was lit. */
    public boolean put(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!AbstractCandleBlock.isLit(state)) {
            return false;
        }
        AbstractCandleBlock.extinguish(null, state, level, pos);
        return true;
    }

    /** Whether the player is within {@code radius} blocks of the altar. */
    public boolean near(int radius) {
        return player.level() == level && player.blockPosition().closerThan(altar, radius);
    }
}
