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

/** Rolls haunt events for every player's bound spirit. */
public final class Director {
    private static final int CHECK_INTERVAL = 20;

    private Director() {}

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % CHECK_INTERVAL != 0) {
            return;
        }
        long now = server.overworld().getGameTime();
        HauntData data = HauntData.get(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            HauntData.Haunting haunting = haunting(player, data, now);
            if (now < haunting.nextEventAt()) {
                continue;
            }
            SpiritType spirit = spirit(haunting);
            if (!player.isSpectator() && !player.isCreative()) {
                pick(player, spirit).ifPresent(event -> fire(player, event));
            }
            schedule(player, data, spirit, now);
        }
    }

    public static HauntData.Haunting haunting(ServerPlayer player, HauntData data, long now) {
        HauntData.Haunting haunting = data.get(player.getUUID());
        if (haunting == null || Spirits.byId(haunting.spirit()).isEmpty()) {
            List<SpiritType> all = Spirits.all();
            SpiritType spirit = all.get(player.getRandom().nextInt(all.size()));
            haunting = new HauntData.Haunting(spirit.id(), now + cooldown(player, spirit));
            data.put(player.getUUID(), haunting);
        }
        return haunting;
    }

    public static SpiritType spirit(HauntData.Haunting haunting) {
        return Spirits.byId(haunting.spirit()).orElseThrow();
    }

    public static void schedule(ServerPlayer player, HauntData data, SpiritType spirit, long now) {
        data.put(player.getUUID(), new HauntData.Haunting(spirit.id(), now + cooldown(player, spirit)));
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
