package dev.keelfy.restless.content.client;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;

/** The wax that pooled on an altar stand takes the colour of the candle burning on it; bare, it is plain wax. */
public final class AltarWax implements BlockTintSource {
    /** Bare wax is a dull cream, never white. */
    private static final int PLAIN = 0xFFE4D8B4;
    /** How far a candle's colour is lightened, so the wax reads as wax and not as dye. */
    private static final float PALE = 0.15f;

    @Override
    public int color(BlockState state) {
        return PLAIN;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        Block candle = level.getBlockState(pos.above()).getBlock();
        int color = PLAIN;
        if (candle instanceof CandleBlock && candle != Blocks.CANDLE) {
            int map = candle.defaultMapColor().col;
            color = 0xFF000000 | pale(map >> 16 & 255) << 16 | pale(map >> 8 & 255) << 8 | pale(map & 255);
        }
        return shade(color, pos);
    }

    private static int pale(int channel) {
        return Math.round(channel + (255 - channel) * PALE);
    }

    /** Each stand's wax is a touch lighter or darker, so a row of stands does not look stamped. */
    private static int shade(int color, BlockPos pos) {
        float k = 0.82f + (Math.floorMod(pos.hashCode() * 0x9E3779B1, 100) / 100f) * 0.18f;
        return 0xFF000000
                | Math.round((color >> 16 & 255) * k) << 16
                | Math.round((color >> 8 & 255) * k) << 8
                | Math.round((color & 255) * k);
    }
}
