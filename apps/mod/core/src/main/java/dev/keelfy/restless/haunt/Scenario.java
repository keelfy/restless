package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
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
 * the rest follow through {@link Scheduler}, as the same spirit, from the same point. A later beat with nothing to act
 * on is skipped; death or logout drops the rest.
 */
public final class Scenario implements HauntEvent {
    /** One event of the script, {@code after} seconds after the one before it (0: together with it). */
    public record Beat(int after, HauntEvent event) {}

    private final Identifier id;
    private final List<Beat> beats;
    private final HauntTier tier;
    private final Set<EventTag> tags;

    public Scenario(Identifier id, List<Beat> beats) {
        if (beats.isEmpty() || beats.getFirst().event().tier() != HauntTier.LIGHT) {
            throw new IllegalArgumentException("Scenario " + id + " must open with a light event");
        }
        if (beats.getFirst().after() != 0 || beats.stream().anyMatch(b -> b.after() < 0)) {
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
     * same time"; no {@code +N} means 0.
     */
    public static Scenario parse(Identifier id, String script, Function<Identifier, HauntEvent> events) {
        List<Beat> beats = new ArrayList<>();
        int after = 0;
        for (String token : script.trim().split("\\s+")) {
            if (token.startsWith("+")) {
                after += Integer.parseInt(token.substring(1));
            } else {
                beats.add(new Beat(after, events.apply(Identifier.parse(token))));
                after = 0;
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

    /** Only the opening beat is checked: the rest may find nothing to act on and are skipped. */
    @Override
    public boolean canRun(ServerPlayer player) {
        return beats.getFirst().event().canRun(player);
    }

    @Override
    public void run(ServerPlayer player) {
        Vec3 at = Location.current();
        UUID spirit = Said.current();
        beats.getFirst().event().run(player);
        int delay = 0;
        for (Beat beat : beats.subList(1, beats.size())) {
            delay += beat.after() * Ticks.SECOND;
            Scheduler.later(player, delay, p -> play(p, beat.event(), at, spirit));
        }
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
