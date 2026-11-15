package dev.keelfy.restless.divine;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.entity.SpiritBody;
import dev.keelfy.restless.haunt.event.kit.PacketEntity;
import dev.keelfy.restless.state.HauntData;
import dev.keelfy.restless.state.Session;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

/**
 * The spirit's body, seen only through a tool: to a player holding a lit spirit candle or the camera obscura, every
 * spirit they sense stands at its point (see {@link Sense}) as a {@link SpiritBody}, within {@code body.radius}. It is
 * shown to that player alone and taken away when they lower the tool.
 */
public final class Body {
    private record Shown(PacketEntity figure, Vec3 at) {}

    /** What each player is being shown, by spirit. */
    private static final Map<UUID, Map<UUID, Shown>> SHOWN = Session.SERVER.keep(new ConcurrentHashMap<>());

    private Body() {}

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % Config.section("body").integer("step_ticks") != 0) {
            return;
        }
        HauntData data = HauntData.get(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            update(player, data);
        }
        SHOWN.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
    }

    /** Shows, moves or takes away the bodies this player sees now. */
    public static void update(ServerPlayer player, HauntData data) {
        Map<UUID, Shown> shown = SHOWN.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
        Set<UUID> seen = new HashSet<>();
        if (sees(player)) {
            double radius = Config.section("body").num("radius");
            for (Sense.Spirit spirit : Sense.of(player, data)) {
                Vec3 at = spirit.at();
                if (at == null || at.distanceToSqr(player.position()) > radius * radius) {
                    continue;
                }
                UUID id = spirit.haunting().uuid();
                seen.add(id);
                float yaw = PacketEntity.yawToward(at, player.position());
                Shown was = shown.get(id);
                if (was == null) {
                    shown.put(id, new Shown(show(player, spirit.haunting(), at, yaw), at));
                } else {
                    was.figure().move(player, at, yaw);
                    shown.put(id, new Shown(was.figure(), at));
                }
            }
        }
        for (UUID id : List.copyOf(shown.keySet())) {
            if (!seen.contains(id)) {
                shown.remove(id).figure().remove(player);
            }
        }
    }

    /** Where this player is shown the spirit's body, if they are. */
    public static Optional<Vec3> at(ServerPlayer player, UUID spirit) {
        return Optional.ofNullable(
                        SHOWN.getOrDefault(player.getUUID(), Map.of()).get(spirit))
                .map(Shown::at);
    }

    /** Whether the player holds a tool that shows bodies: a lit spirit candle or the camera obscura. */
    public static boolean sees(ServerPlayer player) {
        if (Candle.holds(player) && !Candle.isOut(player)) {
            return true;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).is(Content.CAMERA_OBSCURA.get())) {
                return true;
            }
        }
        return false;
    }

    /** Shows the spirit's body to the player alone, at {@code at}. */
    @SuppressWarnings("unchecked")
    public static PacketEntity show(ServerPlayer player, HauntData.Haunting spirit, Vec3 at, float yaw) {
        PacketEntity figure = PacketEntity.show(player, Content.SPIRIT_BODY.get(), at, yaw);
        SpiritBody model = new SpiritBody((EntityType<SpiritBody>) Content.SPIRIT_BODY.get(), player.level());
        model.setSpirit(spirit.spirit(), spirit.seed());
        List<SynchedEntityData.DataValue<?>> values = model.getEntityData().getNonDefaultValues();
        if (values != null && !values.isEmpty()) {
            player.connection.send(new ClientboundSetEntityDataPacket(figure.id(), values));
        }
        return figure;
    }
}
