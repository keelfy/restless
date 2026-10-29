package dev.keelfy.restless.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.slf4j.Logger;

/**
 * Every tunable value of the mod. The tree is split over bundled files ({@link #FILES}); {@code config/restless/}
 * holds a copy of each, written on first run, and whatever a copy overrides wins. A value missing from both is a bug
 * and fails loudly. Each file has a {@code version}; an older copy is migrated (see {@link Migrations}), a newer one
 * is ignored. No Minecraft types, so the pure math can read it in unit tests.
 */
public final class Config {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** The bundled files, each a slice of the same tree (an {@code events} object in several of them is merged). */
    static final List<String> FILES = List.of(
        "haunt", "spirits", "lights", "mechanisms", "sounds", "afflictions", "assaults", "items", "blocks", "mobs", "presence");
    /** The namespace of events that use only vanilla content. */
    static final String DEFAULT_NAMESPACE = "minecraft";
    static final String VERSION = "version";
    private static final String DIR = "restless";
    private static final String RESOURCES = "/restless-config/";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static volatile JsonObject root;

    private Config() {}

    /** Reads (or first writes) the files in {@code <configDir>/restless/}; call once, before any event can run. */
    public static void load(Path configDir) {
        root = read(configDir.resolve(DIR), Config::bundledText, Migrations.BUILT_IN);
    }

    /** One group of values by dotted path, e.g. {@code danger} or {@code spirits.shade.affinity}. */
    public static Section section(String path) {
        JsonObject json = root();
        for (String part : path.split("\\.")) {
            json = json.has(part) && json.get(part).isJsonObject()
                ? json.getAsJsonObject(part)
                : fail("Missing config section " + path);
        }
        return new Section(json, path);
    }

    /** The values of one haunt event, by its id; written like a vanilla one, so no namespace means {@code minecraft}. */
    public static Section event(String id) {
        return section("events." + eventKey(id));
    }

    /** The key an event has in the files: its id with the namespace spelled out. */
    static String eventKey(String id) {
        return id.indexOf(':') < 0 ? DEFAULT_NAMESPACE + ":" + id : id;
    }

    static JsonObject read(Path dir, Function<String, String> bundledText, Migrations migrations) {
        JsonObject tree = new JsonObject();
        for (String name : FILES) {
            merge(tree, loadFile(dir, name, bundledText.apply(name), migrations));
        }
        return tree;
    }

    /** The bundled files merged, with no player copies: what a fresh install runs on. */
    static JsonObject defaults() {
        JsonObject tree = new JsonObject();
        for (String name : FILES) {
            JsonObject file = JsonParser.parseString(bundledText(name)).getAsJsonObject();
            file.remove(VERSION);
            merge(tree, file);
        }
        return tree;
    }

