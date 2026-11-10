package dev.keelfy.restless.photo;

import dev.keelfy.restless.Restless;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** A client's answer to {@link ShutterPayload}: the picture for the photo marked with {@code film}. */
public record DevelopPayload(int film, Picture picture) implements CustomPacketPayload {
    public static final Type<DevelopPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Restless.MOD_ID, "develop"));
    public static final StreamCodec<ByteBuf, DevelopPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            DevelopPayload::film,
            Picture.STREAM_CODEC,
            DevelopPayload::picture,
            DevelopPayload::new);

    @Override
    public Type<DevelopPayload> type() {
        return TYPE;
    }
}
