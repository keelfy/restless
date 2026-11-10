package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.event.kit.Nearby;
import dev.keelfy.restless.haunt.event.kit.PacketEntity;
import dev.keelfy.restless.haunt.event.kit.Place;
import dev.keelfy.restless.haunt.event.kit.SimpleEvent;
import dev.keelfy.restless.haunt.event.kit.Swaps;
import dev.keelfy.restless.haunt.event.kit.Where;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Events bound to a place: the desert, the swamp, the snow, the mountains, the sea. Each is rolled only where
 * {@link Where} says the player is; what they change in the world goes back through {@link Restore}.
 */
final class Wilds {
    private Wilds() {}

    static List<HauntEvent> all() {
        return List.of(
                // 467: a husk stands far off in the heat haze, until the player comes near
                SimpleEvent.light(
                        "desert_husk_haze",
                        EventTag.MOBS,
                        player -> desert(player) && huskSpot(player) != null,
                        Wilds::huskHaze),
                // 468: sand trickles from the ceiling above the player
                SimpleEvent.light(
                        "desert_sand_trickle",
                        EventTag.BLOCKS,
                        player -> desert(player) && ceiling(player) != null,
                        Wilds::sandTrickle),
                // 469: a stone pressure plate clicks nearby, with no plate there
                SimpleEvent.light("desert_plate_click", EventTag.SOUND, Wilds::desert, player -> {
                    Config.Section config = Config.event("desert_plate_click");
                    Nearby.sound(
                            player,
                            SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON,
                            Nearby.source(player, config.num("distance")),
                            config.tone("sound"));
                }),
                // 470: a lily pad drifts toward the player a block at a time, and goes home later
                SimpleEvent.light(
                        "swamp_lily_drift",
                        EventTag.WATER,
                        player -> swamp(player) && !drifters(player).isEmpty(),
                        Wilds::lilyDrift),
                // 471: a cauldron bubbles
                SimpleEvent.light(
                        "cauldron",
                        EventTag.BLOCKS,
                        player -> Nearby.any(player, "cauldron", Wilds::cauldron),
                        Wilds::cauldronBubbles),
                // 472: the firefly bushes around the player show as plain bushes
                SimpleEvent.light(
                        "fireflies_dark",
                        EventTag.BLOCKS,
                        player -> Nearby.any(player, "fireflies_dark", s -> s.is(Blocks.FIREFLY_BUSH)),
                        player -> Swaps.show(
                                player,
                                Nearby.all(player, "fireflies_dark", s -> s.is(Blocks.FIREFLY_BUSH)),
                                Blocks.BUSH.defaultBlockState(),
                                Config.event("fireflies_dark").integer("ticks"))),
                // 473: real snow piles against the outside of a door
                SimpleEvent.light(
                        "snow_door_drift",
                        EventTag.BLOCKS,
                        player -> snowy(player) && !drifts(player).isEmpty(),
                        Wilds::doorDrift),
                // 474: a snowball comes out of nowhere; it does no damage
                SimpleEvent.harmful("snow_snowball", EventTag.ASSAULT, Wilds::snowy, Wilds::snowball),
                // 475: the path ahead turns to powder snow, one block deep
                SimpleEvent.harmful(
                        "snow_powder_path",
                        EventTag.ASSAULT,
                        player -> snowy(player)
                                && Assaults.healthy(player, "snow_powder_path")
                                && powderSpot(player) != null,
                        Wilds::powderPath),
                // 476: far off a slope lets go, with a rumble and a cloud of snow falling
                SimpleEvent.light("mountain_avalanche", EventTag.SOUND, Wilds::mountains, Wilds::avalanche),
                // 477: wind howls on a cliff edge and pushes toward it
                SimpleEvent.harmful(
                        "mountain_wind_push",
                        EventTag.ASSAULT,
                        player -> mountains(player)
                                && Assaults.healthy(player, "mountain_wind_push")
                                && Place.ledge(player) != null,
                        Wilds::windPush),
                // 478: a ship's bell tolls from the open sea
                SimpleEvent.light(
                        "ocean_ship_bell",
                        EventTag.SOUND,
                        player -> Where.biome(player, BiomeTags.IS_OCEAN),
                        Wilds::bell));
    }

