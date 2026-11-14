package dev.keelfy.restless.ward;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.haunt.Scan;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.event.DoubleEvent;
import dev.keelfy.restless.haunt.event.EventTag;
import dev.keelfy.restless.haunt.event.HauntEvent;
import dev.keelfy.restless.haunt.event.kit.Swaps;
import dev.keelfy.restless.haunt.recall.Said;
import dev.keelfy.restless.spirit.Focus;
import dev.keelfy.restless.spirit.Traits;
import dev.keelfy.restless.state.HauntData;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

/**
 * What holds a spirit back outside the altar: the material of its nature ({@link Traits#weakness}). Iron stops an item
 * or door event of an objects spirit beside an iron door or a container under iron bars; glowstone stops a light event
 * of a darkness spirit; holy water poured at the player's feet ends the running events of a malice spirit; a mirror in
 * the hand shows a fake block or a double of a deceit spirit for what it is. Each shows the same "blocked" as a ward.
 */
public final class Weakness {
    private Weakness() {}

    /** The nature of the spirit {@code id}, if it is known. */
    private static @Nullable Focus nature(MinecraftServer server, UUID id) {
        return HauntData.get(server)
                .find(id)
                .map(h -> Traits.weakness(Traits.of(h.traits())))
                .orElse(null);
    }

    private static boolean is(MinecraftServer server, UUID id, Focus focus) {
        return nature(server, id) == focus;
    }

    /** Whether the material of the running spirit's nature stops this event on the player: iron or glowstone. */
    static boolean stops(ServerPlayer player, HauntEvent event) {
        UUID spirit = Said.current();
        if (spirit == null) {
            return false;
        }
        Focus nature = nature(player.level().getServer(), spirit);
        Config.Section config = Config.section("weakness");
        if (nature == Focus.IRON
                && (event.tags().contains(EventTag.DOORS) || event.tags().contains(EventTag.ITEMS))) {
            return near(player, config.integer("iron_radius"), (level, pos) -> {
                var state = level.getBlockState(pos);
                return state.is(Blocks.IRON_DOOR)
                        || level.getBlockEntity(pos) instanceof Container
                                && level.getBlockState(pos.above()).is(Blocks.IRON_BARS);
            });
        }
        return nature == Focus.GLOWSTONE
                && event.tags().contains(EventTag.LIGHTS)
                && near(
                        player,
                        config.integer("glow_radius"),
                        (level, pos) -> level.getBlockState(pos).is(Blocks.GLOWSTONE));
    }

    private static boolean near(
            ServerPlayer player,
            int radius,
            java.util.function.BiPredicate<ServerLevel, net.minecraft.core.BlockPos> test) {
        ServerLevel level = player.level();
        boolean[] found = {false};
        Scan.each(
                level, player.blockPosition(), radius, (pos, state) -> found[0] |= !found[0] && test.test(level, pos));
        return found[0];
    }

    /**
     * Holy water poured at the player's feet: the running events of malice spirits end where they stand.
     *
     * @return whether one was ended
     */
    public static boolean pour(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        boolean ended = Scheduler.end(player, id -> is(server, id, Focus.HOLY_WATER)) > 0;
        if (ended) {
            Wards.blocked(player);
        }
        return ended;
    }

    /**
     * A mirror in the hand: what a deceit spirit shows this player (a fake block, a double) is shown for what it is.
     *
     * @return whether something was unmasked
     */
    public static boolean mirror(ServerPlayer player) {
        if (!player.isHolding(Content.MIRROR.get())) {
            return false;
        }
        MinecraftServer server = player.level().getServer();
        boolean shown = Swaps.unmask(player, id -> is(server, id, Focus.MIRROR)) > 0;
        shown |= DoubleEvent.dismiss(player, id -> is(server, id, Focus.MIRROR));
        if (shown) {
            Wards.blocked(player);
        }
        return shown;
    }

    /** Call once per server tick. */
    public static void tick(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(Weakness::mirror);
    }
}
