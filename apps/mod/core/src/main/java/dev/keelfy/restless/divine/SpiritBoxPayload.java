package dev.keelfy.restless.divine;

import dev.keelfy.restless.Restless;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** A spirit box at {@code box} speaks its answer to the asker, one part after another. */
public record SpiritBoxPayload(BlockPos box, List<Part> parts) implements CustomPacketPayload {
    /** The rate of a part's {@code pcm}. */
    public static final int RATE = 16_000;

    /**
     * One part: mono 16-bit samples of the asker's own voice, little endian; when there are none, the clip
     * {@code spirit_box.<language>.<voice>}, or {@code spirit_box.hiss} for {@code hiss}.
     */
    public record Part(String voice, byte[] pcm) {
        static final StreamCodec<ByteBuf, Part> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Part::voice, ByteBufCodecs.BYTE_ARRAY, Part::pcm, Part::new);
    }

    public static final Type<SpiritBoxPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Restless.MOD_ID, "spirit_box"));
    public static final StreamCodec<ByteBuf, SpiritBoxPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SpiritBoxPayload::box,
            Part.CODEC.apply(ByteBufCodecs.list()),
            SpiritBoxPayload::parts,
            SpiritBoxPayload::new);

    @Override
    public Type<SpiritBoxPayload> type() {
        return TYPE;
    }
}
