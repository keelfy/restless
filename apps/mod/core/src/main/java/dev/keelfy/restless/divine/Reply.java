package dev.keelfy.restless.divine;

import dev.keelfy.restless.config.Config;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * What a spirit box says back to a line: the trait word, which is the clue, broken up by a few words that answer what
 * was asked, an echo of the asker's own word and static. Only the trait word tells anything about the spirit; the rest
 * never uses a trait word, so it cannot be read as one.
 */
public final class Reply {
    private static final String SEPARATOR = "... ";
    /** Shorter words of a line are not worth echoing. */
    private static final int ECHO_LENGTH = 4;

    private static final int MOST_PARTS = 3;

    private Reply() {}

    /** The answer to {@code line} around the clue {@code word}; an empty line gets the word with static at most. */
    public static Component of(String line, Component word, Random random) {
        Config.Section config = Config.section("spirit_box");
        List<String> said = words(line);
        Optional<Config.Section> question = question(config.section("questions"), said);
        List<Component> parts = new ArrayList<>();
        parts.add(word);
        question.filter(q -> random.nextDouble() < config.num("answer_chance")).ifPresent(q -> {
            List<String> answers = q.texts("answers");
            Component answer = answer(answers.get(random.nextInt(answers.size())));
            parts.add(random.nextBoolean() ? 0 : 1, answer);
        });
        if (random.nextDouble() < config.num("glitch_chance")) {
            if (random.nextBoolean()) {
                parts.add(parts.indexOf(word) + 1, word);
            } else {
                parts.add(random.nextInt(parts.size() + 1), Component.translatable("spirit_box.restless.hiss"));
            }
        }
        List<String> echoes = said.stream()
                .filter(w -> w.length() >= ECHO_LENGTH
                        && question.map(q -> !heard(q, w)).orElse(true))
                .toList();
        if (parts.size() < MOST_PARTS && !echoes.isEmpty() && random.nextDouble() < config.num("echo_chance")) {
            parts.addFirst(Component.literal(echo(echoes.get(random.nextInt(echoes.size())), random)));
        }
        MutableComponent out = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            out.append(i == 0 ? Component.empty() : Component.literal(SEPARATOR))
                    .append(parts.get(i));
        }
        return out;
    }

    /** The first question, in config order, one of whose words was said. */
    static Optional<Config.Section> question(Config.Section questions, List<String> said) {
        for (String key : questions.keys()) {
            Config.Section question = questions.section(key);
            if (said.stream().anyMatch(w -> heard(question, w))) {
                return Optional.of(question);
            }
        }
        return Optional.empty();
    }

    /** A question's word is matched whole; one that ends in {@code *} matches every word it begins. */
    private static boolean heard(Config.Section question, String word) {
        for (String heard : question.texts("heard")) {
            boolean match =
                    heard.endsWith("*") ? word.startsWith(heard.substring(0, heard.length() - 1)) : word.equals(heard);
            if (match) {
                return true;
            }
        }
        return false;
    }

    /** The words of a line, lower case, without punctuation. */
    static List<String> words(String line) {
        List<String> out = new ArrayList<>();
        for (String w : line.toLowerCase(Locale.ROOT).split("[^\\p{L}]+")) {
            if (!w.isEmpty()) {
                out.add(w);
            }
        }
        return out;
    }

    /** The asker's word as the box mangles it: cut short, stuttered or as it was. */
    private static String echo(String word, Random random) {
        return switch (random.nextInt(3)) {
            case 0 -> word.substring(0, word.length() / 2 + 1) + "—";
            case 1 -> word.charAt(0) + "-" + word;
            default -> word;
        };
    }

    /** An answer in the listener's language; one added in config without a translation stays as written. */
    private static Component answer(String id) {
        return Component.translatableWithFallback("spirit_box.restless." + id, id.replace('_', ' '));
    }
}
