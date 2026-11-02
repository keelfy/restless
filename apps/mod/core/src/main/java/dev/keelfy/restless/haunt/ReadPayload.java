package dev.keelfy.restless.haunt;

import dev.keelfy.restless.Restless;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** A reader picked the journal up and wants its pages for the book in their hands, see {@link Journal#sync}. */
public record ReadPayload() implements CustomPacketPayload {
    public static final Type<ReadPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Restless.MOD_ID, "read"));
    public static final StreamCodec<ByteBuf, ReadPayload> CODEC = StreamCodec.unit(new ReadPayload());

    @Override
    public Type<ReadPayload> type() {
        return TYPE;
    }
}
