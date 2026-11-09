package dev.keelfy.restless.content;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

/**
 * What the consumables do on the server's clock and at the loader's hooks, gathered where {@code Restless} calls it.
 */
public final class Consumables {
    private Consumables() {}

    /** Call every server tick. */
    public static void tick(MinecraftServer server) {
        Draughts.tick(server);
        int tick = server.getTickCount();
        if (tick % RagEntity.EVERY == 0) {
            RagEntity.tick(server);
        }
        DomovoiPorridge.tick(server);
        if (tick % 5 == 0) {
            RestlessKnot.tick(server);
        }
        if (tick % 20 == 0) {
            DuskSalt.tick(server);
        }
    }

    /** Whether a mob about to spawn is kept out by dusk salt. */
    public static boolean spawn(ServerLevel level, Mob mob, EntitySpawnReason reason) {
        return DuskSalt.keeps(level, mob, reason);
    }

    /** The anvil's result for a worn house key and a bowl of porridge; empty if the two are something else. */
    public static ItemStack anvil(ItemStack left, ItemStack right) {
        return DomovoiPorridge.mend(left, right);
    }

    /** Whether a draught lets its drinker sleep despite monsters near. */
    public static boolean sleepsAnyway(ServerPlayer player) {
        return Draughts.dreamless(player);
    }
}
