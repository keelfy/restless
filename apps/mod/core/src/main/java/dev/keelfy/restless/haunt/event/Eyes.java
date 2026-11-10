package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.Ticks;
import dev.keelfy.restless.state.Session;
import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The camera piece: one player's view jumps to another entity, then back. Only packets are sent; the player stays where
 * they are on the server (the client sends no movement and ignores the keys while the camera is elsewhere). The view
 * goes back when the time is up, the target is gone, the player is hurt, moves off the ground or changes dimension, and
 * from {@link Restore} after a short grace when the scheduler has dropped the player (death, logout) and at server
 * stop. A client that respawns or reconnects resets its camera by itself. The view is reset before a ghost target is
 * removed, so the camera never rests on an entity that is gone.
 */
final class Eyes {
    /** Grace after the planned end before the safety net resets a view the scheduler dropped (death, logout). */
    private static final int NET_TICKS = Ticks.SECOND;

    private static final Map<UUID, Eyes> ACTIVE = Session.SERVER.keep(new HashMap<>());

    private final ServerPlayer player;
    private final ServerLevel level;
    private final float health;
    private final @Nullable Entity target;
    private final @Nullable PacketEntity ghost;
    private boolean over;

    private Eyes(ServerPlayer player, @Nullable Entity target, @Nullable PacketEntity ghost) {
        this.player = player;
        this.level = player.level();
        this.health = player.getHealth();
        this.target = target;
        this.ghost = ghost;
    }

    /** No view is away from this player now. */
    static boolean idle(ServerPlayer player) {
        Eyes eyes = ACTIVE.get(player.getUUID());
        return eyes == null || eyes.over;
    }

    /** The player stands still enough to be left looking elsewhere: idle camera, on the ground, not in a fluid. */
    static boolean steady(ServerPlayer player) {
        return idle(player)
                && player.isAlive()
                && player.onGround()
                && !player.isInWater()
                && !player.isInLava()
                && !player.isPassenger()
                && !player.isSleeping();
    }

    /** Through the eyes of a real entity near the player. */
    static void through(ServerPlayer player, Entity target, int ticks) {
        new Eyes(player, target, null).begin(target.getId(), ticks);
    }

    /**
     * Through an entity only this player's client knows, of the given type, with its eyes at {@code eye}, turned by
     * {@code yaw} and {@code pitch}.
     */
    static void ghost(ServerPlayer player, EntityType<?> type, Vec3 eye, float yaw, float pitch, int ticks) {
        Vec3 feet = eye.subtract(0.0, type.getDimensions().eyeHeight(), 0.0);
        PacketEntity ghost = new PacketEntity(player.level().getNextEntityId());
        player.connection.send(new ClientboundAddEntityPacket(
                ghost.id(),
                Mth.createInsecureUUID(player.getRandom()),
                feet.x,
                feet.y,
                feet.z,
                pitch,
                yaw,
                type,
                0,
                Vec3.ZERO,
                yaw));
        new Eyes(player, null, ghost).begin(ghost.id(), ticks);
    }

    /** The pitch, as entities look, of looking from {@code from} toward {@code to}. */
    static float pitchToward(Vec3 from, Vec3 to) {
        return (float) -(Mth.atan2(to.y - from.y, Math.hypot(to.x - from.x, to.z - from.z)) * Mth.RAD_TO_DEG);
    }

    private void begin(int viewId, int ticks) {
        ACTIVE.put(player.getUUID(), this);
        view(viewId);
        Scheduler.repeat(player, 1, 1, (p, n) -> {
            if (over) {
                return false;
            }
            if (n + 1 >= ticks || lost()) {
                end();
                return false;
            }
            return true;
        });
        // The scheduler forgets a player who died or left, and the server may stop first.
        Restore.after(level, ticks + NET_TICKS, force -> {
            end();
            return true;
        });
    }

    private boolean lost() {
        return player.level() != level
                || !player.isAlive()
                || player.hasDisconnected()
                || player.getHealth() < health
                || (target != null && (!target.isAlive() || target.isRemoved() || target.level() != level));
    }

    private void end() {
        if (over) {
            return;
        }
        over = true;
        if (ACTIVE.get(player.getUUID()) == this) {
            ACTIVE.remove(player.getUUID());
        }
        if (player.hasDisconnected() || player.connection == null) {
            return;
        }
        view(player.getId());
        if (ghost != null) {
            ghost.remove(player);
        }
    }

    private void view(int id) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeVarInt(id);
            player.connection.send(ClientboundSetCameraPacket.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}
