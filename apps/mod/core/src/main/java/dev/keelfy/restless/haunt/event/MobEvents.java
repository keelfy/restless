package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Location;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.Comparator;
import java.util.List;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariants;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Animals and monsters reacting to something the player cannot see, or turning on the player. */
final class MobEvents {
    private static final double BEHIND = 180.0;

    private MobEvents() {}

    static List<HauntEvent> all() {
        return List.of(
            // 86
            SimpleEvent.light("cat_hiss", EventTag.MOBS, player -> !near(player, Cat.class, "cat_hiss").isEmpty(), player -> {
                List<Cat> cats = near(player, Cat.class, "cat_hiss");
                cats.get(player.getRandom().nextInt(cats.size())).hiss();
            }),
            // 129: the cats shy away from the spirit's point, which also tells where it is
            SimpleEvent.light("cat_flee", EventTag.MOBS, player -> !fleeing(player).isEmpty(), MobEvents::catsFlee),
            // 87
            SimpleEvent.light("dog_barks", EventTag.MOBS, player -> !near(player, Wolf.class, "dog_barks").isEmpty(), player -> {
                List<Wolf> wolves = near(player, Wolf.class, "dog_barks");
                Wolf wolf = wolves.get(player.getRandom().nextInt(wolves.size()));
                wolf.setYRot(player.getRandom().nextFloat() * 360.0F);
                wolf.playAmbientSound();
            }),
            // 85
            SimpleEvent.light("animals_stare", EventTag.MOBS, player -> near(player, Animal.class, "animals_stare").size() >= Config.event("animals_stare").integer("min_animals"),
                player -> stare(player, near(player, Animal.class, "animals_stare"), "animals_stare")),
            // 88
            SimpleEvent.light("villager_stares", EventTag.MOBS, player -> !near(player, Villager.class, "villager_stares").isEmpty(), player -> {
                List<Villager> villagers = near(player, Villager.class, "villager_stares");
                stare(player, List.of(villagers.get(player.getRandom().nextInt(villagers.size()))), "villager_stares");
            }),
            // 192: the villager turns its head from side to side and says no
            SimpleEvent.light("villager_refuses", EventTag.MOBS, player -> !near(player, Villager.class, "villager_refuses").isEmpty(), player -> {
                List<Villager> villagers = near(player, Villager.class, "villager_refuses");
                Villager villager = villagers.get(player.getRandom().nextInt(villagers.size()));
                villager.setUnhappyCounter(Config.event("villager_refuses").integer("shake_ticks"));
                Config.Tone tone = Config.event("villager_refuses").tone("sound");
                villager.level().playSound(null, villager, SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, tone.volume(), tone.pitch());
            }),
            // 89
            SimpleEvent.light("bats", EventTag.MOBS, player -> airSpot(player) != null, player -> {
                BlockPos spot = airSpot(player);
                if (spot != null) {
                    for (int i = 0, n = Config.event("bats").integer("count"); i < n; i++) {
                        EntityTypes.BAT.spawn(player.level(), spot, EntitySpawnReason.EVENT);
                    }
                }
            }),
            // 90
            SimpleEvent.harmful("open_pen", EventTag.MOBS, player -> penGate(player) != null, player -> {
                BlockPos gate = penGate(player);
                if (gate != null) {
                    ServerLevel level = player.level();
                    level.setBlockAndUpdate(gate, level.getBlockState(gate).setValue(BlockStateProperties.OPEN, true));
                    Config.Tone tone = Config.event("open_pen").tone("sound");
                    level.playSound(null, gate, SoundEvents.FENCE_GATE_OPEN, SoundSource.BLOCKS, tone.volume(), tone.pitch());
                }
            }),
            // 91
            SimpleEvent.harmful("wild_pet", EventTag.MOBS, player -> !pets(player).isEmpty(), player -> {
                List<TamableAnimal> pets = pets(player);
                TamableAnimal pet = pets.get(player.getRandom().nextInt(pets.size()));
                pet.setTame(false, false);
                pet.setOwner(null);
                pet.setTarget(player);
            }),
            // 92
            SimpleEvent.harmful("copycat", EventTag.MOBS,
                player -> player.level().getDifficulty() != Difficulty.PEACEFUL && groundSpot(player) != null,
                player -> {
                    BlockPos spot = groundSpot(player);
                    if (spot == null) {
                        return;
                    }
                    Zombie zombie = EntityTypes.ZOMBIE.spawn(player.level(), spot, EntitySpawnReason.EVENT);
                    if (zombie == null) {
                        return;
                    }
                    for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                        zombie.setItemSlot(slot, player.getItemBySlot(slot).copy());
                        zombie.setDropChance(slot, 0.0F);
                    }
                    zombie.setTarget(player);
                }),
            // 193: the flower is offered for a while, to nobody
            SimpleEvent.light("golem_flower", EventTag.MOBS, player -> !golems(player).isEmpty(), player -> {
                List<IronGolem> golems = golems(player);
                IronGolem golem = golems.get(player.getRandom().nextInt(golems.size()));
                golem.offerFlower(true);
                Scheduler.later(player, Config.event("golem_flower").integer("hold_ticks"), p -> golem.offerFlower(false));
            }),
            SimpleEvent.harmful("golem_rage", EventTag.MOBS, player -> hostileGolem(player) != null, player -> {
                IronGolem golem = hostileGolem(player);
                if (golem != null) {
                    golem.offerFlower(false);
                    golem.setPersistentAngerTarget(EntityReference.of(player));
                    golem.startPersistentAngerTimer();
                    golem.setTarget(player);
                }
            }),
            // 194: sits, looks at the empty space beside the player and growls; stands again when it is over
            SimpleEvent.light("dog_growls", EventTag.MOBS, player -> !standingDogs(player).isEmpty(), MobEvents::growl),
            // 195: the pen's animals crowd into one corner and stand facing it
            SimpleEvent.light("pen_corner", EventTag.MOBS, player -> pen(player) != null, MobEvents::cornered),
            // 196: the bees come out as if the hive had been broken
            SimpleEvent.harmful("angry_bees", EventTag.MOBS, player -> hive(player) != null, player -> {
                BlockPos pos = hive(player);
                if (pos != null && player.level().getBlockEntity(pos) instanceof BeehiveBlockEntity hive) {
                    hive.emptyAllLivingFromHive(player, player.level().getBlockState(pos), BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
                }
            }),
            // 197: the goat charges at where the player stands, as if its ram cooldown had run out
            SimpleEvent.harmful("goat_rams", EventTag.MOBS, player -> !goats(player).isEmpty(), player -> {
                List<Goat> goats = goats(player);
                Goat goat = goats.get(player.getRandom().nextInt(goats.size()));
                // A stroll in progress would erase the charge's walk target when it stops, so let it stop first.
                goat.getNavigation().stop();
                goat.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                goat.getBrain().eraseMemory(MemoryModuleType.PATH);
                goat.getBrain().eraseMemory(MemoryModuleType.RAM_COOLDOWN_TICKS);
                Scheduler.later(player, 2, p -> {
                    if (goat.isAlive()) {
                        goat.getBrain().setMemory(MemoryModuleType.RAM_TARGET, p.position());
                    }
                });
                Config.Tone tone = Config.event("goat_rams").tone("sound");
                player.level().playSound(null, goat, SoundEvents.GOAT_PREPARE_RAM, SoundSource.NEUTRAL, tone.volume(), tone.pitch());
            }),
            // 180: an empty boat in the water slides away from the player
            SimpleEvent.light("boat_drifts", EventTag.WATER, player -> !boats(player).isEmpty(), player -> {
                List<AbstractBoat> boats = boats(player);
                AbstractBoat boat = boats.get(player.getRandom().nextInt(boats.size()));
                push(player, boat, "boat_drifts", boat.position().subtract(player.position()));
            }),
            // 179: an empty minecart on a rail rolls toward the player
            SimpleEvent.light("minecart_rolls", EventTag.MOBS, player -> !carts(player).isEmpty(), player -> {
                List<AbstractMinecart> carts = carts(player);
                AbstractMinecart cart = carts.get(player.getRandom().nextInt(carts.size()));
                push(player, cart, "minecart_rolls", player.position().subtract(cart.position()));
            }),
            // 181: the knot is dropped and the animals are loose
            SimpleEvent.harmful("leash_snaps", EventTag.MOBS, player -> !leashed(player).isEmpty(), player -> {
                List<Mob> mobs = leashed(player);
                Mob mob = mobs.get(player.getRandom().nextInt(mobs.size()));
                Config.Tone tone = Config.event("leash_snaps").tone("sound");
                player.level().playSound(null, mob.blockPosition(), SoundEvents.LEAD_BREAK, SoundSource.NEUTRAL, tone.volume(), tone.pitch());
                mob.dropLeash();
            }),
            // 198: a block of the build ends up in an enderman's hands
            SimpleEvent.harmful("enderman_takes", EventTag.MOBS, player -> !takers(player).isEmpty() && !builds(player).isEmpty(), player -> {
                List<Enderman> takers = takers(player);
                List<BlockPos> builds = builds(player);
                Enderman taker = takers.get(player.getRandom().nextInt(takers.size()));
                BlockPos pos = builds.get(player.getRandom().nextInt(builds.size()));
                ServerLevel level = player.level();
                BlockState block = level.getBlockState(pos);
                Swaps.show(player, pos, Blocks.AIR.defaultBlockState(), Config.event("enderman_takes").integer("restore_ticks"));
                taker.setCarriedBlock(block);
                Restore.revert(level, Config.event("enderman_takes").integer("restore_ticks"), force -> {
                    if (taker.getCarriedBlock() != null && taker.getCarriedBlock().is(block.getBlock())) {
                        taker.setCarriedBlock(null);
                    }
                    return true;
                });
                Config.Tone tone = Config.event("enderman_takes").tone("sound");
                level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, tone.volume(), tone.pitch());
            })
        );
    }

    private static List<Enderman> takers(ServerPlayer player) {
        return near(player, Enderman.class, "enderman_takes").stream().filter(e -> e.getCarriedBlock() == null).toList();
    }

    /** Exposed blocks that look placed by hand, not part of the land. */
    private static List<BlockPos> builds(ServerPlayer player) {
        ServerLevel level = player.level();
        return Nearby.all(player, "enderman_takes", s -> !s.hasBlockEntity()
            && (s.is(BlockTags.PLANKS) || s.is(BlockTags.WOOL) || s.is(BlockTags.STONE_BRICKS) || s.is(Blocks.BRICKS) || s.is(Blocks.BOOKSHELF)))
            .stream().filter(pos -> java.util.Arrays.stream(Direction.values()).anyMatch(d -> level.getBlockState(pos.relative(d)).isAir())).toList();
    }

    /** Adult goats far enough from the player to get a run-up. */
    private static List<Goat> goats(ServerPlayer player) {
        double run = Config.event("goat_rams").num("min_run");
        return near(player, Goat.class, "goat_rams").stream()
            .filter(g -> !g.isBaby() && g.distanceToSqr(player) >= run * run).toList();
    }

    private static List<AbstractBoat> boats(ServerPlayer player) {
        return near(player, AbstractBoat.class, "boat_drifts").stream()
            .filter(b -> !b.isVehicle() && b.level().getFluidState(b.blockPosition()).is(FluidTags.WATER)).toList();
    }

    private static List<AbstractMinecart> carts(ServerPlayer player) {
        return near(player, AbstractMinecart.class, "minecart_rolls").stream()
            .filter(c -> !c.isVehicle() && BaseRailBlock.isRail(c.level().getBlockState(c.blockPosition()))).toList();
    }

    /** A steady horizontal shove along {@code way} for the event's push time; stops early if the vehicle gets a rider. */
    private static void push(ServerPlayer player, Entity vehicle, String event, Vec3 way) {
        Config.Section config = Config.event(event);
        Vec3 heading = new Vec3(way.x, 0.0, way.z).normalize().scale(config.num("speed"));
        Scheduler.repeat(player, 0, 1, (p, n) -> {
            if (!vehicle.isAlive() || vehicle.isVehicle()) {
                return false;
            }
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(heading));
            return n < config.integer("push_ticks");
        });
    }

    private static List<IronGolem> golems(ServerPlayer player) {
        return near(player, IronGolem.class, "golem_flower").stream().filter(g -> g.getTarget() == null && g.getOfferFlowerTick() == 0).toList();
    }

    private static @Nullable IronGolem hostileGolem(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator() || player.level().getDifficulty() == Difficulty.PEACEFUL) {
            return null;
        }
        double radius = Nearby.radius("golem_rage");
        return near(player, IronGolem.class, "golem_rage").stream()
            .filter(g -> g.isAlive() && !g.isNoAi() && g.getTarget() == null && g.canAttack(player)
                && g.distanceToSqr(player) <= radius * radius)
            .min(Comparator.comparingDouble(g -> g.distanceToSqr(player))).orElse(null);
    }

    private static List<Wolf> standingDogs(ServerPlayer player) {
        return near(player, Wolf.class, "dog_growls").stream().filter(w -> w.isTame() && !w.isOrderedToSit() && !w.isInSittingPose()).toList();
    }

    private static void growl(ServerPlayer player) {
        List<Wolf> dogs = standingDogs(player);
        if (dogs.isEmpty()) {
            return;
        }
        Wolf dog = dogs.get(player.getRandom().nextInt(dogs.size()));
        Config.Section config = Config.event("dog_growls");
        Vec3 empty = Nearby.around(player, config.num("distance"), player.getRandom().nextDouble() * Nearby.FULL_TURN);
        dog.getNavigation().stop();
        dog.setOrderedToSit(true);
        dog.setInSittingPose(true);
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            if (!dog.isAlive() || !dog.isOrderedToSit()) {
                return false;
            }
            dog.lookAt(EntityAnchorArgument.Anchor.EYES, empty);
            if (n % config.integer("growl_steps") == 0) {
                Config.Tone tone = config.tone("sound");
                dog.level().playSound(null, dog.blockPosition(), SoundEvents.WOLF_SOUNDS.get(WolfSoundVariants.SoundSet.CLASSIC).adultSounds().growlSound().value(), SoundSource.NEUTRAL, tone.volume(), tone.pitch());
            }
            return n * config.integer("interval_ticks") < config.integer("growl_ticks");
        });
        Restore.after(player.level(), config.integer("growl_ticks"), force -> {
            if (dog.isAlive() && dog.isOrderedToSit()) {
                dog.setOrderedToSit(false);
                dog.setInSittingPose(false);
            }
            return true;
        });
    }

    /** Fenced on four sides, from {@code minX, minZ} to {@code maxX, maxZ} inside, with the farm animals in it. */
    private record Pen(int minX, int minZ, int maxX, int maxZ, int y, List<Animal> animals) {}

    /** The first pen with enough farm animals in it (none tamable), read off the fences in line with one of them. */
    private static @Nullable Pen pen(ServerPlayer player) {
        Config.Section config = Config.event("pen_corner");
        List<Animal> farm = near(player, Animal.class, "pen_corner").stream().filter(a -> !(a instanceof TamableAnimal) && !a.isLeashed()).toList();
        for (Animal animal : farm) {
            BlockPos at = animal.blockPosition();
            int[] reach = new int[4];
            boolean fenced = true;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                int k = fence(player.level(), at, d, config.integer("max_size"));
                if (k < 0) {
                    fenced = false;
                    break;
                }
                reach[d.get2DDataValue()] = k - 1;
            }
            if (!fenced) {
                continue;
            }
            int minX = at.getX() - reach[Direction.WEST.get2DDataValue()];
            int maxX = at.getX() + reach[Direction.EAST.get2DDataValue()];
            int minZ = at.getZ() - reach[Direction.NORTH.get2DDataValue()];
            int maxZ = at.getZ() + reach[Direction.SOUTH.get2DDataValue()];
            List<Animal> in = farm.stream().filter(a -> a.getBlockX() >= minX && a.getBlockX() <= maxX && a.getBlockZ() >= minZ && a.getBlockZ() <= maxZ
                && Math.abs(a.getBlockY() - at.getY()) <= 1).toList();
            if (in.size() >= config.integer("min_animals")) {
                return new Pen(minX, minZ, maxX, maxZ, at.getY(), in);
            }
        }
        return null;
    }

    /** Blocks from {@code from} to a fence, wall or gate along {@code d}, or -1 if something else or nothing comes first. */
    private static int fence(ServerLevel level, BlockPos from, Direction d, int max) {
        for (int k = 1; k <= max; k++) {
            BlockPos pos = from.relative(d, k);
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS) || state.getBlock() instanceof FenceGateBlock) {
                return k;
            }
            if (!Place.open(level, pos)) {
                return -1;
            }
        }
        return -1;
    }

    /** Each animal walks to the corner; once there it stops and keeps looking into it. Let go when the time is up. */
    private static void cornered(ServerPlayer player) {
        Pen pen = pen(player);
        if (pen == null) {
            return;
        }
        Config.Section config = Config.event("pen_corner");
        boolean east = player.getRandom().nextBoolean();
        boolean south = player.getRandom().nextBoolean();
        Vec3 corner = new Vec3((east ? pen.maxX() : pen.minX()) + 0.5, pen.y(), (south ? pen.maxZ() : pen.minZ()) + 0.5);
        Vec3 wall = corner.add(east ? 1.0 : -1.0, 0.0, south ? 1.0 : -1.0);
        double reach = config.num("reach");
        int interval = config.integer("interval_ticks");
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            for (Animal animal : pen.animals()) {
                if (!animal.isAlive()) {
                    continue;
                }
                if (animal.position().multiply(1, 0, 1).distanceTo(corner.multiply(1, 0, 1)) > reach) {
                    animal.getNavigation().moveTo(corner.x, corner.y, corner.z, config.num("speed"));
                } else {
                    animal.getNavigation().stop();
                    animal.lookAt(EntityAnchorArgument.Anchor.EYES, wall.add(0.0, animal.getEyeHeight(), 0.0));
                    animal.setYBodyRot(animal.getYRot());
                }
            }
            return n * interval < config.integer("ticks");
        });
    }

    private static @Nullable BlockPos hive(ServerPlayer player) {
        return Nearby.all(player, "angry_bees", s -> s.is(BlockTags.BEEHIVES)).stream()
            .filter(pos -> player.level().getBlockEntity(pos) instanceof BeehiveBlockEntity hive && !hive.isEmpty())
            .findFirst().orElse(null);
    }

    /** Mobs tied to a fence post. */
    private static List<Mob> leashed(ServerPlayer player) {
        return near(player, Mob.class, "leash_snaps").stream().filter(m -> m.getLeashHolder() instanceof LeashFenceKnotEntity).toList();
    }

    private static <T extends Entity> List<T> near(ServerPlayer player, Class<T> type, String event) {
        return player.level().getEntitiesOfClass(type, new AABB(player.blockPosition()).inflate(Nearby.radius(event)));
    }

    private static List<Cat> fleeing(ServerPlayer player) {
        return near(player, Cat.class, "cat_flee").stream().filter(c -> !c.isInSittingPose()).toList();
    }

    /** Every pulse the cats run to a point on the far side of themselves from the spirit; the first hisses. */
    private static void catsFlee(ServerPlayer player) {
        Config.Section config = Config.event("cat_flee");
        Vec3 spirit = Location.source(player.position());
        List<Cat> cats = fleeing(player);
        cats.get(player.getRandom().nextInt(cats.size())).hiss();
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            for (Cat cat : cats) {
                Vec3 away = cat.position().subtract(spirit).multiply(1, 0, 1);
                if (cat.isAlive() && !cat.isInSittingPose() && away.lengthSqr() > 1.0E-4) {
                    Vec3 to = cat.position().add(away.normalize().scale(config.num("distance")));
                    cat.getNavigation().moveTo(to.x, to.y, to.z, config.num("speed"));
                }
            }
            return n + 1 < config.integer("pulses");
        });
    }

    /** The mobs stop and face the player for the event's stare time. */
    private static void stare(ServerPlayer player, List<? extends Mob> mobs, String event) {
        Config.Section config = Config.event(event);
        int interval = config.integer("look_interval_ticks");
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            for (Mob mob : mobs) {
                if (mob.isAlive()) {
                    mob.getNavigation().stop();
                    mob.lookAt(EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
                }
            }
            return n * interval < config.integer("stare_ticks");
        });
    }

    private static List<TamableAnimal> pets(ServerPlayer player) {
        return near(player, TamableAnimal.class, "wild_pet").stream().filter(p -> p.isTame() && p.isOwnedBy(player)).toList();
    }

    /** A closed fence gate with a farm animal right next to it. */
    private static @Nullable BlockPos penGate(ServerPlayer player) {
        for (BlockPos pos : Nearby.all(player, "open_pen", MobEvents::closedGate)) {
            if (!player.level().getEntitiesOfClass(Animal.class, new AABB(pos).inflate(Config.event("open_pen").integer("animal_radius"))).isEmpty()) {
                return pos;
            }
        }
        return null;
    }

    private static boolean closedGate(BlockState state) {
        return state.getBlock() instanceof FenceGateBlock && !state.getValue(BlockStateProperties.OPEN);
    }

    /** Standing room (floor plus two free blocks) behind the player, within a step up or down. */
    private static @Nullable BlockPos groundSpot(ServerPlayer player) {
        return spot(player, Config.event("copycat").num("distance"), BEHIND, true);
    }

    /** Free air to spawn into, at a random bearing. */
    private static @Nullable BlockPos airSpot(ServerPlayer player) {
        return spot(player, Config.event("bats").num("distance"), player.getRandom().nextDouble() * Nearby.FULL_TURN, false);
    }

    private static @Nullable BlockPos spot(ServerPlayer player, double distance, double yawOffset, boolean needsFloor) {
        Vec3 at = Nearby.around(player, distance, yawOffset);
        BlockPos base = BlockPos.containing(at.x, player.getY(), at.z);
        for (int dy = 1; dy >= -1; dy--) {
            BlockPos pos = base.above(dy);
            ServerLevel level = player.level();
            boolean room = level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above());
            boolean floor = !needsFloor || level.getBlockState(pos.below()).isSolidRender();
            if (room && floor) {
                return pos;
            }
        }
        return null;
    }
}
