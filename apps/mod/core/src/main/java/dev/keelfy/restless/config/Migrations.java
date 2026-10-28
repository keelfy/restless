package dev.keelfy.restless.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * How an old config file becomes a current one. Every file carries a {@code version}; a release that renames, moves
 * or drops a key bumps it by adding one step here, and a player's file is walked through the steps it has not seen.
 * Step {@code i} (from 0) turns version {@code i + 1} into version {@code i + 2}. Keys that are new need no step:
 * they come from the bundled defaults.
 */
public final class Migrations {
    /** Version 2 of every event file: event ids carry a namespace ({@code cat_hiss} became {@code minecraft:cat_hiss}). */
    private static final List<UnaryOperator<JsonObject>> NAMESPACED_EVENTS = List.of(Migrations::namespaceEvents);

    /** Steps per file name (without {@code .json}). A file with no entry has never changed shape: version 1. */
    static final Migrations BUILT_IN = new Migrations(Map.of(
        "lights", NAMESPACED_EVENTS, "mechanisms", NAMESPACED_EVENTS, "sounds", NAMESPACED_EVENTS,
        "afflictions", NAMESPACED_EVENTS, "assaults", NAMESPACED_EVENTS, "items", NAMESPACED_EVENTS,
        "blocks", NAMESPACED_EVENTS, "mobs", NAMESPACED_EVENTS, "presence", NAMESPACED_EVENTS));

    private final Map<String, List<UnaryOperator<JsonObject>>> steps;

    Migrations(Map<String, List<UnaryOperator<JsonObject>>> steps) {
        this.steps = steps;
    }

    /** These steps plus the given ones for one more file; for tests that exercise a step the real ones lack. */
    Migrations plus(String file, List<UnaryOperator<JsonObject>> more) {
        Map<String, List<UnaryOperator<JsonObject>>> all = new HashMap<>(steps);
        all.put(file, more);
        return new Migrations(all);
    }

    /** The version the bundled file of this name must have. */
    int latest(String file) {
        return 1 + steps.getOrDefault(file, List.of()).size();
    }

    /** Runs every step from version {@code from} up to the latest; the result is not stamped with a version. */
    JsonObject upgrade(String file, JsonObject json, int from) {
        List<UnaryOperator<JsonObject>> all = steps.getOrDefault(file, List.of());
        for (int version = from; version <= all.size(); version++) {
            json = all.get(version - 1).apply(json);
        }
        return json;
    }

    private static JsonObject namespaceEvents(JsonObject root) {
        JsonElement events = root.get("events");
        if (events != null && events.isJsonObject()) {
            JsonObject namespaced = new JsonObject();
            for (var entry : events.getAsJsonObject().entrySet()) {
                namespaced.add(Config.eventKey(entry.getKey()), entry.getValue());
            }
            root.add("events", namespaced);
        }
        return root;
    }

    /** For use inside a step: moves the value at a dotted path to another, creating objects on the way. No-op if absent. */
    public static void move(JsonObject root, String from, String to) {
        JsonElement value = remove(root, from);
        if (value == null) {
            return;
        }
        String[] path = to.split("\\.");
        JsonObject at = root;
        for (int i = 0; i < path.length - 1; i++) {
            JsonElement next = at.get(path[i]);
            if (next == null || !next.isJsonObject()) {
                next = new JsonObject();
                at.add(path[i], next);
            }
            at = next.getAsJsonObject();
        }
        at.add(path[path.length - 1], value);
    }

    /** For use inside a step: removes the value at a dotted path and returns it, or null if absent. */
    public static JsonElement remove(JsonObject root, String path) {
        String[] parts = path.split("\\.");
        JsonObject at = root;
        for (int i = 0; i < parts.length - 1; i++) {
            JsonElement next = at.get(parts[i]);
            if (next == null || !next.isJsonObject()) {
                return null;
            }
            at = next.getAsJsonObject();
        }
        return at.remove(parts[parts.length - 1]);
    }
}
