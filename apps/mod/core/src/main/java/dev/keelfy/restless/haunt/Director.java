package dev.keelfy.restless.haunt;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.spirit.SpiritType;
import dev.keelfy.restless.spirit.Spirits;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/** Rolls haunt events for every spirit bound to every player; each spirit runs its own timer. */
public final class Director {
    private static final int CHECK_INTERVAL = 20;
    /** Spirits a player gets on first join. */
    public static final int DEFAULT_SPIRITS = 3;

    private Director() {}

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % CHECK_INTERVAL != 0) {
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
                SpiritType spirit = spirit(spirits.get(i));
                if (!player.isSpectator() && !player.isCreative()) {
                    pick(player, spirit).ifPresent(event -> fire(player, event));
                }
                spirits.set(i, bind(player, spirit, now));
                changed = true;
            }
            if (changed) {
                data.put(player.getUUID(), spirits);
            }
        }
    }

    /** Spirits haunting the player; rolls {@link #DEFAULT_SPIRITS} random ones on first sight. */
    public static List<HauntData.Haunting> hauntings(ServerPlayer player, HauntData data, long now) {
        List<HauntData.Haunting> spirits = data.get(player.getUUID());
        if (spirits == null) {
            List<SpiritType> all = Spirits.all();
            List<HauntData.Haunting> rolled = new ArrayList<>();
            for (int i = 0; i < DEFAULT_SPIRITS; i++) {
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
        return new HauntData.Haunting(spirit.id(), now + cooldown(player, spirit));
    }

    public static void fire(ServerPlayer player, HauntEvent event) {
        Restless.LOGGER.debug("Haunt {} -> {}", event.id(), player.getName().getString());
        event.run(player);
    }

    public static double danger(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        long days = server.overworld().getOverworldClockTime() / 24000L;
        return Danger.of(days, advancements(player));
    }

    /** Completed advancements that have a toast; recipe unlocks are hidden ones and do not count. */
    public static int advancements(ServerPlayer player) {
        int done = 0;
        for (AdvancementHolder holder : player.level().getServer().getAdvancements().getAllAdvancements()) {
            if (holder.value().display().isPresent() && player.getAdvancements().getOrStartProgress(holder).isDone()) {
                done++;
            }
        }
        return done;
    }

    private static long cooldown(ServerPlayer player, SpiritType spirit) {
        return Danger.cooldownTicks(danger(player), spirit.activity(), player.getRandom().nextDouble());
    }

    private static Optional<HauntEvent> pick(ServerPlayer player, SpiritType spirit) {
        double danger = danger(player);
        List<HauntEvent> candidates = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        double total = 0;
        for (HauntEvent event : HauntEvents.all()) {
            double weight = Danger.weight(event.tier(), event.tags(), event.baseWeight(), spirit, danger);
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
