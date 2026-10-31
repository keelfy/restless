package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Location;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** More animals and monsters doing something that has no cause: calling, walking off, staring, hanging, going still. */
final class Creatures {
    private Creatures() {}

    static List<HauntEvent> all() {
        return List.of(
                // 265: the frogs croak one after another, then keep quiet for a while
                SimpleEvent.light(
                        "frogs_croak",
                        EventTag.MOBS,
                        player -> frogs(player).size()
                                >= Config.event("frogs_croak").integer("min_frogs"),
                        Creatures::croak),
                // 266: squid ink drifts up in the water, with no squid
                SimpleEvent.light("ink_cloud", EventTag.WATER, player -> inkSpot(player) != null, Creatures::ink),
                // 268: the rabbits bolt away from the player
                SimpleEvent.light(
                        "rabbits_stream",
                        EventTag.MOBS,
                        player -> !near(player, Rabbit.class, "rabbits_stream").isEmpty(),
                        player -> flee(player, near(player, Rabbit.class, "rabbits_stream"), "rabbits_stream", false)),
                // 269: a witch laughs out of sight, then a splash of darkness hits the player
                SimpleEvent.harmful(
                        "witch_giggles",
                        EventTag.MOBS,
                        player -> !hidden(player).isEmpty(),
                        Creatures::giggle),
                // 270: hostile mobs drop their target and walk away, as if something worse stood behind the player
                SimpleEvent.light(
                        "hostiles_retreat",
                        EventTag.MOBS,
                        player -> !hostiles(player).isEmpty(),
                        player -> flee(player, hostiles(player), "hostiles_retreat", true)),
                // 271: a creeper next to the player stops swelling and walks off
                SimpleEvent.light("creeper_calms", EventTag.MOBS, player -> creeper(player) != null, player -> {
                    Creeper creeper = creeper(player);
                    if (creeper != null) {
                        flee(player, List.of(creeper), "creeper_calms", true);
                    }
                }),
                // 273: the endermen stare at the player without anger and step away when looked at
                SimpleEvent.light(
                        "endermen_stare", EventTag.MOBS, player -> !calm(player).isEmpty(), Creatures::stareDown),
                // 274: a llama spits at empty air, away from the player
                SimpleEvent.light(
                        "llama_spits",
                        EventTag.MOBS,
                        player -> !near(player, Llama.class, "llama_spits").isEmpty(),
                        Creatures::spit),
                // 275: a screaming goat far off in the mountains at night
                SimpleEvent.light(
                        "goat_screams",
                        EventTag.SOUND,
                        player -> player.level().dimension() == Level.OVERWORLD
                                && player.level().isDarkOutside()
                                && player.getY() >= Config.event("goat_screams").num("min_y"),
                        player -> {
                            Config.Section config = Config.event("goat_screams");
                            Nearby.sound(
                                    player,
                                    SoundEvents.GOAT_SCREAMING_AMBIENT,
                                    Nearby.source(player, config.num("distance")),
                                    config.tone("sound"));
                        }),
                // 276: bats hang from the ceiling of a dark room, and are gone before long
                SimpleEvent.light("bats_hang", EventTag.MOBS, player -> roost(player, null) != null, Creatures::hang),
                // 277: a sniffer digs up a head or a bone
                SimpleEvent.light(
                        "sniffer_digs_up",
                        EventTag.MOBS,
                        player -> !sniffers(player).isEmpty(),
                        Creatures::dig),
                // 279: the player's cat leaves home and is found sitting at the spirit's point
                SimpleEvent.harmful(
                        "cat_at_anchor", EventTag.MOBS, player -> !cats(player).isEmpty(), Creatures::carry),
                // 280: a gust bursts in the room and shoves loose items
                SimpleEvent.light(
                        "wind_burst",
                        EventTag.PRESENCE,
                        player -> !player.level().canSeeSky(player.blockPosition()),
                        Creatures::gust),
                // 281: everything near goes still and silent for a while
                SimpleEvent.harmful(
                        "mobs_go_still",
                        EventTag.MOBS,
                        player -> !stillable(player).isEmpty(),
                        player -> hush(
                                player.level(),
                                stillable(player),
                                true,
                                Config.event("mobs_go_still").integer("still_ticks"))));
    }

    private static <T extends Entity> List<T> near(ServerPlayer player, Class<T> type, String event) {
        return player.level().getEntitiesOfClass(type, new AABB(player.blockPosition()).inflate(Nearby.radius(event)));
    }

    private static <T> T any(ServerPlayer player, List<T> list) {
        return list.get(player.getRandom().nextInt(list.size()));
    }

    private static void sound(Entity at, SoundEvent sound, SoundSource source, Config.Tone tone) {
        at.level().playSound(null, at, sound, source, tone.volume(), tone.pitch());
    }

