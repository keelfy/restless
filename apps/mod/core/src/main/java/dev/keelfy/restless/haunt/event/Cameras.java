package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Location;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The player's view jumps to another entity for a few seconds, then back; see {@link Eyes}. Nothing on the server
 * changes, and a view cut short (death, logout, dimension change, server stop) is reset.
 */
final class Cameras {
    private Cameras() {}

    static List<HauntEvent> all() {
        return List.of(
                // 347: through the eyes of a spider, creeper or enderman nearby; vanilla adds its shader
                SimpleEvent.light(
                        "mob_eyes",
                        EventTag.PRESENCE,
                        player -> Eyes.steady(player) && !watchers(player).isEmpty(),
                        player -> {
                            Config.Section config = Config.event("mob_eyes");
                            List<Mob> mobs = watchers(player);
                            if (!mobs.isEmpty()) {
                                Eyes.through(
                                        player,
                                        mobs.get(player.getRandom().nextInt(mobs.size())),
                                        config.integer("ticks"));
                            }
                        }),
                // 348: out of body, from behind and a little above, looking at the back of one's own head
                SimpleEvent.light("out_of_body", EventTag.PRESENCE, Eyes::steady, player -> {
                    Config.Section config = Config.event("out_of_body");
                    Vec3 head = player.getEyePosition();
                    float yaw = player.getYRot();
                    double radians = Math.toRadians(yaw);
                    // Backs off from the full distance until the spot is free.
                    for (double distance = config.num("distance"); distance >= 1.0; distance -= 0.5) {
                        Vec3 eye = head.add(
                                Math.sin(radians) * distance, config.num("height"), -Math.cos(radians) * distance);
                        BlockPos spot = BlockPos.containing(eye);
                        if (player.level()
                                .getBlockState(spot)
                                .getCollisionShape(player.level(), spot)
                                .isEmpty()) {
                            Eyes.ghost(
                                    player,
                                    entity(config.text("entity")),
                                    eye,
                                    yaw,
                                    Eyes.pitchToward(eye, head),
                                    config.integer("ticks"));
                            return;
                        }
                    }
                }),
                // 349: one second at the spirit's spot, looking back at the player; a clue to where it is
                SimpleEvent.light("spirit_sight", EventTag.PRESENCE, Eyes::steady, player -> {
                    Config.Section config = Config.event("spirit_sight");
                    Vec3 head = player.getEyePosition();
                    Vec3 eye = Location.source(Nearby.randomAround(player, config.num("fallback_distance")))
                            .add(0.0, config.num("height"), 0.0);
                    // Only where the client has the land to show; a far anchor shows nothing.
                    if (eye.distanceTo(head) > config.num("max_distance")
                            || !player.level().isLoaded(BlockPos.containing(eye))) {
                        return;
                    }
                    Eyes.ghost(
                            player,
                            entity(config.text("entity")),
                            eye,
                            PacketEntity.yawToward(eye, head),
                            Eyes.pitchToward(eye, head),
                            config.integer("ticks"));
                }),
                // 366: the view lingers in the doorway of a door left open, looking in toward the player
                SimpleEvent.light(
                        "open_door_view",
                        EventTag.DOORS,
                        player -> Eyes.steady(player) && openDoor(player) != null,
                        player -> {
                            Config.Section config = Config.event("open_door_view");
                            BlockPos door = openDoor(player);
                            if (door == null) {
                                return;
                            }
                            Vec3 head = player.getEyePosition();
                            Vec3 eye = Vec3.atBottomCenterOf(door).add(0.0, config.num("height"), 0.0);
                            Eyes.ghost(
                                    player,
                                    entity(config.text("entity")),
                                    eye,
                                    PacketEntity.yawToward(eye, head),
                                    Eyes.pitchToward(eye, head),
                                    config.integer("ticks"));
                        }));
    }

    private static EntityType<?> entity(String id) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id)).orElse(EntityTypes.ZOMBIE);
    }

    /** The configured kinds of mob alive near the player. */
    private static List<Mob> watchers(ServerPlayer player) {
        Config.Section config = Config.event("mob_eyes");
        List<String> kinds = config.texts("mobs");
        return player.level()
                .getEntitiesOfClass(
                        Mob.class,
                        player.getBoundingBox().inflate(config.num("radius")),
                        mob -> mob.isAlive()
                                && kinds.contains(BuiltInRegistries.ENTITY_TYPE
                                        .getKey(mob.getType())
                                        .toString()));
    }

    /** The lower half of the nearest open door, not too near the player to be in it. */
    private static @Nullable BlockPos openDoor(ServerPlayer player) {
        Config.Section config = Config.event("open_door_view");
        double least = config.num("min_distance");
        return Nearby.all(player, config.integer("radius"), Cameras::isOpenDoor).stream()
                .filter(pos -> Vec3.atCenterOf(pos).distanceTo(player.position()) >= least)
                .min(Comparator.comparingDouble(pos -> pos.distSqr(player.blockPosition())))
                .orElse(null);
    }

    private static boolean isOpenDoor(BlockState state) {
        return state.getBlock() instanceof DoorBlock
                && state.getValue(DoorBlock.OPEN)
                && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
    }
}
