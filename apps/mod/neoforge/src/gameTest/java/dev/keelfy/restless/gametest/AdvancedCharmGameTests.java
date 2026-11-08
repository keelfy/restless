package dev.keelfy.restless.gametest;

import static dev.keelfy.restless.gametest.CharmGameTests.sanityChannel;
import static dev.keelfy.restless.gametest.CharmGameTests.stack;
import static dev.keelfy.restless.gametest.CharmGameTests.use;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.content.Charms;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.Rest;
import dev.keelfy.restless.haunt.Clues;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntEvents;
import dev.keelfy.restless.haunt.Sanity;
import dev.keelfy.restless.haunt.Wards;
import dev.keelfy.restless.spirit.Clue;
import dev.keelfy.restless.spirit.Trait;
import dev.keelfy.restless.spirit.Traits;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The advanced charms: a base charm and one more essence each, and what each does that its base does not. */
final class AdvancedCharmGameTests {
    private static final Map<String, String[]> UPGRADES = Map.ofEntries(
            Map.entry("fetch_path", new String[] {"lost_step", "phantom_essence"}),
            Map.entry("changeling_mask", new String[] {"borrowed_face", "trickster_essence"}),
            Map.entry("nightwalk_shroud", new String[] {"wraith_shroud", "shade_essence"}),
            Map.entry("blood_debt", new String[] {"grudge", "demon_essence"}),
            Map.entry("requital_wail", new String[] {"death_wail", "revenant_essence"}),
            Map.entry("tidecaller", new String[] {"drowned_stone", "lost_one_essence"}),
            Map.entry("magpies_hook", new String[] {"telekinesis_amulet", "hoarder_essence"}),
            Map.entry("truthseers_eye", new String[] {"dark_eye", "banshee_essence"}),
            Map.entry("hearth_dream", new String[] {"borrowed_dream", "domovoi_essence"}),
            Map.entry("keepers_key", new String[] {"house_key", "hoarder_essence"}),
            Map.entry("grave_satchel", new String[] {"stash", "lost_one_essence"}),
            Map.entry("devils_coin", new String[] {"trickster_coin", "demon_essence"}),
            Map.entry("twins_mirror", new String[] {"phantom_double", "mimic_essence"}),
            Map.entry("black_bargain", new String[] {"bargain", "wraith_essence"}));

