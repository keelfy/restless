package dev.keelfy.restless.content.item;

import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.haunt.Journal;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

/**
 * A torn page from a dead person's diary, found in chests of structures. It describes how to make one thing; reading it
 * adds that recipe to the reader's recipe book. The page names the recipe in {@link Content#RECIPE}, and the thing it
 * makes is the item of the same id, which is how the page is titled.
 */
public final class DiaryPage extends Item {
    public DiaryPage(Item.Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Identifier recipe = stack.get(Content.RECIPE.get());
        return recipe == null
                ? super.getName(stack)
                : Component.translatable("item.restless.diary_page.of", makes(recipe));
    }

    private static Component makes(Identifier recipe) {
        return BuiltInRegistries.ITEM.getValue(recipe).getDefaultInstance().getHoverName();
    }

    /** Reading a page nobody could make sense of, or one already read, keeps it. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Identifier recipe = stack.get(Content.RECIPE.get());
        if (recipe == null) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer reader) {
            ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, recipe);
            if (reader.getRecipeBook().contains(key)) {
                reader.sendSystemMessage(Component.translatable("message.restless.page_known"), true);
                return InteractionResult.SUCCESS_SERVER;
            }
            reader.awardRecipesByKey(java.util.List.of(key));
            reader.sendSystemMessage(Component.translatable("message.restless.page_read", makes(recipe)), true);
            stack.consume(1, reader);
            Journal.sync(reader);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.SUCCESS;
    }
}
