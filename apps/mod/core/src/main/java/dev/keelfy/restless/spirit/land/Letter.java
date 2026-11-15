package dev.keelfy.restless.spirit.land;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.spirit.Story;
import dev.keelfy.restless.state.HauntData;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * {@link Quest#LETTER}: the reader takes the spirit's last letter and carries it to a villager (any villager counts as
 * the nearest village). Once it is handed over the next visit to the grave lays the spirit to rest.
 */
public final class Letter {
    // Keeps the descendant's name apart from the story's own name roll on the same seed.
    private static final long SALT = 0x1E77E4L;
    private static final String KEY = "restless_letter";

    private Letter() {}

    /** Who the letter is for. */
    @SuppressWarnings("java:S2245") // Seeded so the name is reproducible; not security-sensitive.
    static String addressee(long seed) {
        return Story.nickname(new Random(seed ^ SALT));
    }

    /** Gives the letter on the first reading, and a new one if the reader has lost it. @return the addressee */
    static Component read(ServerPlayer player, HauntData data, HauntData.Haunting spirit, HauntData.Grave grave) {
        if (grave.stage() == 0) {
            data.putGrave(spirit.uuid(), grave.staged(1, Optional.empty()));
        }
        if (grave.stage() <= 1 && !carries(player, spirit.uuid())) {
            player.getInventory().placeItemBackInInventory(letter(spirit), Prediction.SERVER_ONLY);
        }
        return Component.literal(addressee(spirit.seed()));
    }

    public static ItemStack letter(HauntData.Haunting spirit) {
        ItemStack stack = new ItemStack(Items.PAPER);
        stack.set(DataComponents.ITEM_NAME, Component.translatable("item.restless.letter", addressee(spirit.seed())));
        return Graves.tag(stack, KEY, spirit.uuid());
    }

    private static boolean carries(ServerPlayer player, UUID spirit) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (Graves.tagged(player.getInventory().getItem(i), KEY, spirit)) {
                return true;
            }
        }
        return false;
    }

    /** Hands the letters over for every stage-1 letter grave. */
    static void tick(ServerLevel level, HauntData data) {
        for (var entry : data.graves().entrySet()) {
            if (entry.getValue().stage() == 1) {
                data.find(entry.getKey())
                        .filter(h -> Quest.of(h.seed()) == Quest.LETTER)
                        .ifPresent(h -> level.players().forEach(p -> handOver(p, data, h)));
            }
        }
    }

    /** A player carrying the letter within reach of a villager gives it away. @return whether it was handed over */
    public static boolean handOver(ServerPlayer player, HauntData data, HauntData.Haunting spirit) {
        HauntData.Grave grave = data.graves().get(spirit.uuid());
        if (grave == null || grave.stage() != 1 || !carries(player, spirit.uuid())) {
            return false;
        }
        double radius = Config.section("grave").num("letter_radius");
        if (player.level()
                .getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(radius))
                .isEmpty()) {
            return false;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (Graves.tagged(stack, KEY, spirit.uuid())) {
                stack.shrink(1);
            }
        }
        data.putGrave(spirit.uuid(), grave.staged(2, Optional.empty()));
        player.sendSystemMessage(
                Component.translatable("message.restless.letter_delivered", addressee(spirit.seed())), true);
        return true;
    }
}
