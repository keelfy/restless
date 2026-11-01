package dev.keelfy.restless.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;
import java.util.Arrays;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * What a developed photograph shows: {@link #WIDTH} by {@link #HEIGHT} grey levels, row by row, toned sepia when shown.
 */
public record Picture(byte[] pixels) implements TooltipComponent {
    public static final int WIDTH = 128;
    public static final int HEIGHT = 96;
    public static final int SIZE = WIDTH * HEIGHT;

    public static final Codec<Picture> CODEC = Codec.BYTE_BUFFER.comapFlatMap(
            buffer -> buffer.remaining() == SIZE
                    ? DataResult.success(new Picture(bytes(buffer)))
                    : DataResult.error(() -> "A picture holds " + SIZE + " bytes"),
            picture -> ByteBuffer.wrap(picture.pixels()));
    public static final StreamCodec<ByteBuf, Picture> STREAM_CODEC =
            ByteBufCodecs.byteArray(SIZE).map(Picture::new, Picture::pixels);

    public Picture {
        if (pixels.length != SIZE) {
            throw new IllegalArgumentException("A picture holds " + SIZE + " bytes, not " + pixels.length);
        }
    }

    private static byte[] bytes(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return bytes;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Picture picture && Arrays.equals(pixels, picture.pixels);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(pixels);
    }

    @Override
    public String toString() {
        return "Picture[" + Integer.toHexString(hashCode()) + "]";
    }
}
