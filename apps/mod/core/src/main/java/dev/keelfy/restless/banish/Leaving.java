package dev.keelfy.restless.banish;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** The leaving: the spirit's strength is spent and it lets go; the candles must still burn to the end. */
final class Leaving {
    private Leaving() {}

    static void enter(Circle circle) {
        circle.player.sendSystemMessage(Component.translatable("message.restless.ritual_leaving"), true);
        circle.level.playSound(null, circle.altar, SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.0f, 0.6f);
    }

    /** @return whether the ritual goes on */
    static boolean wave(Circle circle) {
        return true;
    }
}
