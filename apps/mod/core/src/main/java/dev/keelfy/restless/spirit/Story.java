package dev.keelfy.restless.spirit;

import dev.keelfy.restless.config.Config;
import java.util.List;
import java.util.Random;

/**
 * Who the spirit was: a made-up player nickname, how many days it had been dead when it woke and how it died. All of it
 * comes from the spirit's seed, so it is never saved and stays the same for the spirit's whole life while the
 * {@code story} config is unchanged.
 */
public record Story(String name, int daysDead, String cause) {
    // Keeps the story's rolls apart from the trait roll, which also starts from the bare seed.
    private static final long SALT = 0x5DEECE66DL;

    public static Story of(long seed) {
        Config.Section config = Config.section("story");
        Random random = new Random(seed ^ SALT);
        String name = nickname(random);
        int min = config.integer("days_min");
        int days = min + random.nextInt(config.integer("days_max") - min + 1);
        return new Story(name, days, pick(config.texts("causes"), random));
    }

    @Override
    public String toString() {
        return name + " (" + daysDead + " days dead, " + cause + ")";
    }

    /** Two words, sometimes joined by an underscore and sometimes followed by a number, like a real nickname. */
    public static String nickname(Random random) {
        Config.Section config = Config.section("story");
        String name = pick(config.texts("name_first"), random)
                + (random.nextBoolean() ? "_" : "")
                + pick(config.texts("name_second"), random);
        return random.nextBoolean() ? name + random.nextInt(config.integer("name_number_max") + 1) : name;
    }

    private static String pick(List<String> options, Random random) {
        return options.get(random.nextInt(options.size()));
    }
}
