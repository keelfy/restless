package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.harm.Band;
import dev.keelfy.restless.haunt.recall.Overheard;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The mimic gives back what it took from the player: their own words, and a chest that is not one. */
final class Copies {
    private Copies() {}

    static List<HauntEvent> all() {
        return List.of(
                // mimic: the player's own line comes back, with a word changed
                SimpleEvent.light(
                        "mimic_parrot",
                        EventTag.PLAYER,
                        player -> !Overheard.recent(
                                        player, Config.event("mimic_parrot").integer("max_age_ticks"))
                                .isEmpty(),
                        Copies::parrot),
                // mimic: a second chest beside one the player has, that bites
                SimpleEvent.harmful("mimic_chest", EventTag.BLOCKS, player -> twin(player) != null, Copies::bite));
    }

    // ---- the parrot

    private static void parrot(ServerPlayer player) {
        Config.Section config = Config.event("mimic_parrot");
        List<Overheard.Line> lines = Overheard.recent(player, config.integer("max_age_ticks"));
        if (lines.isEmpty()) {
            return;
        }
        List<String> words =
                new ArrayList<>(List.of(lines.getFirst().text().trim().split("\\s+")));
        List<String> pool = new ArrayList<>(config.texts("words"));
        lines.stream()
                .skip(1)
                .forEach(line -> pool.addAll(List.of(line.text().trim().split("\\s+"))));
        pool.removeAll(words);
        if (words.isEmpty() || pool.isEmpty()) {
            return;
        }
        words.set(
                player.getRandom().nextInt(words.size()),
                pool.get(player.getRandom().nextInt(pool.size())));
        Component said = Component.translatable("chat.type.text", player.getDisplayName(), String.join(" ", words));
        Scheduler.later(player, config.integer("delay_ticks"), p -> p.sendSystemMessage(said));
    }

    // ---- the chest

    /** A free cell beside a chest of the player's, away from every other chest so the two do not join. */
    private record Twin(BlockPos at, Direction facing) {}

    private static @Nullable Twin twin(ServerPlayer player) {
        ServerLevel level = player.level();
        List<BlockPos> chests = Nearby.all(player, "mimic_chest", s -> s.is(Blocks.CHEST));
        List<Twin> found = new ArrayList<>();
        for (BlockPos chest : chests) {
            BlockState state = level.getBlockState(chest);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos cell = chest.relative(d);
                boolean alone = Direction.Plane.HORIZONTAL.stream()
                        .map(cell::relative)
                        .noneMatch(next ->
                                !next.equals(chest) && level.getBlockState(next).is(Blocks.CHEST));
                if (alone && level.isEmptyBlock(cell) && Place.solid(level, cell.below())) {
                    found.add(new Twin(cell, state.getValue(ChestBlock.FACING).getClockWise()));
                }
            }
        }
        return found.isEmpty() ? null : found.get(player.getRandom().nextInt(found.size()));
    }

    private static void bite(ServerPlayer player) {
        Twin twin = twin(player);
        if (twin == null) {
            return;
        }
        Config.Section config = Config.event("mimic_chest");
        ServerLevel level = player.level();
        BlockPos at = twin.at();
        level.setBlockAndUpdate(at, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, twin.facing()));
        if (!(level.getBlockEntity(at) instanceof ChestBlockEntity chest)) {
            return;
        }
        Scheduler.repeat(player, 0, 2, (p, n) -> {
            if (!level.getBlockState(at).is(Blocks.CHEST)) {
                return false;
            }
            if (p.containerMenu instanceof ChestMenu menu && menu.getContainer() == chest) {
                snap(p, at, config);
                return false;
            }
            return n * 2 < config.integer("restore_ticks");
        });
        Restore.revert(level, config.integer("restore_ticks"), force -> {
            if (!force && !level.isLoaded(at)) {
                return false;
            }
            if (level.getBlockState(at).is(Blocks.CHEST)) {
                level.removeBlock(at, false);
            }
            return true;
        });
    }

    /** The lid shuts on the player with a bite, and the chest is gone. */
    private static void snap(ServerPlayer player, BlockPos at, Config.Section config) {
        ServerLevel level = player.level();
        player.closeContainer();
        level.removeBlock(at, false);
        level.playSound(null, at, SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 1.0F, 1.0F);
        level.playSound(null, at, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.sendParticles(
                ParticleTypes.CRIT, at.getX() + 0.5, at.getY() + 0.7, at.getZ() + 0.5, 20, 0.3, 0.2, 0.3, 0.3);
        float allowed = Math.min((float) config.num("damage"), Assaults.room(player, Band.THREAT));
        if (allowed > 0.0F) {
            player.hurtServer(level, Taken.by(level, Taken.TAKEN), allowed);
        }
    }
}
