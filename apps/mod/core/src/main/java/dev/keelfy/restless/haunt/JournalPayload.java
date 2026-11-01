package dev.keelfy.restless.haunt;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.content.Picture;
import dev.keelfy.restless.haunt.Notebook.Kind;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Only discovered, displayable text and pictures are sent; hidden spirit state stays on the server. */
public record JournalPayload(List<JournalPages.Section> sections) implements CustomPacketPayload {
    public static final Type<JournalPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Restless.MOD_ID, "journal"));
    private static final StreamCodec<RegistryFriendlyByteBuf, JournalPages.Filing> FILING_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            JournalPages.Filing::record,
            ByteBufCodecs.VAR_INT,
            JournalPages.Filing::dossier,
            JournalPages.Filing::new);
    private static final StreamCodec<RegistryFriendlyByteBuf, JournalPages.Entry> ENTRY_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ByteBufCodecs.idMapper(i -> Kind.values()[i], Kind::ordinal)),
            JournalPages.Entry::kind,
            ComponentSerialization.STREAM_CODEC,
            JournalPages.Entry::text,
            ByteBufCodecs.optional(FILING_CODEC),
            JournalPages.Entry::filing,
            JournalPages.Entry::new);
    private static final StreamCodec<RegistryFriendlyByteBuf, JournalPages.Card> CARD_CODEC = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC,
            JournalPages.Card::heading,
            ByteBufCodecs.optional(Picture.STREAM_CODEC),
            JournalPages.Card::picture,
            ENTRY_CODEC.apply(ByteBufCodecs.list()),
            JournalPages.Card::entries,
            JournalPages.Card::new);
    private static final StreamCodec<RegistryFriendlyByteBuf, JournalPages.Section> SECTION_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    JournalPages.Section::key,
                    CARD_CODEC.apply(ByteBufCodecs.list()),
                    JournalPages.Section::cards,
                    JournalPages.Section::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, JournalPayload> CODEC = StreamCodec.composite(
            SECTION_CODEC.apply(ByteBufCodecs.list()), JournalPayload::sections, JournalPayload::new);

    @Override
    public Type<JournalPayload> type() {
        return TYPE;
    }
}
