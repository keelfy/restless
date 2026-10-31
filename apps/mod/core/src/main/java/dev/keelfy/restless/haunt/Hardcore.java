package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import net.minecraft.server.level.ServerLevel;

/**
 * Whether this world runs hardcore haunting: what an event changes stays changed (see {@link Restore#revert}) and
 * tricks that would only be shown can be made real. Fixed per world the first time it is asked, from the
 * {@code hardcore.mode} config: {@code auto} follows the world's own hardcore flag.
 */
public final class Hardcore {
    private Hardcore() {}

    public static boolean on(ServerLevel level) {
        return HauntData.get(level.getServer())
                .hardcore(() -> switch (Config.section("hardcore").text("mode")) {
                    case "auto" -> level.getServer().getWorldData().isHardcore();
                    case "true" -> true;
                    case "false" -> false;
                    default -> throw new IllegalStateException("hardcore.mode must be auto, true or false");
                });
    }
}
