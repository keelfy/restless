package dev.keelfy.restless.banish;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.event.HauntTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.AbstractCandleBlock;

/**
 * The fury: the spirit fights for the candles. Each wave it may snuff one (never past a censer) and may strike the
 * player (only with harmful events past a censer), may act by its nature (see {@link Natures}), and once wipes a tile
 * of the ring (see {@link Ring}).
 */
final class Fury {
    private Fury() {}

    static void enter(Circle circle) {
        circle.player.sendSystemMessage(Component.translatable("message.restless.ritual_fury"), true);
        circle.level.playSound(null, circle.altar, SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 0.7f, 0.6f);
    }

    /** @return whether the ritual goes on */
    static boolean wave(Circle circle) {
        Config.Section config = Config.section("ritual");
        var random = circle.player.getRandom();
        if (!circle.censer && random.nextDouble() < config.num("snuff_chance")) {
            BlockPos candle = circle.candles.get(random.nextInt(circle.candles.size()));
            // every candle was lit when the wave began
            AbstractCandleBlock.extinguish(null, circle.level.getBlockState(candle), circle.level, candle);
            circle.player.sendSystemMessage(Component.translatable("message.restless.candle_snuffed"), true);
        }
        if (random.nextDouble() < config.num("fury_chance")) {
            Director.act(
                    circle.player,
                    circle.spirit.haunting(),
                    circle.spirit.at(),
                    circle.censer ? HauntTier.HARMFUL : null);
        }
        Natures.fight(circle);
        circle.ring.wave();
        return true;
    }
}
