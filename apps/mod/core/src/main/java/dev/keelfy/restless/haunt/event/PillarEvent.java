package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntTier;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 121: another player, in a default skin, pillars up on cobblestone some way off, as players do: a jump, a block under
 * the feet, a swing of the arm. Gone at the top, or when approached; the pillar goes later, and stays under hardcore.
 */
final class PillarEvent implements HauntEvent {
    private static final String ID = "pillar";
    private static final BlockState COBBLE = Blocks.COBBLESTONE.defaultBlockState();
    /** The figure each player has now; one at a time. */
    private final Map<UUID, Mannequin> active = new ConcurrentHashMap<>();

    @Override
    public Identifier id() {
        return Identifier.parse(ID);
    }

    @Override
    public HauntTier tier() {
        return HauntTier.LIGHT;
    }

    @Override
    public Set<EventTag> tags() {
        return Set.of(EventTag.PRESENCE, EventTag.PLAYER);
    }

    @Override
    public boolean canRun(ServerPlayer player) {
        return !active.containsKey(player.getUUID()) && spot(player) != null;
    }

    @Override
    public void run(ServerPlayer player) {
        BlockPos spot = spot(player);
        if (spot == null) {
            return;
        }
        Config.Section config = Config.event(ID);
        Mannequin figure = Figure.spawn(player, spot, Figure.stranger(player));
        if (figure == null) {
            return;
        }
        figure.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.COBBLESTONE));
        UUID owner = player.getUUID();
        active.put(owner, figure);
        int interval = config.integer("interval_ticks");
        int height = config.integer("height");
        int steps = height + config.integer("linger_ticks") / interval;
        // The climb ends itself; this catches the cases where the scheduler drops it (the player died or left).
        Restore.after(player.level(), steps * interval + config.integer("cleanup_ticks"), force -> {
            leave(figure, owner);
            return true;
        });
        Scheduler.repeat(player, interval, interval, (p, n) -> {
            double near = config.num("vanish_distance");
            boolean over = !figure.isAlive() || figure.level() != p.level() || n >= steps || p.distanceToSqr(figure) <= near * near
                || n < height && !climb(p.level(), figure, config);
            if (over) {
                leave(figure, owner);
            } else {
                Figure.face(figure, p);
            }
            return !over;
        });
    }

    /** One block up: the figure rises, cobblestone goes where its feet were, the arm swings. False if there is no room. */
    private static boolean climb(ServerLevel level, Mannequin figure, Config.Section config) {
        BlockPos feet = figure.blockPosition();
        if (!level.isEmptyBlock(feet) || !level.isEmptyBlock(feet.above(2))) {
            return false;
        }
        figure.setPos(figure.getX(), feet.getY() + 1, figure.getZ());
        Restore.swap(level, feet, COBBLE, config.integer("restore_ticks"));
        figure.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
        Config.Tone tone = config.tone("sound");
        level.playSound(null, feet, COBBLE.getSoundType().getPlaceSound(), SoundSource.BLOCKS, tone.volume(), tone.pitch());
        return true;
    }

    /** Standing room off to one side, open to the sky above for the whole pillar. */
    private static @Nullable BlockPos spot(ServerPlayer player) {
        Config.Section config = Config.event(ID);
        double angle = (player.getRandom().nextDouble() * 2 - 1) * config.num("max_angle");
        BlockPos spot = Place.standingAt(player, config.num("distance"), angle);
        if (spot == null) {
            return null;
        }
        for (int dy = 2; dy <= config.integer("height") + 1; dy++) {
            if (!player.level().isEmptyBlock(spot.above(dy))) {
                return null;
            }
        }
        return spot;
    }

    private void leave(Mannequin figure, UUID owner) {
        figure.discard();
        active.remove(owner, figure);
    }
}
