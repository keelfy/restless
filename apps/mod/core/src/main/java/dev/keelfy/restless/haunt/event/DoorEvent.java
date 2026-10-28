package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntTier;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Opens a closed wooden door or slams an open one. */
public final class DoorEvent implements HauntEvent {
    @Override
    public String id() {
        return "door";
    }

    @Override
    public HauntTier tier() {
        return HauntTier.LIGHT;
    }

    @Override
    public Set<EventTag> tags() {
        return Set.of(EventTag.DOORS);
    }

    @Override
    public boolean canRun(ServerPlayer player) {
        return find(player) != null;
    }

    @Override
    public void run(ServerPlayer player) {
        BlockPos pos = find(player);
        if (pos == null) {
            return;
        }
        BlockState state = player.level().getBlockState(pos);
        DoorBlock door = (DoorBlock) state.getBlock();
        door.setOpen(null, player.level(), state, pos, !door.isOpen(state));
    }

    private static @Nullable BlockPos find(ServerPlayer player) {
        return Nearby.random(player, Config.event("door").integer("radius"), s -> s.getBlock() instanceof DoorBlock && DoorBlock.isWoodenDoor(s));
    }
}
