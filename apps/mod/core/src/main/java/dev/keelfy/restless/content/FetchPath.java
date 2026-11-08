package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import net.minecraft.server.level.ServerPlayer;

/**
 * The lost one's step with the phantom's help: the holder leaves a double behind where the step began, and hostile
 * things near go for it. It costs a little more sanity and rests a little longer.
 */
public final class FetchPath extends LostStep {
    public FetchPath(Properties properties) {
        super(properties);
    }

    @Override
    protected void leave(ServerPlayer player) {
        PhantomDouble.cast(
                player, Config.section("advanced").integer("fetch_double_ticks"), player.blockPosition(), false);
    }

    @Override
    protected double extraCost() {
        return Config.section("advanced").num("fetch_sanity_cost");
    }

    @Override
    protected int cooldown(Config.Section config) {
        return Config.section("advanced").integer("fetch_cooldown_ticks");
    }
}