    private static boolean desert(ServerPlayer player) {
        return Where.biome(player, Biomes.DESERT);
    }

    private static boolean swamp(ServerPlayer player) {
        return Where.biome(player, Biomes.SWAMP, Biomes.MANGROVE_SWAMP);
    }

    private static boolean snowy(ServerPlayer player) {
        return Where.biome(
                player,
                Biomes.SNOWY_PLAINS,
                Biomes.ICE_SPIKES,
                Biomes.SNOWY_TAIGA,
                Biomes.SNOWY_BEACH,
                Biomes.GROVE,
                Biomes.SNOWY_SLOPES);
    }

    private static boolean mountains(ServerPlayer player) {
        return Where.biome(player, BiomeTags.IS_MOUNTAIN);
    }

    private static @Nullable BlockPos huskSpot(ServerPlayer player) {
        Config.Section config = Config.event("desert_husk_haze");
        return Place.standingAhead(player, config.num("distance"), 0.0, config.num("max_angle"));
    }

    /** Shown facing the player until they come near, or for a while; nothing on the server. */
    private static void huskHaze(ServerPlayer player) {
        Config.Section config = Config.event("desert_husk_haze");
        BlockPos spot = huskSpot(player);
        if (spot == null) {
            return;
        }
        Vec3 at = Vec3.atBottomCenterOf(spot);
        PacketEntity mob =
                PacketEntity.show(player, EntityTypes.HUSK, at, PacketEntity.yawToward(at, player.position()));
        ServerLevel level = player.level();
        double near = config.num("vanish_distance");
        Scheduler.repeat(player, 1, 1, (p, n) -> {
            boolean over = n >= config.integer("max_ticks")
                    || p.level() != level
                    || p.position().distanceToSqr(at) <= near * near;
            if (over) {
                mob.remove(p);
            }
            return !over;
        });
    }

    /** The first solid block above the player's head, within the configured height, with air below it. */
    private static @Nullable BlockPos ceiling(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos head = BlockPos.containing(player.getEyePosition());
        for (int up = 1; up <= Config.event("desert_sand_trickle").integer("height"); up++) {
            BlockPos pos = head.above(up);
            if (level.getBlockState(pos).isSolidRender()) {
                return pos;
            }
            if (!level.getBlockState(pos).isAir()) {
                return null;
            }
        }
        return null;
    }

