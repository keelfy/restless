package dev.keelfy.restless.banish;

/**
 * How many waves each {@link Phase} of a ritual lasts. Wave {@code n} (from 0) belongs to the phase it falls in; the
 * ritual ends at its last wave.
 */
public record Plan(int call, int bargain, int fury, int leaving) {
    public int total() {
        return call + bargain + fury + leaving;
    }

    /** The phase of wave {@code n}; past the end it is the leaving. */
    public Phase at(int n) {
        if (n < call) {
            return Phase.CALL;
        }
        if (n < call + bargain) {
            return Phase.BARGAIN;
        }
        if (n < call + bargain + fury) {
            return Phase.FURY;
        }
        return Phase.LEAVING;
    }

    /** The same plan with the fury longer by {@code waves} (shorter when negative, never below none). */
    public Plan longerFury(int waves) {
        return new Plan(call, bargain, Math.max(0, fury + waves), leaving);
    }
}
