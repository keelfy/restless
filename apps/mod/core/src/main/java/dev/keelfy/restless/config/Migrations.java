package dev.keelfy.restless.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * How an old config file becomes a current one. Every file carries a {@code version}; a release that renames, moves or
 * drops a key bumps it by adding one step here, and a player's file is walked through the steps it has not seen. Step
 * {@code i} (from 0) turns version {@code i + 1} into version {@code i + 2}. Keys that are new need no step: they come
 * from the bundled defaults.
 */
public final class Migrations {
    /**
     * Version 2 of every event file: event ids carry a namespace ({@code cat_hiss} became {@code minecraft:cat_hiss}).
     */
    private static final List<UnaryOperator<JsonObject>> NAMESPACED_EVENTS = List.of(Migrations::namespaceEvents);

    /**
     * Version 2 of haunt: the world-wide {@code danger} section became per-spirit {@code power}; its progression keys
     * are gone. Version 3: {@code hardcore}, {@code swaps} and {@code journal} are player settings now (see
     * {@link Settings}), no longer part of the data files. Version 4: the charms were rebalanced, see
     * {@link #charmsRebalance}.
     */
    private static final List<UnaryOperator<JsonObject>> POWER =
            List.of(Migrations::dangerToPower, Migrations::settingsLeave, Migrations::charmsRebalance);

    /**
     * Version 2 of lies: strangers' names are made up, not listed; {@code false_slot} has more items; the head turn is
     * timed in ticks.
     */
    private static final List<UnaryOperator<JsonObject>> LIES = List.of(Migrations::liesNicknames);

    /**
     * Version 3 of blocks: {@code false_looks} turns any block of its list into another of it, instead of following a
     * fixed map.
     */
    private static final List<UnaryOperator<JsonObject>> BLOCKS = List.of(
            Migrations::namespaceEvents,
            Migrations::falseLooksPool,
            root -> restlessEvents(root, "mock_grave", "ghost_tree"));

    /** Version 3 of presence: the distant walker walks for longer; the nametag's names are made up, not listed. */
    private static final List<UnaryOperator<JsonObject>> PRESENCE =
            List.of(Migrations::namespaceEvents, Migrations::presenceWalker);

    /**
     * Version 2 of rites: every event uses the mod's own blocks or items, so its id is {@code restless:}, not
     * {@code minecraft:}.
     */
    private static final List<UnaryOperator<JsonObject>> RITES = List.of(root -> restlessEvents(
            root,
            "scarecrow_closer",
            "ouija_hi",
            "spirit_box_word",
            "salt_gap",
            "grave_name",
            "journal_page_torn",
            "dreamcatcher_fails",
            "lantern_soul_fire",
            "charm_falls",
            "altar_midnight"));

    /**
     * Version 3 of mechanisms: {@code muddy_footprints} leaves the mod's footprints, so it is a {@code restless:} id.
     */
    private static final List<UnaryOperator<JsonObject>> MECHANISMS =
            List.of(Migrations::namespaceEvents, root -> restlessEvents(root, "muddy_footprints"));

    /** Version 2 of veils: {@code lone_gravestone} shows the mod's gravestone, so it is a {@code restless:} id. */
    private static final List<UnaryOperator<JsonObject>> VEILS =
            List.of(root -> restlessEvents(root, "lone_gravestone"));

    /** Steps per file name (without {@code .json}). A file with no entry has never changed shape: version 1. */
    static final Migrations BUILT_IN = new Migrations(Map.ofEntries(
            Map.entry("haunt", POWER),
            Map.entry("lights", NAMESPACED_EVENTS),
            Map.entry("mechanisms", MECHANISMS),
            Map.entry("rites", RITES),
            Map.entry("veils", VEILS),
            Map.entry("sounds", NAMESPACED_EVENTS),
            Map.entry("afflictions", NAMESPACED_EVENTS),
            Map.entry("assaults", NAMESPACED_EVENTS),
            Map.entry("items", NAMESPACED_EVENTS),
            Map.entry("blocks", BLOCKS),
            Map.entry("mobs", NAMESPACED_EVENTS),
            Map.entry("presence", PRESENCE),
            Map.entry("lies", LIES)));

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

    /**
     * Moves the values of events from {@code minecraft:<id>} to {@code restless:<id>}; settings the player edited stay.
     */
    private static JsonObject restlessEvents(JsonObject root, String... ids) {
        for (String id : ids) {
            move(root, "events.minecraft:" + id, "events.restless:" + id);
        }
        return root;
    }

    private static JsonObject liesNicknames(JsonObject root) {
        // The old item list goes too, so the longer bundled one is used.
        for (String gone :
                List.of("stranger_joins.names", "stranger_death.names", "false_slot.items", "turned_head.steps")) {
            remove(root, "events.minecraft:" + gone);
        }
        return root;
    }

    private static JsonObject falseLooksPool(JsonObject root) {
        remove(root, "events.minecraft:false_looks.looks");
        return root;
    }

    private static JsonObject presenceWalker(JsonObject root) {
        for (String gone : List.of("distant_walker.max_ticks", "nametag.names")) {
            remove(root, "events.minecraft:" + gone);
        }
        return root;
    }

    /** A charm value still at its old default goes, so the bundled new one is used; one the player changed stays. */
    private static JsonObject charmsRebalance(JsonObject root) {
        Map<String, Double> was = Map.of(
                "face_ticks", 1200.0,
                "shroud_ticks", 120.0,
                "shroud_cooldown_ticks", 1200.0,
                "shroud_sanity_cost", 0.15,
                "house_uses", 10.0);
        JsonElement charms = root.get("charms");
        if (charms != null && charms.isJsonObject()) {
            JsonObject section = charms.getAsJsonObject();
            was.forEach((key, old) -> {
                JsonElement value = section.get(key);
                if (value != null && value.isJsonPrimitive() && value.getAsDouble() == old) {
                    section.remove(key);
                }
            });
        }
        return root;
    }

    private static JsonObject settingsLeave(JsonObject root) {
        for (String gone : List.of("hardcore", "swaps", "journal")) {
            remove(root, gone);
        }
        return root;
    }

    private static JsonObject dangerToPower(JsonObject root) {
        for (String gone : List.of("days_to_max", "advancements_to_max", "days_share", "advancements_share")) {
            remove(root, "danger." + gone);
        }
        move(root, "danger", "power");
        return root;
    }

    /**
     * For use inside a step: moves the value at a dotted path to another, creating objects on the way. No-op if absent.
     */
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
