package dev.keelfy.restless.ward;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.block.SaltLineBlock;
import dev.keelfy.restless.content.block.WardBlock;
import dev.keelfy.restless.content.block.WornWard;
import dev.keelfy.restless.content.item.charm.Charms;
import dev.keelfy.restless.divine.Divination;
import dev.keelfy.restless.haunt.Calendar;
import dev.keelfy.restless.haunt.Scan;
import dev.keelfy.restless.haunt.event.EventTag;
import dev.keelfy.restless.haunt.event.HauntEvent;
import dev.keelfy.restless.journal.Journal;
import dev.keelfy.restless.journal.Notebook;
import dev.keelfy.restless.spirit.Clue;
import dev.keelfy.restless.state.HauntData;
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
 * Local protection, checked when a spirit's event is about to run on a player. A ward stops that one event; the
 * spirit's cooldown still runs, so the event is lost, not postponed. Salt: the player stands inside a closed loop of it
 * (walls may close the gaps). Hallowed lantern, dreamcatcher (while the player sleeps), door charm (door events) and
 * scarecrow (any event) work by the blocks near the player; the last two wear out. So does salt: each event a ring
 * stops wears its tiles, and a worn ring is a gap until a tile is laid beside it.
 */
public final class Wards {
    private Wards() {}

    public static boolean stops(ServerPlayer player, HauntEvent event) {
        if (Blessing.quiet(player)) {
            return true;
        }
        ServerLevel level = player.level();
        List<BlockPos> ring = ring(level, player.blockPosition());
        if (!ring.isEmpty()) {
            for (int i = 0; i < wearSteps(level); i++) {
                SaltLineBlock.wear(level, ring);
            }
            return true;
        }
        if (Charms.spared(player, event) || Weakness.stops(player, event)) {
            return true;
        }
        Config.Section config = Config.section("wards");
        Map<WardBlock.Kind, BlockPos> near = new EnumMap<>(WardBlock.Kind.class);
        int radius = Math.max(
                Math.max(config.integer("lantern_radius"), config.integer("dreamcatcher_radius")),
                Math.max(
                        Math.max(config.integer("charm_radius"), config.integer("scarecrow_radius")),
                        Config.section("relics").integer("hollow_radius")));
        BlockPos at = player.blockPosition();
        Scan.each(level, at, radius, (pos, state) -> {
            if (state.getBlock() instanceof WardBlock ward && pos.distSqr(at) <= square(reach(config, ward.kind()))) {
                near.putIfAbsent(ward.kind(), pos.immutable());
            }
        });
        if (near.containsKey(WardBlock.Kind.HOLLOW)) {
            hollowClue(player);
            return true;
        }
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
        return Charms.keeps(player);
    }

    /** What a hollow lantern gives for the event it blocked: one trait of a spirit near the player, in the journal. */
    private static void hollowClue(ServerPlayer player) {
        Divination.read(player, Clue.PLANCHETTE)
                .ifPresent(reading -> Journal.note(
                        player, Notebook.Kind.PLANCHETTE, reading.spirit().haunting(), Divination.phrase(reading)));
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
            case HOLLOW -> Config.section("relics").integer("hollow_radius");
        };
    }

    /** What one stopped event costs a ward: a blood night wears it {@code calendar.blood_wear_factor} times as fast. */
    private static int wearSteps(ServerLevel level) {
        return Calendar.today(level.getServer()).bloodNight()
                ? Config.section("calendar").integer("blood_wear_factor")
                : 1;
    }

    private static void wear(ServerLevel level, BlockPos pos) {
        for (int i = 0; i < wearSteps(level); i++) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof WornWard ward) {
                ward.spend(level, pos, state);
            }
        }
    }

    public static boolean shelters(ServerPlayer player) {
        return ringed(player.level(), player.blockPosition());
    }

    /** Whether {@code pos} lies inside a closed loop of fresh salt, walls closing the gaps. Worn salt is a gap. */
    public static boolean ringed(ServerLevel level, BlockPos pos) {
        return !ring(level, pos).isEmpty();
    }

    /** The salt tiles that close {@code pos} in; empty when it is not ringed. */
    private static List<BlockPos> ring(ServerLevel level, BlockPos pos) {
        return Enclosure.ring(
                        pos.getX(),
                        pos.getZ(),
                        (x, z) -> cell(level, new BlockPos(x, pos.getY(), z)),
                        Config.section("salt").integer("max_cells"))
                .stream()
                .map(c -> new BlockPos(c[0], pos.getY(), c[1]))
                .toList();
    }

    /** Whether {@code pos} is in a room walls close and a roof covers, with no salt needed. */
    public static boolean housed(ServerLevel level, BlockPos pos) {
        return level.getHeight(
                                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                                pos.getX(),
                                pos.getZ())
                        > pos.getY() + 1
                && Enclosure.closed(
                        pos.getX(),
                        pos.getZ(),
                        (x, z) -> cell(level, new BlockPos(x, pos.getY(), z)),
                        Config.section("charms").integer("house_cells"));
    }

    private static Enclosure.Cell cell(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(Content.SALT_LINE.get())) {
            return state.getValue(SaltLineBlock.WORN) ? Enclosure.Cell.WORN : Enclosure.Cell.SALT;
        }
        // The ritual platform must not wall off the flood fill before it reaches the salt outside it.
        if (state.is(Content.ALTAR.get())) {
            return Enclosure.Cell.OPEN;
        }
        boolean solid = !state.getCollisionShape(level, pos).isEmpty()
                || !level.getBlockState(pos.above())
                        .getCollisionShape(level, pos.above())
                        .isEmpty();
        return solid ? Enclosure.Cell.WALL : Enclosure.Cell.OPEN;
    }

    /** The visible and audible "blocked": ash drifts down around the player and something rings out. */
    public static void blocked(ServerPlayer player) {
        ServerLevel level = player.level();
        level.sendParticles(
                ParticleTypes.WHITE_ASH, player.getX(), player.getY() + 1.0, player.getZ(), 24, 0.8, 0.8, 0.8, 0.02);
        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                SoundSource.BLOCKS,
                0.6f,
                1.4f);
    }

    /**
     * A bell rang at {@code pos}: every spirit of a player within earshot, and every territorial one it covers, waits
     * longer.
     */
    public static void rung(ServerLevel level, BlockPos pos) {
        Config.Section config = Config.section("bell");
        int reach = config.integer("radius");
        long delay = config.integer("delay_ticks");
        HauntData data = HauntData.get(level.getServer());
        for (ServerPlayer player : level.players()) {
            List<HauntData.Haunting> spirits = data.get(player.getUUID());
            if (spirits != null && player.blockPosition().distSqr(pos) <= square(reach)) {
                data.put(
                        player.getUUID(),
                        spirits.stream()
                                .map(h -> h.rescheduled(h.nextEventAt() + delay))
                                .toList());
            }
        }
        for (HauntData.Territorial t : data.territorialCovering(level.dimension(), pos, reach)) {
            data.putTerritorial(t.rescheduled(t.haunting().nextEventAt() + delay));
        }
    }
}
