package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.HauntSounds;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.state.HauntData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Events that only make sense in the Nether (all gated on {@link Where#nether}): piglins, ghasts, striders, magma
 * cubes, soul fire and the blocks that belong there. Mobs are told what to do or are only shown to this player's
 * client; blocks go through {@link Swaps} or {@link Restore} so the world is put back.
 */
final class Nether {
    private Nether() {}

    static List<HauntEvent> all() {
        return List.of(
                // 442: every piglin nearby stops and looks at the player
                SimpleEvent.light(
                        "piglins_stare",
                        EventTag.MOBS,
                        player -> Where.nether(player)
                                && !piglins(player, "piglins_stare").isEmpty(),
                        Nether::stare),
                // 443: a piglin looks at gold nobody gave it and throws a player head or a bone
                SimpleEvent.light(
                        "piglin_barter",
                        EventTag.MOBS,
                        player -> Where.nether(player) && !barterers(player).isEmpty(),
                        Nether::barter),
                // 444: a ghast cries far off, then a fireball bursts out of sight
                SimpleEvent.light("ghast_cry", EventTag.SOUND, Where::nether, Nether::ghastCry),
                // 445: a stranger stands on the lava, facing the player
                SimpleEvent.light(
                        "lava_figure",
                        EventTag.PLAYER,
                        player -> Where.nether(player) && !lava(player).isEmpty(),
                        Nether::lavaFigure),
                // 446: a strider shivers in the lava as if it were cold
                SimpleEvent.light(
                        "strider_shiver",
                        EventTag.MOBS,
                        player -> Where.nether(player) && !striders(player).isEmpty(),
                        Nether::shiver),
                // 447: soul fire flames stream toward the player instead of up
                SimpleEvent.light(
                        "soul_fire_lean",
                        EventTag.PRESENCE,
                        player -> Where.nether(player) && !soulFires(player).isEmpty(),
                        Nether::lean),
                // 449: wither skeleton skulls on fences turn to face the player
                SimpleEvent.light(
                        "skulls_turn",
                        EventTag.BLOCKS,
                        player -> !skulls(player).isEmpty(),
                        Nether::turnSkulls),
                // 450: a blaze stands motionless in a corridor, and is gone when approached
                SimpleEvent.light(
                        "still_blaze",
                        EventTag.MOBS,
                        player -> Where.nether(player) && blazeSpot(player) != null,
                        Nether::stillBlaze),
                // 451: every magma cube nearby hops at once
                SimpleEvent.light(
                        "magma_hop",
                        EventTag.MOBS,
                        player -> Where.nether(player) && !cubes(player).isEmpty(),
                        Nether::hop),
                // 452: a respawn anchor takes one more charge by itself
                SimpleEvent.light(
                        "anchor_charge",
                        EventTag.BLOCKS,
                        player -> Where.nether(player) && !anchors(player).isEmpty(),
                        Nether::charge),
                // 453: a weeping vine grows longer toward the player
                SimpleEvent.light(
                        "vines_reach",
                        EventTag.BLOCKS,
                        player -> Where.nether(player) && !tips(player).isEmpty(),
                        Nether::reach),
                // 454: a compass keeps still in the Nether and points at the spirit's anchor
                SimpleEvent.light(
                        "steady_compass",
                        EventTag.ITEMS,
                        player -> Where.nether(player)
                                && anchor(player) != null
                                && !compasses(player).isEmpty(),
                        Nether::steady),
                // 464: on soul sand the legs drag, and something whispers
                SimpleEvent.harmful(
                        "soul_sand_drag",
                        EventTag.AFFLICTION,
                        player -> Where.nether(player)
                                && player.onGround()
                                && !player.hasEffect(MobEffects.SLOWNESS)
                                && player.level()
                                        .getBlockState(player.getBlockPosBelowThatAffectsMyMovement())
                                        .is(BlockTags.SOUL_SPEED_BLOCKS),
                        Nether::drag),
                // 465: piglins treat the player in gold as if they wore none
                SimpleEvent.harmful(
                        "piglins_angered",
                        EventTag.MOBS,
                        player -> Where.nether(player)
                                && PiglinAi.isWearingSafeArmor(player)
                                && !player.isCreative()
                                && !piglinsNear(player).isEmpty(),
                        player -> PiglinAi.angerNearbyPiglins(player.level(), player, false)));
    }

    // 442

    private static List<AbstractPiglin> piglins(ServerPlayer player, String event) {
        return player.level()
                .getEntitiesOfClass(
                        AbstractPiglin.class,
                        new AABB(player.blockPosition()).inflate(Nearby.radius(event)),
                        AbstractPiglin::isAlive);
    }

    /** They lose their AI for the stare, look at the player each pulse, and get the AI back after. */
    private static void stare(ServerPlayer player) {
        Config.Section config = Config.event("piglins_stare");
        ServerLevel level = player.level();
        List<AbstractPiglin> still = piglins(player, "piglins_stare").stream()
                .filter(piglin -> !piglin.isNoAi())
                .toList();
        for (AbstractPiglin piglin : still) {
            piglin.getNavigation().stop();
            piglin.setNoAi(true);
        }
        int interval = config.integer("interval_ticks");
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            for (AbstractPiglin piglin : still) {
                if (piglin.isAlive()) {
                    piglin.lookAt(EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
                    piglin.setYHeadRot(piglin.getYRot());
                }
            }
            return (n + 1) * interval < config.integer("ticks");
        });
        Restore.after(level, config.integer("ticks"), force -> {
            still.forEach(piglin -> piglin.setNoAi(false));
            return true;
        });
    }

    // 443

    private static List<Piglin> barterers(ServerPlayer player) {
        return player.level()
                .getEntitiesOfClass(
                        Piglin.class,
                        new AABB(player.blockPosition()).inflate(Nearby.radius("piglin_barter")),
                        piglin -> piglin.isAlive() && !piglin.isBaby());
    }

    private static void barter(ServerPlayer player) {
        List<Piglin> found = barterers(player);
        if (found.isEmpty()) {
            return;
        }
        Config.Section config = Config.event("piglin_barter");
        ServerLevel level = player.level();
        Piglin piglin = found.get(player.getRandom().nextInt(found.size()));
        Config.Tone tone = config.tone("sound");
        level.playSound(
                null, piglin, SoundEvents.PIGLIN_ADMIRING_ITEM, SoundSource.NEUTRAL, tone.volume(), tone.pitch());
        Scheduler.later(player, config.integer("delay_ticks"), p -> {
            if (!piglin.isAlive() || p.level() != level) {
                return;
            }
            List<String> items = config.texts("items");
            ItemStack thrown = new ItemStack(BuiltInRegistries.ITEM
                    .getOptional(Identifier.parse(items.get(p.getRandom().nextInt(items.size()))))
                    .orElse(Items.BONE));
            if (thrown.is(Items.PLAYER_HEAD)) {
                thrown.set(DataComponents.PROFILE, ResolvableProfile.createResolved(p.getGameProfile()));
            }
            Vec3 from = new Vec3(piglin.getX(), piglin.getEyeY() - 0.3, piglin.getZ());
            Vec3 toward = p.getEyePosition().subtract(from).normalize();
            ItemEntity item = new ItemEntity(level, from.x, from.y, from.z, thrown);
            item.setDeltaMovement(toward.scale(config.num("speed")).add(0.0, config.num("lift"), 0.0));
            level.addFreshEntity(item);
            level.playSound(
                    null, piglin, SoundEvents.PIGLIN_ADMIRING_ITEM, SoundSource.NEUTRAL, tone.volume(), tone.pitch());
            // What it threw is not meant to stay.
            Restore.after(level, config.integer("vanish_ticks"), force -> {
                item.discard();
                return true;
            });
        });
    }

    // 444

    private static void ghastCry(ServerPlayer player) {
        Config.Section config = Config.event("ghast_cry");
        Vec3 at = Nearby.source(player, config.num("distance"));
        Nearby.sound(player, SoundEvents.GHAST_SCREAM, at, config.tone("cry"));
        Scheduler.later(player, config.integer("shoot_delay_ticks"), p -> {
            Nearby.sound(p, SoundEvents.GHAST_SHOOT, at, config.tone("shoot"));
            Scheduler.later(
                    p,
                    config.integer("blast_delay_ticks"),
                    q -> Nearby.sound(q, SoundEvents.GENERIC_EXPLODE.value(), at, config.tone("blast")));
        });
    }

    // 445

    /** Lava source with two free blocks above it, not too near. */
    private static List<BlockPos> lava(ServerPlayer player) {
        ServerLevel level = player.level();
        double min = Config.event("lava_figure").num("min_distance");
        return Nearby.all(
                        player,
                        "lava_figure",
                        state -> state.getFluidState().isSource()
                                && state.getFluidState().is(FluidTags.LAVA))
                .stream()
                .filter(pos -> level.isEmptyBlock(pos.above())
                        && level.isEmptyBlock(pos.above(2))
                        && player.position().distanceToSqr(Vec3.atCenterOf(pos)) >= min * min)
                .toList();
    }

    private static void lavaFigure(ServerPlayer player) {
        List<BlockPos> found = lava(player);
        if (found.isEmpty()) {
            return;
        }
        Config.Section config = Config.event("lava_figure");
        ServerLevel level = player.level();
        BlockPos lava = found.get(player.getRandom().nextInt(found.size()));
        Mannequin figure = Figure.spawn(player, lava.above(), Figure.stranger(player));
        if (figure == null) {
            return;
        }
        // The lava's surface is a little under the top of its block.
        figure.setPos(figure.getX(), lava.getY() + config.num("surface"), figure.getZ());
        double near = config.num("vanish_distance");
        int interval = config.integer("interval_ticks");
        Scheduler.repeat(player, interval, interval, (p, n) -> {
            boolean over = !figure.isAlive()
                    || p.level() != level
                    || (n + 1) * interval >= config.integer("max_ticks")
                    || p.position().distanceToSqr(figure.position()) <= near * near;
            if (over) {
                figure.discard();
            } else {
                Figure.face(figure, p);
            }
            return !over;
        });
        // If the player leaves, the scheduled steps are dropped with them.
        Restore.after(level, config.integer("max_ticks"), force -> {
            figure.discard();
            return true;
        });
    }

    // 446

    private static List<Strider> striders(ServerPlayer player) {
        return player.level()
                .getEntitiesOfClass(
                        Strider.class,
                        new AABB(player.blockPosition()).inflate(Nearby.radius("strider_shiver")),
                        strider -> strider.isAlive() && !strider.isSuffocating());
    }

    /** The cold flag is shown on, and the real one sent again after the shiver; nothing changes on the server. */
    private static void shiver(ServerPlayer player) {
        Config.Section config = Config.event("strider_shiver");
        ServerLevel level = player.level();
        Strider model = EntityTypes.STRIDER.create(level, EntitySpawnReason.EVENT);
        if (model == null) {
            return;
        }
        model.setSuffocating(true);
        List<SynchedEntityData.DataValue<?>> cold = model.getEntityData().getNonDefaultValues();
        if (cold == null) {
            return;
        }
        SynchedEntityData.DataValue<?> flag = cold.stream()
                .filter(value -> Boolean.TRUE.equals(value.value()))
                .findFirst()
                .orElse(null);
        if (flag == null) {
            return;
        }
        List<Strider> found = striders(player);
        for (Strider strider : found) {
            player.connection.send(new ClientboundSetEntityDataPacket(strider.getId(), List.of(flag)));
        }
        Scheduler.later(player, config.integer("ticks"), p -> {
            if (p.level() == level) {
                for (Strider strider : found) {
                    // Whatever the strider really feels by now (it may have left the lava).
                    p.connection.send(new ClientboundSetEntityDataPacket(
                            strider.getId(),
                            List.of(new SynchedEntityData.DataValue<>(
                                    flag.id(), EntityDataSerializers.BOOLEAN, strider.isSuffocating()))));
                }
            }
        });
    }

    // 447

    private static List<BlockPos> soulFires(ServerPlayer player) {
        return Nearby.all(player, "soul_fire_lean", state -> state.is(Blocks.SOUL_FIRE));
    }

    private static void lean(ServerPlayer player) {
        List<BlockPos> fires = new ArrayList<>(soulFires(player));
        if (fires.isEmpty()) {
            return;
        }
        Config.Section config = Config.event("soul_fire_lean");
        Collections.shuffle(fires, new Random(player.getRandom().nextLong()));
        List<BlockPos> chosen = fires.subList(0, Math.min(fires.size(), config.integer("max_fires")));
        ServerLevel level = player.level();
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            for (BlockPos fire : chosen) {
                Vec3 toward = p.position().subtract(Vec3.atCenterOf(fire)).multiply(1, 0, 1);
                if (p.level() != level || toward.lengthSqr() < 1.0E-4) {
                    continue;
                }
                Vec3 along = toward.normalize();
                for (int i = 0; i < config.integer("per_pulse"); i++) {
                    // With a count of 0 the offsets are the direction of travel, scaled by the speed.
                    level.sendParticles(
                            ParticleTypes.SOUL_FIRE_FLAME,
                            fire.getX() + 0.5,
                            fire.getY() + 0.3,
                            fire.getZ() + 0.5,
                            0,
                            along.x,
                            config.num("lift"),
                            along.z,
                            config.num("speed"));
                }
            }
            return n + 1 < config.integer("pulses");
        });
    }

    // 449

    /** Floor and wall skulls, on any support and in any dimension. */
    private static List<BlockPos> skulls(ServerPlayer player) {
        return Nearby.all(
                player,
                "skulls_turn",
                state -> state.getBlock() instanceof SkullBlock || state.getBlock() instanceof WallSkullBlock);
    }

    private static void turnSkulls(ServerPlayer player) {
        Map<BlockPos, BlockState> shown = new LinkedHashMap<>();
        for (BlockPos pos : skulls(player)) {
            // A skull faces the one who placed it, who looked from there toward it.
            double yaw =
                    Mth.atan2(-(pos.getX() + 0.5 - player.getX()), pos.getZ() + 0.5 - player.getZ()) * Mth.RAD_TO_DEG;
            int segment = Mth.floor(yaw * 16.0 / 360.0 + 0.5) & SkullBlock.MAX;
            BlockState state = player.level().getBlockState(pos);
            if (state.hasProperty(SkullBlock.ROTATION)) {
                shown.put(pos, state.setValue(SkullBlock.ROTATION, segment));
            } else {
                Vec3 toward = player.position().subtract(Vec3.atCenterOf(pos));
                shown.put(
                        pos,
                        state.setValue(WallSkullBlock.FACING, Direction.getApproximateNearest(toward.x, 0, toward.z)));
            }
        }
        Swaps.show(player, shown, Config.event("skulls_turn").integer("ticks"));
    }

    // 450

    /** Standing room ahead, including open caverns. */
    private static @Nullable BlockPos blazeSpot(ServerPlayer player) {
        Config.Section config = Config.event("still_blaze");
        return Place.standingAhead(player, config.num("distance"), config.num("min_angle"), config.num("max_angle"));
    }

    private static void stillBlaze(ServerPlayer player) {
        BlockPos spot = blazeSpot(player);
        if (spot == null) {
            return;
        }
        Config.Section config = Config.event("still_blaze");
        ServerLevel level = player.level();
        Vec3 at = Vec3.atBottomCenterOf(spot);
        PacketEntity blaze =
                PacketEntity.show(player, EntityTypes.BLAZE, at, PacketEntity.yawToward(at, player.position()));
        double near = config.num("vanish_distance");
        int interval = config.integer("interval_ticks");
        Scheduler.repeat(player, interval, interval, (p, n) -> {
            boolean over = p.level() != level
                    || (n + 1) * interval >= config.integer("max_ticks")
                    || p.position().distanceToSqr(at) <= near * near;
            if (over) {
                blaze.remove(p);
            }
            return !over;
        });
    }

    // 451

    private static List<MagmaCube> cubes(ServerPlayer player) {
        return player.level()
                .getEntitiesOfClass(
                        MagmaCube.class,
                        new AABB(player.blockPosition()).inflate(Nearby.radius("magma_hop")),
                        cube -> cube.isAlive() && cube.onGround());
    }

    private static void hop(ServerPlayer player) {
        Config.Tone tone = Config.event("magma_hop").tone("sound");
        for (MagmaCube cube : cubes(player)) {
            cube.jumpFromGround();
            player.level()
                    .playSound(
                            null, cube, SoundEvents.MAGMA_CUBE_JUMP, SoundSource.HOSTILE, tone.volume(), tone.pitch());
        }
    }

    // 452

    private static List<BlockPos> anchors(ServerPlayer player) {
        ServerLevel level = player.level();
        return Nearby.all(
                        player,
                        "anchor_charge",
                        state -> state.is(Blocks.RESPAWN_ANCHOR)
                                && state.getValue(RespawnAnchorBlock.CHARGE) < RespawnAnchorBlock.MAX_CHARGES)
                .stream()
                .filter(pos -> level.getBlockEntity(pos) == null)
                .toList();
    }

    private static void charge(ServerPlayer player) {
        List<BlockPos> found = anchors(player);
        if (found.isEmpty()) {
            return;
        }
        Config.Section config = Config.event("anchor_charge");
        ServerLevel level = player.level();
        BlockPos pos = found.get(player.getRandom().nextInt(found.size()));
        BlockState state = level.getBlockState(pos);
        Swaps.show(
                player,
                pos,
                state.setValue(RespawnAnchorBlock.CHARGE, state.getValue(RespawnAnchorBlock.CHARGE) + 1),
                config.integer("ticks"));
        Config.Tone tone = config.tone("sound");
        level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, tone.volume(), tone.pitch());
    }

    // 453

    /** The ends of weeping vines with air below. */
    private static List<BlockPos> tips(ServerPlayer player) {
        ServerLevel level = player.level();
        return Nearby.all(player, "vines_reach", state -> state.is(Blocks.WEEPING_VINES)).stream()
                .filter(pos -> level.isEmptyBlock(pos.below()))
                .toList();
    }

    /**
     * The vine nearest the player grows down into the air below it; the new blocks end in a full-grown head so it
     * cannot grow on by itself, and all of it is put back (it stays under hardcore).
     */
    private static void reach(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos tip = tips(player).stream()
                .min(Comparator.comparingDouble(pos -> player.position().distanceToSqr(Vec3.atCenterOf(pos))))
                .orElse(null);
        if (tip == null) {
            return;
        }
        Config.Section config = Config.event("vines_reach");
        int ticks = config.integer("ticks");
        int grown = 0;
        while (grown < config.integer("length") && level.isEmptyBlock(tip.below(grown + 1))) {
            grown++;
        }
        BlockState plant = Blocks.WEEPING_VINES_PLANT.defaultBlockState();
        BlockState head = Blocks.WEEPING_VINES
                .defaultBlockState()
                .setValue(GrowingPlantHeadBlock.AGE, GrowingPlantHeadBlock.MAX_AGE);
        Restore.swap(level, tip, plant, ticks);
        for (int i = 1; i <= grown; i++) {
            Restore.swap(level, tip.below(i), i == grown ? head : plant, ticks);
        }
    }

    // 454

    /** The anchor of the nearest territorial spirit whose reach holds the player. */
    private static @Nullable BlockPos anchor(ServerPlayer player) {
        BlockPos nearest = null;
        double best = Double.MAX_VALUE;
        for (HauntData.Territorial spirit :
                HauntData.get(player.level().getServer()).territorialReaching(player)) {
            double distance = spirit.anchor().distSqr(player.blockPosition());
            if (distance < best) {
                nearest = spirit.anchor();
                best = distance;
            }
        }
        return nearest;
    }

    /** Inventory indexes of compasses that point nowhere in particular. */
    private static List<Integer> compasses(ServerPlayer player) {
        List<Integer> found = new ArrayList<>();
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.COMPASS) && !stack.has(DataComponents.LODESTONE_TRACKER)) {
                found.add(i);
            }
        }
        return found;
    }

    /** Only this client is shown the compasses locked on the anchor; the real ones are sent again after. */
    private static void steady(ServerPlayer player) {
        BlockPos anchor = anchor(player);
        if (anchor == null) {
            return;
        }
        LodestoneTracker tracker =
                new LodestoneTracker(Optional.of(GlobalPos.of(player.level().dimension(), anchor)), false);
        List<Integer> slots = new ArrayList<>();
        for (int index : compasses(player)) {
            int slot = index < Inventory.getSelectionSize() ? InventoryMenu.USE_ROW_SLOT_START + index : index;
            ItemStack shown = player.getInventory().getItem(index).copy();
            shown.set(DataComponents.LODESTONE_TRACKER, tracker);
            show(player, slot, shown);
            slots.add(slot);
        }
        ServerLevel level = player.level();
        Scheduler.later(player, Config.event("steady_compass").integer("ticks"), p -> {
            if (p.level() == level) {
                slots.forEach(slot ->
                        show(p, slot, p.inventoryMenu.getSlot(slot).getItem().copy()));
            }
        });
    }

    private static void show(ServerPlayer player, int menuSlot, ItemStack shown) {
        InventoryMenu menu = player.inventoryMenu;
        player.connection.send(
                new ClientboundContainerSetSlotPacket(menu.containerId, menu.getStateId(), menuSlot, shown));
    }

    // 464

    private static void drag(ServerPlayer player) {
        Config.Section config = Config.event("soul_sand_drag");
        player.addEffect(
                new MobEffectInstance(MobEffects.SLOWNESS, config.integer("ticks"), config.integer("amplifier")), null);
        Nearby.sound(
                player, HauntSounds.WHISPER, Nearby.randomAround(player, config.num("distance")), config.tone("sound"));
    }

    // 465

    private static List<Piglin> piglinsNear(ServerPlayer player) {
        return player.level()
                .getEntitiesOfClass(
                        Piglin.class,
                        new AABB(player.blockPosition()).inflate(Nearby.radius("piglins_angered")),
                        Piglin::isAlive);
    }
}
