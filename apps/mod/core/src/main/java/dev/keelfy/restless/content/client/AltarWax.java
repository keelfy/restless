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
    private static final int PLAIN = 0xFFFFFFFF;
    /** How far a candle's colour is lightened, so the wax reads as wax and not as dye. */
    private static final float PALE = 0.15f;

    @Override
    public int color(BlockState state) {
        return PLAIN;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        Block candle = level.getBlockState(pos.above()).getBlock();
        if (!(candle instanceof CandleBlock) || candle == Blocks.CANDLE) {
            return PLAIN;
        }
        int color = candle.defaultMapColor().col;
        return 0xFF000000 | pale(color >> 16 & 255) << 16 | pale(color >> 8 & 255) << 8 | pale(color & 255);
    }

    private static int pale(int channel) {
        return Math.round(channel + (255 - channel) * PALE);
    }
}
