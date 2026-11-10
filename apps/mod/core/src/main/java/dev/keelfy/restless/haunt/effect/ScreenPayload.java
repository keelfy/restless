package dev.keelfy.restless.haunt.effect;

import dev.keelfy.restless.Restless;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Tells a client to lay one full-screen picture over its view for {@code ticks}: a handprint, frost on the edges or a
 * face. {@code strength} is the peak opacity; {@code seed} picks where a handprint lands.
 */
public record ScreenPayload(int kind, int ticks, float strength, int seed) implements CustomPacketPayload {
    public static final int HANDPRINT = 0;
    public static final int FROST = 1;
    public static final int FACE = 2;

    public static final Type<ScreenPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Restless.MOD_ID, "screen"));
    public static final StreamCodec<ByteBuf, ScreenPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ScreenPayload::kind,
            ByteBufCodecs.VAR_INT,
            ScreenPayload::ticks,
            ByteBufCodecs.FLOAT,
            ScreenPayload::strength,
            ByteBufCodecs.VAR_INT,
            ScreenPayload::seed,
            ScreenPayload::new);

    @Override
    public Type<ScreenPayload> type() {
        return TYPE;
    }
}
