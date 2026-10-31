package dev.keelfy.restless.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LangTest {
    @Test
    void everyEventHasANameAndADescriptionInEveryLanguage() {
        var events = Config.defaults().getAsJsonObject("events").keySet();
        for (String code : Lang.LANGUAGES) {
            JsonObject file = Lang.defaults().get(code).getAsJsonObject("events");
            assertEquals(events, file.keySet(), code + " lists different events than the config");
            for (String id : events) {
                JsonObject entry = file.getAsJsonObject(id);
                assertFalse(entry.get("name").getAsString().isBlank(), code + " name of " + id);
                assertFalse(entry.get("description").getAsString().isBlank(), code + " description of " + id);
            }
        }
    }

    @Test
    void languageIsChosenByCodeAndFallsBackToEnglishThenToTheId() {
        assertEquals("Cat Hiss", Lang.name("en_us", "cat_hiss"));
        assertEquals("Шипение кошки", Lang.name("ru_ru", "minecraft:cat_hiss"));
        assertEquals("Cat Hiss", Lang.name("fr_fr", "cat_hiss"));
        assertEquals("minecraft:nope", Lang.name("ru_ru", "minecraft:nope"));
        assertEquals("", Lang.description("ru_ru", "nope"));
    }

    @Test
    void userCopyOverridesOnlyWhatItSets(@TempDir Path dir) throws IOException {
        Files.createDirectories(dir.resolve("restless/lang"));
        Files.writeString(
                dir.resolve("restless/lang/ru.json"),
                "{\"version\":1,\"events\":{\"minecraft:cat_hiss\":{\"name\":\"Мяу\"}}}");
        Lang.load(dir);
        try {
            assertEquals("Мяу", Lang.name("ru_ru", "cat_hiss"));
            assertTrue(Files.exists(dir.resolve("restless/lang/en.json")));
            assertEquals(
                    Lang.defaults()
                            .get("ru")
                            .getAsJsonObject("events")
                            .getAsJsonObject("minecraft:cat_hiss")
                            .get("description")
                            .getAsString(),
                    Lang.description("ru_ru", "cat_hiss"));
        } finally {
            Lang.load(dir.resolve("none"));
        }
    }
}