    private AdvancedCharmGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("every_base_charm_has_an_upgrade_that_keeps_what_it_holds", helper -> {
            for (var upgrade : UPGRADES.entrySet()) {
                CraftingInput input =
                        CraftingInput.of(2, 1, List.of(stack(upgrade.getValue()[0]), stack(upgrade.getValue()[1])));
                ResourceKey<Recipe<?>> key = ResourceKey.create(
                        Registries.RECIPE, Identifier.fromNamespaceAndPath(Restless.MOD_ID, upgrade.getKey()));
                var holder = helper.getLevel().recipeAccess().byKey(key);
                check(helper, holder.isPresent(), upgrade.getKey() + " cannot be crafted");
                var recipe = (CraftingRecipe) holder.get().value();
                check(helper, recipe.matches(input, helper.getLevel()), upgrade.getKey() + " does not match its parts");
                check(
                        helper,
                        recipe.assemble(input).is(stack(upgrade.getKey()).getItem()),
                        upgrade.getKey() + " makes the wrong thing");
            }
            // A filled stash becomes a satchel with its stack, and a worn key leaves no wear on the keeper's key.
            ItemStack stash = stack("stash");
            stash.set(Content.STASHED.get(), ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 5))));
            ItemStack satchel = assemble(helper, "grave_satchel", stash, stack("lost_one_essence"));
            check(
                    helper,
                    satchel.getOrDefault(Content.STASHED.get(), ItemContainerContents.EMPTY)
                                    .copyOne()
                                    .getCount()
                            == 5,
                    "the satchel must keep the stash's stack");
            ItemStack worn = stack("house_key");
            worn.setDamageValue(3);
            ItemStack keeper = assemble(helper, "keepers_key", worn, stack("hoarder_essence"));
            check(helper, keeper.getDamageValue() == 0, "the keeper's key must not wear");
            helper.succeed();
        });
        tests.put("fetch_path_leaves_a_double_where_the_step_began", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            sanityChannel(player);
            Sanity.set(player, 1.0);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 1, 1, 1);
            BlockPos start = helper.absolutePos(new BlockPos(4, 2, 1));
            player.setPos(start.getX() + 0.5, start.getY(), start.getZ() + 0.5);
            zombie.setTarget(player);
            use(helper, player, "fetch_path");
            check(helper, player.position().z > start.getZ() + 2.0, "the step must still carry the holder");
            check(helper, zombie.getTarget() instanceof Mannequin, "hostiles must go for the double left behind");
            check(helper, Sanity.of(player) < 0.9, "the path must cost more than a plain step");
            helper.succeed();
        });
        tests.put("changeling_mask_frames_the_first_blow_and_breaks_on_the_second", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            Zombie struck = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 1, 1, 1);
            Zombie near = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 2, 1, 1);
            use(helper, player, "changeling_mask");
            var blow = player.damageSources().playerAttack(player);
            Restless.onHurt(struck, blow, 1.0f);
            check(
                    helper,
                    player.hasEffect(Charms.holder(Content.BORROWED_FACE)),
                    "the first blow must not break the mask");
            check(helper, near.getTarget() == struck, "the nearest hostile must turn on the struck one");
            Restless.onHurt(struck, blow, 1.0f);
            check(helper, !player.hasEffect(Charms.holder(Content.BORROWED_FACE)), "the second blow must break it");
            helper.succeed();
        });
        HauntEventGameTests.TIMEOUT.put("nightwalk_shroud_hides_the_holder_in_the_dark_only", 80);
        tests.put("nightwalk_shroud_hides_the_holder_in_the_dark_only", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            sanityChannel(player);
            Sanity.set(player, 1.0);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 1, 1, 1);
            BlockPos at = helper.absolutePos(new BlockPos(4, 2, 4));
            box(helper, at);
            player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            use(helper, player, "nightwalk_shroud");
            player.getInventory().setItem(5, player.getMainHandItem());
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            check(helper, Charms.unseen(zombie, player), "hostiles must lose track of a walker");
            // Light is worked out by the engine between ticks.
            helper.runAfterDelay(10, () -> {
                Charms.held(player);
                check(helper, Sanity.of(player) < 1.0, "the walk must cost sanity");
                check(helper, Charms.unseen(zombie, player), "a walk in the dark must go on");
                helper.getLevel().setBlock(at.above(), Blocks.TORCH.defaultBlockState(), 3);
                helper.runAfterDelay(10, () -> {
                    Charms.held(player);
                    check(helper, !Charms.unseen(zombie, player), "light must end the walk");
                    helper.succeed();
                });
            });
        });
        tests.put("blood_debt_has_no_cap_and_heals_on_a_kill", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            sanityChannel(player);
            Sanity.set(player, 1.0);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 1, 1, 1);
            player.setItemInHand(InteractionHand.OFF_HAND, stack("blood_debt"));
            var blow = player.damageSources().playerAttack(player);
            Restless.onHurt(player, player.damageSources().generic(), 60.0f);
            check(helper, Restless.onHurt(zombie, blow, 4.0f) == 34.0f, "the debt must pay out without a cap");
            check(helper, Sanity.of(player) < 1.0, "a release must cost sanity");
            player.setHealth(1.0f);
            Restless.onKill(player, zombie);
            check(helper, player.getHealth() > 20.0f - 1.0f, "a kill with the debt's blow must heal what was stored");
            helper.succeed();
        });
        tests.put("requital_wail_marks_those_it_throws_back", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 1, 1, 1);
            player.setPos(zombie.position().add(-1, 0, 0));
            player.getInventory().setItem(5, stack("requital_wail"));
            var blow = player.damageSources().playerAttack(player);
            check(helper, Restless.onHurt(zombie, blow, 4.0f) == 4.0f, "an unmarked mob takes the plain blow");
            Restless.onHurt(player, player.damageSources().generic(), player.getHealth() - 1.0f);
            check(helper, zombie.getDeltaMovement().x > 0, "the wail must throw the hostile back");
            check(helper, zombie.hasEffect(MobEffects.GLOWING), "the one thrown back must glow");
            check(helper, Restless.onHurt(zombie, blow, 4.0f) == 6.0f, "the one thrown back must take half again");
            helper.succeed();
        });
        tests.put("tidecaller_dashes_in_water_and_is_a_stone_in_the_off_hand", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            BlockPos at = helper.absolutePos(new BlockPos(4, 2, 4));
            player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack("tidecaller"));
            check(
                    helper,
                    !player.getMainHandItem()
                            .use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                            .consumesAction(),
                    "there must be no dash on dry ground");
            helper.getLevel().setBlock(at, Blocks.WATER.defaultBlockState(), 3);
            helper.getLevel().setBlock(at.above(), Blocks.WATER.defaultBlockState(), 3);
            player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            player.setDeltaMovement(Vec3.ZERO);
            player.baseTick();
            check(helper, player.isInWater(), "the test player must be in water");
            use(helper, player, "tidecaller");
            check(helper, player.getDeltaMovement().lengthSqr() > 1.0, "the holder must be thrown forward");
            HauntEvent water = HauntEvents.all().stream()
                    .filter(e -> e.tags().contains(EventTag.WATER))
                    .findFirst()
                    .orElseThrow();
            player.setItemInHand(InteractionHand.OFF_HAND, stack("tidecaller"));
            check(helper, Wards.stops(player, water), "the tidecaller must spare its holder the nix");
            helper.succeed();
        });
        tests.put("magpies_hook_draws_items_and_sends_the_rest_to_a_linked_chest", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            BlockPos chest = helper.absolutePos(new BlockPos(6, 2, 6));
            helper.getLevel().setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
            player.setPos(chest.getX() - 2.5, chest.getY(), chest.getZ() + 0.5);
            ItemStack hook = stack("magpies_hook");
            player.setItemInHand(InteractionHand.MAIN_HAND, hook);
            player.setShiftKeyDown(true);
            check(
                    helper,
                    hook.useOn(new UseOnContext(
                                    player,
                                    InteractionHand.MAIN_HAND,
                                    new BlockHitResult(Vec3.atCenterOf(chest), Direction.UP, chest, false)))
                            .consumesAction(),
                    "sneaking on a chest must link it");
            check(helper, hook.has(Content.HOOK_LINK.get()), "the link must be on the hook");
            player.setShiftKeyDown(false);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, hook);
            ItemEntity near = new ItemEntity(
                    helper.getLevel(), player.getX() + 4, player.getY(), player.getZ(), new ItemStack(Items.DIAMOND));
            helper.getLevel().addFreshEntity(near);
            Charms.held(player);
            check(helper, near.getDeltaMovement().x < 0, "an item in reach must be drawn to the holder");
            for (int slot = 0; slot < 36; slot++) {
                player.getInventory().setItem(slot, new ItemStack(Items.DIAMOND_SWORD));
            }
            Charms.held(player);
            Container box = (Container) helper.getLevel().getBlockEntity(chest);
            int stored = 0;
            for (int slot = 0; slot < box.getContainerSize(); slot++) {
                stored +=
                        box.getItem(slot).is(Items.DIAMOND) ? box.getItem(slot).getCount() : 0;
            }
            check(helper, stored == 1, "what does not fit must go to the chest: " + stored);
            helper.getLevel().setBlock(chest, Blocks.AIR.defaultBlockState(), 3);
            Charms.held(player);
            check(helper, !hook.has(Content.HOOK_LINK.get()), "the link must break with the container");
            helper.succeed();
        });
        tests.put("truthseers_eye_keeps_one_reading_honest_a_day", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            check(helper, !dev.keelfy.restless.content.Advanced.truthful(player, Clue.VOICE), "no eye, no truth");
            player.setItemInHand(InteractionHand.OFF_HAND, stack("truthseers_eye"));
            check(helper, dev.keelfy.restless.content.Advanced.truthful(player, Clue.VOICE), "the eye must hold");
            check(
                    helper,
                    !dev.keelfy.restless.content.Advanced.truthful(player, Clue.FOOTPRINTS),
                    "a tool that cannot lie needs no eye");
            List<Trait> liar = Traits.all().stream().filter(Trait::lies).toList();
            check(helper, !liar.isEmpty(), "the test needs a lying trait");
            for (int i = 0; i < 300; i++) {
                check(
                        helper,
                        Clues.reveal(liar, Traits.all(), Clue.VOICE, new Random(i), true)
                                .map(liar::contains)
                                .orElse(false),
                        "an honest reading must name the spirit's own trait");
            }
            dev.keelfy.restless.content.Advanced.truthSpent(player);
            check(helper, !dev.keelfy.restless.content.Advanced.truthful(player, Clue.VOICE), "the eye must rest");
            helper.succeed();
        });
        tests.put("hearth_dream_asks_for_a_shut_roofed_room", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            sanityChannel(player);
            Sanity.set(player, 0.1);
            BlockPos at = helper.absolutePos(new BlockPos(4, 2, 4));
            player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack("hearth_dream"));
            check(
                    helper,
                    !player.getMainHandItem()
                            .use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                            .consumesAction(),
                    "the dream must not come in the open");
            check(helper, Sanity.of(player) == 0.1, "nothing must be given in the open");
            box(helper, at);
            use(helper, player, "hearth_dream");
            check(helper, Sanity.of(player) == 1.0, "in a room the dream must fill sanity");
            check(helper, !Rest.ready(player.getMainHandItem(), helper.getLevel()), "the dream must rest a day");
            helper.succeed();
        });
        tests.put("keepers_key_binds_to_its_room_and_never_wears", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            BlockPos at = helper.absolutePos(new BlockPos(4, 2, 4));
            box(helper, at);
            player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            player.setItemInHand(InteractionHand.OFF_HAND, stack("keepers_key"));
            check(helper, Charms.keeps(player), "the first room must be sheltered and bound");
            check(helper, player.getOffhandItem().has(Content.KEY_ROOM.get()), "the key must know its room");
            check(helper, Charms.keeps(player), "the bound room must stay sheltered");
            check(helper, player.getOffhandItem().getDamageValue() == 0, "the key must not wear");
            player.getOffhandItem()
                    .set(Content.KEY_ROOM.get(), GlobalPos.of(helper.getLevel().dimension(), at.offset(200, 0, 0)));
            check(helper, !Charms.keeps(player), "elsewhere the key must do nothing");
            helper.succeed();
        });
        tests.put("grave_satchel_holds_three_and_survives_death_once_in_three_days", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            ItemStack satchel = stack("grave_satchel");
            player.setItemInHand(InteractionHand.MAIN_HAND, satchel);
            for (int i = 0; i < 4; i++) {
                player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.DIAMOND, 1 + i));
                satchel.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            }
            // The fourth could not go in, so the satchel gave all three back and took nothing.
            check(
                    helper,
                    player.getInventory().countItem(Items.DIAMOND) >= 1 + 2 + 3,
                    "a full satchel must empty on use");
            check(helper, !satchel.has(Content.STASHED.get()), "an emptied satchel must hold nothing");
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            for (int i = 0; i < 3; i++) {
                player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.GOLD_INGOT, 1));
                satchel.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            }
            player.setItemInHand(InteractionHand.OFF_HAND, satchel);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            Restless.onKill(null, player);
            check(helper, player.getOffhandItem().isEmpty(), "the satchel must not drop with the rest");
            Restless.onRespawn(player);
            ItemStack back = ItemStack.EMPTY;
            for (int slot = 0; slot < 36; slot++) {
                if (player.getInventory().getItem(slot).is(satchel.getItem())) {
                    back = player.getInventory().getItem(slot);
                }
            }
            check(
                    helper,
                    back.is(satchel.getItem()) && !Rest.ready(back, helper.getLevel()),
                    "the satchel must come back resting");
            player.setItemInHand(InteractionHand.OFF_HAND, back);
            Restless.onKill(null, player);
            check(helper, player.getOffhandItem().is(back.getItem()), "a resting satchel must not keep itself");
            helper.succeed();
        });
        tests.put("devils_coin_wagers_every_kill_until_a_loss", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            sanityChannel(player);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 1, 1, 1);
            use(helper, player, "devils_coin");
            boolean won = false;
            boolean lost = false;
            for (int i = 0; i < 400 && !(won && lost); i++) {
                if (!player.hasEffect(Charms.holder(Content.GAMBLE))) {
                    player.addEffect(new MobEffectInstance(Charms.holder(Content.GAMBLE), 600));
                }
                List<ItemEntity> drops = new ArrayList<>();
                drops.add(new ItemEntity(helper.getLevel(), 0, 0, 0, new ItemStack(Items.BONE)));
                Restless.onDrops(player, zombie, drops);
                if (drops.isEmpty()) {
                    lost = true;
                    check(helper, !player.hasEffect(Charms.holder(Content.GAMBLE)), "a loss must end the window");
                } else {
                    won = true;
                    check(helper, player.hasEffect(Charms.holder(Content.GAMBLE)), "a win must keep the window open");
                }
            }
            check(helper, won && lost, "the devil's coin must win some and lose some");
            helper.succeed();
        });
        tests.put("twins_mirror_stands_the_double_where_the_holder_looks", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 1, 1, 1);
            BlockPos at = helper.absolutePos(new BlockPos(4, 2, 1));
            player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            zombie.setTarget(player);
            use(helper, player, "twins_mirror");
            check(helper, zombie.getTarget() instanceof Mannequin, "the hostile must go for the double");
            Mannequin twin = (Mannequin) zombie.getTarget();
            check(helper, twin.distanceTo(player) > 4.0, "the double must stand where the holder looked");
            check(helper, !twin.getMainHandItem().isEmpty(), "the double must hold the holder's item");
            helper.succeed();
        });
        tests.put("black_bargain_is_paid_in_sanity_not_hearts", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            sanityChannel(player);
            Sanity.set(player, 1.0);
            player.setHealth(1.0f);
            double max = player.getMaxHealth();
            use(helper, player, "black_bargain");
            check(helper, player.getHealth() == player.getMaxHealth(), "the bargain must heal in full");
            check(helper, player.getMaxHealth() == max, "no hearts must be taken");
            check(helper, Sanity.of(player) < 0.7, "the debt must be paid in sanity");
            MobEffectInstance strength = player.getEffect(MobEffects.STRENGTH);
            check(helper, strength != null && strength.getDuration() > 800, "the fury must last 45 seconds");
            check(
                    helper,
                    !player.getMainHandItem()
                            .use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                            .consumesAction(),
                    "the bargain must rest");
            helper.succeed();
        });
    }

    private static ItemStack assemble(GameTestHelper helper, String id, ItemStack base, ItemStack essence) {
        ResourceKey<Recipe<?>> key =
                ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(Restless.MOD_ID, id));
        var recipe = (CraftingRecipe)
                helper.getLevel().recipeAccess().byKey(key).orElseThrow().value();
        return recipe.assemble(CraftingInput.of(2, 1, List.of(base, essence)));
    }

    /** A shut, roofed, unlit room around {@code at}. */
    private static void box(GameTestHelper helper, BlockPos at) {
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-2, -1, -2), at.offset(2, 2, 2))) {
            boolean shell = pos.getX() == at.getX() - 2
                    || pos.getX() == at.getX() + 2
                    || pos.getZ() == at.getZ() - 2
                    || pos.getZ() == at.getZ() + 2
                    || pos.getY() == at.getY() - 1
                    || pos.getY() == at.getY() + 2;
            helper.getLevel()
                    .setBlock(pos, shell ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }
}
