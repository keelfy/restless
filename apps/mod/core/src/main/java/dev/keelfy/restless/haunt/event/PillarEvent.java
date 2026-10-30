package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Restore;
import net.minecraft.core.BlockPos;
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
 * 121: another player pillars up on cobblestone some way off, as players do: a jump, a block under the feet, a swing of
 * the arm. It waits at the top until its time is up; the pillar goes later, and stays under hardcore.
 */
final class PillarEvent extends StrangerAct {
    private static final String ID = "pillar";
    private static final BlockState COBBLE = Blocks.COBBLESTONE.defaultBlockState();

    PillarEvent() {
        super(ID);
    }

    @Override
    void prepare(Mannequin figure) {
        figure.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.COBBLESTONE));
    }

    @Override
    boolean step(ServerPlayer player, Mannequin figure, Config.Section config, int n) {
        Figure.face(figure, player);
        return n >= config.integer("height") || climb(player, figure, config);
    }

    /** One block up: the figure rises, cobblestone goes where its feet were, the arm swings. False if there is no room. */
    private static boolean climb(ServerPlayer player, Mannequin figure, Config.Section config) {
        ServerLevel level = player.level();
        BlockPos feet = figure.blockPosition();
        if (!Place.open(level, feet) || !Place.open(level, feet.above(2))) {
            return false;
        }
        figure.setPos(figure.getX(), feet.getY() + 1, figure.getZ());
        Swaps.show(player, feet, COBBLE, config.integer("restore_ticks"));
        figure.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
        Config.Tone tone = config.tone("sound");
        level.playSound(null, feet, COBBLE.getSoundType().getPlaceSound(), SoundSource.BLOCKS, tone.volume(), tone.pitch());
        return true;
    }

    /** Standing room off to one side, open to the sky above for the whole pillar. */
    @Override
    @Nullable BlockPos spot(ServerPlayer player) {
        Config.Section config = Config.event(ID);
        return Place.standingAhead(player, config.num("distance"), 0.0, config.num("max_angle"), spot -> {
            for (int dy = 2; dy <= config.integer("height") + 1; dy++) {
                if (!player.level().isEmptyBlock(spot.above(dy))) {
                    return false;
                }
            }
            return true;
        });
    }
}
