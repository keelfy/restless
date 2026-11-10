package dev.keelfy.restless.haunt.recall;

import dev.keelfy.restless.haunt.Reactions;
import dev.keelfy.restless.spirit.Trait;
import dev.keelfy.restless.state.Session;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * When each player last woke from a bed, for the events that only make sense just after a night's sleep. A wake also
 * counts as using a bed for the spirits that react to one, so they roll soon after it. In memory only.
 */
public final class Wakes {
    private static final Map<UUID, Long> WOKE = Session.SERVER.keep(new ConcurrentHashMap<>());

    private Wakes() {}

    /** Call when a player wakes. */
    public static void woke(ServerPlayer player) {
        WOKE.put(player.getUUID(), player.level().getGameTime());
        Reactions.trigger(player, Trait.Trigger.BED);
    }

    /** Whether the player woke within the last {@code ticks}. */
    public static boolean within(Player player, int ticks) {
        Long at = WOKE.get(player.getUUID());
        return at != null && player.level().getGameTime() - at <= ticks;
    }

    /** Forgets the wake, so a night is answered once. */
    public static void spent(Player player) {
        WOKE.remove(player.getUUID());
    }
}
