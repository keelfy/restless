package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntSounds;
import dev.keelfy.restless.haunt.Location;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariants;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
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
                SimpleEvent.light(
                        "cat_hiss",
                        EventTag.MOBS,
                        player -> !near(player, Cat.class, "cat_hiss").isEmpty(),
                        player -> {
                            List<Cat> cats = near(player, Cat.class, "cat_hiss");
                            cats.get(player.getRandom().nextInt(cats.size())).hiss();
                        }),
                // 129: the cats shy away from the spirit's point, which also tells where it is
                SimpleEvent.light(
                        "cat_flee", EventTag.MOBS, player -> !fleeing(player).isEmpty(), MobEvents::catsFlee),
                // 87
                SimpleEvent.light(
                        "dog_barks",
                        EventTag.MOBS,
                        player -> !near(player, Wolf.class, "dog_barks").isEmpty(),
                        player -> {
                            List<Wolf> wolves = near(player, Wolf.class, "dog_barks");
                            Wolf wolf = wolves.get(player.getRandom().nextInt(wolves.size()));
                            wolf.setYRot(player.getRandom().nextFloat() * 360.0F);
                            wolf.playAmbientSound();
                        }),
                // 85
                SimpleEvent.light(
                        "animals_stare",
                        EventTag.MOBS,
                        player -> near(player, Animal.class, "animals_stare").size()
                                >= Config.event("animals_stare").integer("min_animals"),
                        player -> stare(player, near(player, Animal.class, "animals_stare"), "animals_stare")),
                // 88
                SimpleEvent.light(
                        "villager_stares",
                        EventTag.MOBS,
                        player ->
                                !near(player, Villager.class, "villager_stares").isEmpty(),
                        player -> {
                            List<Villager> villagers = near(player, Villager.class, "villager_stares");
                            stare(
                                    player,
                                    List.of(villagers.get(player.getRandom().nextInt(villagers.size()))),
                                    "villager_stares");
                        }),
                // 192: the villager turns its head from side to side and says no
                SimpleEvent.light(
                        "villager_refuses",
                        EventTag.MOBS,
                        player -> !near(player, Villager.class, "villager_refuses")
                                .isEmpty(),
                        player -> {
                            List<Villager> villagers = near(player, Villager.class, "villager_refuses");
                            Villager villager = villagers.get(player.getRandom().nextInt(villagers.size()));
                            villager.setUnhappyCounter(
                                    Config.event("villager_refuses").integer("shake_ticks"));
                            Config.Tone tone = Config.event("villager_refuses").tone("sound");
                            villager.level()
                                    .playSound(
                                            null,
                                            villager,
                                            SoundEvents.VILLAGER_NO,
                                            SoundSource.NEUTRAL,
                                            tone.volume(),
                                            tone.pitch());
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
                        level.setBlockAndUpdate(
                                gate, level.getBlockState(gate).setValue(BlockStateProperties.OPEN, true));
                        Config.Tone tone = Config.event("open_pen").tone("sound");
                        level.playSound(
                                null,
                                gate,
                                SoundEvents.FENCE_GATE_OPEN,
                                SoundSource.BLOCKS,
                                tone.volume(),
                                tone.pitch());
                    }
                }),
                // 91
                SimpleEvent.harmful(
                        "wild_pet", EventTag.MOBS, player -> !pets(player).isEmpty(), player -> {
                            List<TamableAnimal> pets = pets(player);
                            TamableAnimal pet = pets.get(player.getRandom().nextInt(pets.size()));
                            pet.setTame(false, false);
                            pet.setOwner(null);
                            pet.setTarget(player);
                        }),
                // 92
                SimpleEvent.harmful(
                        "copycat",
                        EventTag.MOBS,
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
                            for (EquipmentSlot slot : new EquipmentSlot[] {
                                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
                            }) {
                                zombie.setItemSlot(
                                        slot, player.getItemBySlot(slot).copy());
                                zombie.setDropChance(slot, 0.0F);
                            }
                            zombie.setTarget(player);
                        }),
                // 193: the flower is offered for a while, to nobody
                SimpleEvent.light(
                        "golem_flower", EventTag.MOBS, player -> !golems(player).isEmpty(), player -> {
                            List<IronGolem> golems = golems(player);
                            IronGolem golem = golems.get(player.getRandom().nextInt(golems.size()));
                            golem.offerFlower(true);
                            Scheduler.later(
                                    player,
                                    Config.event("golem_flower").integer("hold_ticks"),
                                    p -> golem.offerFlower(false));
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
                SimpleEvent.harmful(
                        "wolf_rage",
                        EventTag.MOBS,
                        player -> !hostileWolves(player).isEmpty(),
                        player -> {
                            for (Wolf wolf : hostileWolves(player)) {
                                wolf.setPersistentAngerTarget(EntityReference.of(player));
                                wolf.startPersistentAngerTimer();
                                wolf.setTarget(player);
                            }
                        }),
                // 194: sits, looks at the empty space beside the player and growls; stands again when it is over
                SimpleEvent.light(
                        "dog_growls",
                        EventTag.MOBS,
                        player -> !standingDogs(player).isEmpty(),
                        MobEvents::growl),
                // 195: the pen's animals crowd into one corner and stand facing it
                SimpleEvent.light("pen_corner", EventTag.MOBS, player -> pen(player) != null, MobEvents::cornered),
                // 196: the bees come out as if the hive had been broken
                SimpleEvent.harmful(
                        "angry_bees", EventTag.MOBS, player -> hive(player, "angry_bees") != null, player -> {
                            BlockPos pos = hive(player, "angry_bees");
                            if (pos != null && player.level().getBlockEntity(pos) instanceof BeehiveBlockEntity hive) {
                                hive.emptyAllLivingFromHive(
                                        player,
                                        player.level().getBlockState(pos),
                                        BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
                            }
                        }),
                // 197: the goat charges at where the player stands, as if its ram cooldown had run out
                SimpleEvent.harmful(
                        "goat_rams", EventTag.MOBS, player -> !goats(player).isEmpty(), player -> {
                            List<Goat> goats = goats(player);
                            Goat goat = goats.get(player.getRandom().nextInt(goats.size()));
                            Scheduler.later(player, 2, p -> {
                                if (goat.isAlive()) {
                                    // All at once. A stroll still running would erase the charge's walk target when
                                    // it stops, and a ram activity without a target would look for one, find none and
                                    // put the goat back on its cooldown.
                                    Brain<Goat> brain = goat.getBrain();
                                    goat.getNavigation().stop();
                                    brain.stopAll(p.level(), goat);
                                    brain.eraseMemory(MemoryModuleType.RAM_COOLDOWN_TICKS);
                                    brain.setActiveActivityIfPossible(Activity.RAM);
                                    brain.setMemory(MemoryModuleType.RAM_TARGET, p.position());
                                }
                            });
                            Config.Tone tone = Config.event("goat_rams").tone("sound");
                            player.level()
                                    .playSound(
                                            null,
                                            goat,
                                            SoundEvents.GOAT_PREPARE_RAM,
                                            SoundSource.NEUTRAL,
                                            tone.volume(),
                                            tone.pitch());
                        }),
                // 180: an empty boat in the water slides away from the player
                SimpleEvent.light(
                        "boat_drifts", EventTag.WATER, player -> !boats(player).isEmpty(), player -> {
                            List<AbstractBoat> boats = boats(player);
                            AbstractBoat boat = boats.get(player.getRandom().nextInt(boats.size()));
                            push(player, boat, "boat_drifts", boat.position().subtract(player.position()));
                        }),
                // 179: an empty minecart on a rail rolls toward the player
                SimpleEvent.light(
                        "minecart_rolls",
                        EventTag.MOBS,
                        player -> !carts(player).isEmpty(),
                        player -> {
                            List<AbstractMinecart> carts = carts(player);
                            AbstractMinecart cart = carts.get(player.getRandom().nextInt(carts.size()));
                            push(
                                    player,
                                    cart,
                                    "minecart_rolls",
                                    player.position().subtract(cart.position()));
                        }),
                // 181: the knot is dropped and the animals are loose
                SimpleEvent.harmful(
                        "leash_snaps", EventTag.MOBS, player -> !leashed(player).isEmpty(), player -> {
                            List<Mob> mobs = leashed(player);
                            Mob mob = mobs.get(player.getRandom().nextInt(mobs.size()));
                            Config.Tone tone = Config.event("leash_snaps").tone("sound");
                            player.level()
                                    .playSound(
                                            null,
                                            mob.blockPosition(),
                                            SoundEvents.LEAD_BREAK,
                                            SoundSource.NEUTRAL,
                                            tone.volume(),
                                            tone.pitch());
                            mob.dropLeash();
                        }),
                // 198: a block of the build ends up in an enderman's hands
                SimpleEvent.harmful(
                        "enderman_takes",
                        EventTag.MOBS,
                        player -> !takers(player).isEmpty() && !builds(player).isEmpty(),
                        player -> {
                            List<Enderman> takers = takers(player);
                            List<BlockPos> builds = builds(player);
                            Enderman taker = takers.get(player.getRandom().nextInt(takers.size()));
                            BlockPos pos = builds.get(player.getRandom().nextInt(builds.size()));
                            ServerLevel level = player.level();
                            BlockState block = level.getBlockState(pos);
                            Swaps.show(
                                    player,
                                    pos,
                                    Blocks.AIR.defaultBlockState(),
                                    Config.event("enderman_takes").integer("restore_ticks"));
                            taker.setCarriedBlock(block);
                            Restore.revert(level, Config.event("enderman_takes").integer("restore_ticks"), force -> {
                                if (taker.getCarriedBlock() != null
                                        && taker.getCarriedBlock().is(block.getBlock())) {
                                    taker.setCarriedBlock(null);
                                }
                                return true;
                            });
                            Config.Tone tone = Config.event("enderman_takes").tone("sound");
                            level.playSound(
                                    null,
                                    pos,
                                    SoundEvents.ENDERMAN_TELEPORT,
                                    SoundSource.HOSTILE,
                                    tone.volume(),
                                    tone.pitch());
                        }),
                // 247: the parrot says a whisper or the player's own hurt sound; only this player hears it
                SimpleEvent.light(
                        "parrot_echo",
                        EventTag.MOBS,
                        player -> !near(player, Parrot.class, "parrot_echo").isEmpty(),
                        MobEvents::parrotEchoes),
                // 248: an allay with a bone in its hand flies to the player and lets it fall
                SimpleEvent.light(
                        "allay_bone",
                        EventTag.MOBS,
                        player -> !handsFree(player).isEmpty(),
                        MobEvents::allayBrings),
                // 249: a fox comes at night for what the player holds and runs with it in its mouth
                SimpleEvent.harmful(
                        "fox_snatch",
                        EventTag.MOBS,
                        player -> player.level().isDarkOutside()
                                && !player.getMainHandItem().isEmpty()
                                && !thieves(player).isEmpty(),
                        MobEvents::foxSnatches),
                // 250: an armadillo curls up, which also tells where the spirit is
                SimpleEvent.light(
                        "armadillo_curls",
                        EventTag.MOBS,
                        player -> !uncurled(player).isEmpty(),
                        MobEvents::armadilloCurls),
                // 251: the sheep turn black one after another, then back
                SimpleEvent.light(
                        "sheep_blacken",
                        EventTag.MOBS,
                        player -> blackenable(player).size()
                                >= Config.event("sheep_blacken").integer("min_sheep"),
                        MobEvents::sheepBlacken),
                // 252: the horse bucks the rider off and bolts toward the spirit
                SimpleEvent.harmful(
                        "horse_bolts",
                        EventTag.MOBS,
                        player -> player.getVehicle() instanceof AbstractHorse,
                        MobEvents::horseBolts),
                // 253: an axolotl lies still as if dead, with nothing near it
                SimpleEvent.light(
                        "axolotl_plays_dead",
                        EventTag.MOBS,
                        player -> !living(player).isEmpty(),
                        player -> {
                            List<Axolotl> axolotls = living(player);
                            Axolotl axolotl = axolotls.get(player.getRandom().nextInt(axolotls.size()));
                            axolotl.getBrain()
                                    .setMemory(
                                            MemoryModuleType.PLAY_DEAD_TICKS,
                                            Config.event("axolotl_plays_dead").integer("dead_ticks"));
                            axolotl.setPlayingDead(true);
                        }),
                // 254: a cat leaves a bone where it sits, in the morning light
                SimpleEvent.light(
                        "cat_gift",
                        EventTag.MOBS,
                        player -> player.level().isBrightOutside()
                                && !near(player, Cat.class, "cat_gift").isEmpty(),
                        player -> {
                            List<Cat> cats = near(player, Cat.class, "cat_gift");
                            Cat cat = cats.get(player.getRandom().nextInt(cats.size()));
                            cat.playAmbientSound();
                            cat.spawnAtLocation(player.level(), new ItemStack(Items.BONE));
                        }),
                // 255: the chickens stop and look straight up
                SimpleEvent.light(
                        "chickens_look_up",
                        EventTag.MOBS,
                        player ->
                                near(player, Chicken.class, "chickens_look_up").size()
                                        >= Config.event("chickens_look_up").integer("min_animals"),
                        player -> gaze(
                                player,
                                near(player, Chicken.class, "chickens_look_up"),
                                "chickens_look_up",
                                chicken -> chicken.getEyePosition().add(0.0, 16.0, 0.0))),
                // 256: at night the cows all face the spirit's point
                SimpleEvent.light(
                        "cows_face_anchor",
                        EventTag.MOBS,
                        player -> player.level().isDarkOutside()
                                && near(player, AbstractCow.class, "cows_face_anchor")
                                                .size()
                                        >= Config.event("cows_face_anchor").integer("min_animals"),
                        player -> {
                            Vec3 anchor = Location.source(player.position());
                            gaze(
                                    player,
                                    near(player, AbstractCow.class, "cows_face_anchor"),
                                    "cows_face_anchor",
                                    cow -> new Vec3(anchor.x, cow.getEyeY(), anchor.z));
                        }),
                // 258: a villager forgets its trade for a while; the trades come back with it
                SimpleEvent.light(
                        "villager_nitwit",
                        EventTag.MOBS,
                        player -> !tradesmen(player).isEmpty(),
                        MobEvents::villagerNitwit),
                // 259: the villagers have heard something bad about the player; the gossip fades by itself
                SimpleEvent.light(
                        "villager_prices",
                        EventTag.MOBS,
                        player ->
                                !near(player, Villager.class, "villager_prices").isEmpty(),
                        player -> {
                            for (Villager villager : near(player, Villager.class, "villager_prices")) {
                                villager.getGossips()
                                        .add(
                                                player.getUUID(),
                                                GossipType.MINOR_NEGATIVE,
                                                Config.event("villager_prices").integer("amount"));
                            }
                        }),
                // 262: the bees come out of the hive and will not go back for a while
                SimpleEvent.harmful(
                        "bees_stay_out", EventTag.MOBS, player -> hive(player, "bees_stay_out") != null, player -> {
                            BlockPos pos = hive(player, "bees_stay_out");
                            if (pos != null && player.level().getBlockEntity(pos) instanceof BeehiveBlockEntity hive) {
                                hive.emptyAllLivingFromHive(
                                        null,
                                        player.level().getBlockState(pos),
                                        BeehiveBlockEntity.BeeReleaseStatus.BEE_RELEASED);
                                for (Bee bee :
                                        player.level().getEntitiesOfClass(Bee.class, new AABB(pos).inflate(3.0))) {
                                    bee.setStayOutOfHiveCountdown(
                                            Config.event("bees_stay_out").integer("stay_ticks"));
                                }
                            }
                        }),
                // 263: the fish all turn to face the player
                SimpleEvent.light(
                        "fish_face",
                        EventTag.MOBS,
                        player -> near(player, AbstractFish.class, "fish_face").size()
                                >= Config.event("fish_face").integer("min_fish"),
                        player -> gaze(
                                player,
                                near(player, AbstractFish.class, "fish_face"),
                                "fish_face",
                                fish -> player.getEyePosition())),
                // 264: the snow golem drips and melts away on a cold day
                SimpleEvent.harmful(
                        "snowman_melts",
                        EventTag.MOBS,
                        player ->
                                !near(player, SnowGolem.class, "snowman_melts").isEmpty(),
                        MobEvents::snowmanMelts));
    }

    private static void parrotEchoes(ServerPlayer player) {
        List<Parrot> parrots = near(player, Parrot.class, "parrot_echo");
        Parrot parrot = parrots.get(player.getRandom().nextInt(parrots.size()));
        SoundEvent sound = player.getRandom().nextBoolean() ? HauntSounds.WHISPER : SoundEvents.PLAYER_HURT;
        Config.Tone tone = Config.event("parrot_echo").tone("sound");
        player.connection.send(new ClientboundSoundEntityPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                SoundSource.NEUTRAL,
                parrot,
                tone.volume(),
                tone.pitch(),
                player.getRandom().nextLong()));
    }

    private static List<Allay> handsFree(ServerPlayer player) {
        return near(player, Allay.class, "allay_bone").stream()
                .filter(a -> a.isAlive() && !a.hasItemInHand())
                .toList();
    }

    /** The allay flies to the player with a bone and drops it there; with no player in reach the bone is taken back. */
    private static void allayBrings(ServerPlayer player) {
        List<Allay> allays = handsFree(player);
        Allay allay = allays.get(player.getRandom().nextInt(allays.size()));
        Config.Section config = Config.event("allay_bone");
        int interval = config.integer("interval_ticks");
        allay.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BONE));
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            if (!allay.isAlive() || !allay.getMainHandItem().is(Items.BONE)) {
                return false;
            }
            double reach = config.num("drop_reach");
            if (allay.distanceToSqr(p) <= reach * reach) {
                allay.spawnAtLocation(p.level(), allay.getMainHandItem().copy());
                allay.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                Config.Tone tone = config.tone("sound");
                p.level()
                        .playSound(
                                null,
                                allay,
                                SoundEvents.ALLAY_ITEM_GIVEN,
                                SoundSource.NEUTRAL,
                                tone.volume(),
                                tone.pitch());
                return false;
            }
            allay.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(p, (float) config.num("speed"), 2));
            return n * interval < config.integer("ticks");
        });
        Restore.after(player.level(), config.integer("ticks") + interval, force -> {
            if (allay.isAlive() && allay.getMainHandItem().is(Items.BONE)) {
                allay.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
            return true;
        });
    }

    private static List<Fox> thieves(ServerPlayer player) {
        return near(player, Fox.class, "fox_snatch").stream()
                .filter(f -> f.isAlive()
                        && !f.isBaby()
                        && !f.isSleeping()
                        && f.getMainHandItem().isEmpty())
                .toList();
    }

    /** The fox runs at the player; in reach it takes the held stack into its mouth and drops it again later. */
    private static void foxSnatches(ServerPlayer player) {
        List<Fox> foxes = thieves(player);
        Fox fox = foxes.get(player.getRandom().nextInt(foxes.size()));
        Config.Section config = Config.event("fox_snatch");
        int interval = config.integer("interval_ticks");
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            if (!fox.isAlive() || !fox.getMainHandItem().isEmpty()) {
                return false;
            }
            double reach = config.num("reach");
            if (fox.distanceToSqr(p) <= reach * reach) {
                ItemStack held = p.getMainHandItem();
                if (held.isEmpty()) {
                    return false;
                }
                fox.setItemSlot(EquipmentSlot.MAINHAND, held.copy());
                fox.setDropChance(EquipmentSlot.MAINHAND, 1.0F);
                p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                Config.Tone tone = config.tone("sound");
                p.level().playSound(null, fox, SoundEvents.FOX_BITE, SoundSource.NEUTRAL, tone.volume(), tone.pitch());
                Restore.after(p.level(), config.integer("hold_ticks"), force -> {
                    if (fox.isAlive() && !fox.getMainHandItem().isEmpty()) {
                        fox.spawnAtLocation(p.level(), fox.getMainHandItem().copy());
                        fox.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    }
                    return true;
                });
                return false;
            }
            fox.getNavigation().moveTo(p, config.num("speed"));
            return n * interval < config.integer("ticks");
        });
    }

    /** Not baby, not rolled up yet. */
    private static List<Armadillo> uncurled(ServerPlayer player) {
        return near(player, Armadillo.class, "armadillo_curls").stream()
                .filter(a -> a.isAlive() && !a.isBaby() && !a.isScared())
                .toList();
    }

    /** The armadillo nearest the spirit's point rolls up and is kept rolled for the hold time. */
    private static void armadilloCurls(ServerPlayer player) {
        Vec3 spirit = Location.source(player.position());
        Armadillo armadillo = uncurled(player).stream()
                .min(Comparator.comparingDouble(a -> a.distanceToSqr(spirit)))
                .orElse(null);
        if (armadillo == null) {
            return;
        }
        Config.Section config = Config.event("armadillo_curls");
        int interval = config.integer("interval_ticks");
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            if (!armadillo.isAlive()) {
                return false;
            }
            if (!armadillo.isScared()) {
                armadillo.rollUp();
            }
            return n * interval < config.integer("hold_ticks");
        });
        Restore.after(player.level(), config.integer("hold_ticks") + interval, force -> {
            if (armadillo.isAlive() && armadillo.isScared()) {
                armadillo.rollOut();
            }
            return true;
        });
    }

    private static List<Sheep> blackenable(ServerPlayer player) {
        return near(player, Sheep.class, "sheep_blacken").stream()
                .filter(s -> s.isAlive() && s.getColor() != DyeColor.BLACK)
                .toList();
    }

    /** One sheep at a time turns black; when the time is up they all get their own colour back. */
    private static void sheepBlacken(ServerPlayer player) {
        List<Sheep> sheep = new ArrayList<>(blackenable(player));
        Collections.shuffle(sheep, new java.util.Random(player.getRandom().nextLong()));
        Config.Section config = Config.event("sheep_blacken");
        int interval = config.integer("interval_ticks");
        List<Sheep> picked = sheep.subList(0, Math.min(sheep.size(), config.integer("max_sheep")));
        Map<Sheep, DyeColor> was = new LinkedHashMap<>();
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            Sheep next = picked.get(n);
            if (next.isAlive() && next.getColor() != DyeColor.BLACK) {
                was.put(next, next.getColor());
                next.setColor(DyeColor.BLACK);
            }
            return n + 1 < picked.size();
        });
        Restore.revert(player.level(), picked.size() * interval + config.integer("hold_ticks"), force -> {
            was.forEach((s, color) -> {
                if (!s.isRemoved() && s.getColor() == DyeColor.BLACK) {
                    s.setColor(color);
                }
            });
            return true;
        });
    }

    /** The horse rears, throws the rider and runs toward the spirit's point (or straight ahead if there is none). */
    private static void horseBolts(ServerPlayer player) {
        if (!(player.getVehicle() instanceof AbstractHorse horse)) {
            return;
        }
        Config.Section config = Config.event("horse_bolts");
        Vec3 anchor = Location.source(player.position());
        horse.ejectPassengers();
        horse.setStanding(config.integer("rear_ticks"));
        Config.Tone tone = config.tone("sound");
        player.level()
                .playSound(null, horse, SoundEvents.HORSE_ANGRY, SoundSource.NEUTRAL, tone.volume(), tone.pitch());
        int interval = config.integer("interval_ticks");
        Scheduler.repeat(player, config.integer("rear_ticks"), interval, (p, n) -> {
            if (!horse.isAlive()) {
                return false;
            }
            Vec3 way = anchor.subtract(horse.position()).multiply(1, 0, 1);
            if (way.lengthSqr() < 1.0) {
                way = horse.getLookAngle().multiply(1, 0, 1);
            }
            Vec3 to = horse.position().add(way.normalize().scale(config.num("distance")));
            horse.getNavigation().moveTo(to.x, to.y, to.z, config.num("speed"));
            return n * interval < config.integer("run_ticks");
        });
    }

    /** In water and not already lying still. */
    private static List<Axolotl> living(ServerPlayer player) {
        return near(player, Axolotl.class, "axolotl_plays_dead").stream()
                .filter(a -> a.isAlive() && a.isInWater() && !a.isPlayingDead())
                .toList();
    }

    /** Villagers with a trade to forget. */
    private static List<Villager> tradesmen(ServerPlayer player) {
        return near(player, Villager.class, "villager_nitwit").stream()
                .filter(v -> v.isAlive()
                        && !v.isBaby()
                        && !v.getVillagerData().profession().is(VillagerProfession.NONE)
                        && !v.getVillagerData().profession().is(VillagerProfession.NITWIT))
                .toList();
    }

    private static void villagerNitwit(ServerPlayer player) {
        List<Villager> villagers = tradesmen(player);
        Villager villager = villagers.get(player.getRandom().nextInt(villagers.size()));
        VillagerData data = villager.getVillagerData();
        MerchantOffers offers = villager.getOffers();
        villager.setVillagerData(data.withProfession(player.level().registryAccess(), VillagerProfession.NITWIT));
        Restore.revert(player.level(), Config.event("villager_nitwit").integer("restore_ticks"), force -> {
            if (!villager.isRemoved()) {
                villager.setVillagerData(data);
                villager.setOffers(offers);
            }
            return true;
        });
    }

    /** Drip particles on the golem and a point of fire damage every pulse until it is gone. */
    private static void snowmanMelts(ServerPlayer player) {
        List<SnowGolem> golems = near(player, SnowGolem.class, "snowman_melts");
        SnowGolem golem = golems.get(player.getRandom().nextInt(golems.size()));
        Config.Section config = Config.event("snowman_melts");
        int interval = config.integer("interval_ticks");
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            if (!golem.isAlive()) {
                return false;
            }
            ServerLevel level = p.level();
            level.sendParticles(
                    ParticleTypes.DRIPPING_WATER,
                    golem.getX(),
                    golem.getY() + 1.0,
                    golem.getZ(),
                    8,
                    0.2,
                    0.4,
                    0.2,
                    0.0);
            golem.hurtServer(level, golem.damageSources().onFire(), (float) config.num("damage"));
            return n * interval < config.integer("max_ticks");
        });
    }

    private static List<Enderman> takers(ServerPlayer player) {
        return near(player, Enderman.class, "enderman_takes").stream()
                .filter(e -> e.getCarriedBlock() == null)
                .toList();
    }

    /** Exposed blocks that look placed by hand, not part of the land. */
    private static List<BlockPos> builds(ServerPlayer player) {
        ServerLevel level = player.level();
        return Nearby.all(
                        player,
                        "enderman_takes",
                        s -> !s.hasBlockEntity()
                                && (s.is(BlockTags.PLANKS)
                                        || s.is(BlockTags.WOOL)
                                        || s.is(BlockTags.STONE_BRICKS)
                                        || s.is(Blocks.BRICKS)
                                        || s.is(Blocks.BOOKSHELF)))
                .stream()
                .filter(pos -> java.util.Arrays.stream(Direction.values())
                        .anyMatch(d -> level.getBlockState(pos.relative(d)).isAir()))
                .toList();
    }

    /** Adult goats far enough from the player to get a run-up. */
    private static List<Goat> goats(ServerPlayer player) {
        double run = Config.event("goat_rams").num("min_run");
        return near(player, Goat.class, "goat_rams").stream()
                .filter(g -> !g.isBaby() && g.distanceToSqr(player) >= run * run)
                .toList();
    }

    private static List<AbstractBoat> boats(ServerPlayer player) {
        return near(player, AbstractBoat.class, "boat_drifts").stream()
                .filter(b -> !b.isVehicle()
                        && b.level().getFluidState(b.blockPosition()).is(FluidTags.WATER))
                .toList();
    }

    private static List<AbstractMinecart> carts(ServerPlayer player) {
        return near(player, AbstractMinecart.class, "minecart_rolls").stream()
                .filter(c -> !c.isVehicle() && BaseRailBlock.isRail(c.level().getBlockState(c.blockPosition())))
                .toList();
    }

    /**
     * A steady horizontal shove along {@code way} for the event's push time; stops early if the vehicle gets a rider.
     */
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
        return near(player, IronGolem.class, "golem_flower").stream()
                .filter(g -> g.getTarget() == null && g.getOfferFlowerTick() == 0)
                .toList();
    }

    private static @Nullable IronGolem hostileGolem(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator() || player.level().getDifficulty() == Difficulty.PEACEFUL) {
            return null;
        }
        double radius = Nearby.radius("golem_rage");
        return near(player, IronGolem.class, "golem_rage").stream()
                .filter(g -> g.isAlive()
                        && !g.isNoAi()
                        && g.getTarget() == null
                        && g.canAttack(player)
                        && g.distanceToSqr(player) <= radius * radius)
                .min(Comparator.comparingDouble(g -> g.distanceToSqr(player)))
                .orElse(null);
    }

    private static List<Wolf> hostileWolves(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator() || player.level().getDifficulty() == Difficulty.PEACEFUL) {
            return List.of();
        }
        double radius = Nearby.radius("wolf_rage");
        return near(player, Wolf.class, "wolf_rage").stream()
                .filter(w -> !w.isTame()
                        && w.isAlive()
                        && !w.isNoAi()
                        && w.getTarget() == null
                        && w.canAttack(player)
                        && w.distanceToSqr(player) <= radius * radius)
                .toList();
    }

    private static List<Wolf> standingDogs(ServerPlayer player) {
        return near(player, Wolf.class, "dog_growls").stream()
                .filter(w -> w.isTame() && !w.isOrderedToSit() && !w.isInSittingPose())
                .toList();
    }

    private static void growl(ServerPlayer player) {
        List<Wolf> dogs = standingDogs(player);
        if (dogs.isEmpty()) {
            return;
        }
        Wolf dog = dogs.get(player.getRandom().nextInt(dogs.size()));
        Config.Section config = Config.event("dog_growls");
        Vec3 empty =
                Nearby.around(player, config.num("distance"), player.getRandom().nextDouble() * Nearby.FULL_TURN);
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
                dog.level()
                        .playSound(
                                null,
                                dog.blockPosition(),
                                SoundEvents.WOLF_SOUNDS
                                        .get(WolfSoundVariants.SoundSet.CLASSIC)
                                        .adultSounds()
                                        .growlSound()
                                        .value(),
                                SoundSource.NEUTRAL,
                                tone.volume(),
                                tone.pitch());
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
        List<Animal> farm = near(player, Animal.class, "pen_corner").stream()
                .filter(a -> !(a instanceof TamableAnimal) && !a.isLeashed())
                .toList();
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
            List<Animal> in = farm.stream()
                    .filter(a -> a.getBlockX() >= minX
                            && a.getBlockX() <= maxX
                            && a.getBlockZ() >= minZ
                            && a.getBlockZ() <= maxZ
                            && Math.abs(a.getBlockY() - at.getY()) <= 1)
                    .toList();
            if (in.size() >= config.integer("min_animals")) {
                return new Pen(minX, minZ, maxX, maxZ, at.getY(), in);
            }
        }
        return null;
    }

    /**
     * Blocks from {@code from} to a fence, wall or gate along {@code d}, or -1 if something else or nothing comes
     * first.
     */
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
        Vec3 corner =
                new Vec3((east ? pen.maxX() : pen.minX()) + 0.5, pen.y(), (south ? pen.maxZ() : pen.minZ()) + 0.5);
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

    private static @Nullable BlockPos hive(ServerPlayer player, String event) {
        return Nearby.all(player, event, s -> s.is(BlockTags.BEEHIVES)).stream()
                .filter(pos -> player.level().getBlockEntity(pos) instanceof BeehiveBlockEntity hive && !hive.isEmpty())
                .findFirst()
                .orElse(null);
    }

    /** Mobs tied to a fence post. */
    private static List<Mob> leashed(ServerPlayer player) {
        return near(player, Mob.class, "leash_snaps").stream()
                .filter(m -> m.getLeashHolder() instanceof LeashFenceKnotEntity)
                .toList();
    }

    private static <T extends Entity> List<T> near(ServerPlayer player, Class<T> type, String event) {
        return player.level().getEntitiesOfClass(type, new AABB(player.blockPosition()).inflate(Nearby.radius(event)));
    }

    private static List<Cat> fleeing(ServerPlayer player) {
        return near(player, Cat.class, "cat_flee").stream()
                .filter(c -> !c.isInSittingPose())
                .toList();
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
        gaze(player, mobs, event, mob -> player.getEyePosition());
    }

    /** The mobs stop and keep looking at the point {@code target} gives for each, for the event's stare time. */
    private static void gaze(ServerPlayer player, List<? extends Mob> mobs, String event, Function<Mob, Vec3> target) {
        Config.Section config = Config.event(event);
        int interval = config.integer("look_interval_ticks");
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            for (Mob mob : mobs) {
                if (mob.isAlive()) {
                    mob.getNavigation().stop();
                    mob.lookAt(EntityAnchorArgument.Anchor.EYES, target.apply(mob));
                }
            }
            return n * interval < config.integer("stare_ticks");
        });
    }

    private static List<TamableAnimal> pets(ServerPlayer player) {
        return near(player, TamableAnimal.class, "wild_pet").stream()
                .filter(p -> p.isTame() && p.isOwnedBy(player))
                .toList();
    }

    /** A closed fence gate with a farm animal right next to it. */
    private static @Nullable BlockPos penGate(ServerPlayer player) {
        for (BlockPos pos : Nearby.all(player, "open_pen", MobEvents::closedGate)) {
            if (!player.level()
                    .getEntitiesOfClass(
                            Animal.class,
                            new AABB(pos).inflate(Config.event("open_pen").integer("animal_radius")))
                    .isEmpty()) {
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
        return spot(
                player,
                Config.event("bats").num("distance"),
                player.getRandom().nextDouble() * Nearby.FULL_TURN,
                false);
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
