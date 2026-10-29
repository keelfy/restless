package dev.keelfy.restless.haunt;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.spirit.SpiritType;
import dev.keelfy.restless.spirit.Spirits;
import dev.keelfy.restless.spirit.Trait;
import dev.keelfy.restless.spirit.Traits;
import java.util.EnumSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Rolls haunt events for every personal spirit on its player and every territorial spirit on a player near its anchor; each spirit runs its own timer. */
public final class Director {
    private Director() {}

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % Config.section("director").integer("check_interval_ticks") != 0) {
            return;
        }
        long now = server.overworld().getGameTime();
        HauntData data = HauntData.get(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Reactions.watch(player, now);
            List<HauntData.Haunting> spirits = new ArrayList<>(hauntings(player, data, now));
            boolean changed = false;
            int sounds = Reactions.takeSounds(player.getUUID());
            for (int i = 0; i < spirits.size(); i++) {
                HauntData.Haunting haunting = Reactions.fed(spirits.get(i), sounds, player.isCrouching());
                changed |= haunting != spirits.get(i);
                spirits.set(i, haunting);
                Vec3 at = Location.roam(haunting.uuid(), player);
                if (now < haunting.nextEventAt()) {
                    continue;
                }
                SpiritType spirit = spirit(haunting);
                if (canHaunt(player)) {
                    pick(player, spirit, Traits.of(haunting.traits()), power(haunting, now)).ifPresent(event -> fire(player, event, at));
                }
                spirits.set(i, haunting.rescheduled(now + cooldown(player.level(), player.blockPosition(), haunting, now, player.getRandom())));
                changed = true;
            }
            if (changed) {
                data.put(player.getUUID(), spirits);
            }
        }
        tickTerritorial(server, data, now);
    }

    /** A due territorial spirit acts on one random player inside its radius; it reschedules even when nobody is near. */
    private static void tickTerritorial(MinecraftServer server, HauntData data, long now) {
        List<HauntData.Territorial> spirits = new ArrayList<>(data.territorial());
        boolean changed = false;
        for (int i = 0; i < spirits.size(); i++) {
            HauntData.Territorial territorial = spirits.get(i);
            ServerLevel level = server.getLevel(territorial.dimension());
            if (now < territorial.haunting().nextEventAt() || level == null) {
                continue;
            }
            List<ServerPlayer> near = level.players().stream().filter(p -> canHaunt(p) && territorial.reaches(p)).toList();
            if (!near.isEmpty()) {
                ServerPlayer player = near.get(level.getRandom().nextInt(near.size()));
                pick(player, spirit(territorial.haunting()), Traits.of(territorial.haunting().traits()), power(territorial.haunting(), now))
                    .ifPresent(event -> fire(player, event, Vec3.atCenterOf(territorial.anchor())));
            }
            long next = now + cooldown(level, territorial.anchor(), territorial.haunting(), now, level.getRandom());
            spirits.set(i, territorial.rescheduled(next));
            changed = true;
        }
        if (changed) {
            data.putTerritorial(spirits);
        }
    }

    private static boolean canHaunt(ServerPlayer player) {
        return !player.isSpectator() && !player.isCreative();
    }

    /** Spirits haunting the player; rolls the configured number of random ones on first sight. */
    public static List<HauntData.Haunting> hauntings(ServerPlayer player, HauntData data, long now) {
        List<HauntData.Haunting> spirits = data.get(player.getUUID());
        if (spirits == null) {
            List<SpiritType> all = Spirits.personal();
            List<HauntData.Haunting> rolled = new ArrayList<>();
            for (int i = 0, n = Config.section("director").integer("default_spirits"); i < n; i++) {
                rolled.add(bind(player, all.get(player.getRandom().nextInt(all.size())), now));
            }
            data.put(player.getUUID(), rolled);
            return data.get(player.getUUID());
        }
        // Drop spirits whose type no longer exists (renamed/removed between versions) or became territorial.
        List<HauntData.Haunting> known = spirits.stream()
            .filter(h -> Spirits.byId(h.spirit()).filter(s -> s.kind() == SpiritType.Kind.PERSONAL).isPresent()).toList();
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

    /** Places a territorial spirit of the given type at {@code anchor} with the configured radius. */
    public static void anchor(HauntData data, SpiritType spirit, ServerLevel level, BlockPos anchor, long now) {
        List<HauntData.Territorial> spirits = new ArrayList<>(data.territorial());
        HauntData.Haunting born = HauntData.Haunting.born(spirit.id(), now, level.getRandom());
        int radius = Config.section("director").integer("territorial_radius");
        spirits.add(new HauntData.Territorial(born.rescheduled(now + cooldown(level, anchor, born, now, level.getRandom())),
            level.dimension(), anchor, radius));
        data.putTerritorial(spirits);
    }

    /** Territorial spirits of one dimension in saved order, as listed by /haunt info after the personal ones. */
    public static List<HauntData.Territorial> territorial(HauntData data, ServerLevel level) {
        return data.territorial().stream().filter(t -> t.dimension().equals(level.dimension())).toList();
    }

    /** @param index 0-based position as listed by /haunt info: the player's own spirits, then the territorial ones of their dimension */
    public static boolean remove(ServerPlayer player, HauntData data, int index, long now) {
        List<HauntData.Haunting> spirits = new ArrayList<>(hauntings(player, data, now));
        if (index >= spirits.size()) {
            List<HauntData.Territorial> here = territorial(data, player.level());
            if (index - spirits.size() >= here.size()) {
                return false;
            }
            List<HauntData.Territorial> all = new ArrayList<>(data.territorial());
            all.remove(here.get(index - spirits.size()));
            data.putTerritorial(all);
            return true;
        }
        if (index < 0) {
            return false;
        }
        Location.forget(spirits.remove(index).uuid());
        data.put(player.getUUID(), spirits);
        return true;
    }

    private static HauntData.Haunting bind(ServerPlayer player, SpiritType spirit, long now) {
        HauntData.Haunting born = HauntData.Haunting.born(spirit.id(), now, player.getRandom());
        return born.rescheduled(now + cooldown(player.level(), player.blockPosition(), born, now, player.getRandom()));
    }

    /** Runs an event with no spirit behind it; free-source events fall back to a random point around the player. */
    public static void fire(ServerPlayer player, HauntEvent event) {
        fire(player, event, null);
    }

    /** @param at where the spirit is; free-source events come from there */
    public static void fire(ServerPlayer player, HauntEvent event, @Nullable Vec3 at) {
        Restless.LOGGER.debug("Haunt {} -> {}", event.id(), player.getName().getString());
        Location.during(at, () -> event.run(player));
    }

    /** How harmful and how frequent the spirit is right now; depends only on the spirit. */
    public static double power(HauntData.Haunting haunting, long now) {
        return Power.of(now - haunting.bornAt(), haunting.anger());
    }

    private static long cooldown(ServerLevel level, BlockPos at, HauntData.Haunting haunting, long now, RandomSource random) {
        double quiet = Power.daylightFactor(level.isBrightOutside(), level.canSeeSky(at), !level.isThundering());
        double activity = spirit(haunting).activity() * Traits.activity(Traits.of(haunting.traits()), conditions(level, at));
        return Power.cooldownTicks(power(haunting, now), activity, random.nextDouble(), quiet);
    }

    private static Set<Trait.Condition> conditions(ServerLevel level, BlockPos at) {
        Set<Trait.Condition> now = EnumSet.of(Trait.Condition.ANY);
        if (level.isRaining()) {
            now.add(Trait.Condition.RAIN);
        }
        if (!level.isBrightOutside()) {
            now.add(Trait.Condition.NIGHT);
        }
        if (level.getMaxLocalRawBrightness(at) < Config.section("director").integer("dark_light_level")) {
            now.add(Trait.Condition.DARK);
        }
        return now;
    }

    /** An event's weight for this spirit as {@link #pick} rolls it; 0 when the event has nothing to act on. */
    public static double weight(HauntEvent event, ServerPlayer player, SpiritType spirit, List<Trait> traits, double power) {
        return event.canRun(player) ? Power.weight(event.tier(), event.tags(), event.baseWeight(), spirit, traits, power) : 0.0;
    }

    private static Optional<HauntEvent> pick(ServerPlayer player, SpiritType spirit, List<Trait> traits, double power) {
        List<HauntEvent> candidates = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        double total = 0;
        for (HauntEvent event : HauntEvents.all()) {
            double weight = weight(event, player, spirit, traits, power);
            if (weight > 0) {
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
