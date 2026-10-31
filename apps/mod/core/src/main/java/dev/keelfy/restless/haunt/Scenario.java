package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A scripted mix of existing events played as one haunt (see {@code docs/scenarios.md}). The director rolls it like any
 * event, so it counts once for the cooldown and for {@link Recent}; it is only rolled by a spirit past its gate
 * ({@code min_power}, and {@code evolved}). The first beat runs at once and must be light, so harm is always announced;
 * the rest follow through {@link Scheduler}, as the same spirit, from the same point. The scenario is only rolled when
 * every beat can run; a later beat that has nothing to act on by its turn is skipped, and death or logout drops the
 * rest.
 */
public final class Scenario implements HauntEvent {
    /**
     * What a beat may wait for before its {@code after} count starts; the rest of the scenario is dropped on timeout.
     */
    public enum Wait {
        /** The figure the previous beat raised is gone. */
        GONE(60),
        /** The player has slept and got up again. */
        WAKING(10 * 60),
        /** The player is in water. */
        WATER(60);

        private final int timeout;

        Wait(int timeout) {
            this.timeout = timeout;
        }

        private boolean met(ServerPlayer player, HauntEvent before, boolean[] slept) {
            return switch (this) {
                case GONE -> !before.busy(player);
                case WAKING -> {
                    slept[0] |= player.isSleeping();
                    yield slept[0] && !player.isSleeping();
                }
                case WATER -> player.isInWater();
            };
        }
    }

    /**
     * One event of the script, {@code after} seconds after the one before it (0: together with it) and after its wait.
     */
    public record Beat(int after, @Nullable Wait until, HauntEvent event) {
        public Beat(int after, HauntEvent event) {
            this(after, null, event);
        }
    }

    /** Ticks between looks at a wait. */
    private static final int POLL = 10;

    private final Identifier id;
    private final List<Beat> beats;
    private final HauntTier tier;
    private final Set<EventTag> tags;

    public Scenario(Identifier id, List<Beat> beats) {
        if (beats.isEmpty() || beats.getFirst().event().tier() != HauntTier.LIGHT) {
            throw new IllegalArgumentException("Scenario " + id + " must open with a light event");
        }
        if (beats.getFirst().after() != 0
                || beats.getFirst().until() != null
                || beats.stream().anyMatch(b -> b.after() < 0)) {
            throw new IllegalArgumentException("Scenario " + id + " must start at once and only move forward");
        }
        if (beats.stream().anyMatch(b -> b.event() instanceof Scenario)) {
            throw new IllegalArgumentException("Scenario " + id + " must be made of events, not scenarios");
        }
        this.id = id;
        this.beats = List.copyOf(beats);
        this.tier = beats.stream().anyMatch(b -> b.event().tier() == HauntTier.HARMFUL)
                ? HauntTier.HARMFUL
                : HauntTier.LIGHT;
        Set<EventTag> all = EnumSet.noneOf(EventTag.class);
        beats.forEach(b -> all.addAll(b.event().tags()));
        this.tags = Set.copyOf(all);
    }

    /**
     * Reads a script written as in the docs: event ids with {@code +N} (seconds) between them, {@code +0} for "at the
     * same time"; no {@code +N} means 0. {@code >gone}, {@code >waking} or {@code >water} before an event makes it
     * wait.
     */
    public static Scenario parse(Identifier id, String script, Function<Identifier, HauntEvent> events) {
        List<Beat> beats = new ArrayList<>();
        int after = 0;
        Wait wait = null;
        for (String token : script.trim().split("\\s+")) {
            if (token.startsWith("+")) {
                after += Integer.parseInt(token.substring(1));
            } else if (token.startsWith(">")) {
                wait = Wait.valueOf(token.substring(1).toUpperCase(Locale.ROOT));
            } else {
                beats.add(new Beat(after, wait, events.apply(Identifier.parse(token))));
                after = 0;
                wait = null;
            }
        }
        return new Scenario(id, beats);
    }

    public List<Beat> beats() {
        return beats;
    }

    @Override
    public Identifier id() {
        return id;
    }

    /** Harmful if any beat is: the director weighs it like a harmful event, and a light-only ritual never calls it. */
    @Override
    public HauntTier tier() {
        return tier;
    }

    /** Every beat's tags, so the spirit's affinity for each of them counts. */
    @Override
    public Set<EventTag> tags() {
        return tags;
    }

    @Override
    public boolean opensAt(double power, boolean evolved) {
        Config.Section config = Config.event(id.toString());
        return power >= config.num("min_power") && (evolved || !config.flag("evolved"));
    }

    /**
     * Every beat must have something to act on when the scenario starts. Each is checked again when its turn comes: the
     * player may have moved by then, and a beat that no longer fits is just skipped.
     */
    @Override
    public boolean canRun(ServerPlayer player) {
        return beats.stream().allMatch(b -> b.event().canRun(player));
    }

    @Override
    public void run(ServerPlayer player) {
        Vec3 at = Location.current();
        UUID spirit = Said.current();
        beats.getFirst().event().run(player);
        follow(player, 1, 0, at, spirit);
    }

    /** Schedules the beats from {@code from} on; a waiting beat schedules the ones after it once its wait is over. */
    private void follow(ServerPlayer player, int from, int start, @Nullable Vec3 at, @Nullable UUID spirit) {
        int delay = start;
        for (int i = from; i < beats.size(); i++) {
            Beat beat = beats.get(i);
            if (beat.until() != null) {
                wait(player, i, delay, at, spirit);
                return;
            }
            delay += beat.after() * Ticks.SECOND;
            Scheduler.later(player, delay, p -> play(p, beat.event(), at, spirit));
        }
    }

    private void wait(ServerPlayer player, int index, int delay, @Nullable Vec3 at, @Nullable UUID spirit) {
        Beat beat = beats.get(index);
        Wait wait = beat.until();
        HauntEvent before = beats.get(index - 1).event();
        boolean[] slept = {false};
        int hold = beat.after() * Ticks.SECOND;
        Scheduler.repeat(player, delay, POLL, (p, n) -> {
            if (!wait.met(p, before, slept)) {
                return n * POLL < wait.timeout * Ticks.SECOND;
            }
            Scheduler.later(p, hold, q -> play(q, beat.event(), at, spirit));
            follow(p, index + 1, hold, at, spirit);
            return false;
        });
    }

    private static void play(ServerPlayer player, HauntEvent event, @Nullable Vec3 at, @Nullable UUID spirit) {
        if (!event.canRun(player)) {
            return;
        }
        Location.during(at, () -> {
            if (spirit == null) {
                event.run(player);
            } else {
                Said.during(spirit, () -> event.run(player));
            }
        });
    }
}
