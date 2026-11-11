package dev.keelfy.restless.divine;

import java.util.Map;

/**
 * What a player said near a spirit box: the line, and for a spoken one the sound of each word as speech recognition cut
 * it out, mono at {@code rate}, by its lower case text. A typed line, or one not made out, has no sound.
 */
public record Heard(String line, Map<String, short[]> words, int rate) {
    public static Heard typed(String line) {
        return new Heard(line, Map.of(), 0);
    }
}
