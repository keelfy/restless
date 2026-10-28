package dev.keelfy.restless.debug;

import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.HauntData;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntEvents;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.Ticks;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

/** Server side of the {@code /haunt} debug screen. */
public final class DebugMenu {
    /** Gives the player time to close the screen and look at the world before the event runs. */
    static final int FIRE_DELAY = Ticks.SECOND;

    private DebugMenu() {}

    public static void open(ServerPlayer viewer) {
        long now = viewer.level().getServer().overworld().getGameTime();
        List<DebugPayload.Spirit> spirits = Director.hauntings(viewer, HauntData.get(viewer.level().getServer()), now).stream()
            .map(h -> new DebugPayload.Spirit(h.spirit(), (int) Math.max(0, (h.nextEventAt() - now) / Ticks.SECOND)))
            .toList();
        List<DebugPayload.Event> events = HauntEvents.all().stream()
            .map(e -> new DebugPayload.Event(e.id(), e.tier(), List.copyOf(e.tags()), e.baseWeight(), e.canRun(viewer)))
            .toList();
        viewer.connection.send(new ClientboundCustomPayloadPacket(new DebugPayload(viewer.getName().getString(), Director.danger(viewer),
            (int) (viewer.level().getServer().overworld().getOverworldClockTime() / Ticks.DAY), Director.advancements(viewer), spirits, events)));
    }

    /** Runs the event on the sender after {@link #FIRE_DELAY}; the sender must be allowed to use {@code /haunt}. */
    public static void fire(ServerPlayer player, FirePayload request) {
        if (!player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return;
        }
        HauntEvents.byId(request.event()).ifPresent(event -> Scheduler.later(player, FIRE_DELAY, p -> run(p, event)));
    }

    private static void run(ServerPlayer player, HauntEvent event) {
        if (event.canRun(player)) {
            Director.fire(player, event);
        } else {
            player.sendSystemMessage(Component.literal(event.id() + ": nothing to act on here"));
        }
    }
}
