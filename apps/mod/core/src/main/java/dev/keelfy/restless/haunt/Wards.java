package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.WardBlock;
import dev.keelfy.restless.content.WornWard;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Local protection, checked when a spirit's event is about to run on a player. A ward stops that one event; the spirit's
 * cooldown still runs, so the event is lost, not postponed. Salt: the player stands inside a closed loop of it (walls may
 * close the gaps). Hallowed lantern, dreamcatcher (while the player sleeps), door charm (door events) and scarecrow (any
 * event) work by the blocks near the player; the last two wear out.
 */
public final class Wards {
    private Wards() {}

    public static boolean stops(ServerPlayer player, HauntEvent event) {
        if (shelters(player)) {
            return true;
        }
        ServerLevel level = player.level();
        Config.Section config = Config.section("wards");
        Map<WardBlock.Kind, BlockPos> near = new EnumMap<>(WardBlock.Kind.class);
        int radius = Math.max(Math.max(config.integer("lantern_radius"), config.integer("dreamcatcher_radius")),
            Math.max(config.integer("charm_radius"), config.integer("scarecrow_radius")));
        BlockPos at = player.blockPosition();
        Scan.each(level, at, radius, (pos, state) -> {
            if (state.getBlock() instanceof WardBlock ward && pos.distSqr(at) <= square(reach(config, ward.kind()))) {
                near.putIfAbsent(ward.kind(), pos.immutable());
            }
        });
        if (near.containsKey(WardBlock.Kind.LANTERN)) {
            return true;
        }
        if (near.containsKey(WardBlock.Kind.DREAMCATCHER) && player.isSleeping()) {
            return true;
        }
        if (near.containsKey(WardBlock.Kind.CHARM) && event.tags().contains(EventTag.DOORS)) {
            wear(level, near.get(WardBlock.Kind.CHARM));
            return true;
        }
        if (near.containsKey(WardBlock.Kind.SCARECROW)) {
            wear(level, near.get(WardBlock.Kind.SCARECROW));
            return true;
        }
        return false;
    }

    private static double square(int radius) {
        return (double) radius * radius;
    }

    private static int reach(Config.Section config, WardBlock.Kind kind) {
        return switch (kind) {
            case LANTERN -> config.integer("lantern_radius");
            case DREAMCATCHER -> config.integer("dreamcatcher_radius");
            case CHARM -> config.integer("charm_radius");
            case SCARECROW -> config.integer("scarecrow_radius");
        };
    }

    private static void wear(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof WornWard ward) {
            ward.spend(level, pos, state);
        }
    }

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

    /** A bell rang at {@code pos}: every spirit of a player within earshot, and every territorial one it covers, waits longer. */
    public static void rung(ServerLevel level, BlockPos pos) {
        Config.Section config = Config.section("bell");
        int reach = config.integer("radius");
        long delay = config.integer("delay_ticks");
        HauntData data = HauntData.get(level.getServer());
        for (ServerPlayer player : level.players()) {
            List<HauntData.Haunting> spirits = data.get(player.getUUID());
            if (spirits != null && player.blockPosition().distSqr(pos) <= square(reach)) {
                data.put(player.getUUID(), spirits.stream().map(h -> h.rescheduled(h.nextEventAt() + delay)).toList());
            }
        }
        data.putTerritorial(data.territorial().stream()
            .map(t -> t.dimension().equals(level.dimension()) && t.covers(pos, reach) ? t.rescheduled(t.haunting().nextEventAt() + delay) : t).toList());
    }
}
