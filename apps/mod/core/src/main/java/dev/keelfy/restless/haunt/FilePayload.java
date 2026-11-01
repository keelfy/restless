package dev.keelfy.restless.haunt;

import dev.keelfy.restless.Restless;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** The reader's choice in the journal: a banishment filed under a spirit's dossier, see {@link JournalPages.Filing}. */
public record FilePayload(int record, int dossier) implements CustomPacketPayload {
    public static final Type<FilePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Restless.MOD_ID, "file"));
    public static final StreamCodec<ByteBuf, FilePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FilePayload::record, ByteBufCodecs.VAR_INT, FilePayload::dossier, FilePayload::new);

    @Override
    public Type<FilePayload> type() {
        return TYPE;
    }
}
