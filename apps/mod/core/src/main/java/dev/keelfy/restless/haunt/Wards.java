package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

/** Local protection. A player inside a closed loop of salt (walls may close the gaps) cannot be reached: the spirit's event does not fire. */
public final class Wards {
    private Wards() {}

    public static boolean shelters(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos feet = player.blockPosition();
        return Enclosure.sealed(feet.getX(), feet.getZ(), (x, z) -> cell(level, new BlockPos(x, feet.getY(), z)),
            Config.section("salt").integer("max_cells"));
    }

    private static Enclosure.Cell cell(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(Content.SALT_LINE.get())) {
            return Enclosure.Cell.SALT;
        }
        boolean solid = !state.getCollisionShape(level, pos).isEmpty() || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
        return solid ? Enclosure.Cell.WALL : Enclosure.Cell.OPEN;
    }

    /** The visible and audible "blocked": ash drifts down around the player and something rings out. */
    public static void blocked(ServerPlayer player) {
        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.WHITE_ASH, player.getX(), player.getY() + 1.0, player.getZ(), 24, 0.8, 0.8, 0.8, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 0.6f, 1.4f);
    }
}
