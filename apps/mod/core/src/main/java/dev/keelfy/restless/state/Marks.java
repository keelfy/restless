package dev.keelfy.restless.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * What a spirit carries beyond its {@link HauntData.Haunting} record, saved beside it so old saves and the record's
 * many callers stay as they are.
 *
 * @param appeased how many times the spirit was appeased at an altar; each time costs one more of what it wants
 */
public record Marks(int appeased) {
    public static final Marks NONE = new Marks(0);

    public static final Codec<Marks> CODEC = RecordCodecBuilder.create(i -> i.group(
                    Codec.INT.optionalFieldOf("appeased", 0).forGetter(Marks::appeased))
            .apply(i, Marks::new));

    public Marks appeasedOnce() {
        return new Marks(appeased + 1);
    }
}
