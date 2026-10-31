package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Clues;
import dev.keelfy.restless.haunt.HauntData;
import dev.keelfy.restless.haunt.Journal;
import dev.keelfy.restless.haunt.Notebook;
import dev.keelfy.restless.haunt.Sense;
import dev.keelfy.restless.spirit.Clue;
import dev.keelfy.restless.spirit.Traits;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;

/**
 * Ash on the floor. Some time after it is laid, a spirit near it that leaves footprints is read off it: the print is
 * one of {@link #PRINTS} shapes, picked by the spirit's seed, so the same spirit always leaves the same print.
 */
public final class AshBlock extends CarpetBlock {
    public static final int PRINTS = 4;
    /** 0 is plain ash; 1..PRINTS are prints. */
    public static final IntegerProperty PRINT = IntegerProperty.create("print", 0, PRINTS);

    public AshBlock(BlockBehaviour.Properties properties) {
        super(properties.noCollision().instabreak().sound(SoundType.SAND).pushReaction(PushReaction.POPPED));
        registerDefaultState(stateDefinition.any().setValue(PRINT, 0));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(PRINT);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide() && state.getValue(PRINT) == 0 && !oldState.is(this)) {
            level.scheduleTick(pos, this, Config.section("ash").integer("reveal_ticks"));
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        HauntData data = HauntData.get(level.getServer());
        for (Sense.Spirit spirit :
                Sense.around(level, pos, Config.section("ash").integer("reach"), data)) {
            HauntData.Haunting haunting = spirit.haunting();
            if (Clues.reveal(Traits.of(haunting.traits()), Traits.all(), Clue.FOOTPRINTS, new Random(haunting.seed()))
                    .isPresent()) {
                int print = 1 + (int) Math.floorMod(haunting.seed(), (long) PRINTS);
                level.setBlock(pos, state.setValue(PRINT, print), 3);
                int reach = Config.section("ash").integer("reach");
                level.players().stream()
                        .filter(p -> p.blockPosition().closerThan(pos, reach))
                        .forEach(p -> Journal.note(
                                p,
                                Notebook.Kind.FOOTPRINTS,
                                haunting,
                                Component.translatable("journal.restless.print." + print)));
                return;
            }
        }
    }
}
