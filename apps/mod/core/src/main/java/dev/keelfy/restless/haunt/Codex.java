package dev.keelfy.restless.haunt;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.haunt.JournalPages.Card;
import dev.keelfy.restless.haunt.JournalPages.Entry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * The pages of things to craft: a page per charm and relic the player knows how to make, and the recipes their recipe
 * book holds. A charm or relic page opens once the journal knows every spirit type whose essence it is made of (and,
 * for an advanced charm, its base charm's page is open); the recipe book's recipes come from reading diary pages.
 *
 * @param charms pages of charms, advanced ones after the basic
 * @param relics pages of relics
 * @param recipes the recipes of the player's recipe book that make one of the mod's items
 */
public record Codex(List<Card> charms, List<Card> relics, List<Card> recipes) {
    /** No pages: a notebook seen without a player. */
    public static final Codex NONE = new Codex(List.of(), List.of(), List.of());

    private static final List<String> CHARMS = List.of(
            "telekinesis_amulet",
            "dark_eye",
            "drowned_stone",
            "phantom_double",
            "borrowed_face",
            "wraith_shroud",
            "borrowed_dream",
            "death_wail",
            "grudge",
            "lost_step",
            "house_key",
            "stash",
            "bargain",
            "trickster_coin",
            "fetch_path",
            "changeling_mask",
            "nightwalk_shroud",
            "blood_debt",
            "requital_wail",
            "tidecaller",
            "magpies_hook",
            "truthseers_eye",
            "hearth_dream",
            "keepers_key",
            "grave_satchel",
            "devils_coin",
            "twins_mirror",
            "black_bargain");

    private static final List<String> RELICS =
            List.of("hollow_lantern", "ferrymans_coin", "exorcists_censer", "hoarders_hollow", "maras_cradle");

    private static final String ESSENCE = "_essence";

    /** What the player may read about, as of now. */
    public static Codex of(ServerPlayer player, Notebook notebook) {
        return new Codex(pages(player, notebook, CHARMS), pages(player, notebook, RELICS), recipes(player));
    }

    private static List<Card> pages(ServerPlayer player, Notebook notebook, List<String> items) {
        List<Card> cards = new ArrayList<>();
        for (String id : items) {
            if (!open(player, notebook, id)) {
                continue;
            }
            Item item = item(id);
            List<Entry> entries = new ArrayList<>();
            entries.add(Entry.plain(Component.translatable("item.restless." + id + ".tip")));
            recipe(player, id).ifPresent(entries::add);
            cards.add(new Card(item.getDefaultInstance().getHoverName(), Optional.empty(), entries));
        }
        return cards;
    }

    /** Whether the journal knows what the item is made of: each essence's spirit, and the charm it upgrades. */
    private static boolean open(ServerPlayer player, Notebook notebook, String id) {
        Optional<RecipeHolder<?>> holder = holder(player, id);
        if (holder.isEmpty()) {
            return false;
        }
        for (Ingredient ingredient : holder.get().value().placementInfo().ingredients()) {
            Optional<Identifier> made = ingredient
                    .items()
                    .findFirst()
                    .flatMap(h -> h.unwrapKey())
                    .map(ResourceKey::identifier)
                    .filter(i -> i.getNamespace().equals(Restless.MOD_ID));
            if (made.isEmpty()) {
                continue;
            }
            String path = made.get().getPath();
            boolean open = path.endsWith(ESSENCE)
                    ? notebook.knows(path.substring(0, path.length() - ESSENCE.length()))
                    : !CHARMS.contains(path) || open(player, notebook, path);
            if (!open) {
                return false;
            }
        }
        return true;
    }

    /** The recipes of the recipe book that make an item of the mod, in the order the recipe manager lists them. */
    private static List<Card> recipes(ServerPlayer player) {
        List<Card> cards = new ArrayList<>();
        for (RecipeHolder<?> holder :
                player.level().getServer().getRecipeManager().getRecipes()) {
            Identifier id = holder.id().identifier();
            if (id.getNamespace().equals(Restless.MOD_ID)
                    && BuiltInRegistries.ITEM.containsKey(id)
                    && player.getRecipeBook().contains(holder.id())) {
                recipe(player, id.getPath())
                        .ifPresent(row -> cards.add(new Card(
                                item(id.getPath()).getDefaultInstance().getHoverName(),
                                Optional.empty(),
                                List.of(row))));
            }
        }
        return cards;
    }

    /** A recipe as a row of its ingredients and its result, with the ingredients' names under it. */
    private static Optional<Entry> recipe(ServerPlayer player, String id) {
        return holder(player, id).map(holder -> {
            List<ItemStack> row = new ArrayList<>();
            MutableComponent names = Component.empty();
            for (Ingredient ingredient : holder.value().placementInfo().ingredients()) {
                ingredient.items().findFirst().ifPresent(h -> {
                    ItemStack stack = new ItemStack(h);
                    if (!row.isEmpty()) {
                        names.append(" + ");
                    }
                    row.add(stack);
                    names.append(stack.getHoverName());
                });
            }
            row.add(new ItemStack(item(id)));
            return Entry.recipe(Component.translatable("journal.restless.recipe", names), row);
        });
    }

    private static Optional<RecipeHolder<?>> holder(ServerPlayer player, String id) {
        return player.level()
                .getServer()
                .getRecipeManager()
                .byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(Restless.MOD_ID, id)));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(Restless.MOD_ID, id));
    }
}
