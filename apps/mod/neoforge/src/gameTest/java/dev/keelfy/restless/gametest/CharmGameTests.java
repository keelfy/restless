package dev.keelfy.restless.gametest;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Charms;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.haunt.Sanity;
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
            "mimic_double",
            "dark_cloak",
            "borrowed_dream",
            "death_wail",
            "grudge",
            "way_back",
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
            helper.succeed();
        });
        tests.put("trickster_coin_doubles_or_takes_the_drops", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 1, 1, 1);
            use(helper, player, "trickster_coin");
            boolean won = false;
            boolean lost = false;
            for (int i = 0; i < 200 && !(won && lost); i++) {
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
        tests.put("mimic_double_draws_hostiles_away", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 1, 1, 1);
            player.setPos(zombie.position().add(2, 0, 0));
            zombie.setTarget(player);
            use(helper, player, "mimic_double");
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
        tests.put("dark_cloak_hides_far_hostiles_in_the_dark", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 1, 1, 1);
            BlockPos at = helper.absolutePos(new BlockPos(6, 2, 6));
            for (BlockPos pos : BlockPos.betweenClosed(at.offset(-5, -1, -5), at.offset(5, 3, 5))) {
                boolean shell = pos.getX() == at.getX() - 5
                        || pos.getX() == at.getX() + 5
                        || pos.getZ() == at.getZ() - 5
                        || pos.getZ() == at.getZ() + 5
                        || pos.getY() == at.getY() - 1
                        || pos.getY() == at.getY() + 3;
                helper.getLevel()
                        .setBlock(pos, shell ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
            }
            player.setPos(at.getX() - 3.5, at.getY(), at.getZ() + 0.5);
            zombie.setPos(player.position().add(1.5, 0, 0));
            helper.runAfterDelay(20, () -> {
                check(helper, !Charms.unseen(zombie, player), "without the cloak a hostile must see the player");
                player.setItemInHand(InteractionHand.OFF_HAND, stack("dark_cloak"));
                check(helper, !Charms.unseen(zombie, player), "a hostile close by must still see the cloaked player");
                zombie.setPos(player.position().add(7, 0, 0));
                check(
                        helper,
                        Charms.unseen(zombie, player),
                        "a hostile far off must not see the cloaked player in the dark");
                helper.succeed();
            });
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
            sanityChannel(player);
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

    /** The sanity payload may go only to a client that has its channel; the mock player has none, so it gets one. */
    private static void sanityChannel(ServerPlayer player) {
        net.neoforged.neoforge.network.registration.ChannelAttributes.getOrCreateCommonChannels(
                        player.connection.getConnection(), net.minecraft.network.ConnectionProtocol.PLAY)
                .add(dev.keelfy.restless.haunt.SanityPayload.TYPE.id());
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
