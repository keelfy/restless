package dev.keelfy.restless.debug;

import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.HauntData;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntEvents;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.Ticks;
import dev.keelfy.restless.spirit.Traits;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
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
        HauntData data = HauntData.get(viewer.level().getServer());
        List<HauntData.Haunting> own = Director.hauntings(viewer, data, now);
        List<HauntData.Haunting> shown = Stream.concat(
                        own.stream(),
                        Director.territorial(data, viewer.level()).stream()
                                .filter(t -> t.reaches(viewer))
                                .map(HauntData.Territorial::haunting))
                .toList();
        int personal = own.size();
        List<DebugPayload.Spirit> spirits = IntStream.range(0, shown.size())
                .mapToObj(i -> spirit(shown.get(i), i >= personal, now))
                .toList();
        // weights[s][e]: what spirit s would roll for event e right now
        double[][] weights = shown.stream()
                .map(h -> HauntEvents.all().stream()
                        .mapToDouble(e -> Director.weight(
                                e,
                                viewer,
                                Director.spirit(h),
                                Traits.of(h.traits()),
                                Director.power(h, now),
                                h.evolved()))
                        .toArray())
                .toArray(double[][]::new);
        double[] totals = Arrays.stream(weights)
                .mapToDouble(row -> Arrays.stream(row).sum())
                .toArray();
        List<HauntEvent> all = HauntEvents.all();
        List<DebugPayload.Event> events = IntStream.range(0, all.size())
                .mapToObj(e -> {
                    int index = e;
                    List<Double> chances = IntStream.range(0, shown.size())
                            .mapToObj(s -> totals[s] == 0 ? 0.0 : weights[s][index] / totals[s])
                            .toList();
                    HauntEvent event = all.get(e);
                    return new DebugPayload.Event(
                            event.id(),
                            event.tier(),
                            List.copyOf(event.tags()),
                            event.baseWeight(),
                            event.canRun(viewer),
                            chances);
                })
                .toList();
        viewer.connection.send(new ClientboundCustomPayloadPacket(
                new DebugPayload(viewer.getName().getString(), spirits, events)));
    }

    private static DebugPayload.Spirit spirit(HauntData.Haunting h, boolean territorial, long now) {
        return new DebugPayload.Spirit(h.spirit(), territorial, h.seed(), h.traits(), Director.power(h, now), (int)
                Math.max(0, (h.nextEventAt() - now) / Ticks.SECOND));
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
