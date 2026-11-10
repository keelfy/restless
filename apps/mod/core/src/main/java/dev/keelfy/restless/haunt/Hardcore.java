package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Settings;
import dev.keelfy.restless.state.HauntData;
import net.minecraft.server.level.ServerLevel;

/**
 * Whether this world runs hardcore haunting: what an event changes stays changed (see {@link Restore#revert}) and
 * tricks that would only be shown can be made real. Fixed per world the first time it is asked, from the
 * {@linkplain Settings#hardcore() hardcore setting}: {@code AUTO} follows the world's own hardcore flag.
 */
public final class Hardcore {
    private Hardcore() {}

    public static boolean on(ServerLevel level) {
        return HauntData.get(level.getServer())
                .hardcore(() -> switch (Settings.get().hardcore()) {
                    case AUTO -> level.getServer().getWorldData().isHardcore();
                    case ON -> true;
                    case OFF -> false;
                });
    }
}
