package dev.keelfy.restless.spirit;

/**
 * The shape of the offering a kind of spirit takes. The {@link Focus} says which category the altar needs; the form
 * says which item of it. A form the spirit does not take still works, but the ritual runs longer.
 */
public enum Form {
    /** The thing itself: an ingot, a block, a bottle. */
    WHOLE,
    /** A little of it: a nugget, dust, salt. */
    BIT,
    /** Something made of it. */
    CRAFTED
}
