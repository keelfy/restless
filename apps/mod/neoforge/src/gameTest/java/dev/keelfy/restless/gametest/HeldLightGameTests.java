package dev.keelfy.restless.gametest;

import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.UnlitLights;
import dev.keelfy.restless.haunt.HauntEvents;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

final class HeldLightGameTests {
    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("held_light_out", helper -> {
            var player = HauntEventGameTests.connected(helper);
            var event = HauntEvents.byId(Identifier.withDefaultNamespace("held_light_out"))
                    .orElseThrow();
            check(helper, !event.canRun(player), "Empty hands must not enable extinguishing");
            for (var item : UnlitLights.ITEMS) {
                for (var hand : InteractionHand.values()) {
                    ItemStack original = UnlitLights.relit(new ItemStack(item.get(), 64));
                    original.set(DataComponents.CUSTOM_NAME, Component.literal("My light"));
                    player.setItemInHand(hand, original);
                    check(helper, event.canRun(player), "Both hands must enable extinguishing");
                    event.run(player);
                    ItemStack unlit = player.getItemInHand(hand);
                    check(
                            helper,
                            unlit.is(item.get()) && unlit.getCount() == 64,
                            "Extinguishing must convert the entire stack to the correct unlit type");
                    check(
                            helper,
                            ItemStack.matches(original, UnlitLights.relit(unlit)),
                            "Extinguishing must preserve components");
                    check(helper, !event.canRun(player), "Already unlit hands must not enable the event");
                    player.setItemInHand(hand, ItemStack.EMPTY);
                }
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TORCH, 64));
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.LANTERN, 16));
            event.run(player);
            int changed = (player.getMainHandItem().is(Items.TORCH) ? 0 : 1)
                    + (player.getOffhandItem().is(Items.LANTERN) ? 0 : 1);
            check(helper, changed == 1, "One event must affect exactly one hand");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Content.HALLOWED_LANTERN_ITEM.get()));
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.GLOWSTONE));
            check(helper, !event.canRun(player), "Protected lanterns and unsupported lights must remain untouched");
            player.discard();
            helper.succeed();
        });
        tests.put("relight_crafting", helper -> {
            var level = helper.getLevel();
            for (var item : UnlitLights.ITEMS) {
                for (var igniterItem : List.of(
                        Items.FLINT_AND_STEEL,
                        Items.TORCH,
                        Items.SOUL_TORCH,
                        Items.COPPER_TORCH,
                        Items.REDSTONE_TORCH)) {
                    ItemStack source = new ItemStack(item.get(), 64);
                    source.set(DataComponents.CUSTOM_NAME, Component.literal("My light"));
                    ItemStack igniter = new ItemStack(igniterItem);
                    CraftingInput input =
                            CraftingInput.of(2, 2, List.of(ItemStack.EMPTY, igniter, source, ItemStack.EMPTY));
                    var recipe = level.getServer()
                            .getRecipeManager()
                            .getRecipeFor(RecipeType.CRAFTING, input, level)
                            .orElseThrow(() -> helper.assertionException(
                                    Component.literal("Relighting recipe must load for every unlit item")))
                            .value();
                    check(
                            helper,
                            ItemStack.matches(recipe.assemble(input), UnlitLights.relit(source.copyWithCount(1))),
                            "Crafting must restore the correct light and custom components, one per craft");
                    var remaining = recipe.getRemainingItems(input);
                    var returned =
                            remaining.stream().filter(stack -> !stack.isEmpty()).toList();
                    check(
                            helper,
                            returned.size() == 1
                                    && returned.getFirst().is(igniterItem)
                                    && returned.getFirst().getCount() == 1
                                    && returned.getFirst().getDamageValue()
                                            == (igniterItem == Items.FLINT_AND_STEEL ? 1 : 0),
                            "Crafting must return the torch or damage the flint once");
                    check(
                            helper,
                            source.getCount() == 64 && igniter.getDamageValue() == 0,
                            "Recipe previews must not mutate the ingredients");
                    check(
                            helper,
                            !recipe.matches(CraftingInput.of(2, 1, List.of(source, new ItemStack(Items.STICK))), level),
                            "A stick must not ignite a light");
                    check(
                            helper,
                            !recipe.matches(CraftingInput.of(2, 1, List.of(source, new ItemStack(item.get()))), level),
                            "An unlit source must not be an igniter");
                    check(
                            helper,
                            !recipe.matches(
                                    CraftingInput.of(3, 1, List.of(source, igniter, new ItemStack(Items.STICK))),
                                    level),
                            "Extra ingredients must reject the recipe");
                    if (igniterItem == Items.FLINT_AND_STEEL) {
                        igniter.setDamageValue(igniter.getMaxDamage() - 1);
                        check(
                                helper,
                                recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty),
                                "Flint must break on its last durability point");
                    }
                }
            }
            // Repeated crafting consumes the extinguished stack, never its reusable torch.
            ItemStack source = new ItemStack(UnlitLights.ITEMS.getFirst().get(), 64);
            ItemStack torch = new ItemStack(Items.TORCH);
            int restored = 0;
            while (!source.isEmpty()) {
                var input = CraftingInput.of(2, 1, List.of(source, torch));
                var recipe = level.getServer()
                        .getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, input, level)
                        .orElseThrow()
                        .value();
                restored += recipe.assemble(input).getCount();
                torch = recipe.getRemainingItems(input).get(1);
                source.shrink(1);
            }
            check(
                    helper,
                    restored == 64 && torch.is(Items.TORCH) && torch.getCount() == 1,
                    "A full extinguished stack must be recoverable using one lit torch");
            helper.succeed();
        });
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) throw helper.assertionException(Component.literal(message));
    }
}
