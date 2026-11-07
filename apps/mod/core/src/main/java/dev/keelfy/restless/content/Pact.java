package dev.keelfy.restless.content;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** An effect of a charm: it only marks the player; what it means is done where the charm's hooks read it. */
public final class Pact extends MobEffect {
    public Pact(MobEffectCategory category, int color) {
        super(category, color);
    }
}
