package dev.keelfy.restless.haunt.event;

import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PositionPath;
import net.minecraft.world.phys.Vec3;

/**
 * An entity only one player sees: their client is told of it under an id no server entity has, so there is nothing on
 * the server to touch, and an attack on it is ignored as one on an unknown id. A client that drops it (the player left,
 * changed dimension, went out of range) just forgets it.
 */
record PacketEntity(int id) {
    static PacketEntity show(ServerPlayer player, EntityType<?> type, Vec3 at, float yaw) {
        PacketEntity entity = new PacketEntity(player.level().getNextEntityId());
        player.connection.send(new ClientboundAddEntityPacket(entity.id, Mth.createInsecureUUID(player.getRandom()), at.x, at.y, at.z,
            0.0F, yaw, type, 0, Vec3.ZERO, yaw));
        return entity;
    }

    void move(ServerPlayer player, Vec3 to, float yaw) {
        player.connection.send(new ClientboundEntityPositionSyncPacket(id, PositionPath.of(to), yaw, 0.0F, false));
    }

    void remove(ServerPlayer player) {
        player.connection.send(new ClientboundRemoveEntitiesPacket(id));
    }

    /** The yaw, as entities face, of looking from {@code from} toward {@code to}. */
    static float yawToward(Vec3 from, Vec3 to) {
        return (float) (Mth.atan2(to.z - from.z, to.x - from.x) * Mth.RAD_TO_DEG) - 90.0F;
    }
}
