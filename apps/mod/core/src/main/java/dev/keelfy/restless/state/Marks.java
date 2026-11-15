package dev.keelfy.restless.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.keelfy.restless.spirit.Focus;
import java.util.Optional;

/**
 * What a spirit carries beyond its {@link HauntData.Haunting} record, saved beside it so old saves and the record's
 * many callers stay as they are.
 *
 * @param appeased how many times the spirit was appeased at an altar; each time costs one more of what it wants
 * @param deathForm born since the form of offering comes from the cause of death; an older spirit, without this
 *     mark, keeps the form of its type
 * @param merged the nature of the spirit it swallowed when two merged; its ritual asks for an offering of it too
 */
public record Marks(int appeased, boolean deathForm, Optional<Focus> merged) {
    public static final Marks NONE = new Marks(0, false, Optional.empty());

    private static final Codec<Focus> FOCUS = Codec.STRING.xmap(Focus::valueOf, Focus::name);

    public static final Codec<Marks> CODEC = RecordCodecBuilder.create(i -> i.group(
                    Codec.INT.optionalFieldOf("appeased", 0).forGetter(Marks::appeased),
                    Codec.BOOL.optionalFieldOf("death_form", false).forGetter(Marks::deathForm),
                    FOCUS.optionalFieldOf("merged").forGetter(Marks::merged))
            .apply(i, Marks::new));

    public Marks appeasedOnce() {
        return new Marks(appeased + 1, deathForm, merged);
    }

    public Marks withDeathForm() {
        return new Marks(appeased, true, merged);
    }

    public Marks mergedWith(Focus nature) {
        return new Marks(appeased, deathForm, Optional.of(nature));
    }
}
