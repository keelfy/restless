package dev.keelfy.restless.debug;

import dev.keelfy.restless.Restless;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** The debug screen asking the server to run an event on the sender. */
public record FirePayload(Identifier event) implements CustomPacketPayload {
    public static final Type<FirePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Restless.MOD_ID, "debug_fire"));
    public static final StreamCodec<ByteBuf, FirePayload> CODEC =
            Identifier.STREAM_CODEC.map(FirePayload::new, FirePayload::event);

    @Override
    public Type<FirePayload> type() {
        return TYPE;
    }
}
