package dev.keelfy.restless.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * What a spirit carries beyond its {@link HauntData.Haunting} record, saved beside it so old saves and the record's
 * many callers stay as they are.
 *
 * @param appeased how many times the spirit was appeased at an altar; each time costs one more of what it wants
 * @param deathForm born since the form of offering comes from the cause of death; an older spirit, without this
 *     mark, keeps the form of its type
 */
public record Marks(int appeased, boolean deathForm) {
    public static final Marks NONE = new Marks(0, false);

    public static final Codec<Marks> CODEC = RecordCodecBuilder.create(i -> i.group(
                    Codec.INT.optionalFieldOf("appeased", 0).forGetter(Marks::appeased),
                    Codec.BOOL.optionalFieldOf("death_form", false).forGetter(Marks::deathForm))
            .apply(i, Marks::new));

    public Marks appeasedOnce() {
        return new Marks(appeased + 1, deathForm);
    }

    public Marks withDeathForm() {
        return new Marks(appeased, true);
    }
}
