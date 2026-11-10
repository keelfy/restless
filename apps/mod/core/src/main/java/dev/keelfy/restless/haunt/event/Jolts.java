package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.Silhouette;
import dev.keelfy.restless.content.SilhouetteRenderer;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntSounds;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Jump scares: a loud sound, then a black figure of no known kind, close enough to crowd the player, there for about a
 * second and gone; or a rustle out of sight and a figure that crosses the gap to the player's face in a few ticks. Rare
 * by weight. The figure is a {@link Silhouette} sent as a {@link PacketEntity}, so only this player's client is told of
 * it and nothing is left on the server.
 */
final class Jolts {
    private Jolts() {}

    static List<HauntEvent> all() {
        return List.of(
                // 536: a bang right behind the player; turning round shows a figure that is gone a moment later
                jolt("jolt_behind", true),
                // 537: the same, at the edge of the view
                jolt("jolt_side", true),
                // 538: in a dark spot ahead, the figure and the bang at the same instant
                jolt("jolt_dark", false),
                // 539: a faint rustle in the dark out of sight; come close and the figure is in your face at once
                SimpleEvent.light("jolt_lunge", EventTag.PRESENCE, player -> lair(player) != null, Jolts::ambush));
    }

    private static HauntEvent jolt(String id, boolean waits) {
        return SimpleEvent.light(
                id, EventTag.PRESENCE, player -> spot(player, id) != null, player -> scare(player, id, waits));
    }

    /** Standing room at the event's distance and angles; where it has a {@code dark_max}, no brighter than that. */
    private static @Nullable BlockPos spot(ServerPlayer player, String id) {
        Config.Section config = Config.event(id);
        return Place.standingAhead(
                player,
                config.num("distance"),
                config.num("min_angle"),
                config.num("max_angle"),
                pos -> !config.has("dark_max")
                        || player.level().getMaxLocalRawBrightness(pos) <= config.integer("dark_max"));
    }

    /**
     * The bang and the figure at the same spot. With {@code waits} the figure is unseen until the player looks at it
     * (and goes unseen after {@code max_ticks}); it is gone {@code hold_ticks} after the player first sees it.
     */
    private static void scare(ServerPlayer player, String id, boolean waits) {
        Config.Section config = Config.event(id);
        BlockPos spot = spot(player, id);
        if (spot != null) {
            show(player, config, spot, waits, config.integer("max_ticks"));
        }
    }

    /**
     * {@link #scare} at a spot already found, unseen for at most {@code maxTicks}; reads {@code sound},
     * {@code look_angle} and {@code hold_ticks}.
     */
    static void show(ServerPlayer player, Config.Section config, BlockPos spot, boolean waits, int maxTicks) {
        ServerLevel level = player.level();
        Vec3 at = Vec3.atBottomCenterOf(spot);
        Nearby.sound(
                player,
                HauntSounds.JOLT,
                at.add(0, Config.section("place").num("sound_height"), 0),
                config.tone("sound"));
        PacketEntity figure =
                PacketEntity.show(player, Content.SILHOUETTE.get(), at, PacketEntity.yawToward(at, player.position()));
        shape(player, figure);
        int[] seen = {-1};
        Scheduler.repeat(player, 1, 1, (p, n) -> {
            if (seen[0] < 0 && (!waits || watched(p, at.add(0, 1.0, 0), config.num("look_angle")))) {
                seen[0] = n;
            }
            boolean over =
                    p.level() != level || seen[0] >= 0 && n - seen[0] >= config.integer("hold_ticks") || n >= maxTicks;
            if (over) {
                figure.remove(p);
            }
            return !over;
        });
    }

    /** A dark empty spot out of the player's sight, {@code min_distance} to {@code max_distance} blocks away. */
    private static @Nullable BlockPos lair(ServerPlayer player) {
        Config.Section config = Config.event("jolt_lunge");
        ServerLevel level = player.level();
        return Place.near(
                player,
                player.blockPosition(),
                config.integer("min_distance"),
                config.integer("max_distance"),
                pos -> Place.floored(level, pos)
                        && level.getMaxLocalRawBrightness(pos) <= config.integer("dark_max")
                        && !clear(player, Vec3.atBottomCenterOf(pos).add(0, 1.0, 0)));
    }

