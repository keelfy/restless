package dev.keelfy.restless.haunt;

import dev.keelfy.restless.Restless;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Tells a client to turn its view by {@code degrees} of yaw over {@code ticks}; see {@link HeadTurn}. */
public record TurnPayload(float degrees, int ticks) implements CustomPacketPayload {
    public static final Type<TurnPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Restless.MOD_ID, "turn"));
    public static final StreamCodec<ByteBuf, TurnPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, TurnPayload::degrees, ByteBufCodecs.VAR_INT, TurnPayload::ticks, TurnPayload::new);

    @Override
    public Type<TurnPayload> type() {
        return TYPE;
    }
}
