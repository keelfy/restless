package dev.keelfy.restless.haunt.harm;

/**
 * What a player has been through that gates the worst events. Pure math on game time, so it is unit-testable.
 *
 * @param lastLethalAt game time of the last lethal event, or -1
 * @param warned whether a brink event has happened since the last lethal one
 * @param mercyUntil game time until which no harmful event runs
 */
public record Menace(long lastLethalAt, boolean warned, long mercyUntil) {
    public static final Menace NONE = new Menace(-1L, false, 0L);

    public boolean merciful(long now) {
        return now < mercyUntil;
    }

    /** Warned by a brink event and not killed lately. */
    public boolean lethalOpen(long now, long cooldownTicks) {
        return warned && (lastLethalAt < 0 || now - lastLethalAt >= cooldownTicks);
    }

    public Menace brink(long now, long mercyTicks) {
        return new Menace(lastLethalAt, true, now + mercyTicks);
    }

    public Menace lethal(long now) {
        return new Menace(now, false, mercyUntil);
    }
}
