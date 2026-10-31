package dev.keelfy.restless.haunt;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Which spirits a tool can sense, and where they are. */
public final class Sense {
    /** @param at the spirit's point; null for a personal spirit that has not yet roamed */
    public record Spirit(
            HauntData.Haunting haunting, @Nullable Vec3 at) {}

    private Sense() {}

    /** The player's own spirits and the territorial ones whose radius holds them. */
    public static List<Spirit> of(ServerPlayer player, HauntData data) {
        List<Spirit> found = new ArrayList<>();
        List<HauntData.Haunting> personal = data.get(player.getUUID());
        if (personal != null) {
            for (HauntData.Haunting haunting : personal) {
                found.add(new Spirit(haunting, Location.roaming(haunting.uuid())));
            }
        }
        for (HauntData.Territorial territorial : data.territorial()) {
            if (territorial.reaches(player)) {
                found.add(new Spirit(territorial.haunting(), Vec3.atCenterOf(territorial.anchor())));
            }
        }
        return found;
    }

    /**
     * What is at a place rather than at a player: spirits of players within {@code reach} of it, and territorial ones
     * covering it.
     */
    public static List<Spirit> around(ServerLevel level, BlockPos pos, int reach, HauntData data) {
        List<Spirit> found = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            List<HauntData.Haunting> personal = data.get(player.getUUID());
            if (personal != null && player.blockPosition().distSqr(pos) <= (double) reach * reach) {
                personal.forEach(h -> found.add(new Spirit(h, Location.roaming(h.uuid()))));
            }
        }
        for (HauntData.Territorial territorial : data.territorial()) {
            if (territorial.dimension().equals(level.dimension()) && territorial.covers(pos, reach)) {
                found.add(new Spirit(territorial.haunting(), Vec3.atCenterOf(territorial.anchor())));
            }
        }
        return found;
    }
}
