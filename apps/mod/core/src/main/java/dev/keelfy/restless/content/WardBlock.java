package dev.keelfy.restless.content;

/** A block that protects the area around it; {@code Wards} decides when. */
public interface WardBlock {
    enum Kind {
        LANTERN,
        DREAMCATCHER,
        CHARM,
        SCARECROW
    }

    Kind kind();
}
