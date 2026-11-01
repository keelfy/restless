package dev.keelfy.restless.haunt.event;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/** Where the player is: the dimension and the biome at their feet, for events that only make sense in one place. */
final class Where {
    private Where() {}

    static boolean overworld(ServerPlayer player) {
        return player.level().dimension() == Level.OVERWORLD;
    }

    static boolean nether(ServerPlayer player) {
        return player.level().dimension() == Level.NETHER;
    }

    static boolean end(ServerPlayer player) {
        return player.level().dimension() == Level.END;
    }

    static boolean biome(ServerPlayer player, TagKey<Biome> tag) {
        return biome(player).is(tag);
    }

    /** True if the biome at the player's feet is one of these. */
    @SafeVarargs
    static boolean biome(ServerPlayer player, ResourceKey<Biome>... keys) {
        Holder<Biome> here = biome(player);
        for (ResourceKey<Biome> key : keys) {
            if (here.is(key)) {
                return true;
            }
        }
        return false;
    }

    private static Holder<Biome> biome(ServerPlayer player) {
        return player.level().getBiome(player.blockPosition());
    }
}
