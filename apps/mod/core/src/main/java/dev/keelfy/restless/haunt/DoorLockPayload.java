package dev.keelfy.restless.haunt;

import dev.keelfy.restless.Restless;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Tells nearby clients a door is locked, so they refuse to open it instead of opening it and being snapped back. */
public record DoorLockPayload(BlockPos pos, int ticks) implements CustomPacketPayload {
    public static final Type<DoorLockPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Restless.MOD_ID, "door_lock"));
    public static final StreamCodec<ByteBuf, DoorLockPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            DoorLockPayload::pos,
            ByteBufCodecs.VAR_INT,
            DoorLockPayload::ticks,
            DoorLockPayload::new);

    @Override
    public Type<DoorLockPayload> type() {
        return TYPE;
    }
}
