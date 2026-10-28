package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Ticks;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/** Status effects and other things done to the player's body. None of them can kill. */
final class Afflictions {
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
            harmful("bad_luck", MobEffects.UNLUCK)
        );
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
