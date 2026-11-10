package dev.keelfy.restless.content.item.charm;

import dev.keelfy.restless.content.Content;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A charm's long rest, kept on the stack as the game time it ends at. The vanilla cooldown is only for the bar on the
 * client: it is not saved, so a relog would clear it, and the rest must not go with it.
 */
public final class Rest {
    private Rest() {}

    public static boolean ready(ItemStack stack, Level level) {
        return level.getGameTime() >= stack.getOrDefault(Content.REST_UNTIL.get(), 0L);
    }

    /** Starts the rest of {@code ticks}; the stack must be the player's own, not a copy. */
    public static void start(ServerPlayer player, ItemStack stack, int ticks) {
        stack.set(Content.REST_UNTIL.get(), player.level().getGameTime() + ticks);
        player.getCooldowns().addCooldown(stack, ticks);
    }
}
