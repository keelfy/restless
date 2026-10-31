package dev.keelfy.restless.neoforge.compat;

import java.util.List;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

@REIPluginClient
public final class RelightingReiPlugin implements REIClientPlugin {
    private static final CategoryIdentifier<Display> TYPE = CategoryIdentifier.of("restless", "relighting");

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new Category());
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        Relighting.recipes().forEach(recipe -> registry.add(new Display(recipe)));
    }

    private static final class Display extends BasicDisplay {
        Display(Relighting recipe) {
            super(
                    List.of(EntryIngredients.of(recipe.input()), EntryIngredients.ofItemStacks(Relighting.igniters())),
                    List.of(EntryIngredients.of(recipe.output())));
        }

        @Override
        public CategoryIdentifier<Display> getCategoryIdentifier() {
            return TYPE;
        }
        // Displays are generated locally on the client; they are never synchronized or persisted.
        @Override
        public me.shedaniel.rei.api.common.display.DisplaySerializer<Display> getSerializer() {
            return null;
        }
    }

    private static final class Category implements DisplayCategory<Display> {
        @Override
        public CategoryIdentifier<Display> getCategoryIdentifier() {
            return TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("restless.relighting.title");
        }

        @Override
        public Renderer getIcon() {
            return EntryStacks.of(Items.FLINT_AND_STEEL);
        }

        @Override
        public int getDisplayHeight() {
            return 64;
        }

        @Override
        public int getDisplayWidth(Display display) {
            return 160;
        }

        @Override
        public List<Widget> setupDisplay(Display display, Rectangle bounds) {
            int x = bounds.x + 8;
            int y = bounds.y + 8;
            return List.of(
                    Widgets.createRecipeBase(bounds),
                    Widgets.createSlot(new Point(x, y))
                            .entries(display.getInputEntries().get(0))
                            .markInput(),
                    Widgets.createLabel(new Point(x + 30, y + 5), Component.literal("+"))
                            .noShadow(),
                    Widgets.createSlot(new Point(x + 48, y))
                            .entries(display.getInputEntries().get(1))
                            .markInput(),
                    Widgets.createArrow(new Point(x + 84, y)),
                    Widgets.createSlot(new Point(x + 124, y))
                            .entries(display.getOutputEntries().getFirst())
                            .markOutput(),
                    Widgets.createLabel(new Point(x, y + 28), Component.translatable("restless.relighting.action"))
                            .leftAligned()
                            .noShadow()
                            .tooltip(Component.translatable("restless.relighting.description")));
        }
    }
}
