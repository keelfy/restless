package dev.keelfy.restless.sanity;

/** When low sanity is heard: a bed below {@code overlay_below}, a heartbeat below {@code heartbeat_below}. */
public final class SanitySounds {
    private SanitySounds() {}

    /** Volume of the bed: silent at and above {@code below}, {@code volume} at zero sanity. */
    public static double bedVolume(double sanity, double below, double volume) {
        return Math.clamp((below - sanity) / below, 0.0, 1.0) * volume;
    }

    public static boolean heartbeat(double sanity, double below) {
        return sanity < below;
    }
}
