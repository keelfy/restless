package dev.keelfy.restless.gametest;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.content.Charms;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.DarkEye;
import dev.keelfy.restless.content.Luck;
import dev.keelfy.restless.content.Rest;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntData;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntEvents;
import dev.keelfy.restless.haunt.History;
import dev.keelfy.restless.haunt.Sanity;
import dev.keelfy.restless.haunt.Wards;
import dev.keelfy.restless.spirit.SpiritType;
import dev.keelfy.restless.spirit.Spirits;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;

/** What a spirit leaves behind: an essence for every type, and a charm that answers to its nature. */
final class CharmGameTests {
    private static final List<String> CHARMS = List.of(
            "drowned_stone",
            "borrowed_face",
            "phantom_double",
            "wraith_shroud",
            "borrowed_dream",
            "death_wail",
            "grudge",
            "lost_step",
            "house_key",
            "stash",
            "bargain",
            "trickster_coin");

    private CharmGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("every_spirit_leaves_a_charm", helper -> {
            for (SpiritType type : Spirits.all()) {
                Identifier essence = Identifier.fromNamespaceAndPath(Restless.MOD_ID, type.id() + "_essence");
                check(helper, BuiltInRegistries.ITEM.containsKey(essence), type.id() + " leaves no essence");
            }
            check(helper, CHARMS.size() + 2 == Spirits.all().size(), "a spirit has no charm, or one has two");
            for (String id : CHARMS) {
                ResourceKey<Recipe<?>> recipe =
                        ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(Restless.MOD_ID, id));
                check(
                        helper,
                        helper.getLevel().recipeAccess().byKey(recipe).isPresent(),
                        id + " cannot be crafted from its essence");
            }
            helper.succeed();
        });
        tests.put("borrowed_dream_fills_sanity_and_angers", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            sanityChannel(player);
            Sanity.set(player, 0.1);
            use(helper, player, "borrowed_dream");
            check(helper, Sanity.of(player) == 1.0, "the dream must restore sanity in full");
            check(
                    helper,
                    player.getCooldowns().isOnCooldown(stack("borrowed_dream")),
                    "the dream must be a day's wait");
            // A relog clears the vanilla cooldown, not the rest the stack keeps.
            relog(player, player.getMainHandItem());
            Sanity.set(player, 0.1);
            check(
                    helper,
                    !player.getMainHandItem()
                            .use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                            .consumesAction(),
                    "a relog must not end the dream's rest");
            check(helper, Sanity.of(player) == 0.1, "a dream still resting must give nothing");
            helper.succeed();
        });
        tests.put("trickster_coin_doubles_or_takes_the_drops", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 1, 1, 1);
            use(helper, player, "trickster_coin");
            boolean won = false;
            boolean lost = false;
            for (int i = 0; i < 200 && !(won && lost); i++) {
                // A fresh run each time, so a win pays the plain double.
                player.getMainHandItem().set(Content.LUCK.get(), Luck.NONE);
                if (!player.hasEffect(Charms.holder(Content.GAMBLE))) {
                    player.addEffect(new MobEffectInstance(Charms.holder(Content.GAMBLE), 100));
                }
                List<ItemEntity> drops = new ArrayList<>();
                drops.add(new ItemEntity(helper.getLevel(), 0, 0, 0, new ItemStack(Items.BONE)));
                Restless.onDrops(player, zombie, drops);
                check(helper, !player.hasEffect(Charms.holder(Content.GAMBLE)), "one kill must settle the wager");
                check(helper, drops.size() == 0 || drops.size() == 2, "the coin must double or take all");
                won |= drops.size() == 2;
                lost |= drops.isEmpty();
            }
            check(helper, won && lost, "the coin must land on both sides");
            List<ItemEntity> plain =
                    new ArrayList<>(List.of(new ItemEntity(helper.getLevel(), 0, 0, 0, new ItemStack(Items.BONE))));
            Restless.onDrops(player, zombie, plain);
            check(helper, plain.size() == 1, "without a wager the drops must stay as they are");
            helper.succeed();
        });
        tests.put("trickster_coin_pays_more_for_a_streak_and_a_loss_resets_it", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 1, 1, 1);
            ItemStack coin = stack("trickster_coin");
            player.setItemInHand(InteractionHand.MAIN_HAND, coin);
            long now = helper.getLevel().getGameTime();
            boolean won = false;
            boolean lost = false;
            for (int i = 0; i < 200 && !(won && lost); i++) {
                coin.set(Content.LUCK.get(), new Luck(1, now));
                player.addEffect(new MobEffectInstance(Charms.holder(Content.GAMBLE), 100));
                List<ItemEntity> drops = new ArrayList<>();
                drops.add(new ItemEntity(helper.getLevel(), 0, 0, 0, new ItemStack(Items.BONE)));
                Restless.onDrops(player, zombie, drops);
                Luck after = coin.get(Content.LUCK.get());
                if (drops.isEmpty()) {
                    lost = true;
                    check(helper, after.streak() == 0, "a loss must reset the streak");
                } else {
                    won = true;
                    check(helper, drops.size() == 3, "a streak of one must pay triple: " + drops.size());
                    check(helper, after.streak() == 2, "a win must raise the streak");
                }
            }
            check(helper, won && lost, "the coin must land on both sides");
            helper.succeed();
        });
        tests.put("grudge_returns_half_of_what_hurt", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 1, 1, 1);
            player.setItemInHand(InteractionHand.OFF_HAND, stack("grudge"));
            var blow = player.damageSources().playerAttack(player);
            check(helper, Restless.onHurt(zombie, blow, 4.0f) == 4.0f, "a grudge with nothing stored must add nothing");
            Restless.onHurt(player, player.damageSources().generic(), 6.0f);
            check(helper, Restless.onHurt(zombie, blow, 4.0f) == 7.0f, "the blow must carry half of what hurt");
            check(helper, Restless.onHurt(zombie, blow, 4.0f) == 4.0f, "the grudge must be spent by one blow");
            Restless.onHurt(player, player.damageSources().generic(), 40.0f);
            check(helper, Restless.onHurt(zombie, blow, 4.0f) == 14.0f, "the grudge must stop at its cap");
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            Restless.onHurt(player, player.damageSources().generic(), 6.0f);
            player.setItemInHand(InteractionHand.OFF_HAND, stack("grudge"));
            check(helper, Restless.onHurt(zombie, blow, 4.0f) == 4.0f, "a grudge must store nothing while not held");
            helper.succeed();
        });
        tests.put("phantom_double_draws_hostiles_away", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 1, 1, 1);
            player.setPos(zombie.position().add(2, 0, 0));
            zombie.setTarget(player);
            use(helper, player, "phantom_double");
            check(
                    helper,
                    zombie.getTarget() instanceof net.minecraft.world.entity.decoration.Mannequin,
                    "the hostile must go for the double");
            check(helper, Charms.unseen(zombie, player), "the hostile must not turn back to the player");
            helper.succeed();
        });
        tests.put("drowned_stone_is_a_weight_on_land", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            Charms.held(player);
            check(helper, !player.hasEffect(MobEffects.SLOWNESS), "without the stone nothing must weigh");
            player.setItemInHand(InteractionHand.OFF_HAND, stack("drowned_stone"));
            Charms.held(player);
            check(helper, player.hasEffect(MobEffects.SLOWNESS), "the stone must weigh on dry ground");
            helper.succeed();
        });
        tests.put("wraith_shroud_lets_blows_pass_both_ways", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            sanityChannel(player);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 1, 1, 1);
            var hit = player.damageSources().mobAttack(zombie);
            var blow = player.damageSources().playerAttack(player);
            check(helper, Restless.onHurt(player, hit, 5.0f) == 5.0f, "without the shroud blows must land");
            Sanity.set(player, 1.0);
            use(helper, player, "wraith_shroud");
            check(helper, Restless.onHurt(player, hit, 5.0f) == 0.0f, "a blow must pass through the shade");
            check(helper, Restless.onHurt(zombie, blow, 5.0f) == 0.0f, "the shade's own blow must pass through");
            check(
                    helper,
                    Restless.onHurt(player, player.damageSources().fellOutOfWorld(), 5.0f) == 5.0f,
                    "the void must still reach the shade");
            check(
                    helper,
                    Restless.onHurt(player, player.damageSources().fall(), 5.0f) == 5.0f,
                    "a fall must still hurt the shade");
            check(
                    helper,
                    Restless.onHurt(player, player.damageSources().lava(), 5.0f) == 5.0f,
                    "lava must still burn the shade");
            check(helper, Sanity.of(player) < 1.0, "the shroud must cost sanity");
            helper.succeed();
        });
        tests.put("house_key_shelters_a_shut_roofed_room_and_wears", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            BlockPos at = helper.absolutePos(new BlockPos(4, 2, 4));
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
            player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            check(helper, !Charms.keeps(player), "without the key a room must not shelter");
            player.setItemInHand(InteractionHand.OFF_HAND, stack("house_key"));
            check(helper, Charms.keeps(player), "a shut, roofed room must shelter the holder");
            check(helper, player.getOffhandItem().getDamageValue() == 1, "every event stopped must wear the key");
            helper.getLevel().setBlock(at.offset(0, 2, 0), Blocks.AIR.defaultBlockState(), 3);
            check(helper, !Charms.keeps(player), "an opened room must not shelter");
            helper.succeed();
        });
        tests.put("borrowed_face_hides_until_a_blow", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 1, 1, 1);
            player.setPos(zombie.position());
            zombie.setTarget(player);
            check(helper, !Charms.unseen(zombie, player), "without the face a hostile must see the player");
            use(helper, player, "borrowed_face");
            check(helper, zombie.getTarget() == null, "a hostile that hunted the player must lose them");
            check(helper, Charms.unseen(zombie, player), "a hostile must look past the face");
            Restless.onHurt(zombie, player.damageSources().playerAttack(player), 1.0f);
            check(helper, !Charms.unseen(zombie, player), "a blow must break the face");
            helper.succeed();
        });
        tests.put("death_wail_works_from_the_inventory_and_its_rest_is_saved", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 1, 1, 1);
            player.setPos(zombie.position().add(-1, 0, 0));
            ItemStack wail = stack("death_wail");
            player.getInventory().setItem(5, wail);
            Restless.onHurt(player, player.damageSources().generic(), 1.0f);
            check(helper, zombie.getDeltaMovement().lengthSqr() == 0, "a wound far from death must not wake the wail");
            Restless.onHurt(player, player.damageSources().generic(), player.getHealth() - 1.0f);
            check(helper, zombie.getDeltaMovement().x > 0, "the wail must throw the hostile away from the player");
            check(
                    helper,
                    zombie.hasEffect(MobEffects.SLOWNESS) && zombie.hasEffect(MobEffects.DARKNESS),
                    "those thrown must be slowed and blinded");
            check(helper, !Rest.ready(wail, helper.getLevel()), "the wail must spend its rest");
            relog(player, wail);
            zombie.setDeltaMovement(0, 0, 0);
            Restless.onHurt(player, player.damageSources().generic(), player.getHealth() - 1.0f);
            check(helper, zombie.getDeltaMovement().lengthSqr() == 0, "a relog must not give the wail back");
            helper.succeed();
        });
        HauntEventGameTests.TIMEOUT.put("death_wail_keens_before_an_assault_lands", 120);
        tests.put("death_wail_keens_before_an_assault_lands", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            HauntEvent assault = HauntEvents.all().stream()
                    .filter(e -> e.tags().contains(EventTag.ASSAULT))
                    .findFirst()
                    .orElseThrow();
            String name = player.getName().getString();
            check(helper, !Charms.warn(player, null), "without a wail nothing must keen");
            player.getInventory().setItem(5, stack("death_wail"));
            int before = History.last(name, 100).size();
            check(
                    helper,
                    Director.fireUnlessWarded(player, assault, player.position()) == Director.Outcome.FIRED,
                    "a warned assault must still count as fired");
            check(helper, History.last(name, 100).size() == before, "the assault must wait out the warning");
            helper.runAfterDelay(80, () -> {
                check(helper, History.last(name, 100).size() > before, "the assault must land after the warning");
                helper.succeed();
            });
        });
        tests.put("borrowed_dream_rest_survives_a_relog", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            sanityChannel(player);
            ItemStack dream = stack("borrowed_dream");
            check(helper, Rest.ready(dream, helper.getLevel()), "a new dream must be ready");
            Rest.start(player, dream, 1000);
            relog(player, dream);
            check(helper, !Rest.ready(dream, helper.getLevel()), "the rest must not go with the cooldown");
            helper.succeed();
        });
        tests.put("stash_keeps_its_stack_through_death", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack("stash"));
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.DIAMOND, 3));
            player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            check(helper, player.getOffhandItem().isEmpty(), "the stash must take what the other hand held");
            player.setItemInHand(InteractionHand.OFF_HAND, player.getMainHandItem());
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            Restless.onKill(null, player);
            check(helper, player.getOffhandItem().isEmpty(), "the stash must not drop with the rest");
            // What a server stop keeps is what the saved data holds, so it must be there and not only in memory.
            HauntData data = HauntData.get(helper.getLevel().getServer());
            ItemStack saved = data.takeKept(player.getUUID());
            check(helper, saved.is(Items.DIAMOND) && saved.getCount() == 3, "the stack must be in the saved data");
            data.keep(player.getUUID(), saved);
            Restless.onRespawn(player);
            check(helper, data.takeKept(player.getUUID()).isEmpty(), "the stack must be given back once");
            check(
                    helper,
                    player.getInventory().countItem(Items.DIAMOND) == 3,
                    "the stack must be with the player when they return");
            helper.succeed();
        });
        tests.put("bargain_pays_in_hearts_and_is_not_struck_twice", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            player.setHealth(1.0f);
            use(helper, player, "bargain");
            check(
                    helper,
                    player.getMaxHealth() == 12.0f && player.getHealth() == 12.0f,
                    "the bargain must heal fully and take two hearts of the maximum");
            check(
                    helper,
                    player.hasEffect(MobEffects.STRENGTH) && player.hasEffect(MobEffects.RESISTANCE),
                    "the bargain must give a short fury");
            check(
                    helper,
                    !player.getMainHandItem()
                            .use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                            .consumesAction(),
                    "an unpaid bargain must not be struck again");
            helper.succeed();
        });
        tests.put("lost_step_slips_ahead_and_stops_at_thick_rock", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            sanityChannel(player);
            Sanity.set(player, 1.0);
            BlockPos start = helper.absolutePos(new BlockPos(4, 2, 1));
            player.setPos(start.getX() + 0.5, start.getY(), start.getZ() + 0.5);
            use(helper, player, "lost_step");
            double moved = player.position().z - (start.getZ() + 0.5);
            check(helper, moved >= 2.0 && moved <= 8.5, "the step must carry the holder a few blocks ahead: " + moved);
            check(helper, Sanity.of(player) < 1.0, "the step must cost sanity");
            check(helper, player.getCooldowns().isOnCooldown(stack("lost_step")), "the step must rest before the next");
            // A slab of rock three thick: the step may not cross it.
            ServerPlayer second = HauntEventGameTests.connected(helper);
            sanityChannel(second);
            Sanity.set(second, 1.0);
            BlockPos from = helper.absolutePos(new BlockPos(4, 2, 2));
            for (BlockPos pos : BlockPos.betweenClosed(from.offset(-1, -1, 2), from.offset(1, 2, 4))) {
                helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
            }
            second.setPos(from.getX() + 0.5, from.getY(), from.getZ() + 0.5);
            use(helper, second, "lost_step");
            check(helper, second.getZ() < from.getZ() + 2, "thick rock must stop the step: " + second.getZ());
            helper.succeed();
        });
        tests.put("lost_step_does_not_pass_step_proof_blocks", helper -> {
            BlockPos from = helper.absolutePos(new BlockPos(4, 2, 2));
            ServerPlayer through = HauntEventGameTests.connected(helper);
            sanityChannel(through);
            Sanity.set(through, 1.0);
            for (BlockPos pos : BlockPos.betweenClosed(from.offset(-1, -1, 2), from.offset(1, 2, 2))) {
                helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
            }
            through.setPos(from.getX() + 0.5, from.getY(), from.getZ() + 0.5);
            use(helper, through, "lost_step");
            check(
                    helper,
                    through.getZ() > from.getZ() + 3,
                    "a wall of stone one thick must be passed: " + through.getZ());
            ServerPlayer stopped = HauntEventGameTests.connected(helper);
            sanityChannel(stopped);
            Sanity.set(stopped, 1.0);
            for (BlockPos pos : BlockPos.betweenClosed(from.offset(-1, -1, 2), from.offset(1, 2, 2))) {
                helper.getLevel().setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
            }
            stopped.setPos(from.getX() + 0.5, from.getY(), from.getZ() + 0.5);
            stopped.getInventory().setItem(0, stack("lost_step"));
            stopped.getInventory().setSelectedSlot(0);
            stopped.getMainHandItem().use(helper.getLevel(), stopped, InteractionHand.MAIN_HAND);
            check(helper, stopped.getZ() < from.getZ() + 2, "obsidian must stop the step: " + stopped.getZ());
            helper.succeed();
        });
    }

    /** The extra tests of the rebalance: what the stone, the face, the eye and the amulet now do. */
    static void registerRebalance(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("drowned_stone_spares_its_holder_the_nix", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            HauntEvent water = HauntEvents.all().stream()
                    .filter(e -> e.tags().contains(EventTag.WATER))
                    .findFirst()
                    .orElseThrow();
            HauntEvent other = HauntEvents.all().stream()
                    .filter(e -> !e.tags().contains(EventTag.WATER))
                    .findFirst()
                    .orElseThrow();
            check(helper, !Wards.stops(player, water), "without the stone a water event must run");
            player.setItemInHand(InteractionHand.OFF_HAND, stack("drowned_stone"));
            check(helper, Wards.stops(player, water), "the stone's holder must be spared a water event");
            check(helper, !Wards.stops(player, other), "the stone must not stop other events");
            helper.succeed();
        });
        tests.put("borrowed_face_slips_when_a_hostile_sees_a_chest_opened", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            BlockPos at = helper.absolutePos(new BlockPos(3, 2, 3));
            helper.getLevel().setBlock(at, Blocks.CHEST.defaultBlockState(), 3);
            player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 1.5);
            use(helper, player, "borrowed_face");
            Charms.used(player, at);
            check(
                    helper,
                    player.hasEffect(Charms.holder(Content.BORROWED_FACE)),
                    "with no one near the face must hold");
            Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 3, 2, 5);
            zombie.setPos(player.getX(), player.getY(), player.getZ() + 2.0);
            Charms.used(player, helper.absolutePos(new BlockPos(0, 2, 0)));
            check(
                    helper,
                    player.hasEffect(Charms.holder(Content.BORROWED_FACE)),
                    "plain ground must not drop the face");
            Charms.used(player, at);
            check(
                    helper,
                    !player.hasEffect(Charms.holder(Content.BORROWED_FACE)),
                    "a chest opened under a hostile's eyes must drop the face");
            helper.succeed();
        });
        tests.put("telekinesis_amulet_pulls_experience", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            net.minecraft.world.entity.ExperienceOrb orb = new net.minecraft.world.entity.ExperienceOrb(
                    helper.getLevel(), player.getX() + 5, player.getY(), player.getZ(), 7);
            helper.getLevel().addFreshEntity(orb);
            use(helper, player, "telekinesis_amulet");
            check(helper, orb.position().distanceTo(player.position()) < 0.5, "the amulet must pull the orb in");
            helper.succeed();
        });
        tests.put("dark_eye_marks_where_the_spirits_are", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            HauntData data = HauntData.get(helper.getLevel().getServer());
            check(helper, DarkEye.spots(player, data, 32).isEmpty(), "with no spirit nothing must be marked");
            var spirit = Director.add(
                    player,
                    data,
                    Spirits.byId("shade").orElseThrow(),
                    helper.getLevel().getGameTime());
            check(helper, DarkEye.spots(player, data, 32).isEmpty(), "a spirit not yet roaming has no spot");
            dev.keelfy.restless.haunt.Location.roam(spirit.uuid(), player);
            check(helper, DarkEye.spots(player, data, 32).size() == 1, "a roaming spirit must be marked");
            check(helper, DarkEye.spots(player, data, 0.5).isEmpty(), "a spirit out of reach must not be marked");
            helper.succeed();
        });
    }

    /** The sanity payload may go only to a client that has its channel; the mock player has none, so it gets one. */
    private static void sanityChannel(ServerPlayer player) {
        net.neoforged.neoforge.network.registration.ChannelAttributes.getOrCreateCommonChannels(
                        player.connection.getConnection(), net.minecraft.network.ConnectionProtocol.PLAY)
                .add(dev.keelfy.restless.haunt.SanityPayload.TYPE.id());
    }

    /** A relog drops the vanilla cooldowns, which are not saved; what the stack itself keeps stays. */
    private static void relog(ServerPlayer player, ItemStack stack) {
        player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(stack));
    }

    private static ItemStack stack(String id) {
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(Restless.MOD_ID, id));
        return new ItemStack(item);
    }

    private static void use(GameTestHelper helper, ServerPlayer player, String id) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack(id));
        check(
                helper,
                player.getMainHandItem()
                        .use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                        .consumesAction(),
                id + " must answer a use");
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }
}
