package dev.keelfy.restless.haunt;

import dev.keelfy.restless.Restless;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Tells a client to fade its view toward black ({@code peak} of full) and back, {@code times} over; each phase in ticks. */
public record DimPayload(int fall, int hold, int rise, int times, float peak) implements CustomPacketPayload {
    public static final Type<DimPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Restless.MOD_ID, "dim"));
    public static final StreamCodec<ByteBuf, DimPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, DimPayload::fall, ByteBufCodecs.VAR_INT, DimPayload::hold, ByteBufCodecs.VAR_INT, DimPayload::rise,
        ByteBufCodecs.VAR_INT, DimPayload::times, ByteBufCodecs.FLOAT, DimPayload::peak, DimPayload::new);

    /** One cycle. */
    public int cycle() {
        return fall + hold + rise;
    }

    @Override
    public Type<DimPayload> type() {
        return TYPE;
    }
}