    /**
     * A rustle from the lair every {@code rustle_gap_ticks}. When the player comes within {@code trigger_distance} the
     * figure is there, crosses to their face in {@code lunge_ticks}, bangs, and is gone {@code hold_ticks} later. Given
     * up after {@code max_ticks} if nobody comes.
     */
    private static void ambush(ServerPlayer player) {
        Config.Section config = Config.event("jolt_lunge");
        BlockPos lair = lair(player);
        if (lair == null) {
            return;
        }
        ServerLevel level = player.level();
        Vec3 home = Vec3.atBottomCenterOf(lair);
        PacketEntity[] figure = {null};
        int[] struck = {0};
        Scheduler.repeat(player, 1, 1, (p, n) -> {
            if (p.level() != level || figure[0] == null && n >= config.integer("max_ticks")) {
                return false;
            }
            if (figure[0] == null) {
                if (n % config.integer("rustle_gap_ticks") == 0) {
                    Nearby.sound(
                            p,
                            HauntSounds.RUSTLE,
                            home.add(0, Config.section("place").num("sound_height"), 0),
                            config.tone("sound"));
                }
                if (p.position().distanceTo(home) <= config.num("trigger_distance")) {
                    figure[0] = PacketEntity.show(
                            p, Content.SILHOUETTE.get(), home, PacketEntity.yawToward(home, p.position()));
                    shape(p, figure[0]);
                    struck[0] = n;
                }
                return true;
            }
            int t = n - struck[0];
            int lunge = config.integer("lunge_ticks");
            Vec3 look = p.getLookAngle();
            Vec3 face = p.position().add(new Vec3(look.x, 0, look.z).normalize().scale(config.num("face_distance")));
            if (t <= lunge) {
                figure[0].move(p, home.lerp(face, (double) t / lunge), PacketEntity.yawToward(face, p.position()));
                if (t == lunge) {
                    Nearby.sound(p, HauntSounds.JOLT, face.add(0, p.getEyeHeight(), 0), config.tone("bang"));
                }
            }
            boolean over = t >= lunge + config.integer("hold_ticks");
            if (over) {
                figure[0].remove(p);
            }
            return !over;
        });
    }

    /**
     * A random one of the first silhouette shapes, or the given one: the client is sent the entity data of a model that
     * is never added. The registry is typed {@code EntityType<?>}; this one is built for Silhouette.
     */
    static void shape(ServerPlayer player, PacketEntity figure) {
        shape(player, figure, player.getRandom().nextInt(SilhouetteRenderer.ANY));
    }

    @SuppressWarnings("unchecked")
    static void shape(ServerPlayer player, PacketEntity figure, int shape) {
        Silhouette model = new Silhouette((EntityType<Silhouette>) Content.SILHOUETTE.get(), player.level());
        model.setShape(shape);
        List<SynchedEntityData.DataValue<?>> values = model.getEntityData().getNonDefaultValues();
        if (values != null && !values.isEmpty()) {
            player.connection.send(new ClientboundSetEntityDataPacket(figure.id(), values));
        }
    }

    /** Whether {@code at} is within {@code degrees} of where the player looks, with nothing solid between. */
    static boolean watched(ServerPlayer player, Vec3 at, double degrees) {
        Vec3 eye = player.getEyePosition();
        return player.getLookAngle().dot(at.subtract(eye).normalize()) >= Math.cos(Math.toRadians(degrees))
                && clear(player, at);
    }

    /** Whether nothing solid lies between the player's eyes and {@code at}. */
    private static boolean clear(ServerPlayer player, Vec3 at) {
        return player.level()
                        .clip(new ClipContext(
                                player.getEyePosition(),
                                at,
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                player))
                        .getType()
                == HitResult.Type.MISS;
    }
}