    static String bundledText(String name) {
        String resource = RESOURCES + name + ".json";
        try (InputStream in = Config.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing bundled " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read bundled " + resource, e);
        }
    }

    private static JsonObject root() {
        JsonObject loaded = root;
        if (loaded == null) {
            // Nothing loaded yet (unit tests, or an early class load): the defaults are the config.
            loaded = defaults();
            root = loaded;
        }
        return loaded;
    }

    /** One file's slice: the bundled values under the player's copy, migrated first if the copy is from an older release. */
    static JsonObject loadFile(Path dir, String name, String text, Migrations migrations) {
        JsonObject bundled = JsonParser.parseString(text).getAsJsonObject();
        int current = bundled.get(VERSION).getAsInt();
        if (current != migrations.latest(name)) {
            throw new IllegalStateException(name + ".json is version " + current + " but its migrations lead to " + migrations.latest(name));
        }
        Path file = dir.resolve(name + ".json");
        JsonObject merged = bundled.deepCopy();
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(dir);
                Files.writeString(file, text, StandardCharsets.UTF_8);
            } else {
                JsonObject user = parse(file);
                int version = user.has(VERSION) ? user.get(VERSION).getAsInt() : 1;
                if (version > current) {
                    LOGGER.error("{} is version {}, newer than this mod understands ({}); using the built-in defaults for it", file, version, current);
                } else {
                    if (version < current) {
                        user = migrations.upgrade(name, user, version);
                    }
                    merge(merged, user);
                    merged.addProperty(VERSION, current);
                    if (version < current) {
                        persist(file, version, merged);
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Could not read {}; using the built-in defaults for it", file, e);
            merged = bundled.deepCopy();
        }
        merged.remove(VERSION);
        return merged;
    }

    /** Keeps the old file beside the new one, then writes the migrated, complete file over it. */
    private static void persist(Path file, int from, JsonObject migrated) {
        Path backup = file.resolveSibling(file.getFileName() + ".v" + from + ".bak");
        try {
            Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(file, GSON.toJson(migrated), StandardCharsets.UTF_8);
            LOGGER.info("Migrated {} from version {} (the old file is kept as {})", file, from, backup.getFileName());
        } catch (IOException e) {
            LOGGER.warn("Migrated {} in memory only; could not write it back", file, e);
        }
    }

    private static JsonObject parse(Path file) throws IOException {
        try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(in).getAsJsonObject();
        }
    }

    private static void merge(JsonObject base, JsonObject over) {
        for (Map.Entry<String, JsonElement> entry : over.entrySet()) {
            JsonElement mine = base.get(entry.getKey());
            if (mine != null && mine.isJsonObject() && entry.getValue().isJsonObject()) {
                merge(mine.getAsJsonObject(), entry.getValue().getAsJsonObject());
            } else {
                base.add(entry.getKey(), entry.getValue());
            }
        }
    }

    private static <T> T fail(String message) {
        throw new IllegalStateException(message);
    }

    /** A sound's loudness and pitch. */
    public record Tone(float volume, float pitch) {}

    /** A burst of particles: how many, how far they scatter on each axis, how fast, and the height above the origin. */
    public record Burst(int count, double dx, double dy, double dz, double speed, double offsetY) {}

    /** Typed reads from one group of values. */
    public static final class Section {
        private final JsonObject json;
        private final String path;

        private Section(JsonObject json, String path) {
            this.json = json;
            this.path = path;
        }

        /** For the few values that may be left out; everything else fails loudly when missing. */
        public boolean has(String key) {
            return json.has(key);
        }

        public double num(String key) {
            return get(key).getAsDouble();
        }

        public int integer(String key) {
            return get(key).getAsInt();
        }

        public String text(String key) {
            return get(key).getAsString();
        }

        public List<String> texts(String key) {
            List<String> out = new ArrayList<>();
            for (JsonElement item : array(key)) {
                out.add(item.getAsString());
            }
            return out;
        }

        /** A list of lists of text, e.g. the lines of each sign. */
        public List<List<String>> lines(String key) {
            List<List<String>> out = new ArrayList<>();
            for (JsonElement item : array(key)) {
                List<String> row = new ArrayList<>();
                for (JsonElement part : item.getAsJsonArray()) {
                    row.add(part.getAsString());
                }
                out.add(row);
            }
            return out;
        }

        public Tone tone(String key) {
            Section sound = section(key);
            return new Tone((float) sound.num("volume"), (float) sound.num("pitch"));
        }

        public Burst burst(String key) {
            Section particles = section(key);
            return new Burst(particles.integer("count"), particles.num("dx"), particles.num("dy"), particles.num("dz"),
                particles.num("speed"), particles.num("offset_y"));
        }

        public Section section(String key) {
            return Config.section(path + "." + key);
        }

        public Set<String> keys() {
            return json.keySet();
        }

        private JsonArray array(String key) {
            return get(key).getAsJsonArray();
        }

        private JsonElement get(String key) {
            JsonElement value = json.get(key);
            if (value == null) {
                throw new IllegalStateException("Missing config value " + path + "." + key);
            }
            return value;
        }
    }
}
