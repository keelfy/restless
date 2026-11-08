package dev.keelfy.restless.haunt;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Charms;
import dev.keelfy.restless.content.Draughts;
import dev.keelfy.restless.content.OmenCandle;
import dev.keelfy.restless.spirit.SpiritType;
import dev.keelfy.restless.spirit.Spirits;
import dev.keelfy.restless.spirit.Story;
import dev.keelfy.restless.spirit.Trait;
import dev.keelfy.restless.spirit.Traits;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Rolls haunt events for every personal spirit on its player and every territorial spirit on a player near its anchor;
 * each spirit runs its own timer.
 */
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
                Outcome[] outcome = {Outcome.QUIET};
                // A drinker of the Dreamless Draught is left alone by their mara while the night goes by.
                if (canHaunt(player) && !(Draughts.dreamless(player) && "mara".equals(haunting.spirit()))) {
                    Said.during(
                            haunting.uuid(),
                            () -> outcome[0] = pick(
                                            player,
                                            spirit,
                                            Traits.of(haunting.traits()),
                                            power(haunting, now),
                                            haunting.evolved(),
                                            null)
                                    .map(event -> fireUnlessWarded(player, event, at))
                                    .orElse(Outcome.NOTHING));
                }
                HauntData.Haunting acted = outcome[0] == Outcome.FIRED ? Births.noticing(player, haunting) : haunting;
                spirits.set(
                        i,
                        acted.rescheduled(now
                                + next(
                                        outcome[0],
                                        player.level(),
                                        player.blockPosition(),
                                        haunting,
                                        now,
                                        player.getRandom())));
                changed = true;
            }
            if (changed) {
                data.put(player.getUUID(), spirits);
            }
        }
        tickTerritorial(server, data, now);
    }

    /**
     * A due territorial spirit acts on one random player inside its radius; it reschedules even when nobody is near.
     */
    private static void tickTerritorial(MinecraftServer server, HauntData data, long now) {
        for (HauntData.Territorial territorial : data.territorial()) {
            ServerLevel level = server.getLevel(territorial.dimension());
            if (now < territorial.haunting().nextEventAt() || level == null) {
                continue;
            }
            List<ServerPlayer> near = level.players().stream()
                    .filter(p -> canHaunt(p) && territorial.reaches(p))
                    .toList();
            Outcome[] outcome = {Outcome.QUIET};
            if (!near.isEmpty()) {
                ServerPlayer player = near.get(level.getRandom().nextInt(near.size()));
                Said.during(
                        territorial.haunting().uuid(),
                        () -> outcome[0] = pick(
                                        player,
                                        spirit(territorial.haunting()),
                                        Traits.of(territorial.haunting().traits()),
                                        power(territorial.haunting(), now),
                                        territorial.haunting().evolved(),
                                        null)
                                .map(event -> fireUnlessWarded(player, event, Vec3.atCenterOf(territorial.anchor())))
                                .orElse(Outcome.NOTHING));
            }
            long next =
                    now + next(outcome[0], level, territorial.anchor(), territorial.haunting(), now, level.getRandom());
            data.change(territorial.haunting().uuid(), h -> h.rescheduled(next));
        }
    }

    /** How a due spirit's turn ended. */
    public enum Outcome {
        /** An event ran. */
        FIRED,
        /** A ward stopped the event: the spirit was beaten, so it waits out a full cooldown. */
        WARDED,
        /** Nobody to haunt, or nothing to act on: the spirit waits out a full cooldown. */
        QUIET,
        /** It tried and nothing could run (no event had anything to act on, or the event failed). */
        NOTHING
    }

    /**
     * Ticks until the spirit's next turn: a turn where it had somebody to haunt but nothing could run comes back after
     * {@code director.retry_ticks}, not a full cooldown, so a bare surrounding does not silence it for minutes.
     */
    private static long next(
            Outcome outcome,
            ServerLevel level,
            BlockPos at,
            HauntData.Haunting haunting,
            long now,
            RandomSource random) {
        return outcome == Outcome.NOTHING
                ? Config.section("director").integer("retry_ticks")
                : cooldown(level, at, haunting, now, random);
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
            Journal.begin(player);
            return data.get(player.getUUID());
        }
        // Drop spirits whose type no longer exists (renamed/removed between versions) or became territorial.
        List<HauntData.Haunting> known = spirits.stream()
                .filter(h -> Spirits.byId(h.spirit())
                        .filter(s -> s.kind() == SpiritType.Kind.PERSONAL)
                        .isPresent())
                .toList();
        if (known.size() != spirits.size()) {
            data.put(player.getUUID(), known);
        }
        return known;
    }

    /** The spirit as /haunt history names it: its type and the story of who it was. */
    public static String label(HauntData.Haunting haunting) {
        return haunting.spirit() + " \"" + Story.of(haunting.seed()) + "\"";
    }

    public static SpiritType spirit(HauntData.Haunting haunting) {
        return Spirits.byId(haunting.spirit()).orElseThrow();
    }

    /** Binds a spirit to the player; a type they already have is not bound twice, the one they have is returned. */
    public static HauntData.Haunting add(ServerPlayer player, HauntData data, SpiritType spirit, long now) {
        List<HauntData.Haunting> spirits = new ArrayList<>(hauntings(player, data, now));
        Optional<HauntData.Haunting> same =
                spirits.stream().filter(h -> h.spirit().equals(spirit.id())).findFirst();
        if (same.isPresent()) {
            return same.get();
        }
        HauntData.Haunting bound = bind(player, spirit, now);
        spirits.add(bound);
        data.put(player.getUUID(), spirits);
        return bound;
    }

    /** Places a territorial spirit of the given type at {@code anchor} with the configured radius. */
    public static void anchor(HauntData data, SpiritType spirit, ServerLevel level, BlockPos anchor, long now) {
        HauntData.Haunting born = HauntData.Haunting.born(spirit.id(), now, level.getRandom());
        int radius = Config.section("director").integer("territorial_radius");
        data.putTerritorial(new HauntData.Territorial(
                born.rescheduled(now + cooldown(level, anchor, born, now, level.getRandom())),
                level.dimension(),
                anchor,
                radius));
    }

    /** Territorial spirits of one dimension in saved order, as listed by /haunt info after the personal ones. */
    public static List<HauntData.Territorial> territorial(HauntData data, ServerLevel level) {
        return data.territorial().stream()
                .filter(t -> t.dimension().equals(level.dimension()))
                .toList();
    }

    /**
     * @param index 0-based position as listed by /haunt info: the player's own spirits, then the territorial ones of
     *     their dimension
     */
    public static boolean remove(ServerPlayer player, HauntData data, int index, long now) {
        List<HauntData.Haunting> spirits = new ArrayList<>(hauntings(player, data, now));
        if (index >= spirits.size()) {
            List<HauntData.Territorial> here = territorial(data, player.level());
            if (index - spirits.size() >= here.size()) {
                return false;
            }
            data.removeTerritorial(here.get(index - spirits.size()).haunting().uuid());
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

    /**
     * The spirit acts on the player now, outside its timer and past every ward (a ritual has called it in);
     * {@code tier}, when given, is the only kind of event it may pick.
     *
     * @return the event it ran, empty when none could run
     */
    public static Optional<HauntEvent> act(
            ServerPlayer player, HauntData.Haunting haunting, @Nullable Vec3 at, @Nullable HauntTier tier) {
        long now = player.level().getServer().overworld().getGameTime();
        HauntEvent[] acted = new HauntEvent[1];
        Said.during(
                haunting.uuid(),
                () -> pick(
                                player,
                                spirit(haunting),
                                Traits.of(haunting.traits()),
                                power(haunting, now),
                                haunting.evolved(),
                                tier)
                        .ifPresent(event -> {
                            fire(player, event, at);
                            Journal.unlock(player, Notebook.Chapter.HAUNTED);
                            acted[0] = event;
                        }));
        return Optional.ofNullable(acted[0]);
    }

    /** Runs an event on a player unless a ward stops it; an assault waits out a carried wail first. */
    public static Outcome fireUnlessWarded(ServerPlayer player, HauntEvent event, Vec3 at) {
        if (Wards.stops(player, event)) {
            Wards.blocked(player);
            return Outcome.WARDED;
        }
        boolean keened = event.tags().contains(EventTag.ASSAULT) && Charms.warn(player, at);
        boolean omen = !keened && event.tier() == HauntTier.HARMFUL && OmenCandle.warn(player);
        if (keened || omen) {
            // A wail keens first; the player has the warning's length to act, and wards set up in it still count.
            int warning = Config.section(keened ? "charms" : "consumables")
                    .integer(keened ? "wail_warn_ticks" : "omen_warn_ticks");
            Scheduler.later(player, warning, p -> {
                if (Wards.stops(p, event)) {
                    Wards.blocked(p);
                } else if (fire(p, event, at)) {
                    Journal.unlock(p, Notebook.Chapter.HAUNTED);
                }
            });
            return Outcome.FIRED;
        }
        if (!fire(player, event, at)) {
            return Outcome.NOTHING;
        }
        Journal.unlock(player, Notebook.Chapter.HAUNTED);
        return Outcome.FIRED;
    }

    /** Runs an event with no spirit behind it; free-source events fall back to a random point around the player. */
    public static boolean fire(ServerPlayer player, HauntEvent event) {
        return fire(player, event, null);
    }

    /**
     * @param at where the spirit is; free-source events come from there
     * @return whether the event ran to the end, rather than failing ({@link Faults})
     */
    public static boolean fire(ServerPlayer player, HauntEvent event, @Nullable Vec3 at) {
        Restless.LOGGER.debug("Haunt {} -> {}", event.id(), player.getName().getString());
        Recent.record(player.getUUID(), event.id().toString(), repeatGap());
        MinecraftServer server = player.level().getServer();
        UUID speaker = Said.current();
        History.record(
                server.overworld().getGameTime(),
                player.getName().getString(),
                event.id().toString(),
                speaker == null
                        ? null
                        : HauntData.get(server)
                                .find(speaker)
                                .map(Director::label)
                                .orElse(null));
        boolean[] ran = {false};
        Location.during(at, () -> ran[0] = Faults.run(event, player));
        if (ran[0]) {
            Bands.fired(event, player);
            Sanity.shock(player);
        }
        return ran[0];
    }

    /**
     * Events that must happen before one repeats: {@code director.repeat_share} of all registered events, at least 1.
     */
    private static int repeatGap() {
        return Math.max(1, (int) Math.ceil(Config.section("director").num("repeat_share")
                * HauntEvents.all().size()));
    }

    /** How harmful and how frequent the spirit is right now; depends only on the spirit. */
    public static double power(HauntData.Haunting haunting, long now) {
        return Power.of(now - haunting.bornAt(), haunting.anger(), haunting.evolved());
    }

    private static long cooldown(
            ServerLevel level, BlockPos at, HauntData.Haunting haunting, long now, RandomSource random) {
        double quiet = Power.daylightFactor(level.isBrightOutside(), level.canSeeSky(at), !level.isThundering());
        Set<Trait.Condition> around = conditions(level, at);
        double activity = spirit(haunting).activity(around) * Traits.activity(Traits.of(haunting.traits()), around);
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

    /**
     * An event's weight for this spirit as {@link #pick} rolls it; 0 when the event has nothing to act on or the spirit
     * is not past its gate.
     */
    public static double weight(
            HauntEvent event,
            ServerPlayer player,
            SpiritType spirit,
            List<Trait> traits,
            double power,
            boolean evolved) {
        return open(event, player, spirit, power, evolved)
                ? weight(event, Faults.canRun(event, player), spirit, traits, power, evolved)
                : 0.0;
    }

    /** The checks that cost nothing, made before {@link HauntEvent#canRun} looks around the player. */
    private static boolean open(
            HauntEvent event, ServerPlayer player, SpiritType spirit, double power, boolean evolved) {
        return event.opensAt(power, evolved) && Bands.open(event, player, spirit, power, evolved);
    }

    /**
     * Boosted by {@link Rarity}: an event that seldom has anything to act on is likelier when it does; and by
     * {@link HauntEvent#signature}: an evolved spirit's own scenario is likelier still.
     */
    private static double weight(
            HauntEvent event, boolean ready, SpiritType spirit, List<Trait> traits, double power, boolean evolved) {
        return ready
                ? Power.weight(event.tier(), event.tags(), event.baseWeight(), spirit, traits, power)
                        * Rarity.boost(event.id().toString())
                        * event.signature(spirit, evolved)
                : 0.0;
    }

    /** Events the player went through lately ({@link Recent}) are left out, unless they are all that can run. */
    private static Optional<HauntEvent> pick(
            ServerPlayer player,
            SpiritType spirit,
            List<Trait> traits,
            double power,
            boolean evolved,
            @Nullable HauntTier tier) {
        List<HauntEvent> candidates = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        List<Boolean> stale = new ArrayList<>();
        boolean anyFresh = false;
        for (HauntEvent event : HauntEvents.all()) {
            if (tier != null && event.tier() != tier) {
                continue;
            }
            if (!open(event, player, spirit, power, evolved)) {
                continue;
            }
            String id = event.id().toString();
            boolean ready = Faults.canRun(event, player);
            Rarity.observe(id, ready);
            double weight = weight(event, ready, spirit, traits, power, evolved);
            if (weight > 0) {
                boolean repeat = Recent.blocks(player.getUUID(), id);
                candidates.add(event);
                weights.add(weight);
                stale.add(repeat);
                anyFresh |= !repeat;
            }
        }
        double total = 0;
        for (int i = candidates.size() - 1; i >= 0; i--) {
            if (anyFresh && stale.get(i)) {
                candidates.remove(i);
                weights.remove(i);
            } else {
                total += weights.get(i);
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
