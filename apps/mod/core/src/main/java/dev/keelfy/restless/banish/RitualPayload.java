package dev.keelfy.restless.banish;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.banish.client.RitualCue;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Tells a client the {@link Phase} its player's ritual entered, or -1 when the ritual is over; see {@link RitualCue}.
 */
public record RitualPayload(int phase) implements CustomPacketPayload {
    public static final Type<RitualPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Restless.MOD_ID, "ritual"));
    public static final StreamCodec<ByteBuf, RitualPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, RitualPayload::phase, RitualPayload::new);

    public static RitualPayload of(Optional<Phase> phase) {
        return new RitualPayload(phase.map(Phase::ordinal).orElse(-1));
    }

    /** The phase, empty when the ritual is over. */
    public Optional<Phase> current() {
        return phase >= 0 && phase < Phase.values().length ? Optional.of(Phase.values()[phase]) : Optional.empty();
    }

    @Override
    public Type<RitualPayload> type() {
        return TYPE;
    }
}
