package dev.keelfy.restless.haunt;

import dev.keelfy.restless.Restless;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Tells a client its camera just caught a spirit: grab the next frame, with the spirit standing at {@code at} (or
 * wherever it likes when unknown), looking as the spirit of that type and seed does, and send it back as the picture of
 * the photo marked with {@code film}.
 */
public record ShutterPayload(int film, String spirit, long seed, Optional<Vec3> at) implements CustomPacketPayload {
    public static final Type<ShutterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Restless.MOD_ID, "shutter"));
    public static final StreamCodec<ByteBuf, ShutterPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ShutterPayload::film,
            ByteBufCodecs.STRING_UTF8,
            ShutterPayload::spirit,
            ByteBufCodecs.VAR_LONG,
            ShutterPayload::seed,
            ByteBufCodecs.optional(Vec3.STREAM_CODEC),
            ShutterPayload::at,
            ShutterPayload::new);

    @Override
    public Type<ShutterPayload> type() {
        return TYPE;
    }
}
