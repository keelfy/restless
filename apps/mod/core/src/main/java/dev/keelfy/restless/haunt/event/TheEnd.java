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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Quiet wrongness in the End, and one portal in the Overworld: endermen that turn to look, a shulker that opens, a
 * crystal on a pillar that has none, a wind that drifts the player, a gateway beam, chorus that moved, an egg, ender
 * chests that open, and a portal to nowhere. Each only runs in its dimension ({@link Where}). What is shown goes
 * through {@link Swaps} or packets, so the world is unchanged; the rest is put back through {@link Restore}.
 */
final class TheEnd {
    private TheEnd() {}

    static List<HauntEvent> all() {
        return List.of(
                // 455: every enderman nearby turns to the player and stays calm
                SimpleEvent.light(
                        "endermen_turn",
                        EventTag.MOBS,
                        player -> Where.end(player) && !calm(player).isEmpty(),
                        TheEnd::turnToward),
                // 456: a city shulker opens its shell and shuts it again
                SimpleEvent.light(
                        "shulker_peeks",
                        EventTag.MOBS,
                        player -> Where.end(player) && !shulkers(player).isEmpty(),
                        TheEnd::peek),
                // 457: an end crystal on a pillar that has none
                SimpleEvent.light(
                        "empty_pillar_crystal",
                        EventTag.MOBS,
                        player -> Where.end(player) && !bareTops(player).isEmpty(),
                        TheEnd::crystal),
                // 458: a void wind drifts the player along the ground, never within reach of an edge
                SimpleEvent.harmful(
                        "void_wind",
                        EventTag.ASSAULT,
                        player -> Where.end(player)
                                && Assaults.healthy(player, "void_wind")
                                && player.onGround()
                                && !player.isPassenger()
                                && drift(player) != null,
                        TheEnd::blow),
                // 459: a gateway fires its beam with no one near
                SimpleEvent.light(
                        "gateway_beam",
                        EventTag.BLOCKS,
                        player -> Where.end(player) && !gateways(player).isEmpty(),
                        TheEnd::fire),
                // 460: chorus plants and flowers are a block over from where they were
                SimpleEvent.light(
                        "chorus_shift",
                        EventTag.BLOCKS,
                        player -> Where.end(player) && !shift(player).isEmpty(),
                        player -> Swaps.show(
                                player,
                                shift(player),
                                Config.event("chorus_shift").integer("ticks"))),
                // 461: a dragon egg sits on the ground where there is none
                SimpleEvent.light(
                        "phantom_egg",
                        EventTag.BLOCKS,
                        player -> Where.end(player) && eggSpot(player) != null,
                        TheEnd::egg),
                // 462: ender chests open all around, one after another, and shut
                SimpleEvent.light("ender_chests_open", EventTag.SOUND, Where::end, TheEnd::chests),
                // 463: a lit nether portal in an obsidian frame in the Overworld, leading nowhere
                SimpleEvent.light(
                        "nowhere_portal",
                        EventTag.BLOCKS,
                        player -> Where.overworld(player) && portal(player) != null,
                        TheEnd::lightPortal),
                // 466: an enderman stands off with a block from the player's pack in its hands
                SimpleEvent.light(
                        "enderman_home_block",
                        EventTag.MOBS,
                        player -> Where.end(player) && !packBlocks(player).isEmpty() && homeSpot(player) != null,
                        TheEnd::homeBlock));
    }

    private static AABB around(ServerPlayer player, String event) {
        return new AABB(player.blockPosition()).inflate(Nearby.radius(event));
    }

    // 455

    private static List<Enderman> calm(ServerPlayer player) {
        return player.level().getEntitiesOfClass(Enderman.class, around(player, "endermen_turn")).stream()
                .filter(e -> e.isAlive() && !e.isNoAi() && e.getTarget() == null)
                .toList();
    }

