package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import net.minecraft.server.level.ServerPlayer;

/**
 * The trickster's coin with the demon's appetite: the wager stands for every kill within a window, each settled on its
 * own, and a loss ends the window and the streak, see {@link Charms#drops}. Every toss angers the holder's spirits.
 */
public final class DevilsCoin extends TricksterCoin {
    public DevilsCoin(Properties properties) {
        super(properties);
    }

    @Override
    protected int window(Config.Section config) {
        return Config.section("advanced").integer("devil_ticks");
    }

    @Override
    protected void toss(ServerPlayer player) {
        Advanced.tossed(player);
    }
}
