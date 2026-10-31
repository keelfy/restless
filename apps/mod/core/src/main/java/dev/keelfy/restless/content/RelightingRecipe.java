package dev.keelfy.restless.content;

import dev.keelfy.restless.platform.Platform;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

/** A normal shapeless recipe with a reusable igniter and component-preserving output. */
public final class RelightingRecipe extends NormalCraftingRecipe {
    public static final Supplier<RecipeSerializer<?>> SERIALIZER = Platform.get()
            .register(
                    BuiltInRegistries.RECIPE_SERIALIZER,
                    "relighting",
                    key -> new RecipeSerializer<>(
                            BuiltInRegistries.ITEM
                                    .byNameCodec()
                                    .fieldOf("light")
                                    .xmap(RelightingRecipe::new, recipe -> recipe.light),
                            ByteBufCodecs.registry(net.minecraft.core.registries.Registries.ITEM)
                                    .map(RelightingRecipe::new, recipe -> recipe.light)));

    private final Item light;
    private final ShapelessRecipe delegate;

    public RelightingRecipe(Item light) {
        super(new CommonInfo(true), new CraftingBookInfo(CraftingBookCategory.MISC, "restless_relighting"));
        this.light = light;
        this.delegate = new ShapelessRecipe(
                commonInfo,
                bookInfo,
                new ItemStackTemplate(
                        UnlitLights.relit(((BlockItem) light).getBlock().defaultBlockState())
                                .getBlock()
                                .asItem()),
                List.of(
                        Ingredient.of(light),
                        Ingredient.of(BuiltInRegistries.ITEM.stream().filter(UnlitLights::canIgnite))));
    }

    public static void init() {}

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<RelightingRecipe> getSerializer() {
        return (RecipeSerializer<RelightingRecipe>) SERIALIZER.get();
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return delegate.placementInfo();
    }

    @Override
    public List<RecipeDisplay> display() {
        return delegate.display();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return delegate.matches(input, level);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        for (ItemStack stack : input.items()) {
            if (stack.is(light)) return UnlitLights.relit(stack.copyWithCount(1));
        }
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (!UnlitLights.canIgnite(stack)) continue;
            ItemStack igniter = stack.copyWithCount(1);
            if (igniter.is(Items.FLINT_AND_STEEL) && igniter.isDamageableItem()) {
                igniter.setDamageValue(igniter.getDamageValue() + 1);
                if (igniter.getDamageValue() >= igniter.getMaxDamage()) continue;
            }
            remaining.set(slot, igniter);
        }
        return remaining;
    }
}
