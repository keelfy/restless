package dev.keelfy.restless.haunt;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.spirit.SpiritType;
import dev.keelfy.restless.spirit.Spirits;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/** Rolls haunt events for every spirit bound to every player; each spirit runs its own timer. */
public final class Director {
    private Director() {}

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % Config.section("director").integer("check_interval_ticks") != 0) {
            return;
        }
        long now = server.overworld().getGameTime();
        HauntData data = HauntData.get(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            List<HauntData.Haunting> spirits = new ArrayList<>(hauntings(player, data, now));
            boolean changed = false;
            for (int i = 0; i < spirits.size(); i++) {
                if (now < spirits.get(i).nextEventAt()) {
                    continue;
                }
                HauntData.Haunting haunting = spirits.get(i);
                SpiritType spirit = spirit(haunting);
                if (!player.isSpectator() && !player.isCreative()) {
                    pick(player, spirit, power(haunting, now)).ifPresent(event -> fire(player, event));
                }
                spirits.set(i, haunting.rescheduled(now + cooldown(player, haunting, now)));
                changed = true;
            }
            if (changed) {
                data.put(player.getUUID(), spirits);
            }
        }
    }

    /** Spirits haunting the player; rolls the configured number of random ones on first sight. */
    public static List<HauntData.Haunting> hauntings(ServerPlayer player, HauntData data, long now) {
        List<HauntData.Haunting> spirits = data.get(player.getUUID());
        if (spirits == null) {
            List<SpiritType> all = Spirits.all();
            List<HauntData.Haunting> rolled = new ArrayList<>();
            for (int i = 0, n = Config.section("director").integer("default_spirits"); i < n; i++) {
                rolled.add(bind(player, all.get(player.getRandom().nextInt(all.size())), now));
            }
            data.put(player.getUUID(), rolled);
            return data.get(player.getUUID());
        }
        // Drop spirits whose type no longer exists (renamed/removed between versions).
        List<HauntData.Haunting> known = spirits.stream().filter(h -> Spirits.byId(h.spirit()).isPresent()).toList();
        if (known.size() != spirits.size()) {
            data.put(player.getUUID(), known);
        }
        return known;
    }

    public static SpiritType spirit(HauntData.Haunting haunting) {
        return Spirits.byId(haunting.spirit()).orElseThrow();
    }

    public static void add(ServerPlayer player, HauntData data, SpiritType spirit, long now) {
        List<HauntData.Haunting> spirits = new ArrayList<>(hauntings(player, data, now));
        spirits.add(bind(player, spirit, now));
        data.put(player.getUUID(), spirits);
    }

    /** @param index 0-based position as listed by /haunt info */
    public static boolean remove(ServerPlayer player, HauntData data, int index, long now) {
        List<HauntData.Haunting> spirits = new ArrayList<>(hauntings(player, data, now));
        if (index < 0 || index >= spirits.size()) {
            return false;
        }
        spirits.remove(index);
        data.put(player.getUUID(), spirits);
        return true;
    }

    private static HauntData.Haunting bind(ServerPlayer player, SpiritType spirit, long now) {
        HauntData.Haunting born = HauntData.Haunting.born(spirit.id(), now, player.getRandom());
        return born.rescheduled(now + cooldown(player, born, now));
    }

    public static void fire(ServerPlayer player, HauntEvent event) {
        Restless.LOGGER.debug("Haunt {} -> {}", event.id(), player.getName().getString());
        event.run(player);
    }

    /** How harmful and how frequent the spirit is right now; depends only on the spirit. */
    public static double power(HauntData.Haunting haunting, long now) {
        return Power.of(now - haunting.bornAt(), haunting.anger());
    }

    private static long cooldown(ServerPlayer player, HauntData.Haunting haunting, long now) {
        ServerLevel level = player.level();
        double quiet = Power.daylightFactor(level.isBrightOutside(), level.canSeeSky(player.blockPosition()), !level.isThundering());
        return Power.cooldownTicks(power(haunting, now), spirit(haunting).activity(), player.getRandom().nextDouble(), quiet);
    }

    private static Optional<HauntEvent> pick(ServerPlayer player, SpiritType spirit, double power) {
        List<HauntEvent> candidates = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        double total = 0;
        for (HauntEvent event : HauntEvents.all()) {
            double weight = Power.weight(event.tier(), event.tags(), event.baseWeight(), spirit, power);
            if (weight > 0 && event.canRun(player)) {
                candidates.add(event);
                weights.add(weight);
                total += weight;
            }
        }
        RandomSource random = player.getRandom();
        double roll = random.nextDouble() * total;
        for (int i = 0; i < candidates.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) {
                return Optional.of(candidates.get(i));
            }
        }
        return Optional.empty();
    }
}
