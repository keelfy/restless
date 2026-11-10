package dev.keelfy.restless.content.item.charm;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

/**
 * The hoarder's stash with the lost one's road home: holds three stacks, and carried in the off hand at death it stays
 * with its holder, full. It is not spent: it rests three days after it triggers, see {@link Charms#died}.
 */
public final class GraveSatchel extends Item {
    public GraveSatchel(Item.Properties properties) {
        super(properties);
    }

    static List<ItemStack> held(ItemStack satchel) {
        return satchel.getOrDefault(Content.STASHED.get(), ItemContainerContents.EMPTY)
                .nonEmptyItemCopyStream()
                .toList();
    }

    @Override
    public Component getName(ItemStack stack) {
        int stacks = held(stack).size();
        return stacks == 0
                ? super.getName(stack)
                : Component.translatable("item.restless.grave_satchel.holding", stacks);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server) {
            Pouch.open(server, hand, Config.section("advanced").integer("satchel_stacks"));
        }
        return InteractionResult.SUCCESS;
    }
}
