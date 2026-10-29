package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What angers spirits. A player's own spirits take offence at everything that player does; a territorial spirit takes it
 * only when it happens inside its radius, and also when its anchor is torn up. Anger feeds {@link Power}.
 */
public final class Anger {
    /** What spirits hate; {@code key} names the amount in the {@code anger} config. */
    public enum Source {
        KILL_CAT("kill_cat"), BREAK_GRAVE("break_grave"), BURN_FOREST("burn_forest");

        private final String key;

        Source(String key) {
            this.key = key;
        }

        double amount() {
            return Config.section("anger").num(key);
        }
    }

    private Anger() {}

    /** Call when a player does something spirits hate at {@code at}. */
    public static void provoke(ServerPlayer player, Source source, BlockPos at) {
        MinecraftServer server = player.level().getServer();
        HauntData data = HauntData.get(server);
        double by = source.amount();
        List<HauntData.Haunting> own = new ArrayList<>(Director.hauntings(player, data, server.overworld().getGameTime()));
        if (!own.isEmpty()) {
            own.replaceAll(h -> h.angered(by));
            data.put(player.getUUID(), own);
        }
        raiseTerritorial(data, player, at, by, Integer.MAX_VALUE);
    }

    /** Call when a player breaks a block: graves anger like any source, and breaking a territorial spirit's anchor angers that spirit. */
    public static void broke(ServerPlayer player, BlockState state, BlockPos at) {
        Config.Section config = Config.section("anger");
        if (config.texts("graves").contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString())) {
            provoke(player, Source.BREAK_GRAVE, at);
        }
        int reach = config.integer("anchor_reach");
        raiseTerritorial(HauntData.get(player.level().getServer()), player, at, config.num("break_anchor"), reach);
    }

    /** @param reach how close to the anchor {@code at} must be; a very large value means the spirit's whole radius */
    private static void raiseTerritorial(HauntData data, ServerPlayer player, BlockPos at, double by, int reach) {
        List<HauntData.Territorial> spirits = new ArrayList<>(data.territorial());
        boolean changed = false;
        for (int i = 0; i < spirits.size(); i++) {
            HauntData.Territorial t = spirits.get(i);
            if (t.dimension().equals(player.level().dimension()) && t.covers(at, reach)) {
                spirits.set(i, t.angered(by));
                changed = true;
            }
        }
        if (changed) {
            data.putTerritorial(spirits);
        }
    }
}
