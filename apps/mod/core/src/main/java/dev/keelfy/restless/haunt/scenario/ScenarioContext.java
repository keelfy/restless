package dev.keelfy.restless.haunt.scenario;

import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/** One fictional player's name, shared by a scenario and its scheduled work. */
public final class ScenarioContext {
    private static @Nullable ScenarioContext current;
    private @Nullable String nickname;

    private ScenarioContext() {}

    public static void start(Runnable action) {
        during(new ScenarioContext(), action);
    }

    public static @Nullable ScenarioContext current() {
        return current;
    }

    public static void during(@Nullable ScenarioContext context, Runnable action) {
        ScenarioContext previous = current;
        current = context;
        try {
            action.run();
        } finally {
            current = previous;
        }
    }

    /** Reuses the scenario's first generated name; standalone events still generate their own. */
    public static String nickname(Supplier<String> generate) {
        if (current == null) return generate.get();
        if (current.nickname == null) current.nickname = generate.get();
        return current.nickname;
    }

    /** Resolves an anonymous speaker after a beat has introduced a named stranger. */
    public static String speaker(String fallback) {
        return current != null && current.nickname != null ? current.nickname : fallback;
    }
}
