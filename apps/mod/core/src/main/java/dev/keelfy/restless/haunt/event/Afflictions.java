package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntData;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Journal;
import dev.keelfy.restless.haunt.Notebook;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.Sense;
import dev.keelfy.restless.haunt.Ticks;
import dev.keelfy.restless.spirit.Story;
import dev.keelfy.restless.spirit.Trait;
import dev.keelfy.restless.spirit.Traits;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Difficulty;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Status effects and other things done to the player's body. None of them can kill. */
final class Afflictions {
    /** Ticks without sleep set by insomnia: past the three days after which phantoms come, about half the time. */
    private static final int SLEEPLESS = 144000;

    private Afflictions() {}

    static List<HauntEvent> all() {
        return List.of(
            // 39
            light("nausea", MobEffects.NAUSEA),
            // 40
            light("heaviness", MobEffects.SLOWNESS),
            // 41
            light("fatigue", MobEffects.MINING_FATIGUE),
            // 42
            SimpleEvent.light("hunger_pang", EventTag.AFFLICTION,
                player -> player.getFoodData().getFoodLevel() > Config.event("hunger_pang").integer("min_food"),
                player -> player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - Config.event("hunger_pang").integer("loss"))),
            // 43
            light("darkness", MobEffects.DARKNESS),
            // 44
            SimpleEvent.light("shudder", EventTag.AFFLICTION, player -> true, player -> {
                Config.Section config = Config.event("shudder");
                double angle = player.getRandom().nextDouble() * Math.PI * 2;
                player.push(Math.cos(angle) * config.num("push"), config.num("lift"), Math.sin(angle) * config.num("push"));
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
            }),
            // 46: poison stops at half a heart by itself
            harmful("poison", MobEffects.POISON),
            // 47
            harmful("weakness", MobEffects.WEAKNESS),
            // 48: two seconds of levitation is a drop of a few blocks at most
            harmful("levitation", MobEffects.LEVITATION),
            // 49
            SimpleEvent.harmful("frostbite", EventTag.AFFLICTION,
                player -> player.getHealth() > Config.event("frostbite").num("min_health") && !player.isFreezing(),
                player -> {
                    player.setTicksFrozen(player.getTicksRequiredToFreeze() + Config.event("frostbite").integer("extra_freeze_ticks"));
                    Config.Tone tone = Config.event("frostbite").tone("sound");
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE, tone.volume(), tone.pitch());
                }),
            // 50
            SimpleEvent.harmful("drained", EventTag.AFFLICTION,
                player -> player.totalExperience > 0,
                player -> {
                    Config.Section config = Config.event("drained");
                    player.giveExperiencePoints(-Math.min(player.totalExperience, config.integer("xp_min") + player.getRandom().nextInt(config.integer("xp_spread"))));
                }),
            // 51
            harmful("bad_luck", MobEffects.UNLUCK),
            // 141: more of the same, none of which can kill
            light("ravenous", MobEffects.HUNGER),
            harmful("infested", MobEffects.INFESTED),
            light("oozing", MobEffects.OOZING),
            light("weaving", MobEffects.WEAVING),
            // 143: a slow pull in one direction, as if by a current
            SimpleEvent.light("drift", EventTag.AFFLICTION, player -> player.onGround(), Afflictions::drift),
            // 173: no dream until the player sleeps; they wake to a line, in chat and in the journal, from a spirit near them
            SimpleEvent.light("dream", EventTag.AFFLICTION, player -> !DREAMERS.contains(player.getUUID()) && !Sense.of(player, HauntData.get(player.level().getServer())).isEmpty(), Afflictions::dream),
            // 171: the clock phantoms go by says three days without sleep
            SimpleEvent.harmful("insomnia", EventTag.AFFLICTION,
                player -> player.level().getDifficulty() != Difficulty.PEACEFUL && player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)) < SLEEPLESS,
                player -> player.getStats().setValue(player, Stats.CUSTOM.get(Stats.TIME_SINCE_REST), SLEEPLESS)),
            // 172: no walk until the player sleeps through; they wake on safe ground some way off, outdoors if it can be found
            SimpleEvent.harmful("sleepwalk", EventTag.AFFLICTION, player -> !SLEEPWALKERS.contains(player.getUUID()), Afflictions::sleepwalk)
        );
    }

    /** Players with a dream waiting, so a second one is not stacked on the first. */
    private static final Set<UUID> DREAMERS = new HashSet<>();

    private static void dream(ServerPlayer player) {
        Config.Section config = Config.event("dream");
        List<Sense.Spirit> spirits = new ArrayList<>(Sense.of(player, HauntData.get(player.level().getServer())));
        Sense.Spirit spirit = spirits.get(player.getRandom().nextInt(spirits.size()));
        Story story = Story.of(spirit.haunting().seed());
        List<Trait> traits = Traits.of(spirit.haunting().traits());
        Component line = traits.isEmpty() || player.getRandom().nextBoolean()
            ? Component.translatable("message.restless.dream_story", story.name(), story.cause())
            : Component.translatable("message.restless.dream_trait", traits.get(player.getRandom().nextInt(traits.size())).hint());
        DREAMERS.add(player.getUUID());
        int interval = config.integer("interval_ticks");
        boolean[] slept = {false};
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            if (p.isSleeping()) {
                slept[0] = true;
            } else if (slept[0]) {
                p.sendSystemMessage(line);
                Journal.note(p, Notebook.Kind.DREAM, spirit.haunting(), line);
                DREAMERS.remove(p.getUUID());
                return false;
            }
            if (n * interval >= config.integer("wait_ticks")) {
                DREAMERS.remove(p.getUUID());
                return false;
            }
            return true;
        });
    }

    /** Players with a walk waiting. */
    private static final Set<UUID> SLEEPWALKERS = new HashSet<>();

    private static void sleepwalk(ServerPlayer player) {
        Config.Section config = Config.event("sleepwalk");
        SLEEPWALKERS.add(player.getUUID());
        int interval = config.integer("interval_ticks");
        int[] slept = {0};
        Scheduler.repeat(player, 0, interval, (p, n) -> {
            if (p.isSleeping()) {
                slept[0] += interval;
            } else if (slept[0] >= config.integer("min_sleep_ticks")) {
                BlockPos to = wakeSpot(p, config);
                if (to != null) {
                    p.teleportTo(p.level(), to.getX() + 0.5, to.getY(), to.getZ() + 0.5, Set.of(), p.getRandom().nextFloat() * 360.0F, p.getXRot(), true);
                }
                SLEEPWALKERS.remove(p.getUUID());
                return false;
            } else {
                // Up too soon: a night not slept through does not count.
                slept[0] = 0;
            }
            if (n * interval >= config.integer("wait_ticks")) {
                SLEEPWALKERS.remove(p.getUUID());
                return false;
            }
            return true;
        });
    }

    /** Standing room a few blocks off on a floor that does not burn, away from lava; under open sky if any try finds it. */
    private static @Nullable BlockPos wakeSpot(ServerPlayer player, Config.Section config) {
        ServerLevel level = player.level();
        BlockPos found = null;
        for (int i = 0; i < config.integer("tries"); i++) {
            BlockPos pos = Place.spawnSpot(player, config.integer("min_distance"), config.integer("max_distance"));
            if (pos == null || level.getBlockState(pos.below()).is(Blocks.MAGMA_BLOCK)
                || BlockPos.betweenClosedStream(pos.offset(-1, -1, -1), pos.offset(1, 2, 1)).anyMatch(near -> level.getFluidState(near).is(FluidTags.LAVA))) {
                continue;
            }
            if (level.canSeeSky(pos)) {
                return pos;
            }
            found = pos;
        }
        return found;
    }

    private static void drift(ServerPlayer player) {
        Config.Section config = Config.event("drift");
        double angle = player.getRandom().nextDouble() * Math.PI * 2;
        double push = config.num("push");
        Scheduler.repeat(player, 0, 1, (p, n) -> {
            p.push(Math.cos(angle) * push, 0, Math.sin(angle) * push);
            p.connection.send(new ClientboundSetEntityMotionPacket(p));
            return n + 1 < config.integer("ticks");
        });
    }

    private static HauntEvent light(String id, Holder<MobEffect> effect) {
        return SimpleEvent.light(id, EventTag.AFFLICTION, player -> !player.hasEffect(effect), player -> apply(player, effect, id));
    }

    private static HauntEvent harmful(String id, Holder<MobEffect> effect) {
        return SimpleEvent.harmful(id, EventTag.AFFLICTION, player -> !player.hasEffect(effect), player -> apply(player, effect, id));
    }

    private static void apply(ServerPlayer player, Holder<MobEffect> effect, String id) {
        Config.Section config = Config.event(id);
        player.addEffect(new MobEffectInstance(effect, Ticks.SECOND * config.integer("seconds"), config.integer("amplifier")), null);
        Vec3 at = player.position();
        Config.Tone tone = config.tone("sound");
        player.level().playSound(null, at.x, at.y, at.z, SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, tone.volume(), tone.pitch());
    }
}