    /** The mobs go silent (and, if {@code still}, stop thinking) and are put back after {@code ticks}. */
    private static void hush(ServerLevel level, List<? extends Mob> mobs, boolean still, int ticks) {
        for (Mob mob : mobs) {
            mob.setSilent(true);
            if (still) {
                mob.getNavigation().stop();
                mob.setDeltaMovement(0.0, mob.getDeltaMovement().y, 0.0);
                mob.setNoAi(true);
            }
        }
        Restore.after(level, ticks, force -> {
            for (Mob mob : mobs) {
                mob.setSilent(false);
                if (still) {
                    mob.setNoAi(false);
                }
            }
            return true;
        });
    }

    /** Every pulse the mobs head for a point on the far side of themselves from the player. */
    private static void flee(ServerPlayer player, List<? extends Mob> mobs, String event, boolean calm) {
        Config.Section config = Config.event(event);
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            for (Mob mob : mobs) {
                if (!mob.isAlive()) {
                    continue;
                }
                if (calm) {
                    mob.setTarget(null);
                    if (mob instanceof Creeper creeper) {
                        creeper.setSwellDir(-1);
                    }
                }
                Vec3 away = mob.position().subtract(p.position()).multiply(1, 0, 1);
                if (away.lengthSqr() > 1.0E-4) {
                    Vec3 to = mob.position().add(away.normalize().scale(config.num("distance")));
                    mob.getNavigation().moveTo(to.x, to.y, to.z, config.num("speed"));
                }
            }
            return n + 1 < config.integer("pulses");
        });
    }

    private static List<Frog> frogs(ServerPlayer player) {
        return near(player, Frog.class, "frogs_croak").stream()
                .filter(f -> f.isAlive() && !f.isSilent())
                .toList();
    }

    private static void croak(ServerPlayer player) {
        Config.Section config = Config.event("frogs_croak");
        List<Frog> frogs = new ArrayList<>(frogs(player));
        Collections.shuffle(frogs, new Random(player.getRandom().nextLong()));
        frogs = frogs.subList(0, Math.min(frogs.size(), config.integer("max_frogs")));
        int stagger = config.integer("stagger_ticks");
        Config.Tone tone = config.tone("sound");
        for (int i = 0; i < frogs.size(); i++) {
            Frog frog = frogs.get(i);
            Scheduler.later(player, i * stagger, p -> {
                if (frog.isAlive()) {
                    sound(frog, SoundEvents.FROG_AMBIENT, SoundSource.NEUTRAL, tone);
                }
            });
        }
        hush(player.level(), frogs, false, frogs.size() * stagger + config.integer("silent_ticks"));
    }

    private static @Nullable BlockPos inkSpot(ServerPlayer player) {
        ServerLevel level = player.level();
        List<BlockPos> spots = Nearby.all(player, "ink_cloud", s -> s.is(Blocks.WATER)).stream()
                .filter(pos -> level.getFluidState(pos.above()).is(FluidTags.WATER)
                        && level.getFluidState(pos.below()).is(FluidTags.WATER))
                .toList();
        return spots.isEmpty() ? null : any(player, spots);
    }

    private static void ink(ServerPlayer player) {
        BlockPos spot = inkSpot(player);
        if (spot == null) {
            return;
        }
        Config.Section config = Config.event("ink_cloud");
        Config.Burst ink = config.burst("particles");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            p.level()
                    .sendParticles(
                            ParticleTypes.SQUID_INK,
                            spot.getX() + 0.5,
                            spot.getY() + ink.offsetY(),
                            spot.getZ() + 0.5,
                            ink.count(),
                            ink.dx(),
                            ink.dy(),
                            ink.dz(),
                            ink.speed());
            return n + 1 < config.integer("pulses");
        });
    }

    /** Witches out of the player's sight, far enough that it is a sound and not a fight. */
    private static List<Witch> hidden(ServerPlayer player) {
        double min = Config.event("witch_giggles").num("min_distance");
        return near(player, Witch.class, "witch_giggles").stream()
                .filter(w -> w.isAlive() && w.distanceToSqr(player) >= min * min && !player.hasLineOfSight(w))
                .toList();
    }

    private static void giggle(ServerPlayer player) {
        List<Witch> witches = hidden(player);
        if (witches.isEmpty()) {
            return;
        }
        Config.Section config = Config.event("witch_giggles");
        sound(any(player, witches), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, config.tone("sound"));
        Scheduler.later(player, config.integer("delay_ticks"), p -> {
            Config.Burst splash = config.burst("particles");
            Config.Tone tone = config.tone("splash_sound");
            p.level()
                    .sendParticles(
                            ParticleTypes.WITCH,
                            p.getX(),
                            p.getY() + splash.offsetY(),
                            p.getZ(),
                            splash.count(),
                            splash.dx(),
                            splash.dy(),
                            splash.dz(),
                            splash.speed());
            p.level()
                    .playSound(
                            null,
                            p.getX(),
                            p.getY(),
                            p.getZ(),
                            SoundEvents.SPLASH_POTION_BREAK,
                            SoundSource.HOSTILE,
                            tone.volume(),
                            tone.pitch());
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, config.integer("darkness_ticks"), 0), null);
        });
    }

    private static List<Monster> hostiles(ServerPlayer player) {
        return near(player, Monster.class, "hostiles_retreat").stream()
                .filter(m -> m.isAlive() && !m.isNoAi())
                .toList();
    }

    private static @Nullable Creeper creeper(ServerPlayer player) {
        double reach = Config.event("creeper_calms").num("radius");
        return near(player, Creeper.class, "creeper_calms").stream()
                .filter(c -> c.isAlive() && !c.isNoAi() && c.distanceToSqr(player) <= reach * reach)
                .min(Comparator.comparingDouble(c -> c.distanceToSqr(player)))
                .orElse(null);
    }

    /** Endermen that are not after anyone. */
    private static List<Enderman> calm(ServerPlayer player) {
        return near(player, Enderman.class, "endermen_stare").stream()
                .filter(e -> e.isAlive() && !e.isNoAi() && e.getTarget() == null)
                .toList();
    }

    /** Each one faces the player and keeps its anger down; one the player looks at steps away. */
    private static void stareDown(ServerPlayer player) {
        Config.Section config = Config.event("endermen_stare");
        List<Enderman> men = calm(player);
        int interval = config.integer("interval_ticks");
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            for (Enderman man : men) {
                if (!man.isAlive()) {
                    continue;
                }
                man.setTarget(null);
                man.getNavigation().stop();
                man.lookAt(EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
                Vec3 toMan = man.getEyePosition().subtract(p.getEyePosition()).normalize();
                if (p.getViewVector(1.0F).dot(toMan) > config.num("look_dot") && p.hasLineOfSight(man)) {
                    hop(p, man, config);
                }
            }
            return n * interval < config.integer("ticks");
        });
    }

    private static void hop(ServerPlayer player, Enderman man, Config.Section config) {
        ServerLevel level = player.level();
        BlockPos spot = Place.near(
                player,
                man.blockPosition(),
                config.integer("min_hop"),
                config.integer("max_hop"),
                pos -> Place.floored(level, pos) && level.isEmptyBlock(pos.above()));
        if (spot != null) {
            sound(man, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, config.tone("sound"));
            man.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
            sound(man, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, config.tone("sound"));
        }
    }

    private static void spit(ServerPlayer player) {
        List<Llama> llamas = near(player, Llama.class, "llama_spits");
        if (llamas.isEmpty()) {
            return;
        }
        Config.Section config = Config.event("llama_spits");
        Llama llama = any(player, llamas);
        ServerLevel level = player.level();
        // Anywhere but toward the player: the spit hurts.
        Vec3 toPlayer = player.position().subtract(llama.position());
        double yaw = Math.atan2(toPlayer.z, toPlayer.x)
                + Math.toRadians(90.0 + player.getRandom().nextDouble() * 180.0);
        double dx = Math.cos(yaw);
        double dz = Math.sin(yaw);
        llama.getNavigation().stop();
        llama.lookAt(EntityAnchorArgument.Anchor.EYES, llama.getEyePosition().add(dx, 0.0, dz));
        Projectile.spawnProjectileUsingShoot(
                new LlamaSpit(level, llama),
                level,
                ItemStack.EMPTY,
                dx,
                config.num("lift"),
                dz,
                (float) config.num("power"),
                0.0F);
        sound(llama, SoundEvents.LLAMA_SPIT, SoundSource.NEUTRAL, config.tone("sound"));
    }

    /** Free air under a solid block, dark and out of the sky's view; around the player or around {@code near}. */
    private static @Nullable BlockPos roost(ServerPlayer player, @Nullable BlockPos near) {
        Config.Section config = Config.event("bats_hang");
        ServerLevel level = player.level();
        return Place.near(
                player,
                near == null ? player.blockPosition() : near,
                near == null ? config.integer("min_distance") : 0,
                near == null ? config.integer("max_distance") : config.integer("spread"),
                pos -> level.isLoaded(pos)
                        && level.isEmptyBlock(pos)
                        && level.getBlockState(pos.above()).isSolidRender()
                        && !level.canSeeSky(pos)
                        && level.getMaxLocalRawBrightness(pos) <= config.integer("max_light"));
    }

    private static void hang(ServerPlayer player) {
        BlockPos first = roost(player, null);
        if (first == null) {
            return;
        }
        Config.Section config = Config.event("bats_hang");
        ServerLevel level = player.level();
        List<Bat> bats = new ArrayList<>();
        for (int i = 0; i < config.integer("count"); i++) {
            BlockPos spot = i == 0 ? first : roost(player, first);
            Bat bat = spot == null ? null : EntityTypes.BAT.spawn(level, spot, EntitySpawnReason.EVENT);
            if (bat != null) {
                bat.setResting(true);
                bats.add(bat);
            }
        }
        Restore.after(level, config.integer("lifetime_ticks"), force -> {
            bats.forEach(Bat::discard);
            return true;
        });
    }

    private static List<Sniffer> sniffers(ServerPlayer player) {
        return near(player, Sniffer.class, "sniffer_digs_up").stream()
                .filter(s -> s.isAlive() && !s.isBaby())
                .toList();
    }

    private static void dig(ServerPlayer player) {
        List<Sniffer> sniffers = sniffers(player);
        if (sniffers.isEmpty()) {
            return;
        }
        Config.Section config = Config.event("sniffer_digs_up");
        Sniffer sniffer = any(player, sniffers);
        ServerLevel level = player.level();
        ItemStack found = new ItemStack(BuiltInRegistries.ITEM
                .getOptional(Identifier.parse(any(player, config.texts("items"))))
                .orElse(Items.BONE));
        if (found.is(Items.PLAYER_HEAD)) {
            found.set(DataComponents.PROFILE, ResolvableProfile.createResolved(player.getGameProfile()));
        }
        double yaw = Math.toRadians(sniffer.getYRot());
        ItemEntity item = new ItemEntity(
                level,
                sniffer.getX() - Math.sin(yaw) * config.num("reach"),
                sniffer.getY() + config.num("height"),
                sniffer.getZ() + Math.cos(yaw) * config.num("reach"),
                found);
        level.addFreshEntity(item);
        sound(sniffer, SoundEvents.SNIFFER_DROP_SEED, SoundSource.NEUTRAL, config.tone("sound"));
        // Whatever it dug up is not meant to stay.
        Restore.after(level, config.integer("vanish_ticks"), force -> {
            item.discard();
            return true;
        });
    }

    /** The player's own cats, sitting or not, that nobody has tied up. */
    private static List<Cat> cats(ServerPlayer player) {
        return near(player, Cat.class, "cat_at_anchor").stream()
                .filter(c -> c.isAlive() && c.isTame() && c.isOwnedBy(player) && !c.isLeashed())
                .toList();
    }

    private static void carry(ServerPlayer player) {
        List<Cat> cats = cats(player);
        if (cats.isEmpty()) {
            return;
        }
        Config.Section config = Config.event("cat_at_anchor");
        Cat cat = any(player, cats);
        ServerLevel level = player.level();
        BlockPos spot = Place.standing(
                level, BlockPos.containing(Location.source(Nearby.randomAround(player, config.num("distance")))));
        if (spot == null) {
            spot = Place.spawnSpot(player, config.integer("min_distance"), config.integer("max_distance"));
        }
        if (spot != null) {
            cat.getNavigation().stop();
            cat.setOrderedToSit(true);
            cat.setInSittingPose(true);
            cat.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
        }
    }

    private static void gust(ServerPlayer player) {
        Config.Section config = Config.event("wind_burst");
        ServerLevel level = player.level();
        Vec3 at = Nearby.randomAround(player, config.num("distance"));
        level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        Config.Tone tone = config.tone("sound");
        level.playSound(
                null,
                at.x,
                at.y,
                at.z,
                SoundEvents.WIND_CHARGE_BURST.value(),
                SoundSource.NEUTRAL,
                tone.volume(),
                tone.pitch());
        for (ItemEntity item : level.getEntitiesOfClass(
                ItemEntity.class, new AABB(BlockPos.containing(at)).inflate(config.integer("item_radius")))) {
            Vec3 push = item.position()
                    .subtract(at)
                    .multiply(1, 0, 1)
                    .normalize()
                    .scale(config.num("power"))
                    .add(0.0, config.num("lift"), 0.0);
            item.setDeltaMovement(item.getDeltaMovement().add(push));
        }
    }

    /** Mobs that are free to stop: nobody rides them or sits on them, and they are not already quiet or frozen. */
    private static List<Mob> stillable(ServerPlayer player) {
        return near(player, Mob.class, "mobs_go_still").stream()
                .filter(m -> m.isAlive() && !m.isNoAi() && !m.isSilent() && !m.isVehicle() && !m.isPassenger())
                .toList();
    }
}
