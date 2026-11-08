package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The hoarder's feather: used, a stream of light leads from the player to the nearest container whose loot has not been
 * rolled, a chest, barrel or decorated pot nobody has opened, among the chunks that are loaded.
 */
public final class MagpieFeather extends Item {
    public MagpieFeather(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer server) || !(level instanceof ServerLevel world)) {
            return InteractionResult.SUCCESS;
        }
        Config.Section config = Config.section("consumables");
        BlockPos first = nearest(world, server.blockPosition(), config.num("feather_radius"));
        if (first == null) {
            server.sendSystemMessage(Component.translatable("message.restless.feather_nothing"), true);
            return InteractionResult.FAIL;
        }
        player.getItemInHand(hand).consume(1, player);
        world.playSound(null, server.blockPosition(), SoundEvents.PARROT_AMBIENT, SoundSource.PLAYERS, 1.0f, 1.4f);
        point(server, world, first, config);
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Streams light toward the container until the feather's time is out or nothing unopened is left in reach. */
    private static void point(ServerPlayer player, ServerLevel level, BlockPos first, Config.Section config) {
        AtomicReference<@Nullable BlockPos> target = new AtomicReference<>(first);
        int every = 4;
        int rescan = Math.max(1, config.integer("feather_rescan_ticks") / every);
        int steps = config.integer("feather_ticks") / every;
        Scheduler.repeat(player, 0, every, (p, n) -> {
            BlockPos at = target.get();
            if (at == null || !unopened(level, at) || n % rescan == 0) {
                at = unopened(level, at) && at != null
                        ? nearer(level, p.blockPosition(), at, config.num("feather_radius"))
                        : nearest(level, p.blockPosition(), config.num("feather_radius"));
                target.set(at);
            }
            if (at == null) {
                p.sendSystemMessage(Component.translatable("message.restless.feather_done"), true);
                return false;
            }
            stream(level, p, at);
            return n + 1 < steps;
        });
    }

    private static BlockPos nearer(ServerLevel level, BlockPos from, BlockPos current, double radius) {
        BlockPos best = nearest(level, from, radius);
        return best != null && best.distSqr(from) < current.distSqr(from) ? best : current;
    }

    /** A line of light ahead of the player toward the container, and a column on it. */
    private static void stream(ServerLevel level, ServerPlayer player, BlockPos at) {
        Vec3 goal = Vec3.atCenterOf(at);
        Vec3 eye = player.getEyePosition();
        Vec3 way = goal.subtract(eye);
        double length = way.length();
        if (length > 0.1) {
            Vec3 unit = way.scale(1 / length);
            for (double d = 1.5; d < Math.min(length, 7.0); d += 0.9) {
                Vec3 p = eye.add(unit.scale(d));
                level.sendParticles(ParticleTypes.END_ROD, p.x, p.y - 0.4, p.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
        for (double y = 0; y <= 1.5; y += 0.5) {
            level.sendParticles(ParticleTypes.GLOW, goal.x, goal.y + y, goal.z, 1, 0.1, 0.0, 0.1, 0.0);
        }
    }

    private static boolean unopened(ServerLevel level, @Nullable BlockPos at) {
        return at != null
                && level.isLoaded(at)
                && level.getBlockEntity(at) instanceof RandomizableContainer container
                && container.getLootTable() != null;
    }

    /** The nearest container with a loot table not yet rolled, among the loaded chunks within {@code radius}. */
    public static @Nullable BlockPos nearest(ServerLevel level, BlockPos from, double radius) {
        int chunks = (int) Math.ceil(radius / 16.0);
        ChunkPos centre = ChunkPos.containing(from);
        BlockPos best = null;
        double bestDistance = radius * radius;
        for (int cx = centre.x() - chunks; cx <= centre.x() + chunks; cx++) {
            for (int cz = centre.z() - chunks; cz <= centre.z() + chunks; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    if (entity instanceof RandomizableContainer container
                            && container.getLootTable() != null
                            && entity.getBlockPos().distSqr(from) < bestDistance) {
                        bestDistance = entity.getBlockPos().distSqr(from);
                        best = entity.getBlockPos();
                    }
                }
            }
        }
        return best;
    }
}
