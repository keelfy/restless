package dev.keelfy.restless.banish;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.spirit.Traits;
import dev.keelfy.restless.spirit.land.Graves;
import dev.keelfy.restless.state.HauntData;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;

/**
 * The bargain: the spirit names a gift and offers to stop for it. The gift thrown onto the altar before the fury ends
 * the ritual: the spirit takes it with the offering, grows calmer and stays. Left unanswered, the fury follows.
 */
final class Bargain {
    private Bargain() {}

    /**
     * What the spirit asks for: the first thing it desires, or for a spirit with no desire the thing its grave says it
     * wants back, the same for its whole life.
     */
    static Item gift(Circle circle) {
        return gifts(circle).getFirst();
    }

    /** Every gift the spirit takes: all it desires, or its grave's wanted thing. */
    static List<Item> gifts(Circle circle) {
        HauntData.Haunting spirit = circle.spirit.haunting();
        List<Item> desired = Traits.desire(Traits.of(spirit.traits())).stream()
                .flatMap(d -> d.things().stream())
                .map(id -> BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)))
                .flatMap(java.util.Optional::stream)
                .toList();
        return desired.isEmpty()
                ? List.of(Graves.wanted(HauntData.get(circle.level.getServer()), spirit))
                : desired;
    }

    static void enter(Circle circle) {
        Item gift = gift(circle);
        circle.player.sendSystemMessage(
                Component.translatable(
                        "message.restless.ritual_bargain",
                        gift.getDefaultInstance().getHoverName()),
                true);
        circle.level.playSound(null, circle.altar, SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.0f, 1.2f);
        Twofold.enter(circle);
    }

    /** @return whether the ritual goes on: false once the gift is taken */
    static boolean wave(Circle circle) {
        Twofold.wave(circle);
        List<Item> gifts = gifts(circle);
        double reach = Config.section("ritual").num("gift_radius");
        List<ItemEntity> given = circle.level.getEntitiesOfClass(
                ItemEntity.class,
                new AABB(circle.altar.above()).inflate(reach),
                item -> item.isAlive() && gifts.contains(item.getItem().getItem()));
        if (given.isEmpty()) {
            return true;
        }
        ItemEntity taken = given.getFirst();
        taken.getItem().shrink(1);
        if (taken.getItem().isEmpty()) {
            taken.discard();
        }
        Ritual.clear(circle.level, circle.altar);
        HauntData.get(circle.level.getServer())
                .change(
                        circle.spirit.haunting().uuid(),
                        h -> h.angered(-Config.section("ritual").num("bargain_anger")));
        circle.level.playSound(null, circle.altar, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 1.2f);
        circle.player.sendSystemMessage(Component.translatable("message.restless.ritual_bargained"), true);
        return false;
    }
}
