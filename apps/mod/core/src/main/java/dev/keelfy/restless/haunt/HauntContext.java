package dev.keelfy.restless.haunt;

import dev.keelfy.restless.haunt.recall.Said;
import dev.keelfy.restless.haunt.scenario.ScenarioContext;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * What a running event knows about who started it: the spirit that speaks ({@link Said}), the point it comes from
 * ({@link Location}) and the scenario it belongs to ({@link ScenarioContext}). {@link Scheduler} and {@link Restore}
 * capture it when work is scheduled and run that work inside it, so a later step speaks and sounds like the event that
 * scheduled it.
 */
record HauntContext(
        @Nullable UUID spirit, @Nullable Vec3 at, @Nullable ScenarioContext scenario) {
    static HauntContext capture() {
        return new HauntContext(Said.current(), Location.current(), ScenarioContext.current());
    }

    /** Runs {@code action} inside this context; the outer one comes back afterwards, even if it throws. */
    void during(Runnable action) {
        Said.during(spirit, () -> Location.during(at, () -> ScenarioContext.during(scenario, action)));
    }
}
