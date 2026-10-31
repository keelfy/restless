package dev.keelfy.restless.haunt;

import dev.keelfy.restless.Restless;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Tells a client its player's sanity, 0..1. */
public record SanityPayload(float sanity) implements CustomPacketPayload {
    public static final Type<SanityPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Restless.MOD_ID, "sanity"));
    public static final StreamCodec<ByteBuf, SanityPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.FLOAT, SanityPayload::sanity, SanityPayload::new);

    @Override
    public Type<SanityPayload> type() {
        return TYPE;
    }
}
