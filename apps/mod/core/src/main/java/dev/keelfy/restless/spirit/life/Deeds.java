package dev.keelfy.restless.spirit.life;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.state.HauntData;
import dev.keelfy.restless.state.Session;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.Villager;
import org.jspecify.annotations.Nullable;

/**
 * Where the world's deeds are seen: kills and deaths ({@link #killed}) and the slow check for a long wake
 * ({@link #tick}). Each one calls {@link Births#deed}, which owns the chance, the cooldown and the cap. Counters live
 * in memory, so a restart only makes births rarer.
 */
public final class Deeds {
    /** A kill that counts toward a slaughter: when it happened and what it was worth. */
    private record Blood(long at, int points) {}

    private static final Map<UUID, Deque<Blood>> BLOOD = Session.SERVER.keep(new HashMap<>());
    /** Players already given their roll for the wake they are in; cleared when they sleep. */
    private static final Set<UUID> WAKING = Session.SERVER.keep(new HashSet<>());

    private static final int CHECK_TICKS = 200;

    private Deeds() {}

    /** Call when a living thing dies; {@code killer} is whoever dealt the blow, if anyone. */
    public static void killed(@Nullable Entity killer, LivingEntity dead) {
        Config.Section config = Config.section("births");
        if (dead instanceof ServerPlayer victim) {
            far(victim, config);
        }
        if (dead instanceof OwnableEntity pet && pet.getOwner() instanceof ServerPlayer owner) {
            Births.deed(owner, killer == owner ? Births.Deed.PET_SLAIN : Births.Deed.PET_LOST, true);
        }
        if (killer instanceof ServerPlayer player) {
            slaughter(player, dead, config);
        }
        if (dead instanceof Villager || dead instanceof OwnableEntity) {
            witness(killer, dead, config);
        }
    }

    /** Dying far from home, or deep, and not at the end of a spirit's lethal event, which satisfies it. */
    private static void far(ServerPlayer victim, Config.Section config) {
        BlockPos home = victim.getRespawnConfig() == null
                ? victim.level().getRespawnData().pos()
                : victim.getRespawnConfig().respawnData().pos();
        boolean away =
                victim.blockPosition().distSqr(home) >= square(config.integer("far_distance")) || victim.getY() < 0;
        long lastLethal = HauntData.get(victim.level().getServer())
                .menace(victim.getUUID())
                .lastLethalAt();
        long now = victim.level().getServer().overworld().getGameTime();
        boolean spirits = lastLethal >= 0
                && now - lastLethal <= Config.section("assaults").integer("relief_window_ticks");
        if (away && !spirits) {
            Births.deed(victim, Births.Deed.FAR_DEATH, true);
        }
    }

    private static void slaughter(ServerPlayer player, LivingEntity dead, Config.Section config) {
        Config.Section blood = config.section("slaughter");
        int points = dead instanceof Villager
                ? blood.integer("villager_points")
                : dead instanceof Animal && !(dead instanceof OwnableEntity) ? 1 : 0;
        if (points == 0) {
            return;
        }
        long now = player.level().getServer().overworld().getGameTime();
        Deque<Blood> kills = BLOOD.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
        kills.addLast(new Blood(now, points));
        while (now - kills.peekFirst().at() > blood.integer("window_ticks")) {
            kills.removeFirst();
        }
        if (kills.stream().mapToInt(Blood::points).sum() >= blood.integer("points")) {
            kills.clear();
            Births.deed(player, Births.Deed.SLAUGHTER, true);
        }
    }

    /** A villager or a pet dies near a player who did not do it, and who was not its owner. */
    private static void witness(@Nullable Entity killer, LivingEntity dead, Config.Section config) {
        if (!(dead.level() instanceof ServerLevel level)) {
            return;
        }
        double radius = config.num("witness_radius");
        UUID owner = dead instanceof OwnableEntity pet && pet.getOwnerReference() != null
                ? pet.getOwnerReference().getUUID()
                : null;
        for (ServerPlayer player : level.players()) {
            if (player != killer && !player.getUUID().equals(owner) && player.distanceToSqr(dead) <= radius * radius) {
                Births.deed(player, Births.Deed.WITNESS_DEATH, true);
            }
        }
    }

    /** Call once per server tick: a player who has not slept for two days may be given a lost one. */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % CHECK_TICKS != 0) {
            return;
        }
        server.getPlayerList().getPlayers().forEach(Deeds::wake);
    }

    /** One roll for each wake: the player has gone {@code births.sleepless_ticks} without rest. */
    public static void wake(ServerPlayer player) {
        boolean awake = player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST))
                >= Config.section("births").integer("sleepless_ticks");
        if (!awake) {
            WAKING.remove(player.getUUID());
        } else if (WAKING.add(player.getUUID())) {
            Births.deed(player, Births.Deed.LOST_LONG, true);
        }
    }

    private static long square(int n) {
        return (long) n * n;
    }
}
