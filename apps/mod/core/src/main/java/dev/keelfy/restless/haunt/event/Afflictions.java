package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
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
            light("nausea", MobEffects.NAUSEA, 5, 0),
            // 40
            light("heaviness", MobEffects.SLOWNESS, 8, 1),
            // 41
            light("fatigue", MobEffects.MINING_FATIGUE, 10, 1),
            // 42
            SimpleEvent.light("hunger_pang", EventTag.AFFLICTION, 0.5,
                player -> player.getFoodData().getFoodLevel() > 6,
                player -> player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 3)),
            // 43
            light("darkness", MobEffects.DARKNESS, 6, 0),
            // 44
            SimpleEvent.light("shudder", EventTag.AFFLICTION, 0.5, player -> true, player -> {
                double angle = player.getRandom().nextDouble() * Math.PI * 2;
                player.push(Math.cos(angle) * 0.4, 0.1, Math.sin(angle) * 0.4);
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
            }),
            // 46: poison stops at half a heart by itself
            harmful("poison", MobEffects.POISON, 5, 0),
            // 47
            harmful("weakness", MobEffects.WEAKNESS, 30, 0),
            // 48: two seconds of levitation is a drop of a few blocks at most
            harmful("levitation", MobEffects.LEVITATION, 2, 0),
            // 49
            SimpleEvent.harmful("frostbite", EventTag.AFFLICTION, 0.6,
                player -> player.getHealth() > 6.0F && !player.isFreezing(),
                player -> {
                    player.setTicksFrozen(player.getTicksRequiredToFreeze() + 60);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE, 1.0F, 0.6F);
                }),
            // 50
            SimpleEvent.harmful("drained", EventTag.AFFLICTION, 0.6,
                player -> player.totalExperience > 0,
                player -> player.giveExperiencePoints(-Math.min(player.totalExperience, 5 + player.getRandom().nextInt(10)))),
            // 51
            harmful("bad_luck", MobEffects.UNLUCK, 300, 0)
        );
    }

    private static HauntEvent light(String id, Holder<MobEffect> effect, int seconds, int amplifier) {
        return SimpleEvent.light(id, EventTag.AFFLICTION, 0.6, player -> !player.hasEffect(effect), player -> apply(player, effect, seconds, amplifier));
    }

    private static HauntEvent harmful(String id, Holder<MobEffect> effect, int seconds, int amplifier) {
        return SimpleEvent.harmful(id, EventTag.AFFLICTION, 0.6, player -> !player.hasEffect(effect), player -> apply(player, effect, seconds, amplifier));
    }

    private static void apply(ServerPlayer player, Holder<MobEffect> effect, int seconds, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, 20 * seconds, amplifier), null);
        Vec3 at = player.position();
        player.level().playSound(null, at.x, at.y, at.z, SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 0.8F);
    }
}
