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
import net.minecraft.world.level.material.PushReaction;

/**
 * Ash on the floor. Nearby spirits leave a trail of separate impressions above the powder. The spirit's seed picks one
 * of {@link #PRINTS} shapes, so the same spirit always leaves the same kind of print.
 */
public final class AshBlock extends CarpetBlock {
    public static final int PRINTS = 4;

    public AshBlock(BlockBehaviour.Properties properties) {
        super(properties
                .noCollision()
                .noOcclusion()
                .instabreak()
                .sound(SoundType.SAND)
                .pushReaction(PushReaction.POPPED));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide() && !oldState.is(this)) {
            level.scheduleTick(pos, this, Config.section("ash").integer("reveal_ticks"));
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.scheduleTick(pos, this, Config.section("ash").integer("reveal_ticks"));
        if (!level.getEntitiesOfClass(AshFootprint.class, new net.minecraft.world.phys.AABB(pos).inflate(1.5))
                .isEmpty()) {
            return;
        }
        HauntData data = HauntData.get(level.getServer());
        for (Sense.Spirit spirit :
                Sense.around(level, pos, Config.section("ash").integer("reach"), data)) {
            HauntData.Haunting haunting = spirit.haunting();
            if (Clues.reveal(Traits.of(haunting.traits()), Traits.all(), Clue.FOOTPRINTS, new Random(haunting.seed()))
                    .isPresent()) {
                int print = 1 + (int) Math.floorMod(haunting.seed(), (long) PRINTS);
                double angle = spirit.at() == null
                        ? new Random(haunting.seed()).nextDouble() * Math.PI * 2
                        : Math.atan2(spirit.at().z - pos.getZ() - .5, spirit.at().x - pos.getX() - .5);
                if (!AshFootprint.leaveTrail(level, pos, print, angle)) {
                    continue;
                }
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
