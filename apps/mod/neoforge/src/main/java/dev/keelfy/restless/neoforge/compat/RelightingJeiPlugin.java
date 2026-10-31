package dev.keelfy.restless.neoforge.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

@JeiPlugin
public final class RelightingJeiPlugin implements IModPlugin {
    private static final IRecipeType<Relighting> TYPE = IRecipeType.create("restless", "relighting", Relighting.class);

    @Override
    public Identifier getPluginUid() {
        return Identifier.parse("restless:relighting");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var icon = registration.getJeiHelpers().getGuiHelper().createDrawableItemLike(Items.FLINT_AND_STEEL);
        registration.addRecipeCategories(new Category(icon));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(TYPE, Relighting.recipes());
    }

    private record Category(IDrawable icon) implements IRecipeCategory<Relighting> {
        @Override
        public IRecipeType<Relighting> getRecipeType() {
            return TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("restless.relighting.title");
        }

        @Override
        public int getWidth() {
            return 160;
        }

        @Override
        public int getHeight() {
            return 60;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, Relighting recipe, IFocusGroup focuses) {
            builder.addInputSlot(1, 1).add(recipe.input());
            builder.addInputSlot(49, 1).addItemStacks(Relighting.igniters());
            builder.addOutputSlot(125, 1).add(recipe.output());
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, Relighting recipe, IFocusGroup focuses) {
            builder.addRecipePlusSignWidget().setPosition(25, 3);
            builder.addRecipeArrowWidget().setPosition(85, 1);
            builder.addText(Component.translatable("restless.relighting.action"), 160, 22)
                    .setPosition(0, 30)
                    .setTooltip(Component.translatable("restless.relighting.description"));
        }
    }
}
