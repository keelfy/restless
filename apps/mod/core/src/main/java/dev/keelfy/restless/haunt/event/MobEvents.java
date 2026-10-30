package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.List;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariants;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
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
            // 194: sits, looks at the empty space beside the player and growls; stands again when it is over
            SimpleEvent.light("dog_growls", EventTag.MOBS, player -> !standingDogs(player).isEmpty(), MobEvents::growl),
            // 196: the bees come out as if the hive had been broken
            SimpleEvent.harmful("angry_bees", EventTag.MOBS, player -> hive(player) != null, player -> {
                BlockPos pos = hive(player);
                if (pos != null && player.level().getBlockEntity(pos) instanceof BeehiveBlockEntity hive) {
                    hive.emptyAllLivingFromHive(player, player.level().getBlockState(pos), BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
                }
            }),
            // 181: the knot is dropped and the animals are loose
            SimpleEvent.harmful("leash_snaps", EventTag.MOBS, player -> !leashed(player).isEmpty(), player -> {
                List<Mob> mobs = leashed(player);
                Mob mob = mobs.get(player.getRandom().nextInt(mobs.size()));
                Config.Tone tone = Config.event("leash_snaps").tone("sound");
                player.level().playSound(null, mob.blockPosition(), SoundEvents.LEAD_BREAK, SoundSource.NEUTRAL, tone.volume(), tone.pitch());
                mob.dropLeash();
            })
        );
    }

    private static List<IronGolem> golems(ServerPlayer player) {
        return near(player, IronGolem.class, "golem_flower").stream().filter(g -> g.getTarget() == null && g.getOfferFlowerTick() == 0).toList();
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
