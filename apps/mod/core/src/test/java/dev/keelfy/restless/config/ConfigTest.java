package dev.keelfy.restless.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigTest {
    private static final Path SOURCES = Path.of("src/main/java");
    private static final Pattern EVENT_ID = Pattern.compile("\\b(?:light|harmful|sound|at|on)\\(\"([a-z_:]+)\"");
    private static final Pattern OWN_ID = Pattern.compile("public Identifier id\\(\\) \\{\\s*return Identifier\\.parse\\(\"([a-z_:]+)\"\\);");
    private static final Pattern KEY = Pattern.compile("\\.(?:num|integer|text|texts|lines|section|tone|burst)\\(\"([a-z_]+)\"\\)");
    private static final Pattern LITERAL = Pattern.compile("\"([a-z_]+)\"");

    @Test
    void userFileOverridesOnlyWhatItSets(@TempDir Path dir) throws IOException {
        // No version in the file: taken as version 1, so its bare event ids are namespaced on the way in.
        Files.createDirectories(dir.resolve("restless"));
        Files.writeString(dir.resolve("restless/mechanisms.json"), "{\"events\":{\"door\":{\"radius\":3}}}");
        Config.load(dir);
        try {
            assertEquals(3, Config.event("door").integer("radius"));
            assertEquals(1.0, Config.event("door").num("weight"));
            assertEquals(0.4, Config.event("footsteps").num("weight"));
        } finally {
            Config.load(dir.resolve("none"));
        }
    }

    @Test
    void firstRunWritesEveryFile(@TempDir Path dir) {
        Config.load(dir);
        for (String name : Config.FILES) {
            assertTrue(Files.exists(dir.resolve("restless/" + name + ".json")), name);
        }
    }

    @Test
    void bundledFilesAreVersionedAndDoNotOverlap() {
        int leaves = 0;
        for (String name : Config.FILES) {
            JsonObject file = JsonParser.parseString(Config.bundledText(name)).getAsJsonObject();
            assertEquals(Migrations.BUILT_IN.latest(name), file.get(Config.VERSION).getAsInt(), name + " version");
            file.remove(Config.VERSION);
            leaves += leaves(file);
        }
        assertEquals(leaves, leaves(Config.defaults()), "a key is defined in two files");
    }

    @Test
    void oldFileIsMigratedAndTheOldOneKept(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("haunt.json"), "{\"version\":1,\"director\":{\"check_interval_ticks\":7,\"gone\":1}}");
        Migrations migrations = Migrations.BUILT_IN.plus("haunt", List.<UnaryOperator<JsonObject>>of(json -> {
            Migrations.move(json, "director.check_interval_ticks", "director.check_interval");
            Migrations.remove(json, "director.gone");
            return json;
        }));
        JsonObject tree = Config.read(dir, name -> name.equals("haunt")
            ? "{\"version\":2,\"director\":{\"check_interval\":20,\"default_spirits\":3}}" : Config.bundledText(name), migrations);
        JsonObject director = tree.getAsJsonObject("director");
        assertEquals(7, director.get("check_interval").getAsInt());
        assertEquals(3, director.get("default_spirits").getAsInt());
        assertFalse(director.has("check_interval_ticks") || director.has("gone"));
        JsonObject written = JsonParser.parseString(Files.readString(dir.resolve("haunt.json"))).getAsJsonObject();
        assertEquals(2, written.get(Config.VERSION).getAsInt());
        assertEquals(7, written.getAsJsonObject("director").get("check_interval").getAsInt());
        assertTrue(Files.readString(dir.resolve("haunt.json.v1.bak")).contains("check_interval_ticks"));
    }

    @Test
    void newerOrBrokenFileFallsBackAndIsLeftAlone(@TempDir Path dir) throws IOException {
        String newer = "{\"version\":99,\"director\":{\"check_interval_ticks\":7}}";
        Files.writeString(dir.resolve("haunt.json"), newer);
        Files.writeString(dir.resolve("spirits.json"), "{ not json");
        JsonObject tree = Config.read(dir, Config::bundledText, Migrations.BUILT_IN);
        assertEquals(20, tree.getAsJsonObject("director").get("check_interval_ticks").getAsInt());
        assertTrue(tree.has("spirits"));
        assertEquals(newer, Files.readString(dir.resolve("haunt.json")));
        assertEquals("{ not json", Files.readString(dir.resolve("spirits.json")));
    }

    @Test
    void missingValueFailsLoudly() {
        assertThrows(IllegalStateException.class, () -> Config.event("door").num("nope"));
        assertThrows(IllegalStateException.class, () -> Config.event("no_such_event"));
    }

    @Test
    void everyEventInCodeHasAWeight() throws IOException {
        JsonObject events = Config.defaults().getAsJsonObject("events");
        for (String id : eventIds()) {
            String full = Config.eventKey(id);
            assertTrue(events.has(full) && events.getAsJsonObject(full).has("weight"), "no weight for event " + full);
        }
    }

    @Test
    void everyKeyReadInCodeExistsAndNoKeyIsLeftUnused() throws IOException {
        Set<String> defined = new HashSet<>();
        collectKeys(Config.defaults(), defined);
        // Spirit affinities are keyed by event tag names (upper case) and read as a whole.
        defined.removeIf(key -> !key.matches("[a-z_]+"));
        // Reached through a prefix (Config.event(id), "spirits." + id), never by their own name.
        defined.removeAll(Set.of("events", "spirits"));
        Set<String> read = new HashSet<>();
        Set<String> literals = new HashSet<>();
        for (String source : sources()) {
            Matcher key = KEY.matcher(source);
            while (key.find()) {
                read.add(key.group(1));
            }
            Matcher literal = LITERAL.matcher(source);
            while (literal.find()) {
                literals.add(literal.group(1));
            }
        }
        for (String key : read) {
            assertTrue(defined.contains(key), "code reads a key the defaults lack: " + key);
        }
        for (String key : defined) {
            assertTrue(literals.contains(key), "default never read by code: " + key);
        }
    }

    private static Set<String> eventIds() throws IOException {
        Set<String> ids = new HashSet<>();
        for (String source : sources()) {
            Matcher helper = EVENT_ID.matcher(source);
            while (helper.find()) {
                ids.add(helper.group(1));
            }
            Matcher own = OWN_ID.matcher(source);
            while (own.find() && source.contains("implements HauntEvent")) {
                ids.add(own.group(1));
            }
        }
        return ids;
    }

    private static void collectKeys(JsonObject json, Set<String> into) {
        for (var entry : json.entrySet()) {
            into.add(entry.getKey());
            JsonElement value = entry.getValue();
            if (value.isJsonObject()) {
                collectKeys(value.getAsJsonObject(), into);
            }
        }
    }

    private static List<String> sources() throws IOException {
        try (Stream<Path> files = Files.walk(SOURCES)) {
            return files.filter(p -> p.toString().endsWith(".java")).map(ConfigTest::read).collect(Collectors.toList());
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static int leaves(JsonObject json) {
        int count = 0;
        for (var entry : json.entrySet()) {
            count += entry.getValue().isJsonObject() ? leaves(entry.getValue().getAsJsonObject()) : 1;
        }
        return count;
    }
}
