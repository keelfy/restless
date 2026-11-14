package dev.keelfy.restless.spirit.life;

import dev.keelfy.restless.banish.Requiem;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Location;
import dev.keelfy.restless.haunt.Ticks;
import dev.keelfy.restless.spirit.SpiritType;
import dev.keelfy.restless.spirit.Spirits;
import dev.keelfy.restless.state.HauntData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * What an altar leaves behind. A spirit banished at the altar (not laid to rest at its grave) leaves an echo: a young,
 * calm spirit of the same type and story that haunts on and leaves by itself after {@code births.echo_days}. It refuses
 * every altar until then (so it brings no essence), has no grave, never counts toward a player's births (cap, cooldown,
 * successor) and leaves no echo of its own. A personal spirit's echo stays with its player; a territorial one's lingers
 * at the altar.
 */
public final class Echoes {
    private Echoes() {}

    /** Call once the altar has banished {@code banished} for {@code player}; it is already gone from the data. */
    public static void banished(ServerPlayer player, HauntData.Haunting banished) {
        HauntData data = HauntData.get(player.level().getServer());
        Optional<SpiritType> type = Spirits.byId(banished.spirit());
        if (data.echo(banished.uuid()) != 0L || type.isEmpty() || Requiem.released(player)) {
            data.putEcho(banished.uuid(), 0L);
            return;
        }
        Config.Section config = Config.section("births");
        long now = player.level().getServer().overworld().getGameTime();
        long leaveAt = now + (long) (config.num("echo_days") * Ticks.DAY);
        HauntData.Haunting echo = new HauntData.Haunting(
                UUID.randomUUID(),
                banished.spirit(),
                banished.seed(),
                banished.traits(),
                now,
                0.0,
                now + config.integer("echo_grace_ticks"),
                false,
                0,
                leaveAt);
        data.putEcho(echo.uuid(), leaveAt);
        if (type.get().kind() == SpiritType.Kind.PERSONAL) {
            List<HauntData.Haunting> own = data.get(player.getUUID());
            List<HauntData.Haunting> with = new ArrayList<>(own == null ? List.of() : own);
            with.add(echo);
            data.put(player.getUUID(), with);
        } else {
            data.putTerritorial(new HauntData.Territorial(
                    echo,
                    player.level().dimension(),
                    player.blockPosition(),
                    Config.section("director").integer("territorial_radius")));
        }
    }

    /** Call at each director check: an echo whose time is up leaves, and a mark whose spirit is gone is dropped. */
    public static void tick(HauntData data, long now) {
        for (Map.Entry<UUID, Long> entry : List.copyOf(data.echoes().entrySet())) {
            UUID spirit = entry.getKey();
            if (data.find(spirit).isPresent() && now < entry.getValue()) {
                continue;
            }
            leave(data, spirit);
        }
    }

    /** The echo leaves now. */
    public static void leave(HauntData data, UUID spirit) {
        data.change(spirit, h -> null);
        Location.forget(spirit);
        data.putEcho(spirit, 0L);
    }

    /** Whether {@code spirit} is an echo, which births leave out. */
    public static boolean is(HauntData data, HauntData.Haunting spirit) {
        return data.echo(spirit.uuid()) != 0L;
    }
}
