package dev.keelfy.restless.spirit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.keelfy.restless.config.Config;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Texts the configs name by id live in the lang files; a missing one would show the raw key to the player. */
class LangTest {
    private static final List<String> LANGS = List.of("en_us", "ru_ru");

    @Test
    void everyTraitHintAndDeathCauseIsTranslated() throws IOException {
        for (String lang : LANGS) {
            String file = read("/assets/restless/lang/" + lang + ".json");
            for (Trait trait : Traits.all()) {
                has(file, lang, "trait.restless." + trait.id() + ".hint");
            }
            for (String cause : Config.section("story").texts("causes")) {
                has(file, lang, "story.restless.cause." + cause);
            }
        }
    }

    private static void has(String file, String lang, String key) {
        assertTrue(file.contains("\"" + key + "\""), lang + " lacks " + key);
    }

    private static String read(String path) throws IOException {
        try (InputStream in = LangTest.class.getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
