package dev.keelfy.restless.haunt;

import dev.keelfy.restless.Restless;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Only discovered, displayable text is sent; hidden spirit state stays on the server. */
public record JournalPayload(List<JournalPages.Section> sections) implements CustomPacketPayload {
    public static final Type<JournalPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Restless.MOD_ID, "journal"));
    private static final StreamCodec<RegistryFriendlyByteBuf, JournalPages.Section> SECTION_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, JournalPages.Section::key,
        ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list()), JournalPages.Section::pages,
        JournalPages.Section::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, JournalPayload> CODEC = StreamCodec.composite(
        SECTION_CODEC.apply(ByteBufCodecs.list()), JournalPayload::sections, JournalPayload::new);

    @Override
    public Type<JournalPayload> type() {
        return TYPE;
    }
}
