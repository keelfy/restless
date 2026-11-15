package dev.keelfy.restless.spirit.land;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.spirit.Story;
import dev.keelfy.restless.state.HauntData;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * {@link Quest#KILLER}: the mob that killed the spirit waits near the grave under a name of its own. A player killing
 * it settles the business; if it dies any other way the next reading calls it up again.
 */
final class Killer {
    // Keeps the killer's name apart from the story's own name roll on the same seed.
    private static final long SALT = 0x4B111EL;

    private Killer() {}

    @SuppressWarnings("java:S2245") // Seeded so the name is reproducible; not security-sensitive.
    static String name(long seed) {
        return Story.nickname(new Random(seed ^ SALT));
    }

    /** Calls the killer up on the first reading. @return its name */
    static Component read(ServerPlayer player, HauntData data, HauntData.Haunting spirit, HauntData.Grave grave) {
        // Unread, or the killer left the world without dying (a creeper that blew up): call it up again. Nowhere to
        // stand keeps the grave unread, and the next reading tries again.
        if (grave.stage() == 0
                || grave.stage() == 1
                        && grave.mark().map(player.level()::getEntity).isEmpty()) {
            spawn(player.level(), spirit, grave.pos())
                    .ifPresent(mob -> data.putGrave(spirit.uuid(), grave.staged(1, Optional.of(mob))));
        }
        return Component.literal(name(spirit.seed()));
    }

    private static Optional<UUID> spawn(ServerLevel level, HauntData.Haunting spirit, BlockPos grave) {
        Config.Section config = Config.section("grave");
        Config.Section mobs = config.section("killer_mobs");
        String cause = Story.of(spirit.seed()).cause();
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE
                .getOptional(Identifier.parse(mobs.has(cause) ? mobs.text(cause) : config.text("killer_default")))
                .orElseThrow();
        double radius = config.num("killer_radius");
        double angle = level.getRandom().nextDouble() * Math.PI * 2;
        double distance = level.getRandom().nextDouble() * radius;
        BlockPos spot = level.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlockPos.containing(
                        grave.getX() + Math.cos(angle) * distance, 0, grave.getZ() + Math.sin(angle) * distance));
        if (!(type.spawn(level, spot, EntitySpawnReason.EVENT) instanceof Mob mob)) {
            return Optional.empty();
        }
        mob.setCustomName(Component.literal(name(spirit.seed())));
        mob.setCustomNameVisible(true);
        mob.setPersistenceRequired();
        return Optional.of(mob.getUUID());
    }

    /** A named killer died: a player's blow settles the grave, anything else sends it back to be called up again. */
    static void killed(HauntData data, @Nullable Entity killer, LivingEntity dead) {
        for (Map.Entry<UUID, HauntData.Grave> entry : data.graves().entrySet()) {
            HauntData.Grave grave = entry.getValue();
            if (grave.stage() == 1
                    && grave.mark().filter(dead.getUUID()::equals).isPresent()) {
                boolean settled = killer instanceof ServerPlayer;
                data.putGrave(entry.getKey(), grave.staged(settled ? 2 : 0, Optional.empty()));
                if (killer instanceof ServerPlayer player) {
                    player.sendSystemMessage(
                            Component.translatable("message.restless.killer_dead", dead.getCustomName()), true);
                }
                return;
            }
        }
    }
}
