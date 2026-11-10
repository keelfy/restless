package dev.keelfy.restless.gametest;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.divine.Reply;
import dev.keelfy.restless.spirit.Trait;
import dev.keelfy.restless.spirit.Traits;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

/** The spirit box answers what it is asked around the clue word, and never with another trait's word. */
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
            String reply = Reply.of(line, Component.literal(WORD), new Random(seeds.nextLong()))
                    .getString();
            check(helper, reply.contains(WORD), "the clue word is missing from " + reply);
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
