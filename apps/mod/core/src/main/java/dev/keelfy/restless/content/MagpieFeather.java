package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Scheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The hoarder's feather: used, a stream of light leads from the player to the nearest chest whose loot has not been
 * rolled, one nobody has opened, among the chunks that are loaded. It leads for {@code consumables.feather_ticks} or
 * until that chest is opened.
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

    /** Streams light toward the chest until its time is out or somebody opens it. */
    private static void point(ServerPlayer player, ServerLevel level, BlockPos target, Config.Section config) {
        int every = 4;
        int steps = config.integer("feather_ticks") / every;
        Scheduler.repeat(player, 0, every, (p, n) -> {
            if (!unopened(level, target)) {
                p.sendSystemMessage(Component.translatable("message.restless.feather_done"), true);
                return false;
            }
            stream(level, p, target);
            return n + 1 < steps;
        });
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
                && level.getBlockEntity(at) instanceof ChestBlockEntity chest
                && chest.getLootTable() != null;
    }

    /** The nearest chest with a loot table not yet rolled, among the loaded chunks within {@code radius}. */
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
                    if (entity instanceof ChestBlockEntity chest
                            && chest.getLootTable() != null
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
