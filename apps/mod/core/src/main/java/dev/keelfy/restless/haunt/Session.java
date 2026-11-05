package dev.keelfy.restless.haunt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Where the state that lives only as long as one world is kept. A class that holds such state in a static collection
 * hands it to {@link #SERVER} (or {@link #CLIENT} for what the connected client keeps) when it creates it; the scope is
 * emptied when the server stops (or the client leaves), so nothing from one world reaches the next, and nothing holds
 * on to the old server's players and entities. A class that is never loaded keeps nothing and registers nothing.
 */
public final class Session {
    /** Runtime state of the running server (the integrated one too); emptied from its own thread. */
    public static final Scope SERVER = new Scope();
    /** What the connected client keeps for the world it is in. */
    public static final Scope CLIENT = new Scope();

    private Session() {}

    public static final class Scope {
        private final List<Runnable> clears = new CopyOnWriteArrayList<>();

        private Scope() {}

        /** @return {@code collection}, to be emptied by {@link #end} */
        public <C extends Collection<?>> C keep(C collection) {
            clears.add(collection::clear);
            return collection;
        }

        /** @return {@code map}, to be emptied by {@link #end} */
        public <M extends Map<?, ?>> M keep(M map) {
            clears.add(map::clear);
            return map;
        }

        /** Empties everything kept; the collections stay in use for the next world. */
        public void end() {
            clears.forEach(Runnable::run);
        }
    }
}
