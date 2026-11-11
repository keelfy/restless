package dev.keelfy.restless.divine;

import dev.keelfy.restless.config.Config;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * What a spirit box says back to a line: the trait word, which is the clue, broken up by a few words that answer what
 * was asked, an echo of the asker's own word and static. Only the trait word tells anything about the spirit; the rest
 * never uses a trait word, so it cannot be read as one. Each part is shown and voiced: the clue and the answers by
 * recorded clips, the echo in the asker's own voice when they spoke it.
 */
public record Reply(List<Part> parts) {
    private static final String SEPARATOR = "... ";
    /** Shorter words of a line are not worth echoing. */
    private static final int ECHO_LENGTH = 4;

    private static final int MOST_PARTS = 3;
    private static final String HISS = "hiss";

    /** One part: its text, and the clip that voices it or, for an echo, the asker's voice (empty when typed). */
    public record Part(Component text, String voice, byte[] pcm) {
        Part(Component text, String voice) {
            this(text, voice, new byte[0]);
        }
    }

    /** No spirit has a voice: the box at {@code box} only hisses. */
    public static SpiritBoxPayload hiss(BlockPos box) {
        return new SpiritBoxPayload(box, List.of(new SpiritBoxPayload.Part(HISS, new byte[0])));
    }

    /**
     * The answer to what was {@code heard} around the clue, the trait word {@code word}; an empty line gets the word
     * with static at most.
     */
    public static Reply of(Heard heard, String word, Random random) {
        Config.Section config = Config.section("spirit_box");
        List<String> said = words(heard.line());
        Optional<Config.Section> question = question(config.section("questions"), said);
        Part clue = new Part(Divination.word(word), "word." + word);
        List<Part> parts = new ArrayList<>();
        parts.add(clue);
        question.filter(q -> random.nextDouble() < config.num("answer_chance")).ifPresent(q -> {
            List<String> answers = q.texts("answers");
            String id = answers.get(random.nextInt(answers.size()));
            parts.add(random.nextBoolean() ? 0 : 1, new Part(answer(id), "answer." + id));
        });
        if (random.nextDouble() < config.num("glitch_chance")) {
            if (random.nextBoolean()) {
                parts.add(parts.indexOf(clue) + 1, clue);
            } else {
                parts.add(
                        random.nextInt(parts.size() + 1),
                        new Part(Component.translatable("spirit_box.restless.hiss"), HISS));
            }
        }
        List<String> echoes = said.stream()
                .filter(w -> w.length() >= ECHO_LENGTH
                        && question.map(q -> !heard(q, w)).orElse(true))
                .toList();
        if (parts.size() < MOST_PARTS && !echoes.isEmpty() && random.nextDouble() < config.num("echo_chance")) {
            String echo = echoes.get(random.nextInt(echoes.size()));
            int how = random.nextInt(Echo.WAYS);
            short[] voice = heard.words().get(echo);
            parts.addFirst(new Part(
                    Component.literal(Echo.text(echo, how)),
                    HISS,
                    voice == null ? new byte[0] : Echo.voice(voice, heard.rate(), how, random)));
        }
        return new Reply(List.copyOf(parts));
    }

    /** The parts, shown one after another. */
    public Component text() {
        MutableComponent out = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            out.append(i == 0 ? Component.empty() : Component.literal(SEPARATOR))
                    .append(parts.get(i).text());
        }
        return out;
    }

    /** The parts, voiced by the box at {@code box}. */
    public SpiritBoxPayload voice(BlockPos box) {
        return new SpiritBoxPayload(
                box,
                parts.stream()
                        .map(p -> new SpiritBoxPayload.Part(p.voice(), p.pcm()))
                        .toList());
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

    /** An answer in the listener's language; one added in config without a translation stays as written. */
    private static Component answer(String id) {
        return Component.translatableWithFallback("spirit_box.restless." + id, id.replace('_', ' '));
    }
}
