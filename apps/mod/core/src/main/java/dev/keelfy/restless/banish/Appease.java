package dev.keelfy.restless.banish;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.divine.Sense;
import dev.keelfy.restless.spirit.Trait;
import dev.keelfy.restless.spirit.Traits;
import dev.keelfy.restless.spirit.life.Echoes;
import dev.keelfy.restless.state.HauntData;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * Appeasement: what a spirit near the player desires, laid on a whole altar without candles or a ring, calms it by
 * {@code leanings.appease_anger} and leaves it be. Each time costs the same spirit one more of the item, from
 * {@code leanings.appease_cost}. An echo refuses the altar, and a spirit with no desire takes nothing.
 */
public final class Appease {
    private Appease() {}

    /** @return whether a spirit near the player wants {@code stack}; false leaves everything as it was */
    public static boolean offer(ServerPlayer player, ServerLevel level, BlockPos altar, ItemStack stack) {
        HauntData data = HauntData.get(level.getServer());
        String item = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        Optional<HauntData.Haunting> wanting = Sense.of(player, data).stream()
                .map(Sense.Spirit::haunting)
                .filter(h -> !Echoes.is(data, h)
                        && Traits.desire(Traits.of(h.traits()))
                                .map(Trait::things)
                                .filter(t -> t.contains(item))
                                .isPresent())
                .findFirst();
        if (wanting.isEmpty()) {
            return false;
        }
        HauntData.Haunting spirit = wanting.get();
        int cost = cost(data, spirit);
        if (stack.getCount() < cost) {
            player.sendSystemMessage(
                    Component.translatable("message.restless.appease_more", cost, stack.getHoverName()), true);
            return true;
        }
        stack.consume(cost, player);
        data.change(spirit.uuid(), h -> h.angered(-Config.section("leanings").num("appease_anger")));
        data.putMarks(spirit.uuid(), data.marks(spirit.uuid()).appeasedOnce());
        level.sendParticles(
                ParticleTypes.SOUL, altar.getX() + 0.5, altar.getY() + 1.2, altar.getZ() + 0.5, 10, 0.2, 0.2, 0.2, 0.01);
        level.playSound(null, altar, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, 1.0f, 0.8f);
        player.sendSystemMessage(Component.translatable("message.restless.appeased"), true);
        return true;
    }

    /** How many of its desire the spirit asks for at its next appeasement. */
    public static int cost(HauntData data, HauntData.Haunting spirit) {
        return Config.section("leanings").integer("appease_cost") + data.marks(spirit.uuid()).appeased();
    }
}
