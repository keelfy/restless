package dev.keelfy.restless.haunt.event;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Restore;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookRemovePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Small tricks on the player's own client (a border, a recipe toast, a narrowed view) and two with the fishing rod. The
 * client lies leave nothing on the server; a lie cut short (the player leaves, dies, changes dimension) is corrected by
 * the resync vanilla does then.
 */
final class Tricks {
    /** A recipe book id no real recipe has. */
    private static final RecipeDisplayId FAKE_RECIPE = new RecipeDisplayId(0x3FFF_FF01);

    private Tricks() {}

    static List<HauntEvent> all() {
        return List.of(
                // 351: a border closes in around the player on their screen only
                SimpleEvent.light(
                        "border_closes",
                        EventTag.PRESENCE,
                        player -> player.level().getWorldBorder().getDistanceToBorder(player)
                                > Config.event("border_closes").num("from"),
                        Tricks::borderCloses),
                // 355: the "recipe unlocked" toast for a recipe that does not exist
                SimpleEvent.light("false_recipe", EventTag.PRESENCE, SimpleEvent.ALWAYS, Tricks::falseRecipe),
                // 358: the view narrows as if the player were slowed
                SimpleEvent.light("view_narrows", EventTag.PRESENCE, SimpleEvent.ALWAYS, Tricks::viewNarrows),
                // 418: the line drags the player toward the water
                SimpleEvent.harmful("line_pulls", EventTag.ITEMS, player -> hook(player) != null, Tricks::linePulls),
                // 419: the catch is the player's own head or a bone
                SimpleEvent.light("odd_catch", EventTag.ITEMS, player -> hook(player) != null, Tricks::oddCatch));
    }

    /** The bobber of the player's rod, if it floats. */
    private static @Nullable FishingHook hook(ServerPlayer player) {
        FishingHook hook = player.fishing;
        return hook != null
                        && hook.isAlive()
                        && (hook.isInWater()
                                || player.level()
                                        .getFluidState(hook.blockPosition())
                                        .is(net.minecraft.tags.FluidTags.WATER)
                                || player.level()
                                        .getFluidState(hook.blockPosition().below())
                                        .is(net.minecraft.tags.FluidTags.WATER))
                ? hook
                : null;
    }

    private static void borderCloses(ServerPlayer player) {
        Config.Section config = Config.event("border_closes");
        ServerLevel level = player.level();
        int ticks = config.integer("ticks");
        WorldBorder fake = new WorldBorder();
        fake.setCenter(player.getX(), player.getZ());
        fake.lerpSizeBetween(config.num("from"), config.num("to"), ticks, level.getGameTime());
        player.connection.send(new ClientboundInitializeBorderPacket(fake));
        Scheduler.later(player, ticks + config.integer("hold_ticks"), p -> {
            // Elsewhere the client was given that world's border already.
            if (p.level() == level) {
                p.connection.send(new ClientboundInitializeBorderPacket(level.getWorldBorder()));
            }
        });
    }

    private static void falseRecipe(ServerPlayer player) {
        Config.Section config = Config.event("false_recipe");
        List<String> results = config.texts("results");
        Item result = BuiltInRegistries.ITEM
                .getOptional(Identifier.parse(results.get(player.getRandom().nextInt(results.size()))))
                .orElse(Items.BONE);
        RecipeDisplayEntry recipe = new RecipeDisplayEntry(
                FAKE_RECIPE,
                new ShapelessCraftingRecipeDisplay(
                        List.of(
                                new SlotDisplay.ItemSlotDisplay(Items.BONE.builtInRegistryHolder()),
                                new SlotDisplay.ItemSlotDisplay(Items.SKELETON_SKULL.builtInRegistryHolder())),
                        new SlotDisplay.ItemSlotDisplay(result.builtInRegistryHolder()),
                        new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE.builtInRegistryHolder())),
                OptionalInt.empty(),
                RecipeBookCategories.CRAFTING_MISC,
                Optional.empty());
        player.connection.send(new ClientboundRecipeBookAddPacket(
                List.of(new ClientboundRecipeBookAddPacket.Entry(recipe, true, false)), false));
        Scheduler.later(
                player,
                config.integer("ticks"),
                p -> p.connection.send(new ClientboundRecipeBookRemovePacket(List.of(FAKE_RECIPE))));
    }

    private static void viewNarrows(ServerPlayer player) {
        // The field of view follows the speed attribute, so a low value pinches it.
        AttributeInstance slow = new AttributeInstance(Attributes.MOVEMENT_SPEED, instance -> {});
        slow.setBaseValue(Config.event("view_narrows").num("speed"));
        player.connection.send(new ClientboundUpdateAttributesPacket(player.getId(), List.of(slow)));
        Scheduler.later(player, Config.event("view_narrows").integer("ticks"), p -> {
            AttributeInstance real = p.getAttributes().getInstance(Attributes.MOVEMENT_SPEED);
            if (real != null) {
                p.connection.send(new ClientboundUpdateAttributesPacket(p.getId(), List.of(real)));
            }
        });
    }

    private static void linePulls(ServerPlayer player) {
        FishingHook hook = hook(player);
        if (hook == null) {
            return;
        }
        Config.Section config = Config.event("line_pulls");
        Vec3 toward = hook.position().subtract(player.position()).multiply(1, 0, 1);
        if (toward.lengthSqr() < 1.0E-4) {
            return;
        }
        Vec3 pull = toward.normalize().scale(config.num("pull"));
        player.setDeltaMovement(pull.x, config.num("lift"), pull.z);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        Nearby.sound(player, SoundEvents.FISHING_BOBBER_SPLASH, hook.position(), config.tone("sound"));
    }

    private static void oddCatch(ServerPlayer player) {
        FishingHook hook = hook(player);
        if (hook == null) {
            return;
        }
        Config.Section config = Config.event("odd_catch");
        List<String> items = config.texts("items");
        ItemStack catchStack = new ItemStack(BuiltInRegistries.ITEM
                .getOptional(Identifier.parse(items.get(player.getRandom().nextInt(items.size()))))
                .orElse(Items.BONE));
        if (catchStack.is(Items.PLAYER_HEAD)) {
            catchStack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(player.getGameProfile()));
        }
        ServerLevel level = player.level();
        Vec3 at = hook.position();
        Vec3 to = player.position().subtract(at);
        // As vanilla throws a catch to the player.
        ItemEntity item = new ItemEntity(
                level,
                at.x,
                at.y,
                at.z,
                catchStack,
                to.x * 0.1,
                to.y * 0.1 + Math.sqrt(to.length()) * 0.08,
                to.z * 0.1);
        level.addFreshEntity(item);
        Nearby.sound(player, SoundEvents.FISHING_BOBBER_SPLASH, at, config.tone("sound"));
        // Whatever is not picked up is not meant to stay.
        Restore.after(level, config.integer("vanish_ticks"), force -> {
            item.discard();
            return true;
        });
    }
}
