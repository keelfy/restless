package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;

/**
 * The spirit compass. The needle is the game's own lodestone needle: the compass in a player's hand is aimed at the
 * nearest spirit by writing that point into its tracker, so the client needs no code of its own. With no spirit to
 * point at, the needle spins.
 */
public final class Compass {
    private Compass() {}

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % Config.section("compass").integer("step_ticks") != 0) {
            return;
        }
        HauntData data = null;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                if (stack.is(Content.SPIRIT_COMPASS.get())) {
                    data = data == null ? HauntData.get(server) : data;
                    aim(player, stack, data);
                }
            }
        }
    }

    private static void aim(ServerPlayer player, ItemStack stack, HauntData data) {
        Optional<GlobalPos> target = Sense.of(player, data).stream().filter(s -> s.at() != null)
            .min(Comparator.comparingDouble(s -> s.at().distanceToSqr(player.position())))
            .map(s -> GlobalPos.of(player.level().dimension(), BlockPos.containing(s.at())));
        LodestoneTracker tracker = new LodestoneTracker(target, false);
        if (!tracker.equals(stack.get(DataComponents.LODESTONE_TRACKER))) {
            stack.set(DataComponents.LODESTONE_TRACKER, tracker);
        }
    }
}
