package dev.keelfy.restless.spirit.land;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.spirit.Story;
import dev.keelfy.restless.state.HauntData;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * {@link Quest#REMAINS}: the bones lie off the grave, at a spot the seed picks. They are laid there once that chunk is
 * loaded; carried to the grave they lay the spirit to rest, and while carried the spirit acts more often.
 */
public final class Remains {
    private static final String KEY = "restless_remains";

    private Remains() {}

    /** Where the bones lie (the column; the height is found when they are laid). */
    public static BlockPos spot(long seed, BlockPos grave) {
        int[] by = Quest.bonesOffset(seed, grave.getX(), grave.getZ());
        return new BlockPos(grave.getX() + by[0], 0, grave.getZ() + by[1]);
    }

    /** First reading picks the spot; the stone gives it roughly. @return the hint for the epitaph */
    static Component read(ServerPlayer player, HauntData data, HauntData.Haunting spirit, HauntData.Grave grave) {
        if (grave.stage() == 0) {
            data.putGrave(spirit.uuid(), grave.staged(1, Optional.empty()));
        }
        int round = Config.section("grave").integer("remains_hint_round");
        BlockPos spot = spot(spirit.seed(), grave.pos());
        return Component.translatable(
                "message.restless.remains_near", rounded(spot.getX(), round), rounded(spot.getZ(), round));
    }

    private static int rounded(int value, int step) {
        return Math.round((float) value / step) * step;
    }

    /** The bones of this spirit, as lying at the spot and as carried. */
    public static ItemStack bones(HauntData.Haunting spirit) {
        ItemStack stack = new ItemStack(Items.BONE);
        stack.set(
                net.minecraft.core.component.DataComponents.ITEM_NAME,
                Component.translatable(
                        "item.restless.remains", Story.of(spirit.seed()).name()));
        return Graves.tag(stack, KEY, spirit.uuid());
    }

    static boolean is(ItemStack stack, UUID spirit) {
        return Graves.tagged(stack, KEY, spirit);
    }

    /** Lays the bones on the surface at the column of {@code at}. */
    public static void lay(ServerLevel level, HauntData.Haunting spirit, BlockPos at) {
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at);
        ItemEntity bones =
                new ItemEntity(level, top.getX() + 0.5, top.getY() + 0.2, top.getZ() + 0.5, bones(spirit), 0, 0, 0);
        bones.setUnlimitedLifetime();
        level.addFreshEntity(bones);
    }

    /** Lays the bones once their chunk is loaded. */
    static void tick(ServerLevel level, HauntData data, HauntData.Haunting spirit, HauntData.Grave grave) {
        if (grave.stage() != 1 || Quest.of(spirit.seed()) != Quest.REMAINS) {
            return;
        }
        BlockPos spot = spot(spirit.seed(), grave.pos());
        if (level.getChunkSource()
                        .getChunkNow(
                                SectionPos.blockToSectionCoord(spot.getX()),
                                SectionPos.blockToSectionCoord(spot.getZ()))
                == null) {
            return;
        }
        lay(level, spirit, spot);
        data.putGrave(spirit.uuid(), grave.staged(2, Optional.empty()));
    }

    /** How fast the spirit acts: faster while a player carries its bones. */
    public static double pace(ServerPlayer player, UUID spirit) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (is(player.getInventory().getItem(i), spirit)) {
                return Config.section("grave").num("remains_pace");
            }
        }
        return 1.0;
    }
}
