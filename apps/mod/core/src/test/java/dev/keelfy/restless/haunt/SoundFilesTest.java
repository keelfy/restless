package dev.keelfy.restless.haunt;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SoundFilesTest {
    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final String PREFIX = "restless:";

    private static JsonObject sounds() throws IOException {
        return JsonParser.parseString(Files.readString(RESOURCES.resolve("assets/restless/sounds.json")))
                .getAsJsonObject();
    }

    @Test
    void everySoundFileIsAnOggVorbis() throws IOException {
        for (Map.Entry<String, JsonElement> entry : sounds().entrySet()) {
            for (JsonElement variant : entry.getValue().getAsJsonObject().getAsJsonArray("sounds")) {
                if (variant.isJsonObject()
                        && variant.getAsJsonObject().has("type")
                        && "event".equals(variant.getAsJsonObject().get("type").getAsString())) {
                    continue;
                }
                String name = variant.isJsonObject()
                        ? variant.getAsJsonObject().get("name").getAsString()
                        : variant.getAsString();
                if (!name.startsWith(PREFIX)) {
                    continue;
                }
                Path file = RESOURCES.resolve("assets/restless/sounds/" + name.substring(PREFIX.length()) + ".ogg");
                assertTrue(Files.isRegularFile(file), entry.getKey() + ": missing " + file);
                byte[] head = Arrays.copyOf(Files.readAllBytes(file), 64);
                assertArrayEquals(
                        "OggS".getBytes(StandardCharsets.US_ASCII), Arrays.copyOf(head, 4), file + " is not Ogg");
                assertTrue(new String(head, StandardCharsets.ISO_8859_1).contains("vorbis"), file + " is not Vorbis");
            }
        }
    }

    @Test
    void everyStingIdIsASoundEvent() throws IOException {
        JsonObject sounds = sounds();
        for (String group : new String[] {"scares", "jolts"}) {
            JsonObject root = JsonParser.parseString(
                            Files.readString(RESOURCES.resolve("restless-config/" + group + ".json")))
                    .getAsJsonObject();
            for (Map.Entry<String, JsonElement> event :
                    root.getAsJsonObject("events").entrySet()) {
                for (String key : new String[] {"sound_id", "bang_id"}) {
                    JsonElement id = event.getValue().getAsJsonObject().get(key);
                    if (id != null) {
                        String name = id.getAsString();
                        assertTrue(name.startsWith(PREFIX), event.getKey() + "." + key + " not restless: " + name);
                        assertTrue(
                                sounds.has(name.substring(PREFIX.length())),
                                event.getKey() + "." + key + " names no sound event: " + name);
                    }
                }
            }
        }
    }
}
