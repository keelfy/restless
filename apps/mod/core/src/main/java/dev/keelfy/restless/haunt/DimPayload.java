package dev.keelfy.restless.haunt;

import dev.keelfy.restless.Restless;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Tells a client to fade its view toward black and back over {@code ticks}. */
public record DimPayload(int ticks) implements CustomPacketPayload {
    public static final Type<DimPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Restless.MOD_ID, "dim"));
    public static final StreamCodec<ByteBuf, DimPayload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, DimPayload::ticks, DimPayload::new);

    @Override
    public Type<DimPayload> type() {
        return TYPE;
    }
}
