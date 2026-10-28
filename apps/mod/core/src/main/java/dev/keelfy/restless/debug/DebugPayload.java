package dev.keelfy.restless.debug;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntTier;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** What the debug screen shows: the viewer's haunt state and every event, as the server sees them when it opens. */
public record DebugPayload(String player, double danger, int days, int advancements, List<Spirit> spirits, List<Event> events)
    implements CustomPacketPayload {
    /** @param seconds time to the spirit's next roll */
    public record Spirit(String id, int seconds) {
        static final StreamCodec<ByteBuf, Spirit> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Spirit::id, ByteBufCodecs.VAR_INT, Spirit::seconds, Spirit::new);
    }

    /** @param canRun whether the event has something to act on near the viewer right now */
    public record Event(Identifier id, HauntTier tier, List<EventTag> tags, double weight, boolean canRun) {
        static final StreamCodec<ByteBuf, Event> CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, Event::id,
            ByteBufCodecs.idMapper(i -> HauntTier.values()[i], HauntTier::ordinal), Event::tier,
            ByteBufCodecs.idMapper(i -> EventTag.values()[i], EventTag::ordinal).apply(ByteBufCodecs.list()), Event::tags,
            ByteBufCodecs.DOUBLE, Event::weight,
            ByteBufCodecs.BOOL, Event::canRun,
            Event::new);
    }

    public static final Type<DebugPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Restless.MOD_ID, "debug"));
    public static final StreamCodec<ByteBuf, DebugPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, DebugPayload::player,
        ByteBufCodecs.DOUBLE, DebugPayload::danger,
        ByteBufCodecs.VAR_INT, DebugPayload::days,
        ByteBufCodecs.VAR_INT, DebugPayload::advancements,
        Spirit.CODEC.apply(ByteBufCodecs.list()), DebugPayload::spirits,
        Event.CODEC.apply(ByteBufCodecs.list()), DebugPayload::events,
        DebugPayload::new);

    @Override
    public Type<DebugPayload> type() {
        return TYPE;
    }
}
