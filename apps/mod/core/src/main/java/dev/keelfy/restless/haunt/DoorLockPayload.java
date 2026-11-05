package dev.keelfy.restless.haunt;

import dev.keelfy.restless.Restless;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Tells nearby clients a door is locked, so they refuse to open it instead of opening it and being snapped back. The
 * sound of the refusal comes from the server, so a client with another config still agrees with it.
 */
public record DoorLockPayload(BlockPos pos, int ticks, float volume, float pitch) implements CustomPacketPayload {
    public static final Type<DoorLockPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Restless.MOD_ID, "door_lock"));
    public static final StreamCodec<ByteBuf, DoorLockPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            DoorLockPayload::pos,
            ByteBufCodecs.VAR_INT,
            DoorLockPayload::ticks,
            ByteBufCodecs.FLOAT,
            DoorLockPayload::volume,
            ByteBufCodecs.FLOAT,
            DoorLockPayload::pitch,
            DoorLockPayload::new);

    @Override
    public Type<DoorLockPayload> type() {
        return TYPE;
    }
}
