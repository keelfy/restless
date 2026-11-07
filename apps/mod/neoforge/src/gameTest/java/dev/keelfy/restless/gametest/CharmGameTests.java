package dev.keelfy.restless.gametest;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Charms;
import dev.keelfy.restless.content.Relic;
import dev.keelfy.restless.haunt.Sanity;
import dev.keelfy.restless.spirit.SpiritType;
import dev.keelfy.restless.spirit.Spirits;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
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
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

/** What a spirit leaves behind: an essence for every type, and a charm that answers to its nature. */
final class CharmGameTests {
    /** The charms with a mechanism of their own, besides {@link Relic#ALL}. */
    private static final List<String> OWN = List.of("borrowed_face", "death_wail", "stash", "bargain", "way_back");

    private CharmGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("every_spirit_leaves_a_charm", helper -> {
            for (SpiritType type : Spirits.all()) {
                Identifier essence = Identifier.fromNamespaceAndPath(Restless.MOD_ID, type.id() + "_essence");
                check(helper, BuiltInRegistries.ITEM.containsKey(essence), type.id() + " leaves no essence");
            }
            List<String> charms = new java.util.ArrayList<>(OWN);
            Relic.ALL.forEach(k -> charms.add(k.id()));
            check(helper, charms.size() + 2 == Spirits.all().size(), "a spirit has no charm, or one has two");
            for (String id : charms) {
                ResourceKey<Recipe<?>> recipe =
                        ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(Restless.MOD_ID, id));
                check(
                        helper,
                        helper.getLevel().recipeAccess().byKey(recipe).isPresent(),
                        id + " cannot be crafted from its essence");
            }
            helper.succeed();
        });
        tests.put("relics_answer_their_spirit", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
            use(helper, player, "mara_lullaby");
            check(
                    helper,
                    !player.hasEffect(MobEffects.POISON) && player.hasEffect(MobEffects.REGENERATION),
                    "the lullaby must lift what ails and heal");
            use(helper, player, "tide_charm");
            check(helper, player.hasEffect(MobEffects.WATER_BREATHING), "the tide charm must give the water");
            ServerPlayer gambler = HauntEventGameTests.connected(helper);
            use(helper, gambler, "trickster_coin");
            check(helper, gambler.getActiveEffects().size() == 1, "the coin must land on one effect");
            check(
                    helper,
                    gambler.getCooldowns().isOnCooldown(stack("trickster_coin")),
                    "a used relic must rest before its next use");
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
        tests.put("death_wail_throws_hostiles_back_once", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 1, 1, 1);
            player.setPos(zombie.position().add(-1, 0, 0));
            player.setItemInHand(InteractionHand.OFF_HAND, stack("death_wail"));
            Restless.onHurt(player, player.damageSources().generic(), 1.0f);
            check(helper, zombie.getDeltaMovement().lengthSqr() == 0, "a wound far from death must not wake the wail");
            Restless.onHurt(player, player.damageSources().generic(), player.getHealth() - 1.0f);
            check(helper, zombie.getDeltaMovement().x > 0, "the wail must throw the hostile away from the player");
            check(
                    helper,
                    player.getCooldowns().isOnCooldown(player.getOffhandItem()),
                    "the wail must spend its charge");
            zombie.setDeltaMovement(0, 0, 0);
            Restless.onHurt(player, player.damageSources().generic(), player.getHealth() - 1.0f);
            check(helper, zombie.getDeltaMovement().lengthSqr() == 0, "a spent wail must stay quiet");
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack("death_wail"));
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
            Restless.onRespawn(player);
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
                    !player.getMainHandItem()
                            .use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                            .consumesAction(),
                    "an unpaid bargain must not be struck again");
            helper.succeed();
        });
        tests.put("way_back_returns_at_a_cost", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            net.neoforged.neoforge.network.registration.ChannelAttributes.getOrCreateCommonChannels(
                            player.connection.getConnection(), net.minecraft.network.ConnectionProtocol.PLAY)
                    .add(dev.keelfy.restless.haunt.SanityPayload.TYPE.id());
            net.minecraft.world.phys.Vec3 home = player.position();
            Sanity.set(player, 1.0);
            use(helper, player, "way_back");
            player.setPos(home.add(10, 0, 0));
            player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            check(helper, player.position().distanceTo(home) < 1.0, "the second use must walk the player back");
            check(
                    helper,
                    Sanity.of(player) <= 1.0 - Config.section("charms").num("way_sanity_cost") + 1e-6,
                    "the way must cost sanity");
            helper.succeed();
        });
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
