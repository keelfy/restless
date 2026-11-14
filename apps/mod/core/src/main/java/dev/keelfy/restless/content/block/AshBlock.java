package dev.keelfy.restless.content.block;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.entity.AshFootprint;
import dev.keelfy.restless.divine.Clues;
import dev.keelfy.restless.divine.Sense;
import dev.keelfy.restless.journal.Journal;
import dev.keelfy.restless.journal.Notebook;
import dev.keelfy.restless.spirit.Clue;
import dev.keelfy.restless.spirit.Trait;
import dev.keelfy.restless.spirit.Traits;
import dev.keelfy.restless.state.HauntData;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

/**
 * Ash on the floor. Nearby spirits leave a trail of separate impressions above the powder. The print is the build of
 * the spirit's type (the same build its photo shows), one of {@link #PRINTS} shapes, so it says what kind of spirit
 * walked and nothing about its nature. A spirit that walks in rain leaves damp prints.
 */
public final class AshBlock extends CarpetBlock {
    public static final int PRINTS = 4;

    /** The print a spirit type leaves: the 1-based row of {@code ash.prints} that lists it. */
    public static int print(String spirit) {
        List<List<String>> rows = Config.section("ash").lines("prints");
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).contains(spirit)) {
                return i + 1;
            }
        }
        return 1;
    }

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
            Optional<Trait> shown = Clues.reveal(
                    Traits.of(haunting.traits()), Traits.all(), Clue.FOOTPRINTS, new Random(random.nextLong()));
            if (shown.isPresent()) {
                int print = print(haunting.spirit());
                double angle = spirit.at() == null
                        ? new Random(haunting.seed()).nextDouble() * Math.PI * 2
                        : Math.atan2(spirit.at().z - pos.getZ() - .5, spirit.at().x - pos.getX() - .5);
                if (!AshFootprint.leaveTrail(
                        level, pos, print, angle, haunting.traits().contains("rain_walker"))) {
                    continue;
                }
                int reach = Config.section("ash").integer("reach");
                List<ServerPlayer> near = level.players().stream()
                        .filter(p -> p.blockPosition().distSqr(pos) <= (double) reach * reach)
                        .toList();
                // A personal spirit's prints go only to whoever it haunts; a territorial one's to all who stand near.
                List<ServerPlayer> owners = near.stream()
                        .filter(p -> Optional.ofNullable(data.get(p.getUUID()))
                                .filter(own ->
                                        own.stream().anyMatch(h -> h.uuid().equals(haunting.uuid())))
                                .isPresent())
                        .toList();
                (owners.isEmpty() ? near : owners)
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
