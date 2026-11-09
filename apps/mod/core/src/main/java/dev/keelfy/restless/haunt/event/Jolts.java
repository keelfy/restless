package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntSounds;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Jump scares: a loud sound, then a dark figure that is not a player, there for about a second and gone. Rare by
 * weight. The figure is a {@link PacketEntity}, so only this player's client is told of it and nothing is left on the
 * server.
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
                jolt("jolt_dark", false));
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
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE
                .getOptional(Identifier.parse(config.text("mob")))
                .orElse(null);
        if (spot == null || type == null) {
            return;
        }
        ServerLevel level = player.level();
        Vec3 at = Vec3.atBottomCenterOf(spot);
        Nearby.sound(
                player,
                HauntSounds.JOLT,
                at.add(0, Config.section("place").num("sound_height"), 0),
                config.tone("sound"));
        PacketEntity figure = PacketEntity.show(player, type, at, PacketEntity.yawToward(at, player.position()));
        int[] seen = {-1};
        Scheduler.repeat(player, 1, 1, (p, n) -> {
            if (seen[0] < 0 && (!waits || watched(p, at.add(0, 1.0, 0), config.num("look_angle")))) {
                seen[0] = n;
            }
            boolean over = p.level() != level
                    || seen[0] >= 0 && n - seen[0] >= config.integer("hold_ticks")
                    || n >= config.integer("max_ticks");
            if (over) {
                figure.remove(p);
            }
            return !over;
        });
    }

    /** Whether {@code at} is within {@code degrees} of where the player looks, with nothing solid between. */
    private static boolean watched(ServerPlayer player, Vec3 at, double degrees) {
        Vec3 eye = player.getEyePosition();
        return player.getLookAngle().dot(at.subtract(eye).normalize()) >= Math.cos(Math.toRadians(degrees))
                && player.level()
                                .clip(new ClipContext(
                                        eye, at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
                                .getType()
                        == HitResult.Type.MISS;
    }
}
