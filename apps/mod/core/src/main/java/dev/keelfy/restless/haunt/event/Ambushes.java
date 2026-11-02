package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Band;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariants;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SpeleothemThickness;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Threat-band assaults ({@code assaults.safe_health}); see docs/assaults.md. Each warns first and has a counter. Every
 * mob, projectile and falling block a spirit makes here is marked {@link Band#THREAT}, so what it deals is capped at
 * the safe floor; damage dealt by hand is capped with {@link Assaults#room}.
 */
final class Ambushes {
    private static final String HURLED = "restless:hurled_block";
    private static final String FLUNG = "restless:flung_tools";
    private static final String VOLLEY = "restless:volley";
    private static final String CAVE_IN = "restless:cave_in";
    private static final String DRIPSTONE = "restless:dripstone_rain";
    private static final String POSSESSED = "restless:possessed_mob";
    private static final String FRENZY = "restless:frenzy";
    private static final String PETS = "restless:turned_pets";
    private static final String VEXES = "restless:vex_swarm";
    private static final String CREEPER = "restless:ghost_creeper";
    private static final String STALKER = "restless:stalker";
    private static final String BONES = "restless:hail_of_bones";

    private Ambushes() {}

    static List<HauntEvent> all() {
        return List.of(
                SimpleEvent.harmful(
                        "restless:hurled_block",
                        EventTag.ASSAULT,
                        player -> ready(player, HURLED) && looseBlock(player) != null,
                        Ambushes::hurl),
                SimpleEvent.harmful(
                        "restless:flung_tools",
                        EventTag.ASSAULT,
                        player -> ready(player, FLUNG) && toolChest(player) != null,
                        Ambushes::fling),
                SimpleEvent.harmful(
                        "restless:volley",
                        EventTag.ASSAULT,
                        player -> ready(player, VOLLEY)
                                && player.level().getMaxLocalRawBrightness(player.blockPosition())
                                        < Config.event(VOLLEY).integer("dark_below"),
                        Ambushes::volley),
                SimpleEvent.harmful(
                        "restless:cave_in",
                        EventTag.ASSAULT,
                        player -> ready(player, CAVE_IN) && roof(player, Config.event(CAVE_IN)) != null,
                        Ambushes::caveIn),
                SimpleEvent.harmful(
                        "restless:dripstone_rain",
                        EventTag.ASSAULT,
                        player -> ready(player, DRIPSTONE) && roof(player, Config.event(DRIPSTONE)) != null,
                        Ambushes::dripstoneRain),
                SimpleEvent.harmful(
                        "restless:possessed_mob",
                        EventTag.ASSAULT,
                        player -> ready(player, POSSESSED)
                                && !hostiles(player, Config.event(POSSESSED)).isEmpty(),
                        player -> {
                            Config.Section config = Config.event(POSSESSED);
                            List<Monster> mobs = hostiles(player, config);
                            rage(
                                    player,
                                    List.of(mobs.get(player.getRandom().nextInt(mobs.size()))),
                                    config,
                                    ParticleTypes.SOUL_FIRE_FLAME);
                        }),
                SimpleEvent.harmful(
                        "restless:frenzy",
                        EventTag.ASSAULT,
                        player -> ready(player, FRENZY)
                                && hostiles(player, Config.event(FRENZY)).size()
                                        >= Config.event(FRENZY).integer("min_mobs"),
                        player -> {
                            Config.Section config = Config.event(FRENZY);
                            List<Monster> mobs = hostiles(player, config);
                            rage(
                                    player,
                                    mobs.subList(0, Math.min(mobs.size(), config.integer("max_mobs"))),
                                    config,
                                    ParticleTypes.ANGRY_VILLAGER);
                        }),
                SimpleEvent.harmful(
                        "restless:turned_pets",
                        EventTag.ASSAULT,
                        player -> ready(player, PETS) && !pets(player).isEmpty(),
                        Ambushes::turnPets),
                SimpleEvent.harmful(
                        "restless:vex_swarm", EventTag.ASSAULT, player -> ready(player, VEXES), Ambushes::vexSwarm),
                SimpleEvent.harmful(
                        "restless:ghost_creeper",
                        EventTag.ASSAULT,
                        player -> ready(player, CREEPER),
                        Ambushes::ghostCreeper),
                SimpleEvent.harmful(
                        "restless:stalker",
                        EventTag.ASSAULT,
                        player -> ready(player, STALKER) && stalkerSpot(player) != null,
                        Ambushes::stalk),
                SimpleEvent.harmful(
                        "restless:hail_of_bones",
                        EventTag.ASSAULT,
                        player -> ready(player, BONES)
                                && player.level().dimension() == Level.OVERWORLD
                                && player.level().isDarkOutside()
                                && player.level().canSeeSky(player.blockPosition()),
                        Ambushes::hailOfBones));
    }

    // ---- shared ----

    /** Enough health to start, and never on peaceful. */
    private static boolean ready(ServerPlayer player, String id) {
        return player.level().getDifficulty() != Difficulty.PEACEFUL && Assaults.healthy(player, id);
    }

    /** Hurts by hand, never below the threat floor. */
    private static void strike(ServerPlayer player, float damage) {
        float allowed = Math.min(damage, Assaults.room(player, Band.THREAT));
        if (allowed > 0.0f) {
            player.hurtServer(player.level(), Taken.by(player.level(), Taken.TAKEN), allowed);
        }
    }

    private static <T extends Entity> T marked(T entity) {
        Band.THREAT.mark(entity);
        return entity;
    }

    private static void puff(ServerLevel level, ParticleOptions what, Vec3 at, Config.Burst burst) {
        level.sendParticles(
                what,
                at.x,
                at.y + burst.offsetY(),
                at.z,
                burst.count(),
                burst.dx(),
                burst.dy(),
                burst.dz(),
                burst.speed());
    }

    /** A falling block that hurts what it lands on (capped by its mark) and vanishes when it lands. */
    private static void drop(ServerLevel level, BlockPos at, BlockState state, Config.Section config) {
        FallingBlockEntity block = FallingBlockEntity.fall(level, at, state);
        block.setHurtsEntities((float) config.num("per_block"), config.integer("max_damage"));
        block.disableDrop();
        Band.THREAT.mark(block);
    }

    /** The free cell just under a solid ceiling in the column of {@code from}, at most {@code max} blocks up. */
    private static @Nullable BlockPos under(ServerLevel level, BlockPos from, int max) {
        BlockPos cell = from;
        if (!Place.open(level, cell)) {
            return null;
        }
        for (int i = 0; i < max; i++) {
            BlockPos next = cell.above();
            if (!level.isLoaded(next)) {
                return null;
            }
            if (!Place.open(level, next)) {
                return level.getBlockState(next).isSolidRender() ? cell : null;
            }
            cell = next;
        }
        return null;
    }

    /** The free cell under the ceiling over the player, high enough for a fall to count. */
    private static @Nullable BlockPos roof(ServerPlayer player, Config.Section config) {
        BlockPos cell = under(player.level(), player.blockPosition().above(2), config.integer("ceiling_max"));
        return cell != null && cell.getY() - player.getBlockY() >= config.integer("min_drop") ? cell : null;
    }

    /** The cell under the ceiling over a column beside the player; null if the column has none. */
    private static @Nullable BlockPos roofAt(ServerPlayer player, int x, int z, Config.Section config) {
        BlockPos cell =
                under(player.level(), new BlockPos(x, player.getBlockY() + 2, z), config.integer("ceiling_max"));
        return cell != null && cell.getY() - player.getBlockY() >= config.integer("min_drop") ? cell : null;
    }

    // ---- hurled_block ----

    private static @Nullable BlockPos looseBlock(ServerPlayer player) {
        Config.Section config = Config.event(HURLED);
        List<String> ids = config.texts("blocks");
        double min = config.num("min_distance");
        List<BlockPos> found = Nearby.all(
                        player,
                        HURLED,
                        state -> ids.contains(
                                BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()))
                .stream()
                .filter(pos ->
                        pos.distToCenterSqr(player.position()) >= min * min && Place.open(player.level(), pos.above()))
                .toList();
        return found.isEmpty() ? null : found.get(player.getRandom().nextInt(found.size()));
    }

    /** A loose block shivers and lifts in a cloud of dust, then flies at where the player was; it lands after. */
    private static void hurl(ServerPlayer player) {
        BlockPos pos = looseBlock(player);
        if (pos == null) {
            return;
        }
        Config.Section config = Config.event(HURLED);
        ServerLevel level = player.level();
        BlockState state = level.getBlockState(pos);
        FallingBlockEntity block = FallingBlockEntity.fall(level, pos, state);
        block.setNoGravity(true);
        Band.THREAT.mark(block);
        int warn = config.integer("warn_ticks");
        int flight = config.integer("flight_ticks");
        Restore.after(level, warn + flight + config.integer("cleanup_ticks"), force -> {
            block.setNoGravity(false);
            return true;
        });
        Vec3[] heading = new Vec3[1];
        Scheduler.repeat(player, 0, 1, (p, n) -> {
            if (!block.isAlive()) {
                return false;
            }
            if (n < warn) {
                block.setDeltaMovement(0.0, config.num("rise"), 0.0);
                if (n % config.integer("dust_every") == 0) {
                    puff(
                            level,
                            new BlockParticleOption(ParticleTypes.BLOCK, state),
                            block.position(),
                            config.burst("particles"));
                    Nearby.sound(p, state.getSoundType().getHitSound(), block.position(), config.tone("sound"));
                }
                return true;
            }
            if (n == warn) {
                heading[0] = p.getBoundingBox()
                        .getCenter()
                        .subtract(block.position())
                        .normalize()
                        .scale(config.num("speed"));
            }
            block.setDeltaMovement(heading[0]);
            boolean hit = block.getBoundingBox().intersects(p.getBoundingBox().inflate(config.num("hit_margin")));
            if (hit) {
                Nearby.sound(p, state.getSoundType().getBreakSound(), block.position(), config.tone("hit_sound"));
                strike(p, (float) config.num("damage"));
            }
            if (hit || block.horizontalCollision || n - warn >= flight) {
                block.setNoGravity(false);
                block.setDeltaMovement(Vec3.ZERO);
                return false;
            }
            return true;
        });
    }

    // ---- flung_tools ----

    private static boolean isTool(ItemStack stack) {
        return stack.is(ItemTags.PICKAXES)
                || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.SHOVELS)
                || stack.is(ItemTags.HOES)
                || stack.is(ItemTags.SWORDS);
    }

    private static int toolSlot(ChestBlockEntity chest) {
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            if (isTool(chest.getItem(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private static @Nullable BlockPos toolChest(ServerPlayer player) {
        return Nearby.all(player, FLUNG, state -> state.getBlock() instanceof ChestBlock).stream()
                .filter(pos ->
                        player.level().getBlockEntity(pos) instanceof ChestBlockEntity chest && toolSlot(chest) >= 0)
                .findFirst()
                .orElse(null);
    }

    /**
     * The chest lid rattles and opens, then its tools fly out one by one at the player. Closing the chest for good
     * (taking the tools, breaking it) stops it; the tools that flew can be picked up again.
     */
    private static void fling(ServerPlayer player) {
        BlockPos chestPos = toolChest(player);
        if (chestPos == null) {
            return;
        }
        Config.Section config = Config.event(FLUNG);
        ServerLevel level = player.level();
        Vec3 top = Vec3.atCenterOf(chestPos).add(0.0, config.num("launch_height"), 0.0);
        BlockState chestState = level.getBlockState(chestPos);
        level.blockEvent(chestPos, chestState.getBlock(), 1, 1);
        Nearby.sound(player, SoundEvents.CHEST_OPEN, top, config.tone("sound"));
        int warn = config.integer("warn_ticks");
        int gap = config.integer("throw_gap_ticks");
        int max = config.integer("max_tools");
        int flight = config.integer("flight_ticks");
        List<ItemEntity> flying = new ArrayList<>();
        int[] thrown = {0};
        Scheduler.repeat(player, 0, 1, (p, n) -> {
            if (n < warn) {
                if (n % config.integer("rattle_every") == 0) {
                    Nearby.sound(p, SoundEvents.CHEST_LOCKED, top, config.tone("rattle_sound"));
                }
                return true;
            }
            if (thrown[0] < max && (n - warn) % gap == 0) {
                if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest && toolSlot(chest) >= 0) {
                    flying.add(throwTool(p, chest, top, config));
                    thrown[0]++;
                } else {
                    thrown[0] = max;
                }
            }
            flying.removeIf(item -> {
                boolean hit =
                        item.getBoundingBox().intersects(p.getBoundingBox().inflate(config.num("hit_margin")));
                if (hit) {
                    strike(p, (float) config.num("damage"));
                }
                return hit || !item.isAlive() || item.tickCount >= flight;
            });
            if (thrown[0] >= max && flying.isEmpty()) {
                level.blockEvent(chestPos, chestState.getBlock(), 1, 0);
                Nearby.sound(p, SoundEvents.CHEST_CLOSE, top, config.tone("sound"));
                return false;
            }
            return true;
        });
    }

    private static ItemEntity throwTool(ServerPlayer player, ChestBlockEntity chest, Vec3 from, Config.Section config) {
        ItemStack tool = chest.removeItem(toolSlot(chest), 1);
        Vec3 way = player.getBoundingBox().getCenter().subtract(from);
        double seconds = way.length() / config.num("speed");
        Vec3 velocity = way.normalize().scale(config.num("speed")).add(0.0, config.num("arc") * seconds, 0.0);
        ItemEntity item =
                new ItemEntity(player.level(), from.x, from.y, from.z, tool, velocity.x, velocity.y, velocity.z);
        item.setPickUpDelay(config.integer("pickup_delay_ticks"));
        player.level().addFreshEntity(marked(item));
        Nearby.sound(player, SoundEvents.CHEST_LOCKED, from, config.tone("throw_sound"));
        return item;
    }

    // ---- volley ----

    /** A skeleton rattles in the dark, then arrows come from that one direction. */
    private static void volley(ServerPlayer player) {
        Config.Section config = Config.event(VOLLEY);
        Vec3 from = Nearby.randomAround(player, config.num("distance"));
        int warn = config.integer("warn_ticks");
        int gap = config.integer("arrow_gap_ticks");
        int arrows = config.integer("arrows_min") + player.getRandom().nextInt(config.integer("arrows_spread"));
        Scheduler.repeat(player, 0, 1, (p, n) -> {
            if (n < warn) {
                if (n % config.integer("rattle_every") == 0) {
                    Nearby.sound(p, SoundEvents.SKELETON_AMBIENT, from, config.tone("sound"));
                }
                return true;
            }
            if ((n - warn) % gap != 0) {
                return true;
            }
            double scatter = config.num("scatter");
            Vec3 start = from.add(
                    (p.getRandom().nextDouble() - 0.5) * scatter,
                    (p.getRandom().nextDouble() - 0.5) * scatter,
                    (p.getRandom().nextDouble() - 0.5) * scatter);
            Vec3 aim = p.getBoundingBox().getCenter().subtract(start);
            Arrow arrow = marked(new Arrow(p.level(), start.x, start.y, start.z, new ItemStack(Items.ARROW), null));
            arrow.shoot(
                    aim.x,
                    aim.y + Math.hypot(aim.x, aim.z) * config.num("arc"),
                    aim.z,
                    (float) config.num("power"),
                    (float) config.num("uncertainty"));
            p.level().addFreshEntity(arrow);
            Nearby.sound(p, SoundEvents.SKELETON_SHOOT, start, config.tone("shoot_sound"));
            return (n - warn) / gap + 1 < arrows;
        });
    }

    // ---- cave_in ----

    /** Dust and creaking overhead, then gravel comes down over where the player stood. */
    private static void caveIn(ServerPlayer player) {
        Config.Section config = Config.event(CAVE_IN);
        ServerLevel level = player.level();
        int radius = config.integer("radius");
        List<BlockPos> cells = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if ((dx == 0 && dz == 0) || player.getRandom().nextDouble() < config.num("density")) {
                    BlockPos cell = roofAt(player, player.getBlockX() + dx, player.getBlockZ() + dz, config);
                    if (cell != null) {
                        cells.add(cell);
                    }
                }
            }
        }
        if (cells.isEmpty()) {
            return;
        }
        Scheduler.repeat(player, 0, config.integer("dust_every"), (p, n) -> {
            if (n * config.integer("dust_every") < config.integer("warn_ticks")) {
                for (BlockPos cell : cells) {
                    puff(
                            level,
                            new BlockParticleOption(ParticleTypes.FALLING_DUST, level.getBlockState(cell.above())),
                            Vec3.atCenterOf(cell),
                            config.burst("particles"));
                }
                Nearby.sound(p, SoundEvents.CREAKING_SWAY, Vec3.atCenterOf(cells.get(0)), config.tone("sound"));
                return true;
            }
            for (BlockPos cell : cells) {
                if (Place.open(level, cell)) {
                    drop(level, cell, Blocks.GRAVEL.defaultBlockState(), config);
                }
            }
            Nearby.sound(p, SoundEvents.GRAVEL_BREAK, Vec3.atCenterOf(cells.get(0)), config.tone("fall_sound"));
            return false;
        });
    }

    // ---- dripstone_rain ----

    /** Dripping and cracking, then stalactites break off the ceiling around the player, wave after wave. */
    private static void dripstoneRain(ServerPlayer player) {
        Config.Section config = Config.event(DRIPSTONE);
        ServerLevel level = player.level();
        BlockState spike = Blocks.POINTED_DRIPSTONE
                .defaultBlockState()
                .setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.DOWN)
                .setValue(PointedDripstoneBlock.THICKNESS, SpeleothemThickness.TIP);
        int warn = config.integer("warn_ticks");
        int gap = config.integer("wave_gap_ticks");
        Scheduler.repeat(player, 0, 1, (p, n) -> {
            if (n < warn) {
                if (n % config.integer("crack_every") == 0) {
                    Nearby.sound(
                            p,
                            SoundEvents.DRIPSTONE_BLOCK_BREAK,
                            p.position().add(0.0, config.num("crack_height"), 0.0),
                            config.tone("sound"));
                }
                return true;
            }
            if ((n - warn) % gap != 0) {
                return true;
            }
            for (int i = 0; i < config.integer("per_wave"); i++) {
                double spread = config.num("spread");
                BlockPos cell = roofAt(
                        p,
                        Mth.floor(p.getX() + (p.getRandom().nextDouble() * 2.0 - 1.0) * spread),
                        Mth.floor(p.getZ() + (p.getRandom().nextDouble() * 2.0 - 1.0) * spread),
                        config);
                if (cell != null) {
                    puff(level, ParticleTypes.POOF, Vec3.atCenterOf(cell), config.burst("particles"));
                    Nearby.sound(
                            p, SoundEvents.POINTED_DRIPSTONE_LAND, Vec3.atCenterOf(cell), config.tone("break_sound"));
                    drop(level, cell, spike, config);
                }
            }
            return (n - warn) / gap + 1 < config.integer("waves");
        });
    }

    // ---- possessed_mob and frenzy ----

    /** Hostile mobs near the player that a spirit can turn: nothing as big as a boss. */
    private static List<Monster> hostiles(ServerPlayer player, Config.Section config) {
        return player
                .level()
                .getEntitiesOfClass(
                        Monster.class,
                        new AABB(player.blockPosition()).inflate(config.num("radius")),
                        mob -> mob.isAlive() && mob.getMaxHealth() <= config.num("max_health"))
                .stream()
                .sorted(Comparator.comparingDouble(mob -> mob.distanceToSqr(player)))
                .toList();
    }

    /** Spirit fire flickers around the mobs for a few seconds, then they turn: fast, strong and after the player. */
    private static void rage(ServerPlayer player, List<Monster> mobs, Config.Section config, ParticleOptions flicker) {
        ServerLevel level = player.level();
        Nearby.sound(player, SoundEvents.WARDEN_NEARBY_CLOSE, player.position(), config.tone("sound"));
        for (Monster mob : mobs) {
            Band.THREAT.mark(mob);
        }
        int pulse = config.integer("pulse_ticks");
        int warn = config.integer("warn_ticks");
        Scheduler.repeat(player, 0, pulse, (p, n) -> {
            int elapsed = n * pulse;
            for (Monster mob : mobs) {
                if (!mob.isAlive()) {
                    continue;
                }
                if (elapsed < warn) {
                    puff(level, flicker, mob.position(), config.burst("particles"));
                } else {
                    if (elapsed == warn) {
                        mob.addEffect(new MobEffectInstance(
                                MobEffects.SPEED, config.integer("effect_ticks"), config.integer("speed_amplifier")));
                        mob.addEffect(new MobEffectInstance(
                                MobEffects.STRENGTH,
                                config.integer("effect_ticks"),
                                config.integer("strength_amplifier")));
                    }
                    mob.setTarget(p);
                }
            }
            return elapsed < warn + config.integer("effect_ticks");
        });
    }

    // ---- turned_pets ----

    private static List<TamableAnimal> pets(ServerPlayer player) {
        return player
                .level()
                .getEntitiesOfClass(
                        TamableAnimal.class,
                        new AABB(player.blockPosition())
                                .inflate(Config.event(PETS).num("radius")),
                        pet -> pet.isTame() && pet.isOwnedBy(player) && (pet instanceof Wolf || pet instanceof Cat))
                .stream()
                .toList();
    }

    private static void growl(TamableAnimal pet, Config.Tone tone) {
        if (pet instanceof Cat cat) {
            cat.hiss();
        } else if (pet instanceof Wolf) {
            pet.level()
                    .playSound(
                            null,
                            pet.blockPosition(),
                            SoundEvents.WOLF_SOUNDS
                                    .get(WolfSoundVariants.SoundSet.CLASSIC)
                                    .adultSounds()
                                    .growlSound()
                                    .value(),
                            SoundSource.NEUTRAL,
                            tone.volume(),
                            tone.pitch());
        }
    }

    /**
     * The player's wolves and cats stand and growl, then run at them and bite for a while, and stop. A tame animal's
     * own goals never turn on its owner, so they are walked and made to bite from here.
     */
    private static void turnPets(ServerPlayer player) {
        Config.Section config = Config.event(PETS);
        List<TamableAnimal> pets = pets(player);
        for (TamableAnimal pet : pets) {
            Band.THREAT.mark(pet);
            pet.setOrderedToSit(false);
            pet.setInSittingPose(false);
            pet.getNavigation().stop();
        }
        int gap = config.integer("interval_ticks");
        int warn = config.integer("warn_ticks");
        double reach = config.num("reach");
        Scheduler.repeat(player, 0, gap, (p, n) -> {
            int elapsed = n * gap;
            boolean over = elapsed >= warn + config.integer("attack_ticks");
            for (TamableAnimal pet : pets) {
                if (!pet.isAlive()) {
                    continue;
                }
                if (over) {
                    pet.getNavigation().stop();
                } else if (elapsed < warn) {
                    pet.getNavigation().stop();
                    pet.lookAt(EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
                    if (n % config.integer("growl_every") == 0) {
                        growl(pet, config.tone("sound"));
                    }
                } else if (pet.distanceToSqr(p) > reach * reach) {
                    pet.getNavigation().moveTo(p, config.num("speed"));
                } else {
                    pet.getNavigation().stop();
                    pet.lookAt(EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
                    if (n % config.integer("bite_every") == 0) {
                        pet.doHurtTarget(p.level(), p);
                    }
                }
            }
            return !over;
        });
    }

    // ---- vex_swarm ----

    /** A cold wail and spirit fire around the player, then a few short-lived vexes named "?" come out of the air. */
    private static void vexSwarm(ServerPlayer player) {
        Config.Section config = Config.event(VEXES);
        ServerLevel level = player.level();
        Nearby.sound(player, SoundEvents.VEX_CHARGE, player.position(), config.tone("sound"));
        int pulse = config.integer("pulse_ticks");
        Scheduler.repeat(player, 0, pulse, (p, n) -> {
            if (n * pulse < config.integer("warn_ticks")) {
                puff(level, ParticleTypes.SOUL, p.position(), config.burst("particles"));
                return true;
            }
            int count = config.integer("count_min") + p.getRandom().nextInt(config.integer("count_spread"));
            for (int i = 0; i < count; i++) {
                BlockPos spot = Place.spawnSpot(p, config.integer("min_distance"), config.integer("max_distance"));
                Vex vex = spot == null ? null : EntityTypes.VEX.spawn(level, spot.above(), EntitySpawnReason.EVENT);
                if (vex != null) {
                    Band.THREAT.mark(vex);
                    vex.setLimitedLife(config.integer("life_ticks"));
                    vex.setCustomName(Component.literal("?"));
                    vex.setCustomNameVisible(true);
                    vex.setTarget(p);
                }
            }
            return false;
        });
    }

    // ---- ghost_creeper ----

    /** A hiss behind the player; a moment later a small blast there that breaks nothing. */
    private static void ghostCreeper(ServerPlayer player) {
        Config.Section config = Config.event(CREEPER);
        ServerLevel level = player.level();
        Vec3 spot = Nearby.around(player, config.num("distance"), config.num("yaw_offset"));
        Nearby.sound(player, SoundEvents.CREEPER_PRIMED, spot, config.tone("sound"));
        int pulse = config.integer("pulse_ticks");
        Scheduler.repeat(player, 0, pulse, (p, n) -> {
            if (n * pulse < config.integer("fuse_ticks")) {
                puff(level, ParticleTypes.SMOKE, spot, config.burst("particles"));
                return true;
            }
            level.sendParticles(ParticleTypes.EXPLOSION, spot.x, spot.y, spot.z, 1, 0.0, 0.0, 0.0, 0.0);
            Nearby.sound(p, SoundEvents.GENERIC_EXPLODE.value(), spot, config.tone("blast_sound"));
            Vec3 away = p.getBoundingBox().getCenter().subtract(spot);
            double radius = config.num("radius");
            if (away.length() < radius) {
                strike(p, (float) (config.num("damage") * (1.0 - away.length() / radius)));
                Vec3 push = away.normalize().scale(config.num("knockback"));
                p.setDeltaMovement(push.x, config.num("lift"), push.z);
                p.connection.send(new ClientboundSetEntityMotionPacket(p));
            }
            return false;
        });
    }

    // ---- stalker ----

    private static @Nullable BlockPos stalkerSpot(ServerPlayer player) {
        Config.Section config = Config.event(STALKER);
        return Place.spawnSpot(player, config.integer("min_distance"), config.integer("max_distance"));
    }

    /** A figure appears far off and stands watching, then runs at the player, hits once and is gone. */
    private static void stalk(ServerPlayer player) {
        BlockPos spot = stalkerSpot(player);
        if (spot == null) {
            return;
        }
        Config.Section config = Config.event(STALKER);
        ServerLevel level = player.level();
        Mannequin figure = Figure.spawn(player, spot, Figure.stranger(player));
        if (figure == null) {
            return;
        }
        Band.THREAT.mark(figure);
        Nearby.sound(player, SoundEvents.WARDEN_NEARBY_CLOSE, figure.position(), config.tone("sound"));
        int interval = config.integer("interval_ticks");
        Restore.after(
                level,
                config.integer("delay_ticks") + config.integer("max_ticks") + config.integer("cleanup_ticks"),
                force -> {
                    figure.discard();
                    return true;
                });
        int[] stuck = {0};
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            if (!figure.isAlive()) {
                return false;
            }
            int elapsed = n * interval;
            boolean over = elapsed >= config.integer("delay_ticks") + config.integer("max_ticks")
                    || stuck[0] >= config.integer("stuck_steps");
            double touch = config.num("touch_distance");
            boolean caught = elapsed >= config.integer("delay_ticks") && p.distanceToSqr(figure) <= touch * touch;
            if (caught) {
                Nearby.sound(p, SoundEvents.WARDEN_ATTACK_IMPACT, figure.position(), config.tone("hit_sound"));
                strike(p, (float) config.num("damage"));
            }
            if (caught || over) {
                puff(level, ParticleTypes.LARGE_SMOKE, figure.position(), config.burst("particles"));
                figure.discard();
                return false;
            }
            Figure.face(figure, p);
            if (elapsed >= config.integer("delay_ticks")) {
                stuck[0] = Walk.toward(p, figure, config.num("speed"), config.integer("climb"), config.integer("drop"))
                        ? 0
                        : stuck[0] + 1;
            }
            return true;
        });
    }

    // ---- hail_of_bones ----

    /** Rattling in the night sky, then blocks of bone fall around the player; a roof turns them. */
    private static void hailOfBones(ServerPlayer player) {
        Config.Section config = Config.event(BONES);
        ServerLevel level = player.level();
        BlockState bone = Blocks.BONE_BLOCK.defaultBlockState();
        int warn = config.integer("warn_ticks");
        int gap = config.integer("drop_gap_ticks");
        Scheduler.repeat(player, 0, 1, (p, n) -> {
            Vec3 sky = p.position().add(0.0, config.num("height"), 0.0);
            if (n < warn) {
                if (n % config.integer("rattle_every") == 0) {
                    Nearby.sound(p, SoundEvents.SKELETON_AMBIENT, sky, config.tone("sound"));
                    puff(level, new BlockParticleOption(ParticleTypes.BLOCK, bone), sky, config.burst("particles"));
                }
                return true;
            }
            if ((n - warn) % gap != 0) {
                return true;
            }
            double spread = config.num("spread");
            BlockPos column = BlockPos.containing(
                    p.getX() + (p.getRandom().nextDouble() * 2.0 - 1.0) * spread,
                    p.getY(),
                    p.getZ() + (p.getRandom().nextDouble() * 2.0 - 1.0) * spread);
            BlockPos top = column.above(config.integer("height"));
            if (level.isLoaded(top) && level.canSeeSky(column) && Place.open(level, top)) {
                Nearby.sound(p, SoundEvents.BONE_BLOCK_BREAK, Vec3.atCenterOf(top), config.tone("drop_sound"));
                drop(level, top, bone, config);
            }
            return (n - warn) / gap + 1 < config.integer("bones");
        });
    }
}
