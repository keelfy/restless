package dev.keelfy.restless.haunt;

import dev.keelfy.restless.Restless;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Tells a client to tint its screen's edges red, up to {@code strength} (0..1), for {@code ticks}. */
public record VignettePayload(float strength, int ticks) implements CustomPacketPayload {
    public static final Type<VignettePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Restless.MOD_ID, "vignette"));
    public static final StreamCodec<ByteBuf, VignettePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            VignettePayload::strength,
            ByteBufCodecs.VAR_INT,
            VignettePayload::ticks,
            VignettePayload::new);

    @Override
    public Type<VignettePayload> type() {
        return TYPE;
    }
}
