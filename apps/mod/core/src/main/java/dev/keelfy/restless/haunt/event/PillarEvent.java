package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 121: another player pillars up some way off, as players do, on a block it holds (cobblestone, dirt, planks...): eyes
 * on its feet, a jump, a block under the feet at the top of it, a swing of the arm. It waits at the top, watching the
 * player, until its time is up; the pillar goes later, and stays under hardcore.
 */
final class PillarEvent extends StrangerAct {
    private static final String ID = "pillar";
    /** Looking down at its own feet, where the block goes. */
    private static final float LOOK_DOWN = 80.0F;
    /**
     * Height above the take-off, tick by tick, of a player's jump that lands a block up; vanilla's jump and gravity.
     */
    private static final List<Double> JUMP = jump();

    PillarEvent() {
        super(ID);
    }

    @Override
    void prepare(Mannequin figure) {
        List<String> blocks = Config.event(ID).texts("blocks");
        Item block = BuiltInRegistries.ITEM.getValue(
                Identifier.parse(blocks.get(figure.getRandom().nextInt(blocks.size()))));
        figure.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(block));
    }

    @Override
    boolean step(ServerPlayer player, Mannequin figure, Config.Section config, int n) {
        Figure.face(figure, player);
        return n >= config.integer("height") || climb(player, figure, config);
    }

    /**
     * One block up: the figure looks down and jumps, cobblestone goes where its feet were once they are above it, the
     * arm swings, and it lands on the block. False if there is no room.
     */
    private static boolean climb(ServerPlayer player, Mannequin figure, Config.Section config) {
        ServerLevel level = player.level();
        BlockPos feet = figure.blockPosition();
        if (!Place.open(level, feet) || !Place.open(level, feet.above(2))) {
            return false;
        }
        // The block it holds is the block it puts down.
        BlockState block = Block.byItem(figure.getMainHandItem().getItem()).defaultBlockState();
        figure.setXRot(LOOK_DOWN);
        Scheduler.repeat(player, 1, 1, (p, t) -> {
            if (!figure.isAlive() || t >= JUMP.size()) {
                return false;
            }
            double height = JUMP.get(t);
            figure.setPos(figure.getX(), feet.getY() + height, figure.getZ());
            if (height >= 1.0 && (t == 0 || JUMP.get(t - 1) < 1.0)) {
                Swaps.show(p, feet, block, config.integer("restore_ticks"));
                figure.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
                Config.Tone tone = config.tone("sound");
                level.playSound(
                        null,
                        feet,
                        block.getSoundType().getPlaceSound(),
                        SoundSource.BLOCKS,
                        tone.volume(),
                        tone.pitch());
            }
            return true;
        });
        return true;
    }

    private static List<Double> jump() {
        List<Double> heights = new ArrayList<>();
        double y = 0.0;
        double rise = 0.42;
        while (rise > 0 || y + rise > 1.0) {
            y += rise;
            rise = (rise - 0.08) * 0.98;
            heights.add(y);
        }
        heights.add(1.0);
        return List.copyOf(heights);
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
