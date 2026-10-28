package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
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
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.wolf.Wolf;
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
    private static final double WEIGHT = 0.6;
    private static final int BATS = 5;

    private MobEvents() {}

    static List<HauntEvent> all() {
        return List.of(
            // 86
            SimpleEvent.light("cat_hiss", EventTag.MOBS, WEIGHT, player -> !near(player, Cat.class, 16).isEmpty(), player -> {
                List<Cat> cats = near(player, Cat.class, 16);
                cats.get(player.getRandom().nextInt(cats.size())).hiss();
            }),
            // 87
            SimpleEvent.light("dog_barks", EventTag.MOBS, WEIGHT, player -> !near(player, Wolf.class, 16).isEmpty(), player -> {
                List<Wolf> wolves = near(player, Wolf.class, 16);
                Wolf wolf = wolves.get(player.getRandom().nextInt(wolves.size()));
                wolf.setYRot(player.getRandom().nextFloat() * 360.0F);
                wolf.playAmbientSound();
            }),
            // 85
            SimpleEvent.light("animals_stare", EventTag.MOBS, WEIGHT, player -> near(player, Animal.class, 16).size() >= 2,
                player -> stare(player, near(player, Animal.class, 16), 60)),
            // 88
            SimpleEvent.light("villager_stares", EventTag.MOBS, WEIGHT, player -> !near(player, Villager.class, 16).isEmpty(), player -> {
                List<Villager> villagers = near(player, Villager.class, 16);
                stare(player, List.of(villagers.get(player.getRandom().nextInt(villagers.size()))), 100);
            }),
            // 89
            SimpleEvent.light("bats", EventTag.MOBS, WEIGHT, player -> airSpot(player, 4.0, 360.0) != null, player -> {
                BlockPos spot = airSpot(player, 4.0, 360.0);
                if (spot != null) {
                    for (int i = 0; i < BATS; i++) {
                        EntityTypes.BAT.spawn(player.level(), spot, EntitySpawnReason.EVENT);
                    }
                }
            }),
            // 90
            SimpleEvent.harmful("open_pen", EventTag.MOBS, WEIGHT, player -> penGate(player) != null, player -> {
                BlockPos gate = penGate(player);
                if (gate != null) {
                    ServerLevel level = player.level();
                    level.setBlockAndUpdate(gate, level.getBlockState(gate).setValue(BlockStateProperties.OPEN, true));
                    level.playSound(null, gate, SoundEvents.FENCE_GATE_OPEN, SoundSource.BLOCKS, 1.0F, 0.9F);
                }
            }),
            // 91
            SimpleEvent.harmful("wild_pet", EventTag.MOBS, WEIGHT, player -> !pets(player).isEmpty(), player -> {
                List<TamableAnimal> pets = pets(player);
                TamableAnimal pet = pets.get(player.getRandom().nextInt(pets.size()));
                pet.setTame(false, false);
                pet.setOwner(null);
                pet.setTarget(player);
            }),
            // 92
            SimpleEvent.harmful("copycat", EventTag.MOBS, WEIGHT,
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
                })
        );
    }

    private static <T extends Entity> List<T> near(ServerPlayer player, Class<T> type, int radius) {
        return player.level().getEntitiesOfClass(type, new AABB(player.blockPosition()).inflate(radius));
    }

    /** The mobs stop and face the player for {@code ticks}. */
    private static void stare(ServerPlayer player, List<? extends Mob> mobs, int ticks) {
        Scheduler.repeat(player, 0, 2, (p, n) -> {
            for (Mob mob : mobs) {
                if (mob.isAlive()) {
                    mob.getNavigation().stop();
                    mob.lookAt(EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
                }
            }
            return n * 2 < ticks;
        });
    }

    private static List<TamableAnimal> pets(ServerPlayer player) {
        return near(player, TamableAnimal.class, 32).stream().filter(p -> p.isTame() && p.isOwnedBy(player)).toList();
    }

    /** A closed fence gate with a farm animal right next to it. */
    private static @Nullable BlockPos penGate(ServerPlayer player) {
        for (BlockPos pos : Nearby.all(player, 12, MobEvents::closedGate)) {
            if (!player.level().getEntitiesOfClass(Animal.class, new AABB(pos).inflate(5)).isEmpty()) {
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
        return spot(player, 3.0, 180.0, true);
    }

    /** Free air to spawn into, at a random bearing. */
    private static @Nullable BlockPos airSpot(ServerPlayer player, double distance, double yaw) {
        return spot(player, distance, player.getRandom().nextDouble() * yaw, false);
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
