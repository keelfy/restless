package dev.keelfy.restless.content;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.keelfy.restless.platform.Platform;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

/**
 * An advanced charm: a base charm and one more essence, shapeless. The upgrade takes the base charm's components with
 * it, so what a stash holds or a key has worn is not lost.
 */
public final class UpgradeRecipe extends NormalCraftingRecipe {
    public static final Supplier<RecipeSerializer<?>> SERIALIZER = Platform.get()
            .register(
                    BuiltInRegistries.RECIPE_SERIALIZER,
                    "upgrade",
                    key -> new RecipeSerializer<>(
                            RecordCodecBuilder.mapCodec(i -> i.group(
                                            BuiltInRegistries.ITEM
                                                    .byNameCodec()
                                                    .fieldOf("base")
                                                    .forGetter(r -> r.base),
                                            BuiltInRegistries.ITEM
                                                    .byNameCodec()
                                                    .fieldOf("essence")
                                                    .forGetter(r -> r.essence),
                                            BuiltInRegistries.ITEM
                                                    .byNameCodec()
                                                    .fieldOf("result")
                                                    .forGetter(r -> r.result))
                                    .apply(i, UpgradeRecipe::new)),
                            StreamCodec.composite(
                                    ByteBufCodecs.registry(Registries.ITEM),
                                    r -> r.base,
                                    ByteBufCodecs.registry(Registries.ITEM),
                                    r -> r.essence,
                                    ByteBufCodecs.registry(Registries.ITEM),
                                    r -> r.result,
                                    UpgradeRecipe::new)));

    private final Item base;
    private final Item essence;
    private final Item result;
    private final ShapelessRecipe delegate;

    public UpgradeRecipe(Item base, Item essence, Item result) {
        super(new CommonInfo(true), new CraftingBookInfo(CraftingBookCategory.MISC, "restless_upgrade"));
        this.base = base;
        this.essence = essence;
        this.result = result;
        this.delegate = new ShapelessRecipe(
                commonInfo,
                bookInfo,
                new ItemStackTemplate(result),
                List.of(Ingredient.of(base), Ingredient.of(essence)));
    }

    public static void init() {}

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<UpgradeRecipe> getSerializer() {
        return (RecipeSerializer<UpgradeRecipe>) SERIALIZER.get();
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
            if (stack.is(base)) {
                ItemStack upgraded = new ItemStack(result);
                upgraded.applyComponents(stack.getComponentsPatch());
                if (!upgraded.isDamageableItem()) {
                    upgraded.remove(DataComponents.DAMAGE);
                }
                return upgraded;
            }
        }
        return ItemStack.EMPTY;
    }
}
