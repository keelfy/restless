package dev.keelfy.restless.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Names and descriptions of haunt events, one bundled file per language ({@code lang/<code>.json}); like the rest of
 * the config, {@code config/restless/lang/} holds a copy of each and whatever a copy sets wins. Only what a screen
 * shows lives here, so no Minecraft types. A language or entry that is missing falls back to English, then to the id.
 */
public final class Lang {
    /** The bundled languages, by the part of a Minecraft code before the underscore ({@code ru_ru} is {@code ru}). */
    static final List<String> LANGUAGES = List.of("en", "ru");
    private static final String FALLBACK = "en";
    private static final String DIR = "restless/lang";

    private static volatile Map<String, JsonObject> texts;

    private Lang() {}

    /** Reads (or first writes) the files in {@code <configDir>/restless/lang/}; call once at start. */
    public static void load(Path configDir) {
        texts = LANGUAGES.stream().collect(Collectors.toMap(code -> code,
            code -> Config.loadFile(configDir.resolve(DIR), code, Config.bundledText("lang/" + code), Migrations.BUILT_IN)));
    }

    /** The event's display name in a Minecraft language code such as {@code ru_ru}; the id itself if nobody wrote one. */
    public static String name(String language, String eventId) {
        String name = find(language, eventId, "name");
        return name.isEmpty() ? eventId : name;
    }

    /** What the event does, in plain words; empty if nobody wrote it. */
    public static String description(String language, String eventId) {
        return find(language, eventId, "description");
    }

    static Map<String, JsonObject> defaults() {
        return LANGUAGES.stream().collect(Collectors.toMap(code -> code, code -> {
            JsonObject file = JsonParser.parseString(Config.bundledText("lang/" + code)).getAsJsonObject();
            file.remove(Config.VERSION);
            return file;
        }));
    }

    private static String find(String language, String eventId, String field) {
        Map<String, JsonObject> all = texts();
        String code = language.toLowerCase(Locale.ROOT);
        int cut = code.indexOf('_');
        if (cut >= 0) {
            code = code.substring(0, cut);
        }
        for (String candidate : List.of(code, FALLBACK)) {
            String text = read(all.get(candidate), eventId, field);
            if (!text.isEmpty()) {
                return text;
            }
        }
        return "";
    }

    private static String read(JsonObject file, String eventId, String field) {
        if (file == null || !file.has("events")) {
            return "";
        }
        JsonElement entry = file.getAsJsonObject("events").get(Config.eventKey(eventId));
        JsonElement text = entry != null && entry.isJsonObject() ? entry.getAsJsonObject().get(field) : null;
        return text == null ? "" : text.getAsString();
    }

    private static Map<String, JsonObject> texts() {
        Map<String, JsonObject> loaded = texts;
        if (loaded == null) {
            // Nothing loaded yet (unit tests, or an early class load): the bundled text is the text.
            loaded = defaults();
            texts = loaded;
        }
        return loaded;
    }
}
