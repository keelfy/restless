package dev.keelfy.restless.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.divine.Heard;
import dev.keelfy.restless.divine.Reply;
import dev.keelfy.restless.spirit.Trait;
import dev.keelfy.restless.spirit.Traits;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

/**
 * The spirit box answers what it is asked around the clue word, never with another trait's word, and aloud: every word
 * and answer is recorded in each language, and a spoken word is echoed in the asker's own voice.
 */
final class SpiritBoxGameTests {
    private static final String WORD = "frost";
    private static final int TRIES = 200;

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("spirit_box_answers_the_question", helper -> {
            check(helper, answered(helper, "Where are your bones?", "where"), "where must be answered");
            check(helper, answered(helper, "Кто здесь?", "who"), "кто must be answered as who");
            check(helper, answered(helper, "Чего ты хочешь", "want"), "хочешь must match its stem");
            check(helper, !answered(helper, "Where are your bones?", "leave"), "only the asked question is answered");
            check(helper, !answered(helper, "", "presence"), "an empty line asks nothing");
            helper.succeed();
        });
        tests.put("spirit_box_answers_are_no_clues", helper -> {
            Set<String> clues = Traits.all().stream()
                    .map(Trait::words)
                    .flatMap(List::stream)
                    .map(w -> text("word.restless." + w))
                    .collect(Collectors.toSet());
            Config.Section questions = Config.section("spirit_box.questions");
            for (String key : questions.keys()) {
                for (String answer : questions.section(key).texts("answers")) {
                    String text = text("spirit_box.restless." + answer);
                    check(helper, !text.startsWith("spirit_box."), "answer " + answer + " needs a translation");
                    check(helper, !clues.contains(text), "answer " + answer + " is a trait word");
                }
            }
            helper.succeed();
        });
        tests.put("spirit_box_voices_every_part", helper -> {
            JsonObject sounds = JsonParser.parseReader(new InputStreamReader(
                            Reply.class.getResourceAsStream("/assets/restless/sounds.json"), StandardCharsets.UTF_8))
                    .getAsJsonObject();
            List<String> voices = new java.util.ArrayList<>();
            Traits.all().forEach(t -> t.words().forEach(w -> voices.add("word." + w)));
            Config.Section questions = Config.section("spirit_box.questions");
            questions
                    .keys()
                    .forEach(k -> questions.section(k).texts("answers").forEach(a -> voices.add("answer." + a)));
            for (String language : List.of("en_us", "ru_ru")) {
                for (String voice : voices) {
                    String event = "spirit_box." + language + "." + voice;
                    check(helper, sounds.has(event), event + " is not recorded");
                    for (var sound : sounds.getAsJsonObject(event).getAsJsonArray("sounds")) {
                        String name = sound.getAsJsonObject()
                                .get("name")
                                .getAsString()
                                .replace("restless:", "");
                        check(
                                helper,
                                Reply.class.getResource("/assets/restless/sounds/" + name + ".ogg") != null,
                                name + " is missing");
                    }
                }
            }
            check(helper, sounds.has("spirit_box.hiss"), "the box has no static");
            Reply reply = Reply.of(Heard.typed("Where are you?"), WORD, new Random(1));
            check(
                    helper,
                    reply.voice(net.minecraft.core.BlockPos.ZERO).parts().stream()
                            .anyMatch(p -> p.voice().equals("word." + WORD)),
                    "the clue is not voiced");
            helper.succeed();
        });
        tests.put("spirit_box_echoes_the_askers_voice", helper -> {
            // Nothing asked, so the line's long words are echoed; only the spoken one has a sound to give back.
            short[] sound = new short[8000];
            for (int i = 0; i < sound.length; i++) {
                sound[i] = (short) (Math.sin(i * 0.2) * 8000);
            }
            Heard heard = new Heard("tell your secret", Map.of("secret", sound), 16_000);
            Random seeds = new Random(7);
            boolean voiced = false;
            for (int i = 0; i < TRIES; i++) {
                for (Reply.Part part :
                        Reply.of(heard, WORD, new Random(seeds.nextLong())).parts()) {
                    String text = part.text().getString();
                    boolean secret = text.startsWith("secr") || text.startsWith("s-secret");
                    check(helper, secret == (part.pcm().length > 0), "only the spoken word has a voice: " + text);
                    voiced |= secret;
                }
            }
            check(helper, voiced, "a spoken word must be echoed in the asker's voice");
            helper.succeed();
        });
    }

    /** Whether some reply to {@code line} uses an answer of {@code question}; every reply must bear the clue word. */
    private static boolean answered(GameTestHelper helper, String line, String question) {
        List<String> answers = Config.section("spirit_box.questions." + question).texts("answers").stream()
                .map(a -> text("spirit_box.restless." + a))
                .toList();
        // Seeds drawn from one generator: the first numbers of Randoms with seeds in a row are nearly the same.
        Random seeds = new Random(7);
        boolean any = false;
        for (int i = 0; i < TRIES; i++) {
            String reply = Reply.of(Heard.typed(line), WORD, new Random(seeds.nextLong()))
                    .text()
                    .getString();
            check(helper, reply.contains(text("word.restless." + WORD)), "the clue word is missing from " + reply);
            check(helper, reply.split("\\.\\.\\. ").length <= 3, "too long a reply: " + reply);
            any |= List.of(reply.split("\\.\\.\\. ")).stream().anyMatch(answers::contains);
        }
        return any;
    }

    private static String text(String key) {
        return Language.getInstance().getOrDefault(key);
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) throw helper.assertionException(Component.literal(message));
    }
}