    /** Each faces the player, with its anger kept down, for the time. */
    private static void turnToward(ServerPlayer player) {
        Config.Section config = Config.event("endermen_turn");
        ServerLevel level = player.level();
        List<Enderman> men = calm(player);
        int interval = config.integer("interval_ticks");
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            for (Enderman man : men) {
                if (man.isAlive()) {
                    man.setTarget(null);
                    man.getNavigation().stop();
                    man.lookAt(EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
                }
            }
            return p.level() == level && n * interval < config.integer("ticks");
        });
    }

    // 456

    /** Shulker with its peek data reachable: the key is protected, so a subclass hands it out. */
    private static final class Peek extends Shulker {
        private Peek(EntityType<? extends Shulker> type, Level level) {
            super(type, level);
        }

        static EntityDataAccessor<Byte> amount() {
            return DATA_PEEK_ID;
        }
    }

    private static List<Shulker> shulkers(ServerPlayer player) {
        return player.level().getEntitiesOfClass(Shulker.class, around(player, "shulker_peeks")).stream()
                .filter(s -> s.isAlive() && !s.isNoAi() && s.getTarget() == null)
                .toList();
    }

    private static void peek(ServerPlayer player) {
        List<Shulker> found = shulkers(player);
        if (found.isEmpty()) {
            return;
        }
        Config.Section config = Config.event("shulker_peeks");
        Shulker shulker = found.get(player.getRandom().nextInt(found.size()));
        shell(shulker, config.integer("open"), SoundEvents.SHULKER_OPEN, config.tone("sound"));
        Restore.after(player.level(), config.integer("ticks"), force -> {
            if (shulker.isAlive()) {
                shell(shulker, 0, SoundEvents.SHULKER_CLOSE, config.tone("sound"));
            }
            return true;
        });
    }

    private static void shell(Shulker shulker, int amount, SoundEvent sound, Config.Tone tone) {
        shulker.getEntityData().set(Peek.amount(), (byte) amount);
        shulker.level()
                .playSound(null, shulker.blockPosition(), sound, SoundSource.HOSTILE, tone.volume(), tone.pitch());
    }

    // 457

    /** Bedrock on obsidian with free air above and no crystal on it: the top of a pillar whose crystal is gone. */
    private static List<BlockPos> bareTops(ServerPlayer player) {
        Config.Section config = Config.event("empty_pillar_crystal");
        ServerLevel level = player.level();
        int radius = config.integer("radius");
        double min = config.num("min_distance");
        List<BlockPos> found = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos column = new BlockPos(player.getBlockX() + dx, 0, player.getBlockZ() + dz);
                if (!level.isLoaded(column)) {
                    continue;
                }
                BlockPos top = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, column)
                        .below();
                if (level.getBlockState(top).is(Blocks.BEDROCK)
                        && level.getBlockState(top.below()).is(Blocks.OBSIDIAN)
                        && level.isEmptyBlock(top.above())
                        && player.position().distanceToSqr(Vec3.atCenterOf(top)) >= min * min
                        && level.getEntitiesOfClass(EndCrystal.class, new AABB(top.above()))
                                .isEmpty()) {
                    found.add(top);
                }
            }
        }
        return found;
    }

    private static void crystal(ServerPlayer player) {
        List<BlockPos> found = bareTops(player);
        if (found.isEmpty()) {
            return;
        }
        Vec3 at = Vec3.atBottomCenterOf(
                found.get(player.getRandom().nextInt(found.size())).above());
        linger(player, "empty_pillar_crystal", List.of(show(player, EntityTypes.END_CRYSTAL, at, null)), at, null);
    }

    // 458

    /** Away from the middle of the dimension, where the void is; null near the middle or when an edge is in reach. */
    private static @Nullable Vec3 drift(ServerPlayer player) {
        Vec3 out = new Vec3(player.getX(), 0.0, player.getZ());
        if (out.length() < Config.event("void_wind").num("min_origin")) {
            return null;
        }
        Vec3 way = out.normalize();
        return clear(player, way) ? way : null;
    }

    /** Whether there is ground, a few blocks down at most, every two blocks out to {@code stop_distance} along. */
    private static boolean clear(ServerPlayer player, Vec3 way) {
        Config.Section config = Config.event("void_wind");
        ServerLevel level = player.level();
        for (int k = 2; k <= config.integer("stop_distance"); k += 2) {
            BlockPos at = BlockPos.containing(player.getX() + way.x * k, player.getY(), player.getZ() + way.z * k);
            boolean ground = false;
            for (int d = 0; d <= config.integer("drop") && level.isLoaded(at.below(d)); d++) {
                ground |= level.getBlockState(at.below(d)).isSolidRender();
            }
            if (!ground) {
                return false;
            }
        }
        return true;
    }

    /** A howl first; then, for a while, the player is carried slowly along the ground until an edge is near. */
    private static void blow(ServerPlayer player) {
        Vec3 way = drift(player);
        if (way == null) {
            return;
        }
        Config.Section config = Config.event("void_wind");
        ServerLevel level = player.level();
        int interval = config.integer("interval_ticks");
        int sound = config.integer("sound_ticks");
        double pull = config.num("pull");
        Scheduler.repeat(player, config.integer("delay_ticks"), interval, (p, n) -> {
            if (p.level() != level || p.isPassenger() || !clear(p, way) || n * interval >= config.integer("ticks")) {
                return false;
            }
            if ((n * interval) % sound == 0) {
                Nearby.sound(p, SoundEvents.WIND_CHARGE_BURST.value(), p.position(), config.tone("sound"));
            }
            if (p.onGround()) {
                p.setDeltaMovement(way.x * pull, p.getDeltaMovement().y, way.z * pull);
                p.connection.send(new ClientboundSetEntityMotionPacket(p));
            }
            return true;
        });
        Nearby.sound(player, SoundEvents.WIND_CHARGE_BURST.value(), player.position(), config.tone("sound"));
    }

    // 459

    private static List<BlockPos> gateways(ServerPlayer player) {
        ServerLevel level = player.level();
        int reach = Config.event("gateway_beam").integer("chunks");
        int cx = SectionPos.blockToSectionCoord(player.getBlockX());
        int cz = SectionPos.blockToSectionCoord(player.getBlockZ());
        List<BlockPos> found = new ArrayList<>();
        for (int x = cx - reach; x <= cx + reach; x++) {
            for (int z = cz - reach; z <= cz + reach; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    if (entity instanceof TheEndGatewayBlockEntity) {
                        found.add(entity.getBlockPos());
                    }
                }
            }
        }
        return found;
    }

    /** The gateway's own beam event, the one an entity entering it starts; nobody enters. */
    private static void fire(ServerPlayer player) {
        List<BlockPos> found = gateways(player);
        if (!found.isEmpty()) {
            player.level().blockEvent(found.get(player.getRandom().nextInt(found.size())), Blocks.END_GATEWAY, 1, 0);
        }
    }

    // 460

    private static boolean chorus(BlockState state) {
        return state.is(Blocks.CHORUS_PLANT) || state.is(Blocks.CHORUS_FLOWER);
    }

    /**
     * Each chorus block nearest the player drawn one block over in one direction, where that is free, and gone from
     * where it was; empty when no direction moves any.
     */
    private static Map<BlockPos, BlockState> shift(ServerPlayer player) {
        ServerLevel level = player.level();
        List<BlockPos> blocks = Nearby.all(player, "chorus_shift", TheEnd::chorus).stream()
                .sorted(Comparator.comparingDouble(pos -> pos.distSqr(player.blockPosition())))
                .limit(Config.event("chorus_shift").integer("max_blocks"))
                .toList();
        List<Direction> ways = new ArrayList<>();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            ways.add(d);
        }
        Collections.shuffle(ways, new Random(player.getRandom().nextLong()));
        for (Direction way : ways) {
            Map<BlockPos, BlockState> shown = new LinkedHashMap<>();
            Map<BlockPos, BlockState> moved = new LinkedHashMap<>();
            for (BlockPos pos : blocks) {
                BlockPos to = pos.relative(way);
                if (level.isEmptyBlock(to)) {
                    shown.put(pos, Blocks.AIR.defaultBlockState());
                    moved.put(to, level.getBlockState(pos));
                }
            }
            if (!moved.isEmpty()) {
                shown.putAll(moved);
                return shown;
            }
        }
        return Map.of();
    }

    // 461

    private static @Nullable BlockPos eggSpot(ServerPlayer player) {
        Config.Section config = Config.event("phantom_egg");
        return Place.spawnSpot(player, config.integer("min_distance"), config.integer("max_distance"));
    }

    private static void egg(ServerPlayer player) {
        BlockPos spot = eggSpot(player);
        if (spot != null) {
            Swaps.show(
                    player,
                    spot,
                    Blocks.DRAGON_EGG.defaultBlockState(),
                    Config.event("phantom_egg").integer("ticks"));
        }
    }

    // 462

    /** Open sounds from random points around the player, one after another, and a close sound at each a while after. */
    private static void chests(ServerPlayer player) {
        Config.Section config = Config.event("ender_chests_open");
        double min = config.num("min_distance");
        for (int i = 0; i < config.integer("count"); i++) {
            double distance = min + player.getRandom().nextDouble() * (config.num("max_distance") - min);
            Vec3 at = Nearby.randomAround(player, distance);
            int open = i * config.integer("stagger_ticks");
            Scheduler.later(player, open, p -> Nearby.sound(p, SoundEvents.ENDER_CHEST_OPEN, at, config.tone("sound")));
            Scheduler.later(
                    player,
                    open + config.integer("hold_ticks"),
                    p -> Nearby.sound(p, SoundEvents.ENDER_CHEST_CLOSE, at, config.tone("sound")));
        }
    }

    // 463

    /**
     * A frame of obsidian four wide and five high, without corners, standing on the ground ahead and facing the player,
     * with the portal in it; null where it does not fit.
     */
    private static @Nullable Map<BlockPos, BlockState> portal(ServerPlayer player) {
        Config.Section config = Config.event("nowhere_portal");
        ServerLevel level = player.level();
        BlockPos base = Place.standingAhead(player, config.num("distance"), 0.0, config.num("max_angle"));
        if (base == null) {
            return null;
        }
        Vec3 toPlayer = player.position().subtract(Vec3.atCenterOf(base));
        Direction.Axis axis = Math.abs(toPlayer.x) > Math.abs(toPlayer.z) ? Direction.Axis.Z : Direction.Axis.X;
        Direction along = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        BlockState lit = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, axis);
        Map<BlockPos, BlockState> shown = new LinkedHashMap<>();
        for (int i = -1; i <= 2; i++) {
            for (int j = 0; j <= 4; j++) {
                BlockPos cell = base.relative(along, i).above(j);
                if (!level.isEmptyBlock(cell)
                        || (j == 0 && !level.getBlockState(cell.below()).isSolidRender())) {
                    return null;
                }
                boolean sideI = i == -1 || i == 2;
                boolean sideJ = j == 0 || j == 4;
                if (!(sideI && sideJ)) {
                    shown.put(cell, sideI || sideJ ? obsidian : lit);
                }
            }
        }
        return shown;
    }

    private static void lightPortal(ServerPlayer player) {
        Map<BlockPos, BlockState> shown = portal(player);
        if (shown == null) {
            return;
        }
        Config.Section config = Config.event("nowhere_portal");
        BlockPos middle = shown.keySet().stream().findFirst().orElseThrow();
        Swaps.show(player, shown, config.integer("ticks"));
        Nearby.sound(player, SoundEvents.PORTAL_AMBIENT, Vec3.atCenterOf(middle), config.tone("sound"));
    }

    // 466

    /** The blocks the player carries, which came from wherever they live. */
    private static List<BlockState> packBlocks(ServerPlayer player) {
        List<BlockState> found = new ArrayList<>();
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof BlockItem item) {
                BlockState state = item.getBlock().defaultBlockState();
                if (!state.isAir() && !state.hasBlockEntity()) {
                    found.add(state);
                }
            }
        }
        return found;
    }

    private static @Nullable BlockPos homeSpot(ServerPlayer player) {
        Config.Section config = Config.event("enderman_home_block");
        return Place.standingAhead(player, config.num("distance"), 0.0, config.num("max_angle"));
    }

    private static void homeBlock(ServerPlayer player) {
        List<BlockState> blocks = packBlocks(player);
        BlockPos spot = homeSpot(player);
        if (blocks.isEmpty() || spot == null) {
            return;
        }
        BlockState carried = blocks.get(player.getRandom().nextInt(blocks.size()));
        Vec3 at = Vec3.atBottomCenterOf(spot);
        PacketEntity man = show(player, EntityTypes.ENDERMAN, at, model -> ((Enderman) model).setCarriedBlock(carried));
        linger(player, "enderman_home_block", List.of(man), at, SoundEvents.ENDERMAN_TELEPORT);
    }

    // shared

    /** Shown at {@code at}, facing the player; {@code dress} edits a model whose entity data is then sent. */
    private static PacketEntity show(
            ServerPlayer player, EntityType<?> type, Vec3 at, @Nullable Consumer<Entity> dress) {
        PacketEntity mob = PacketEntity.show(player, type, at, PacketEntity.yawToward(at, player.position()));
        if (dress != null) {
            Entity model = type.create(player.level(), EntitySpawnReason.EVENT);
            if (model != null) {
                dress.accept(model);
                List<SynchedEntityData.DataValue<?>> values =
                        model.getEntityData().getNonDefaultValues();
                if (values != null && !values.isEmpty()) {
                    player.connection.send(new ClientboundSetEntityDataPacket(mob.id(), values));
                }
            }
        }
        return mob;
    }

    /**
     * Keeps the shown mobs until {@code max_ticks} are up, the player comes within {@code vanish_distance}, or (with
     * {@code look_angle}) has had it in view for {@code look_ticks} checks, when {@code vanish} is played. Checked
     * every {@code interval_ticks}.
     */
    private static void linger(
            ServerPlayer player, String id, List<PacketEntity> mobs, Vec3 at, @Nullable SoundEvent vanish) {
        Config.Section config = Config.event(id);
        ServerLevel level = player.level();
        int interval = config.integer("interval_ticks");
        double near = config.num("vanish_distance");
        double look = config.has("look_angle") ? config.num("look_angle") : 0.0;
        int[] seen = {0};
        Scheduler.repeat(player, interval, interval, (p, n) -> {
            boolean looked =
                    look > 0.0 && looking(p, at.add(0, 1.0, 0), look) && ++seen[0] >= config.integer("look_ticks");
            boolean over = looked
                    || p.level() != level
                    || (n + 1) * interval >= config.integer("max_ticks")
                    || p.position().distanceToSqr(at) <= near * near;
            if (over) {
                mobs.forEach(mob -> mob.remove(p));
                if (looked && vanish != null) {
                    Config.Tone tone = config.tone("sound");
                    p.connection.send(new ClientboundSoundPacket(
                            BuiltInRegistries.SOUND_EVENT.wrapAsHolder(vanish),
                            SoundSource.HOSTILE,
                            at.x,
                            at.y,
                            at.z,
                            tone.volume(),
                            tone.pitch(),
                            p.getRandom().nextLong()));
                }
            }
            return !over;
        });
    }

    private static boolean looking(ServerPlayer player, Vec3 at, double degrees) {
        Vec3 to = at.subtract(player.getEyePosition()).normalize();
        return player.getLookAngle().dot(to) >= Math.cos(Math.toRadians(degrees));
    }
}