    private static void sandTrickle(ServerPlayer player) {
        BlockPos roof = ceiling(player);
        if (roof == null) {
            return;
        }
        Config.Section config = Config.event("desert_sand_trickle");
        Config.Burst sand = config.burst("particles");
        BlockParticleOption grains =
                new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState());
        Vec3 at = new Vec3(roof.getX() + 0.5, roof.getY() + sand.offsetY(), roof.getZ() + 0.5);
        Nearby.sound(player, SoundEvents.SAND_BREAK, at, config.tone("sound"));
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            p.level()
                    .sendParticles(
                            p,
                            grains,
                            false,
                            false,
                            at.x,
                            at.y,
                            at.z,
                            sand.count(),
                            sand.dx(),
                            sand.dy(),
                            sand.dz(),
                            sand.speed());
            return n + 1 < config.integer("pulses");
        });
    }

    /** The lily pad's next block toward the player, or null if it cannot go there or has come close enough. */
    private static @Nullable BlockPos driftTo(ServerPlayer player, BlockPos pad) {
        ServerLevel level = player.level();
        Vec3 toward = player.position().subtract(Vec3.atCenterOf(pad));
        BlockPos next = pad.relative(Direction.getApproximateNearest(toward.x, 0.0, toward.z));
        double stop = Config.event("swamp_lily_drift").num("stop_distance");
        return level.isLoaded(next)
                        && level.isEmptyBlock(next)
                        && Blocks.LILY_PAD.defaultBlockState().canSurvive(level, next)
                        && Vec3.atCenterOf(next).distanceToSqr(player.position()) > stop * stop
                ? next
                : null;
    }

    private static List<BlockPos> drifters(ServerPlayer player) {
        return Nearby.all(player, "swamp_lily_drift", s -> s.is(Blocks.LILY_PAD)).stream()
                .filter(pad -> driftTo(player, pad) != null)
                .toList();
    }

    /** One pad steps toward the player every interval; the restore puts it back where it started. */
    private static void lilyDrift(ServerPlayer player) {
        List<BlockPos> pads = drifters(player);
        if (pads.isEmpty()) {
            return;
        }
        Config.Section config = Config.event("swamp_lily_drift");
        ServerLevel level = player.level();
        BlockPos home = pads.get(player.getRandom().nextInt(pads.size()));
        BlockPos[] at = {home};
        BlockState pad = Blocks.LILY_PAD.defaultBlockState();
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            BlockPos next =
                    p.level() == level && level.getBlockState(at[0]).is(Blocks.LILY_PAD) ? driftTo(p, at[0]) : null;
            if (next == null) {
                return false;
            }
            Restore.quiet(level, at[0], Blocks.AIR.defaultBlockState());
            Restore.quiet(level, next, pad);
            at[0] = next;
            return n + 1 < config.integer("steps");
        });
        Restore.revert(level, config.integer("restore_ticks"), force -> {
            if (!force && !(level.isLoaded(at[0]) && level.isLoaded(home))) {
                return false;
            }
            if (!at[0].equals(home) && level.getBlockState(at[0]).is(Blocks.LILY_PAD) && level.isEmptyBlock(home)) {
                Restore.quiet(level, at[0], Blocks.AIR.defaultBlockState());
                Restore.quiet(level, home, pad);
            }
            return true;
        });
    }

    private static boolean cauldron(BlockState state) {
        return state.is(BlockTags.CAULDRONS);
    }

    private static void cauldronBubbles(ServerPlayer player) {
        Config.Section config = Config.event("cauldron");
        BlockPos pot = Nearby.random(player, "cauldron", Wilds::cauldron);
        if (pot == null) {
            return;
        }
        Config.Burst bubbles = config.burst("particles");
        Vec3 at = Vec3.atCenterOf(pot);
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            Nearby.sound(p, SoundEvents.BREWING_STAND_BREW, at, config.tone("sound"));
            p.level()
                    .sendParticles(
                            ParticleTypes.BUBBLE_POP,
                            at.x,
                            pot.getY() + bubbles.offsetY(),
                            at.z,
                            bubbles.count(),
                            bubbles.dx(),
                            bubbles.dy(),
                            bubbles.dz(),
                            bubbles.speed());
            return n + 1 < config.integer("pulses");
        });
    }

    /** A free block on the outside of a wooden door, where snow could lie, and the way out from the door. */
    private record Drift(BlockPos spot, Direction out) {}

    private static boolean lowerDoor(BlockState state) {
        return state.getBlock() instanceof DoorBlock
                && DoorBlock.isWoodenDoor(state)
                && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
    }

    private static List<Drift> drifts(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockState snow = Blocks.SNOW.defaultBlockState();
        List<Drift> found = new ArrayList<>();
        for (BlockPos door : Nearby.all(player, "snow_door_drift", Wilds::lowerDoor)) {
            Direction facing = level.getBlockState(door).getValue(DoorBlock.FACING);
            for (Direction out : new Direction[] {facing, facing.getOpposite()}) {
                BlockPos spot = door.relative(out);
                if (snowReplaceable(level, spot) && level.canSeeSky(spot) && snow.canSurvive(level, spot)) {
                    found.add(new Drift(spot, out));
                }
            }
        }
        return found;
    }

    /** The spot gets the full depth, the blocks beside it along the wall one layer less. */
    private static void doorDrift(ServerPlayer player) {
        List<Drift> drifts = drifts(player);
        if (drifts.isEmpty()) {
            return;
        }
        Config.Section config = Config.event("snow_door_drift");
        ServerLevel level = player.level();
        Drift drift = drifts.get(player.getRandom().nextInt(drifts.size()));
        int layers = config.integer("layers");
        int ticks = config.integer("restore_ticks");
        piles(level, drift.spot(), layers, ticks);
        if (layers > 1) {
            piles(level, drift.spot().relative(drift.out().getClockWise()), layers - 1, ticks);
            piles(level, drift.spot().relative(drift.out().getCounterClockWise()), layers - 1, ticks);
        }
    }

    private static boolean snowReplaceable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir()
                || state.is(Blocks.SNOW)
                || (state.canBeReplaced() && level.getFluidState(pos).isEmpty());
    }

    private static void piles(ServerLevel level, BlockPos pos, int layers, int ticks) {
        BlockState snow = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers);
        if (snowReplaceable(level, pos) && snow.canSurvive(level, pos)) {
            Restore.swap(level, pos, snow, ticks);
        }
    }

    private static void snowball(ServerPlayer player) {
        Config.Section config = Config.event("snow_snowball");
        ServerLevel level = player.level();
        Vec3 from = Nearby.randomAround(player, config.num("distance")).with(Direction.Axis.Y, player.getEyeY());
        Vec3 aim = player.getEyePosition().subtract(from);
        Snowball ball = new Snowball(level, from.x, from.y, from.z, new ItemStack(Items.SNOWBALL));
        ball.shoot(aim.x, aim.y, aim.z, (float) config.num("speed"), 0.0F);
        level.addFreshEntity(ball);
        Nearby.sound(player, SoundEvents.SNOWBALL_THROW, from, config.tone("sound"));
    }

    /** Standing room ahead whose floor is a plain block with solid ground under it, so the pit is one block deep. */
    private static @Nullable BlockPos powderSpot(ServerPlayer player) {
        Config.Section config = Config.event("snow_powder_path");
        ServerLevel level = player.level();
        return Place.standingAhead(
                player,
                config.num("distance"),
                0.0,
                config.num("max_angle"),
                pos -> Place.solid(level, pos.below())
                        && level.getBlockState(pos.below(2)).isSolidRender());
    }

    private static void powderPath(ServerPlayer player) {
        BlockPos spot = powderSpot(player);
        if (spot != null) {
            Restore.swap(
                    player.level(),
                    spot.below(),
                    Blocks.POWDER_SNOW.defaultBlockState(),
                    Config.event("snow_powder_path").integer("restore_ticks"));
        }
    }

    /**
     * A rumble that carries, and snow falling in a cloud at a far point, loud at the start and then every few steps.
     */
    private static void avalanche(ServerPlayer player) {
        Config.Section config = Config.event("mountain_avalanche");
        Vec3 base = Nearby.source(player, config.num("distance"));
        Config.Burst snow = config.burst("particles");
        int steps = config.integer("steps");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            if (n % config.integer("rumble_every") == 0) {
                Nearby.sound(p, SoundEvents.GENERIC_EXPLODE.value(), base, config.tone("sound"));
            }
            double y = p.getY() + config.num("height") * (1.0 - n / (double) steps) + snow.offsetY();
            p.level()
                    .sendParticles(
                            p,
                            ParticleTypes.CLOUD,
                            true,
                            true,
                            base.x,
                            y,
                            base.z,
                            snow.count(),
                            snow.dx(),
                            snow.dy(),
                            snow.dz(),
                            snow.speed());
            return n + 1 < steps;
        });
    }

    private static void windPush(ServerPlayer player) {
        Config.Section config = Config.event("mountain_wind_push");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            Nearby.sound(p, SoundEvents.BREEZE_IDLE_AIR, p.position(), config.tone("sound"));
            if (n + 1 < config.integer("howls")) {
                return true;
            }
            Direction toward = Place.ledge(p);
            if (toward != null) {
                double push = config.num("push");
                p.push(toward.getStepX() * push, 0.0, toward.getStepZ() * push);
                p.connection.send(new ClientboundSetEntityMotionPacket(p));
            }
            return false;
        });
    }

    private static void bell(ServerPlayer player) {
        Config.Section config = Config.event("ocean_ship_bell");
        Vec3 at = Nearby.source(player, config.num("distance"));
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            Nearby.sound(p, SoundEvents.BELL_BLOCK, at, config.tone("sound"));
            return n + 1 < config.integer("tolls");
        });
    }
}
