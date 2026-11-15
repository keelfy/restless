package dev.keelfy.restless.banish;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.block.Altar;
import dev.keelfy.restless.spirit.Focus;
import dev.keelfy.restless.spirit.Traits;
import dev.keelfy.restless.state.HauntData;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

/**
 * A second nature: an evolved spirit's foreign habit ({@link Traits#foreign}), or the nature of a spirit it merged
 * with ({@link dev.keelfy.restless.spirit.life.Merging}), is a second nature. In the bargain it
 * asks for an offering of that nature too, thrown onto the altar like the gift; the fury does not begin without it,
 * and the ritual fails.
 */
public final class Twofold {
    private Twofold() {}

    /**
     * The spirit's second nature, if it has one: the nature of the spirit it merged with, or once evolved its foreign
     * habit's. Never the nature the altar takes it by.
     */
    public static Optional<Focus> of(HauntData data, HauntData.Haunting spirit) {
        List<dev.keelfy.restless.spirit.Trait> traits = Traits.of(spirit.traits());
        Optional<Focus> merged = data.marks(spirit.uuid()).merged();
        Optional<Focus> second = merged.isPresent()
                ? merged
                : spirit.evolved() ? Traits.foreign(traits) : Optional.empty();
        return second.filter(focus -> focus != Traits.weakness(traits));
    }

    /** Whether the ritual may go on into the fury: no second nature, or its offering was taken. */
    static boolean paid(Circle circle) {
        return circle.twofold || second(circle).isEmpty();
    }

    static void enter(Circle circle) {
        second(circle)
                .ifPresent(focus -> circle.player.sendSystemMessage(
                        Component.translatable(
                                "message.restless.ritual_twofold",
                                Component.translatable("focus.restless." + focus.name().toLowerCase(Locale.ROOT))),
                        false));
    }

    /** A bargain wave: takes one offering of the second nature thrown onto the altar. */
    static void wave(Circle circle) {
        Optional<Focus> second = second(circle);
        if (second.isEmpty() || circle.twofold) {
            return;
        }
        double reach = Config.section("ritual").num("gift_radius");
        List<ItemEntity> given = circle.level.getEntitiesOfClass(
                ItemEntity.class, new AABB(circle.altar.above()).inflate(reach), item -> {
                    Altar.Held held = Altar.Held.from(item.getItem());
                    return item.isAlive() && held != null && held.focus == second.get();
                });
        if (given.isEmpty()) {
            return;
        }
        ItemEntity taken = given.getFirst();
        taken.getItem().shrink(1);
        if (taken.getItem().isEmpty()) {
            taken.discard();
        }
        circle.twofold = true;
        circle.level.playSound(null, circle.altar, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0f, 0.8f);
        circle.player.sendSystemMessage(Component.translatable("message.restless.ritual_twofold_taken"), true);
    }

    private static Optional<Focus> second(Circle circle) {
        return of(HauntData.get(circle.level.getServer()), circle.spirit.haunting());
    }
}
