package dev.keelfy.restless.haunt;

import dev.keelfy.restless.Restless;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Tells a client to play the closing screen, see {@link RequiemScreen}. */
public record RequiemPayload() implements CustomPacketPayload {
    public static final Type<RequiemPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Restless.MOD_ID, "requiem"));
    public static final StreamCodec<ByteBuf, RequiemPayload> CODEC = StreamCodec.unit(new RequiemPayload());

    @Override
    public Type<RequiemPayload> type() {
        return TYPE;
    }
}
