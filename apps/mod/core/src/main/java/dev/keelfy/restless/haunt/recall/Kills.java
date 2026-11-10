package dev.keelfy.restless.haunt.recall;

import dev.keelfy.restless.state.Session;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * What each player has killed: the passive animals of the current day by kind, and the last mob of any kind with where
 * and when. For the revenant's events. In memory only, so a restart forgets the day.
 */
public final class Kills {
    /** The last mob a player killed. */
    public record Last(EntityType<?> type, ResourceKey<Level> dimension, BlockPos pos, long at) {}

    private static final class Day {
        private long number = -1;
        private final Map<EntityType<?>, Integer> animals = new LinkedHashMap<>();
    }

    private static final long DAY_TICKS = 24000L;
    private static final Map<UUID, Day> DAYS = Session.SERVER.keep(new HashMap<>());
    private static final Map<UUID, Last> LAST = Session.SERVER.keep(new HashMap<>());

    private Kills() {}

    /** Call when a player has killed {@code dead}. */
    public static void killed(ServerPlayer player, LivingEntity dead) {
        if (!(dead instanceof Mob)) {
            return;
        }
        long now = player.level().getGameTime();
        LAST.put(player.getUUID(), new Last(dead.getType(), player.level().dimension(), dead.blockPosition(), now));
        if (dead instanceof Animal && !(dead instanceof OwnableEntity)) {
            Day day = today(player, now);
            day.animals.merge(dead.getType(), 1, Integer::sum);
        }
    }

    /** The passive animals the player killed today, by kind, in the order first killed. */
    public static Map<EntityType<?>, Integer> animals(ServerPlayer player) {
        return Map.copyOf(today(player, player.level().getGameTime()).animals);
    }

    public static @Nullable Last last(ServerPlayer player) {
        return LAST.get(player.getUUID());
    }

    private static Day today(ServerPlayer player, long now) {
        Day day = DAYS.computeIfAbsent(player.getUUID(), id -> new Day());
        if (day.number != now / DAY_TICKS) {
            day.number = now / DAY_TICKS;
            day.animals.clear();
        }
        return day;
    }
}
