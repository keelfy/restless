package dev.keelfy.restless.journal;

import dev.keelfy.restless.Restless;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * The advancements the game grants from code, which follow the player's path: clue, photograph, banishing, grave. The
 * ones met by holding an essence need no code, see {@code data/restless/advancement}.
 */
public final class Milestones {
    /** The one criterion of an advancement granted from code. */
    public static final String CRITERION = "impossible";

    private Milestones() {}

    /** Grants {@code restless:<path>}; nothing happens when the player has it or it is not loaded. */
    public static void award(ServerPlayer player, String path) {
        AdvancementHolder advancement = player.level()
                .getServer()
                .getAdvancements()
                .get(Identifier.fromNamespaceAndPath(Restless.MOD_ID, path));
        if (advancement != null) {
            player.getAdvancements().award(advancement, CRITERION);
        }
    }
}
