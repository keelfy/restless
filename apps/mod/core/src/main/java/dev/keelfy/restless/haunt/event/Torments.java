package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Band;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.VignettePayload;
import dev.keelfy.restless.platform.Platform;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Threat-band assaults: they leave the player at {@code assaults.safe_health} (see docs/assaults.md). Each warns first
 * and has a way out; all damage goes through {@link #hurt} so it never passes the band's floor, and the mobs a spirit
 * turns are marked so their own blows are capped too.
 */
final class Torments {
    private static final String RAKE = "restless:rake";
    private static final String TEETH = "restless:teeth_in_dark";
    private static final String SEARING = "restless:searing_armor";
    private static final String FROST = "restless:frost_grip";
    private static final String PINNED = "restless:pinned";
    private static final String THROAT = "restless:throat";
    private static final String UNDERTOW = "restless:undertow_dry";
    private static final String LANTERN = "restless:lantern_burst";
    private static final String COUGH = "restless:blood_cough";
    private static final String WRONG_STEP = "restless:wrong_step";
    private static final String ASH = "restless:ash_storm";
    private static final String HAND = "restless:hand_below";
    private static final String BLADE = "restless:blade_behind";
    private static final String DOOR = "restless:door_bite";
    private static final String TERROR = "restless:night_terror";
    private static final String FISTS = "restless:stone_fists";

    private Torments() {}

    static List<HauntEvent> all() {
        return List.of(
                SimpleEvent.harmful(RAKE, EventTag.ASSAULT, player -> Assaults.healthy(player, RAKE), Torments::rake),
                SimpleEvent.harmful(
                        TEETH,
                        EventTag.ASSAULT,
                        player -> Assaults.healthy(player, TEETH) && dark(player),
                        Torments::teeth),
                SimpleEvent.harmful(
                        SEARING,
                        EventTag.AFFLICTION,
                        player -> Assaults.healthy(player, SEARING)
                                && conducts(player)
                                && !player.fireImmune()
                                && !player.isInWater()
                                && !player.hasEffect(MobEffects.FIRE_RESISTANCE),
                        Torments::searing),
                SimpleEvent.harmful(
                        FROST,
                        EventTag.AFFLICTION,
                        player -> Assaults.healthy(player, FROST) && player.canFreeze() && !warm(player),
                        Torments::frost),
                SimpleEvent.harmful(
                        PINNED,
                        EventTag.AFFLICTION,
                        player -> Assaults.healthy(player, PINNED)
                                && !player.hasEffect(MobEffects.SLOWNESS)
                                && !hostiles(player).isEmpty(),
                        Torments::pinned),
                SimpleEvent.harmful(
                        THROAT,
                        EventTag.AFFLICTION,
                        player -> Assaults.healthy(player, THROAT)
                                && !player.isInWater()
                                && !player.hasEffect(MobEffects.UNLUCK),
                        Torments::throat),
                SimpleEvent.harmful(
                        UNDERTOW,
                        EventTag.ASSAULT,
                        player -> Assaults.healthy(player, UNDERTOW) && player.isSprinting(),
                        Torments::undertow),
                SimpleEvent.harmful(
                        LANTERN,
                        EventTag.ASSAULT,
                        player -> Assaults.healthy(player, LANTERN)
                                && Nearby.any(player, LANTERN, state -> state.is(RestlessTags.BURSTABLE_LIGHTS)),
                        Torments::lantern),
                SimpleEvent.harmful(
                        COUGH,
                        EventTag.AFFLICTION,
                        player -> Assaults.healthy(player, COUGH) && !player.hasEffect(MobEffects.NAUSEA),
                        Torments::cough),
                SimpleEvent.harmful(
                        WRONG_STEP,
                        EventTag.BLOCKS,
                        player -> player.onGround() && hole(player, 1) != null,
                        Torments::wrongStep),
                SimpleEvent.harmful(
                        ASH,
                        EventTag.ASSAULT,
                        player ->
                                Assaults.healthy(player, ASH) && player.level().dimension() == Level.NETHER,
                        Torments::ash),
                SimpleEvent.harmful(
                        HAND,
                        EventTag.ASSAULT,
                        player -> Assaults.healthy(player, HAND) && player.onGround(),
                        Torments::hand),
                SimpleEvent.harmful(
                        BLADE, EventTag.ASSAULT, player -> Assaults.healthy(player, BLADE), Torments::blade),
                SimpleEvent.harmful(
                        DOOR,
                        EventTag.DOORS,
                        player -> Assaults.healthy(player, DOOR) && door(player) != null,
                        Torments::doorBite),
                SimpleEvent.harmful(
                        TERROR,
                        EventTag.ASSAULT,
                        player -> Assaults.healthy(player, TERROR)
                                && player.isSleeping()
                                && sleepLeft(player) >= Config.event(TERROR).integer("min_left_ticks"),
                        Torments::terror),
                SimpleEvent.harmful(
                        FISTS,
                        EventTag.ASSAULT,
                        player -> Assaults.healthy(player, FISTS) && wall(player) != null,
                        Torments::fists));
    }

    /** Damage that stops at the threat floor; a hit with no room left does nothing. */
    private static void hurt(ServerPlayer player, DamageSource source, double amount) {
        float allowed = Math.min((float) amount, Assaults.room(player, Band.THREAT));
        if (allowed > 0.0f) {
            player.hurtServer(player.level(), source, allowed);
        }
    }

    private static void hurt(ServerPlayer player, double amount) {
        hurt(player, Taken.by(player.level(), Taken.TAKEN), amount);
    }

    private static void vignette(ServerPlayer player, Config.Section config) {
        if (Platform.get().canReceive(player, VignettePayload.TYPE)) {
            player.connection.send(new ClientboundCustomPayloadPacket(
                    new VignettePayload((float) config.num("vignette_strength"), config.integer("vignette_ticks"))));
        }
    }

    private static void sound(ServerPlayer player, SoundEvent sound, Config.Tone tone) {
        Nearby.sound(player, sound, player.position(), tone);
    }

    private static void particles(
            ServerPlayer player, ParticleOptions type, double x, double y, double z, Config.Burst burst) {
        player.level()
                .sendParticles(
                        type,
                        x,
                        y + burst.offsetY(),
                        z,
                        burst.count(),
                        burst.dx(),
                        burst.dy(),
                        burst.dz(),
                        burst.speed());
    }

    private static void push(ServerPlayer player, double x, double y, double z) {
        player.setDeltaMovement(x, y, z);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    private static boolean dark(ServerPlayer player) {
        return player.level().getMaxLocalRawBrightness(player.blockPosition())
                <= Config.event(TEETH).integer("dark_max");
    }

    // rake: three scratches, then three hits; a player who has moved on between hits is missed
    private static void rake(ServerPlayer player) {
        Config.Section config = Config.event(RAKE);
        int scratches = config.integer("scratches");
        int total = scratches + config.integer("hits");
        double dodge = config.num("dodge_blocks");
        Vec3[] last = {player.position()};
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            if (n < scratches) {
                Nearby.sound(
                        p,
                        SoundEvents.BRUSH_GRAVEL,
                        Nearby.randomAround(p, config.num("distance")),
                        config.tone("sound"));
                last[0] = p.position();
                return true;
            }
            if (p.position().distanceToSqr(last[0]) < dodge * dodge) {
                sound(p, SoundEvents.WARDEN_ATTACK_IMPACT, config.tone("hit_sound"));
                vignette(p, config);
                hurt(p, config.num("damage"));
            }
            last[0] = p.position();
            return n + 1 < total;
        });
    }

    // teeth_in_dark: a growl, then a bite every few seconds for as long as the player stays in the dark
    private static void teeth(ServerPlayer player) {
        Config.Section config = Config.event(TEETH);
        sound(player, SoundEvents.WARDEN_ANGRY, config.tone("sound"));
        // the jaws hang just off to one side, wherever the player goes
        double side = player.getRandom().nextDouble() * Nearby.FULL_TURN;
        Haze.linger(
                player,
                Nearby.around(player, 2.0, side).with(Direction.Axis.Y, player.getY()),
                config.integer("delay_ticks"));
        Scheduler.repeat(player, config.integer("delay_ticks"), config.integer("interval_ticks"), (p, n) -> {
            if (!dark(p) || n >= config.integer("max_bites")) {
                return false;
            }
            Haze.at(p.level(), Nearby.around(p, 2.0, side).with(Direction.Axis.Y, p.getY()));
            sound(p, SoundEvents.PHANTOM_BITE, config.tone("bite_sound"));
            hurt(p, config.num("damage"));
            return true;
        });
    }

    /** Whether the chestplate can take the heat: any worn one but leather. */
    private static boolean conducts(ServerPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return !chest.isEmpty() && !chest.is(Items.LEATHER_CHESTPLATE);
    }

    // searing_armor: the chestplate glows, then burns every second until it is off or the player is in water
    private static void searing(ServerPlayer player) {
        Config.Section config = Config.event(SEARING);
        sound(player, SoundEvents.FIRECHARGE_USE, config.tone("sound"));
        Config.Burst glow = config.burst("particles");
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            if (!conducts(p) || p.isInWater() || n >= config.integer("warn_steps") + config.integer("max_steps")) {
                return false;
            }
            particles(p, ParticleTypes.FLAME, p.getX(), p.getY(), p.getZ(), glow);
            if (n >= config.integer("warn_steps")) {
                sound(p, SoundEvents.PLAYER_HURT_ON_FIRE, config.tone("hit_sound"));
                hurt(p, p.damageSources().onFire(), config.num("damage"));
            }
            return true;
        });
    }

    private static boolean warm(ServerPlayer player) {
        return player.isOnFire()
                || player.level().getBrightness(LightLayer.BLOCK, player.blockPosition())
                        >= Config.event(FROST).integer("warm_light");
    }

    // frost_grip: frozen to just short of the vanilla damage, with our own capped chill, until light or fire is near
    private static void frost(ServerPlayer player) {
        Config.Section config = Config.event(FROST);
        sound(player, SoundEvents.PLAYER_HURT_FREEZE, config.tone("sound"));
        Scheduler.repeat(player, 0, config.integer("interval_ticks"), (p, n) -> {
            if (warm(p) || n >= config.integer("max_steps") || Assaults.room(p, Band.THREAT) <= 0.0f) {
                return false;
            }
            p.setTicksFrozen(p.getTicksRequiredToFreeze() - 1);
            if (n >= config.integer("warn_steps") && n % config.integer("hurt_every") == 0) {
                sound(p, SoundEvents.PLAYER_HURT_FREEZE, config.tone("hit_sound"));
                hurt(p, p.damageSources().freeze(), config.num("damage"));
            }
            return true;
        });
    }

    private static List<Monster> hostiles(ServerPlayer player) {
        return player.level()
                .getEntitiesOfClass(
                        Monster.class,
                        player.getBoundingBox().inflate(Config.event(PINNED).num("radius")),
                        Monster::isAlive);
    }

    // pinned: heavy limbs, and every hostile close by turns on the player (marked, so its blows stop at the floor)
    private static void pinned(ServerPlayer player) {
        Config.Section config = Config.event(PINNED);
        sound(player, SoundEvents.CHAIN_HIT, config.tone("sound"));
        particles(player, ParticleTypes.SOUL, player.getX(), player.getY(), player.getZ(), config.burst("particles"));
        int ticks = config.integer("ticks");
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, config.integer("slow_amplifier")), null);
        player.addEffect(
                new MobEffectInstance(MobEffects.MINING_FATIGUE, ticks, config.integer("fatigue_amplifier")), null);
        for (Monster monster : hostiles(player)) {
            Band.THREAT.mark(monster);
            monster.setTarget(player);
        }
    }

    // throat: the air drains on dry land until the effect is gone (milk) or the player crouches and waits
    private static void throat(ServerPlayer player) {
        Config.Section config = Config.event(THROAT);
        sound(player, SoundEvents.WARDEN_NEARBY_CLOSE, config.tone("sound"));
        player.sendSystemMessage(Component.literal(config.text("whisper")), true);
        int interval = config.integer("interval_ticks");
        player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, config.integer("ticks"), 0), null);
        Scheduler.repeat(player, config.integer("delay_ticks"), interval, (p, n) -> {
            if (!p.hasEffect(MobEffects.UNLUCK) || n >= config.integer("max_steps")) {
                return false;
            }
            if (p.isCrouching() || p.isInWater()) {
                return true;
            }
            p.setAirSupply(Math.max(0, p.getAirSupply() - config.integer("drain")));
            if (p.getAirSupply() == 0 && n % config.integer("pulse_every") == 0) {
                sound(p, SoundEvents.PLAYER_HURT_DROWN, config.tone("hit_sound"));
                hurt(p, p.damageSources().drown(), config.num("damage"));
            }
            return true;
        });
    }

    // undertow_dry: dragged back while sprinting; it lets go as soon as the player stops
    private static void undertow(ServerPlayer player) {
        Config.Section config = Config.event(UNDERTOW);
        sound(player, SoundEvents.PLAYER_SPLASH, config.tone("sound"));
        int interval = config.integer("interval_ticks");
        Scheduler.repeat(player, config.integer("delay_ticks"), interval, (p, n) -> {
            if (!p.isSprinting() || n * interval >= config.integer("max_ticks")) {
                return false;
            }
            Vec3 look = p.getLookAngle();
            double flat = Math.max(Math.hypot(look.x, look.z), 1.0E-4);
            double pull = config.num("pull");
            push(p, -look.x / flat * pull, p.getDeltaMovement().y, -look.z / flat * pull);
            if (n % config.integer("hurt_every") == 0) {
                hurt(p, Taken.by(p.level(), Taken.DRAGGED_UNDER), config.num("damage"));
            }
            return true;
        });
    }

    // lantern_burst: a torch or lantern sputters, then bursts; only someone still close to it is burned
    private static void lantern(ServerPlayer player) {
        Config.Section config = Config.event(LANTERN);
        BlockPos pos = Nearby.random(player, LANTERN, state -> state.is(RestlessTags.BURSTABLE_LIGHTS));
        if (pos == null) {
            return;
        }
        Vec3 at = Vec3.atCenterOf(pos);
        int interval = config.integer("interval_ticks");
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            if (n * interval < config.integer("warn_ticks")) {
                Nearby.sound(p, SoundEvents.FIRE_AMBIENT, at, config.tone("sound"));
                particles(p, ParticleTypes.SMALL_FLAME, at.x, at.y, at.z, config.burst("warn_particles"));
                return true;
            }
            Nearby.sound(p, SoundEvents.GENERIC_EXPLODE.value(), at, config.tone("burst_sound"));
            particles(p, ParticleTypes.FLAME, at.x, at.y, at.z, config.burst("particles"));
            Swaps.show(p, pos, Blocks.AIR.defaultBlockState(), config.integer("ticks"));
            double reach = config.num("burst_radius");
            if (p.position().distanceToSqr(at) <= reach * reach) {
                hurt(p, config.num("damage"));
                if (Assaults.room(p, Band.THREAT) > config.num("ignite_room") && !p.fireImmune()) {
                    p.igniteForSeconds((float) config.num("seconds"));
                }
            }
            return false;
        });
    }

    // blood_cough: a few small hits with particles; the nausea that marks it goes with milk
    private static void cough(ServerPlayer player) {
        Config.Section config = Config.event(COUGH);
        int interval = config.integer("interval_ticks");
        int coughs = config.integer("coughs");
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, interval * coughs, 0), null);
        Scheduler.repeat(player, config.integer("delay_ticks"), interval, (p, n) -> {
            if (!p.hasEffect(MobEffects.NAUSEA) || n >= coughs) {
                return false;
            }
            sound(p, SoundEvents.PLAYER_HURT, config.tone("sound"));
            particles(p, ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY(), p.getZ(), config.burst("particles"));
            hurt(p, config.num("damage"));
            return true;
        });
    }

    /** The top of a column of {@code depth} solid blocks, with solid ground under it, one step ahead of the player. */
    private static @Nullable BlockPos hole(ServerPlayer player, int depth) {
        ServerLevel level = player.level();
        BlockPos top = player.blockPosition()
                .below()
                .relative(player.getDirection(), Config.event(WRONG_STEP).integer("ahead"));
        for (int i = 0; i < depth; i++) {
            if (!level.isLoaded(top.below(i)) || !Place.solid(level, top.below(i))) {
                return null;
            }
        }
        BlockPos bottom = top.below(depth);
        return level.isLoaded(bottom)
                        && level.getBlockState(bottom).isSolidRender()
                        && Place.open(level, top.above())
                        && Place.open(level, top.above(2))
                ? top
                : null;
    }

    // wrong_step: the ground ahead crumbles and is shown gone; in a hardcore world (see Swaps) it is gone for real
    private static void wrongStep(ServerPlayer player) {
        Config.Section config = Config.event(WRONG_STEP);
        int depth = 1 + player.getRandom().nextInt(config.integer("max_depth"));
        while (depth > 1 && hole(player, depth) == null) {
            depth--;
        }
        BlockPos top = hole(player, depth);
        if (top == null) {
            return;
        }
        BlockState ground = player.level().getBlockState(top);
        Vec3 at = Vec3.atCenterOf(top);
        Nearby.sound(player, SoundEvents.GRAVEL_BREAK, at, config.tone("sound"));
        particles(
                player,
                new BlockParticleOption(ParticleTypes.BLOCK, ground),
                at.x,
                top.getY(),
                at.z,
                config.burst("particles"));
        int cells = depth;
        Scheduler.later(player, config.integer("warn_ticks"), p -> {
            if (p.level().getBlockState(top) == ground) {
                Swaps.show(
                        p,
                        java.util.stream.IntStream.range(0, cells)
                                .mapToObj(i -> top.below(i).immutable())
                                .toList(),
                        Blocks.AIR.defaultBlockState(),
                        config.integer("ticks"));
            }
        });
    }

    // ash_storm: a wind of soul fire from one side; whoever is not out of its way is burned every second
    private static void ash(ServerPlayer player) {
        Config.Section config = Config.event(ASH);
        double angle = player.getRandom().nextDouble() * Math.PI * 2;
        Vec3 upwind = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
        sound(player, SoundEvents.PHANTOM_SWOOP, config.tone("sound"));
        int interval = config.integer("interval_ticks");
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            if (p.level().dimension() != Level.NETHER
                    || n >= config.integer("warn_steps") + config.integer("max_steps")) {
                return false;
            }
            Vec3 eye = p.getEyePosition();
            Vec3 source = eye.add(upwind.scale(config.num("shelter_reach")));
            particles(p, ParticleTypes.SOUL_FIRE_FLAME, source.x, p.getY(), source.z, config.burst("particles"));
            boolean sheltered = p.isInWater()
                    || p.level()
                                    .clip(new ClipContext(
                                            eye, source, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p))
                                    .getType()
                            != HitResult.Type.MISS;
            if (n >= config.integer("warn_steps") && !sheltered) {
                sound(p, SoundEvents.PLAYER_HURT_ON_FIRE, config.tone("hit_sound"));
                hurt(p, p.damageSources().onFire(), config.num("damage"));
                if (Assaults.room(p, Band.THREAT) > config.num("ignite_room") && !p.fireImmune()) {
                    p.igniteForSeconds((float) config.num("seconds"));
                }
            }
            return true;
        });
    }

    // hand_below: a circle of souls under the feet; stay in it for long enough and a hand takes hold
    private static void hand(ServerPlayer player) {
        Config.Section config = Config.event(HAND);
        Vec3 origin = player.position();
        double radius = config.num("radius");
        sound(player, SoundEvents.SCULK_BLOCK_SPREAD, config.tone("sound"));
        int interval = config.integer("interval_ticks");
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            if (p.position().distanceToSqr(origin) > radius * radius) {
                return false;
            }
            if (n * interval >= config.integer("stay_ticks")) {
                sound(p, SoundEvents.WARDEN_ATTACK_IMPACT, config.tone("hit_sound"));
                hurt(p, config.num("damage"));
                p.addEffect(
                        new MobEffectInstance(
                                MobEffects.SLOWNESS, config.integer("root_ticks"), config.integer("root_amplifier")),
                        null);
                push(p, 0.0, 0.0, 0.0);
                return false;
            }
            // the haze rises from the ring, thicker the longer they stay
            double rise = p.getRandom().nextDouble() * Math.PI * 2;
            Haze.at(
                    p.level(),
                    new Vec3(origin.x + Math.cos(rise) * radius, origin.y, origin.z + Math.sin(rise) * radius),
                    1 + n * interval / 10);
            int points = config.integer("ring_points");
            for (int i = 0; i < points; i++) {
                double around = i * Math.PI * 2 / points;
                particles(
                        p,
                        ParticleTypes.SOUL,
                        origin.x + Math.cos(around) * radius,
                        origin.y,
                        origin.z + Math.sin(around) * radius,
                        config.burst("particles"));
            }
            return true;
        });
    }

    // blade_behind: a sweep sounds behind the player; whoever has not turned to face the spot by then is cut
    private static void blade(ServerPlayer player) {
        Config.Section config = Config.event(BLADE);
        Vec3 spot = Nearby.around(player, config.num("distance"), 180.0);
        Nearby.sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, spot, config.tone("sound"));
        particles(player, ParticleTypes.SWEEP_ATTACK, spot.x, spot.y, spot.z, config.burst("particles"));
        Scheduler.later(player, config.integer("delay_ticks"), p -> {
            Vec3 look = p.getLookAngle();
            Vec3 toward = spot.subtract(p.getEyePosition());
            double facing = (look.x * toward.x + look.z * toward.z)
                    / Math.max(Math.hypot(look.x, look.z) * Math.hypot(toward.x, toward.z), 1.0E-4);
            if (facing < Math.cos(Math.toRadians(config.num("face_degrees")))) {
                Nearby.sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, p.position(), config.tone("hit_sound"));
                vignette(p, config);
                hurt(p, config.num("damage"));
            }
        });
    }

    private static @Nullable BlockPos door(ServerPlayer player) {
        return Nearby.random(
                player,
                DOOR,
                state -> state.getBlock() instanceof DoorBlock
                        && DoorBlock.isWoodenDoor(state)
                        && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER);
    }

    // door_bite: a door swings open by itself; whoever comes through it is caught as it slams
    private static void doorBite(ServerPlayer player) {
        Config.Section config = Config.event(DOOR);
        BlockPos pos = door(player);
        if (pos == null) {
            return;
        }
        ServerLevel level = player.level();
        BlockState state = level.getBlockState(pos);
        DoorBlock block = (DoorBlock) state.getBlock();
        block.setOpen(null, level, state, pos, true);
        Vec3 at = Vec3.atBottomCenterOf(pos);
        double reach = config.num("bite_distance");
        int interval = config.integer("interval_ticks");
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            BlockState now = p.level().getBlockState(pos);
            if (!(now.getBlock() instanceof DoorBlock)) {
                return false;
            }
            boolean waited = n * interval >= config.integer("warn_ticks");
            boolean caught = waited && p.position().distanceToSqr(at) <= reach * reach;
            if (caught || n * interval >= config.integer("max_ticks")) {
                if (block.isOpen(now)) {
                    block.setOpen(null, p.level(), now, pos, false);
                }
                if (caught) {
                    Nearby.sound(p, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, at, config.tone("sound"));
                    hurt(p, config.num("damage"));
                }
                return false;
            }
            return true;
        });
    }

    /** Ticks until the night is slept through and the sleeper wakes on their own. */
    private static int sleepLeft(ServerPlayer player) {
        return Player.SLEEP_DURATION - player.getSleepTimer();
    }

    // night_terror: a heartbeat in the sleeper's ears; if they are still asleep when it ends they are thrown awake. The
    // wait is cut to what is left of the sleep, so the scream always comes before the night is over.
    private static void terror(ServerPlayer player) {
        Config.Section config = Config.event(TERROR);
        sound(player, SoundEvents.WARDEN_HEARTBEAT, config.tone("sound"));
        int wait = Math.min(config.integer("warn_ticks"), sleepLeft(player) - config.integer("scream_lead_ticks"));
        // it gathers at the bedside while they sleep, and is still there when they wake
        Haze.linger(
                player,
                Nearby.around(player, 1.5, 90.0).with(Direction.Axis.Y, player.getY()),
                wait + config.integer("vignette_ticks"));
        Scheduler.later(player, wait, p -> {
            if (!p.isSleeping()) {
                return;
            }
            p.stopSleepInBed(true, true);
            sound(p, SoundEvents.GHAST_SCREAM, config.tone("scream_sound"));
            vignette(p, config);
            hurt(p, config.num("damage"));
        });
    }

    /** The side of a solid wall the player is leaning on, if any. */
    private static @Nullable Direction wall(ServerPlayer player) {
        ServerLevel level = player.level();
        double lean = Config.event(FISTS).num("lean_blocks");
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos beside = player.blockPosition().relative(side);
            if (!Place.solid(level, beside) && !Place.solid(level, beside.above())) {
                continue;
            }
            boolean x = side.getAxis() == Direction.Axis.X;
            double plane = (x ? beside.getX() : beside.getZ()) + ((x ? side.getStepX() : side.getStepZ()) < 0 ? 1 : 0);
            if (Math.abs((x ? player.getX() : player.getZ()) - plane) <= lean) {
                return side;
            }
        }
        return null;
    }

    // stone_fists: the wall rumbles and sheds dust, then it strikes whoever is still against it
    private static void fists(ServerPlayer player) {
        Config.Section config = Config.event(FISTS);
        int interval = config.integer("interval_ticks");
        int warn = config.integer("warn_steps");
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            Direction side = wall(p);
            if (side == null || n >= warn + config.integer("blows")) {
                return false;
            }
            BlockPos beside = p.blockPosition().relative(side);
            BlockState rock = p.level().getBlockState(beside);
            if (rock.isAir()) {
                rock = p.level().getBlockState(beside.above());
            }
            Vec3 face = p.position()
                    .add(side.getStepX() * config.num("lean_blocks"), 0.0, side.getStepZ() * config.num("lean_blocks"));
            particles(
                    p,
                    new BlockParticleOption(ParticleTypes.BLOCK, rock),
                    face.x,
                    face.y,
                    face.z,
                    config.burst("particles"));
            if (n < warn) {
                Nearby.sound(p, SoundEvents.STONE_BREAK, face, config.tone("sound"));
                return true;
            }
            Nearby.sound(p, SoundEvents.STONE_HIT, face, config.tone("hit_sound"));
            push(
                    p,
                    -side.getStepX() * config.num("knockback"),
                    config.num("lift"),
                    -side.getStepZ() * config.num("knockback"));
            hurt(p, config.num("damage"));
            return true;
        });
    }
}
