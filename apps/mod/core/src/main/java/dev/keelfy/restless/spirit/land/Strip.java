package dev.keelfy.restless.spirit.land;

/** The strip an angry spirit's blight grows in, from its anchor toward the bed of the player who angered it. */
public final class Strip {
    /** A block offset from the anchor. */
    public record Offset(int x, int z) {}

    private Strip() {}

    /**
     * A point in the strip that runs from the anchor toward a target {@code (dx, dz)} away: along the line no further
     * than {@code reach} or the target, across it within {@code width} centred on the line. {@code along} and
     * {@code across} are uniform in 0..1.
     */
    public static Offset offset(double dx, double dz, double reach, double width, double along, double across) {
        double distance = Math.hypot(dx, dz);
        if (distance == 0) {
            return new Offset(0, 0);
        }
        double forward = along * Math.min(reach, distance);
        double side = (across - 0.5) * width;
        double ux = dx / distance;
        double uz = dz / distance;
        return new Offset((int) Math.round(ux * forward - uz * side), (int) Math.round(uz * forward + ux * side));
    }
}
