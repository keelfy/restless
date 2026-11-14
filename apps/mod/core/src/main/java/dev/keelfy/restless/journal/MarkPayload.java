package dev.keelfy.restless.journal;

import dev.keelfy.restless.Restless;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * The reader's click on their case sheet: move one guess of the {@code dossier}th met spirit on, see
 * {@link Notebook#cycled}.
 */
public record MarkPayload(int dossier, String field) implements CustomPacketPayload {
    public static final Type<MarkPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Restless.MOD_ID, "mark"));
    public static final StreamCodec<ByteBuf, MarkPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            MarkPayload::dossier,
            ByteBufCodecs.STRING_UTF8,
            MarkPayload::field,
            MarkPayload::new);

    @Override
    public Type<MarkPayload> type() {
        return TYPE;
    }
}
