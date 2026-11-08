package dev.keelfy.restless.gametest;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.AshFootprint;
import dev.keelfy.restless.content.Charges;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.Draughts;
import dev.keelfy.restless.content.DuskSalt;
import dev.keelfy.restless.content.MagpieFeather;
import dev.keelfy.restless.content.MimicBlock;
import dev.keelfy.restless.content.MimicBlockEntity;
import dev.keelfy.restless.content.OmenCandle;
import dev.keelfy.restless.content.RagEntity;
import dev.keelfy.restless.content.RestlessKnot;
import dev.keelfy.restless.content.SightingMark;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.HauntTier;
import dev.keelfy.restless.haunt.Sanity;
import dev.keelfy.restless.haunt.Spells;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The consumables of the spirits: what each does when used, and what it leaves undone when it cannot. */
final class ConsumableGameTests {
    private static final List<String> IDS = List.of(
            "still_water",
            "dreamless_draught",
            "wayfinders_ash",
            "magpie_feather",
            "changeling_dust",
            "bloodied_rag",
            "wraith_oil",
            "infernal_contract",
            "restless_knot",
            "dusk_salt",
            "sighting_mark",
            "mimic_clay",
            "omen_candle",
            "domovois_porridge");

    private ConsumableGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("consumables_every_spirit_has_one_and_a_recipe", helper -> {
            check(helper, IDS.size() == Content.ESSENCE.size(), "a spirit has no consumable, or one has two");
            for (String id : IDS) {
                Identifier item = Identifier.fromNamespaceAndPath(Restless.MOD_ID, id);
                check(helper, BuiltInRegistries.ITEM.containsKey(item), id + " is not an item");
                ResourceKey<Recipe<?>> recipe = ResourceKey.create(Registries.RECIPE, item);
                check(
                        helper,
                        helper.getLevel().recipeAccess().byKey(recipe).isPresent(),
                        id + " cannot be crafted from its essence");
            }
            helper.succeed();
        });
        tests.put("still_water_clears_the_sky_or_calls_a_storm", helper -> {
            ServerPlayer player = fresh(helper);
            var server = helper.getLevel().getServer();
            server.setWeatherParameters(0, 1000, true, true);
            check(helper, Draughts.still(player, helper.getLevel()), "still water must take effect");
            check(
                    helper,
                    !server.getWeatherData().isRaining()
                            && !server.getWeatherData().isThundering(),
                    "the water must clear rain and thunder");
            check(
                    helper,
                    server.getWeatherData().getClearWeatherTime()
                            == Config.section("consumables").integer("still_clear_ticks"),
                    "the clear sky must last the day");
            player.setShiftKeyDown(true);
            check(helper, Draughts.still(player, helper.getLevel()), "sneaking, the water must take effect");
            check(
                    helper,
                    server.getWeatherData().isRaining()
                            && server.getWeatherData().isThundering(),
                    "sneaking, the water must call a storm");
            ItemStack drunk = Content.STILL_WATER
                    .get()
                    .finishUsingItem(new ItemStack(Content.STILL_WATER.get()), helper.getLevel(), player);
            check(helper, drunk.is(Items.GLASS_BOTTLE), "the drinker must be left the bottle");
            server.setWeatherParameters(6000, 0, false, false);
            discard(player);
            helper.succeed();
        });
        tests.put("dreamless_draught_lets_one_sleep_through_the_night", helper -> {
            ServerPlayer player = fresh(helper);
            var server = helper.getLevel().getServer();
            sanityChannel(player);
            check(helper, !Restless.onSleepProblem(player), "without the draught a bed's problem must stand");
            check(helper, Draughts.dreamless(player, helper.getLevel()), "the draught must be drunk");
            check(helper, Restless.onSleepProblem(player), "a drinker must sleep however near the monsters");
            check(helper, !Draughts.dreamless(player, helper.getLevel()), "one draught at a time");
            int before = server.getGameRules().get(GameRules.PLAYERS_SLEEPING_PERCENTAGE);
            Sanity.set(player, 0.2);
            try {
                BlockPos bed = helper.absolutePos(new BlockPos(2, 1, 2));
                helper.getLevel().setBlock(bed, Blocks.BED.red().defaultBlockState(), 3);
                check(helper, player.startSleeping(bed), "the drinker must lie down");
                sleepTimer(player, 100);
                Draughts.tick(server);
                check(
                        helper,
                        server.getGameRules().get(GameRules.PLAYERS_SLEEPING_PERCENTAGE) == 0,
                        "a drinker deep asleep must count as everyone");
                player.stopSleeping();
                Draughts.tick(server);
                check(
                        helper,
                        server.getGameRules().get(GameRules.PLAYERS_SLEEPING_PERCENTAGE) == before,
                        "the sleeping percentage must come back");
                check(helper, !Draughts.dreamless(player), "the draught must be slept off");
                check(helper, Sanity.of(player) > 0.2, "waking must give sanity back");
            } finally {
                server.getGameRules().set(GameRules.PLAYERS_SLEEPING_PERCENTAGE, before, server);
                discard(player);
            }
            helper.succeed();
        });
        tests.put("wayfinders_ash_lays_a_trail_to_where_the_player_fell", helper -> {
            ServerPlayer player = fresh(helper);
            ServerLevel level = helper.getLevel();
            for (BlockPos pos : BlockPos.betweenClosed(
                    helper.absolutePos(new BlockPos(0, 0, 0)), helper.absolutePos(new BlockPos(24, 0, 6)))) {
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(pos.above(2), Blocks.AIR.defaultBlockState(), 3);
            }
            player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 2))));
            ItemStack ash = new ItemStack(Content.WAYFINDERS_ASH.get(), 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, ash);
            check(
                    helper,
                    !ash.use(level, player, InteractionHand.MAIN_HAND).consumesAction() && ash.getCount() == 2,
                    "with nowhere to lead, the ash must stay unspent");
            player.setLastDeathLocation(
                    Optional.of(GlobalPos.of(level.dimension(), helper.absolutePos(new BlockPos(20, 1, 2)))));
            check(
                    helper,
                    ash.use(level, player, InteractionHand.MAIN_HAND).consumesAction() && ash.getCount() == 1,
                    "the ash must lead to the place of death and be spent");
            check(
                    helper,
                    !ash.use(level, player, InteractionHand.MAIN_HAND).consumesAction() && ash.getCount() == 1,
                    "one trail at a time");
            helper.runAfterDelay(30, () -> {
                AABB path = new AABB(helper.absolutePos(new BlockPos(0, 0, 0)))
                        .inflate(0, 0, 0)
                        .expandTowards(24, 3, 6);
                int prints = level.getEntitiesOfClass(AshFootprint.class, path).size();
                check(helper, prints >= 4, "the trail must show footprints ahead; saw " + prints);
                level.getEntitiesOfClass(AshFootprint.class, path).forEach(AshFootprint::discard);
                discard(player);
                helper.succeed();
            });
        });
        tests.put("magpie_feather_finds_loot_nobody_has_opened", helper -> {
            ServerPlayer player = fresh(helper);
            ServerLevel level = helper.getLevel();
            BlockPos chest = helper.absolutePos(new BlockPos(3, 1, 3));
            level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
            RandomizableContainer container = (RandomizableContainer) level.getBlockEntity(chest);
            container.setLootTable(BuiltInLootTables.SIMPLE_DUNGEON);
            player.setPos(Vec3.atBottomCenterOf(chest.offset(5, 0, 0)));
            check(
                    helper,
                    chest.equals(MagpieFeather.nearest(level, player.blockPosition(), 128)),
                    "the feather must find the unopened chest");
            ItemStack feather = new ItemStack(Content.MAGPIE_FEATHER.get(), 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, feather);
            check(
                    helper,
                    feather.use(level, player, InteractionHand.MAIN_HAND).consumesAction() && feather.getCount() == 1,
                    "a feather that finds something must be spent");
            container.setLootTable(null);
            check(
                    helper,
                    !chest.equals(MagpieFeather.nearest(level, player.blockPosition(), 128)),
                    "a chest whose loot is rolled must not be pointed at");
            level.setBlock(chest, Blocks.AIR.defaultBlockState(), 3);
            discard(player);
            helper.succeed();
        });
        tests.put("changeling_dust_changes_a_block_within_its_family_and_a_new_villager", helper -> {
            ServerPlayer player = fresh(helper);
            ServerLevel level = helper.getLevel();
            BlockPos at = helper.absolutePos(new BlockPos(2, 1, 2));
            level.setBlock(at, Blocks.WOOL.red().defaultBlockState(), 3);
            ItemStack dust = new ItemStack(Content.CHANGELING_DUST.get(), 3);
            check(helper, useOn(level, player, dust, at).consumesAction(), "the dust must turn wool into wool");
            check(
                    helper,
                    level.getBlockState(at).is(net.minecraft.tags.BlockTags.WOOL)
                            && !level.getBlockState(at).is(Blocks.WOOL.red()),
                    "wool must become another wool");
            level.setBlock(at, Blocks.DIAMOND_ORE.defaultBlockState(), 3);
            check(helper, !useOn(level, player, dust, at).consumesAction(), "an ore must be left alone");
            check(helper, level.getBlockState(at).is(Blocks.DIAMOND_ORE), "an ore must stay what it is");
            level.setBlock(at, Blocks.CHEST.defaultBlockState(), 3);
            check(helper, !useOn(level, player, dust, at).consumesAction(), "a block entity must be left alone");
            level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
            check(helper, dust.getCount() == 2, "only the turned block may spend dust");
            Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, 4, 1, 4);
            villager.setVillagerData(villager.getVillagerData()
                    .withProfession(level.registryAccess()
                            .lookupOrThrow(Registries.VILLAGER_PROFESSION)
                            .getOrThrow(VillagerProfession.FARMER)));
            var offers = villager.getOffers();
            check(
                    helper,
                    dust.interactLivingEntity(player, villager, InteractionHand.MAIN_HAND)
                            .consumesAction(),
                    "a villager who never traded must be dealt new trades");
            check(helper, villager.getOffers() != offers, "the trades must be new");
            villager.setVillagerXp(5);
            var traded = villager.getOffers();
            check(
                    helper,
                    !dust.interactLivingEntity(player, villager, InteractionHand.MAIN_HAND)
                            .consumesAction(),
                    "a villager who traded must be left as they are");
            check(helper, villager.getOffers() == traded, "their trades must stand");
            discard(player);
            helper.succeed();
        });
        tests.put("bloodied_rag_turns_a_mobs_kin_on_it", helper -> {
            ServerPlayer player = fresh(helper);
            ServerLevel level = helper.getLevel();
            Zombie victim = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 5, 1, 2);
            Zombie kin = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 5, 1, 6);
            RagEntity rag = new RagEntity(level, player, new ItemStack(Content.BLOODIED_RAG.get()));
            rag.setPos(
                    Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 1, 2))).add(0, 0.5, 0));
            rag.setOwner(player);
            rag.shoot(1, 0, 0, 1.0f, 0.0f);
            level.addFreshEntity(rag);
            helper.runAfterDelay(30, () -> {
                check(helper, kin.getTarget() == victim, "the mob's kin must go for the smeared one");
                check(helper, victim.hasEffect(MobEffects.GLOWING), "the smeared mob must show");
                check(helper, rag.isRemoved(), "the rag must be spent where it struck");
                discard(player);
                helper.succeed();
            });
        });
        tests.put("wraith_oil_coats_a_weapon_and_it_bites_in_the_dark", helper -> {
            ServerPlayer player = fresh(helper);
            ServerLevel level = helper.getLevel();
            ItemStack oil = new ItemStack(Content.WRAITH_OIL.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, oil);
            check(
                    helper,
                    !oil.use(level, player, InteractionHand.MAIN_HAND).consumesAction(),
                    "oil must not coat an empty hand");
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            player.setItemInHand(InteractionHand.OFF_HAND, sword);
            check(
                    helper,
                    oil.use(level, player, InteractionHand.MAIN_HAND).consumesAction(),
                    "oil must coat a weapon in the other hand");
            Charges charges = sword.get(Content.CHARGES.get());
            check(
                    helper,
                    charges != null
                            && charges.left() == Config.section("consumables").integer("oil_charges"),
                    "the coat must hold its charges");
            check(helper, player.getInventory().countItem(Items.GLASS_BOTTLE) == 1, "the bottle must come back");
            BlockPos at = helper.absolutePos(new BlockPos(4, 2, 4));
            room(helper, at);
            player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 4, 2, 5);
            zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
            player.setItemInHand(InteractionHand.MAIN_HAND, sword);
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            sword.set(Content.CHARGES.get(), new Charges(3));
            helper.runAfterDelay(10, () -> {
                var source = player.damageSources().playerAttack(player);
                float dealt = Restless.onHurt(zombie, source, 4.0f);
                check(helper, dealt >= 4.0f * 1.5f, "an oiled blow in the dark must hit harder; dealt " + dealt);
                check(helper, sword.get(Content.CHARGES.get()).left() == 2, "a blow must cost a charge");
                sword.remove(Content.CHARGES.get());
                check(helper, Restless.onHurt(zombie, source, 4.0f) == 4.0f, "a plain weapon must hit as ever");
                discard(player);
                helper.succeed();
            });
        });
        tests.put("infernal_contract_raises_an_enchantment_past_its_peak_once", helper -> {
            ServerPlayer player = fresh(helper);
            ServerLevel level = helper.getLevel();
            var sharpness =
                    level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
            var vanishing = level.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.VANISHING_CURSE);
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.enchant(sharpness, sharpness.value().getMaxLevel());
            player.setItemInHand(InteractionHand.OFF_HAND, sword);
            ItemStack contract = new ItemStack(Content.INFERNAL_CONTRACT.get(), 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, contract);
            check(
                    helper,
                    contract.use(level, player, InteractionHand.MAIN_HAND).consumesAction() && contract.getCount() == 1,
                    "the contract must be signed and spent");
            ItemEnchantments after = sword.get(DataComponents.ENCHANTMENTS);
            check(
                    helper,
                    after.getLevel(sharpness) == sharpness.value().getMaxLevel() + 1,
                    "sharpness must stand one past its peak");
            check(helper, after.getLevel(vanishing) == 1, "the item must take the curse");
            check(
                    helper,
                    !contract.use(level, player, InteractionHand.MAIN_HAND).consumesAction()
                            && contract.getCount() == 1,
                    "an item is signed once");
            ItemStack weak = new ItemStack(Items.DIAMOND_SWORD);
            weak.enchant(sharpness, 3);
            player.setItemInHand(InteractionHand.OFF_HAND, weak);
            check(
                    helper,
                    !contract.use(level, player, InteractionHand.MAIN_HAND).consumesAction(),
                    "nothing at its peak, nothing to raise");
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.ENCHANTED_BOOK));
            check(
                    helper,
                    !contract.use(level, player, InteractionHand.MAIN_HAND).consumesAction(),
                    "a book cannot take the contract");
            discard(player);
            helper.succeed();
        });
        HauntEventGameTests.TIMEOUT.put("omen_candle_burns_a_day_and_announces_harm", 200);
        tests.put("omen_candle_burns_a_day_and_announces_harm", helper -> {
            ServerPlayer player = fresh(helper);
            ServerLevel level = helper.getLevel();
            BlockPos at = helper.absolutePos(new BlockPos(3, 1, 3));
            level.setBlock(at, Content.OMEN_CANDLE.get().defaultBlockState(), 3);
            player.setPos(Vec3.atBottomCenterOf(at.offset(2, 0, 0)));
            check(helper, !OmenCandle.warn(player), "an unlit candle must not warn");
            level.getBlockState(at)
                    .useItemOn(
                            new ItemStack(Items.FLINT_AND_STEEL),
                            level,
                            player,
                            InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false));
            check(helper, AbstractCandleBlock.isLit(level.getBlockState(at)), "fire must light it, as a candle");
            check(helper, OmenCandle.warn(player), "a lit candle near must warn");
            player.setPos(Vec3.atBottomCenterOf(at.offset(40, 0, 0)));
            check(helper, !OmenCandle.warn(player), "a candle far away must not");
            player.setPos(Vec3.atBottomCenterOf(at.offset(2, 0, 0)));
            AtomicInteger ran = new AtomicInteger();
            HauntEvent harm = new HauntEvent() {
                @Override
                public Identifier id() {
                    return Identifier.fromNamespaceAndPath("restless_tests", "omen");
                }

                @Override
                public HauntTier tier() {
                    return HauntTier.HARMFUL;
                }

                @Override
                public Set<EventTag> tags() {
                    return Set.of();
                }

                @Override
                public boolean canRun(ServerPlayer p) {
                    return true;
                }

                @Override
                public void run(ServerPlayer p) {
                    ran.incrementAndGet();
                }
            };
            check(
                    helper,
                    Director.fireUnlessWarded(player, harm, player.position()) == Director.Outcome.FIRED,
                    "the event must go ahead");
            check(helper, ran.get() == 0, "the candle's wail must come before the harm");
            helper.runAfterDelay(Config.section("consumables").integer("omen_warn_ticks") + 10, () -> {
                check(helper, ran.get() == 1, "the harm must follow the warning");
                level.getBlockState(at).tick(level, at, level.getRandom());
                check(helper, level.getBlockState(at).isAir(), "a day on, the candle must be gone");
                discard(player);
                helper.succeed();
            });
        });
        tests.put("restless_knot_takes_in_what_falls_near_a_container", helper -> {
            ServerPlayer player = fresh(helper);
            ServerLevel level = helper.getLevel();
            BlockPos chest = helper.absolutePos(new BlockPos(3, 1, 3));
            level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
            ItemStack knot = new ItemStack(Content.RESTLESS_KNOT.get(), 3);
            check(helper, useOn(level, player, knot, chest).consumesAction(), "the knot must tie a container");
            check(helper, knot.getCount() == 2, "tying must spend a knot");
            check(helper, !useOn(level, player, knot, chest).consumesAction(), "one knot to a container");
            check(helper, knot.getCount() == 2, "a second knot must stay in hand");
            ItemEntity dropped = new ItemEntity(
                    level, chest.getX() + 4.5, chest.getY() + 1, chest.getZ() + 0.5, new ItemStack(Items.DIAMOND, 3));
            dropped.setPickUpDelay(0);
            level.addFreshEntity(dropped);
            RestlessKnot.tick(level.getServer());
            ChestBlockEntity entity = (ChestBlockEntity) level.getBlockEntity(chest);
            check(helper, entity.countItem(Items.DIAMOND) == 3, "the knot must take the dropped items in");
            check(helper, dropped.isRemoved(), "the taken items must leave the ground");
            for (int slot = 0; slot < entity.getContainerSize(); slot++) {
                entity.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
            }
            ItemEntity lost = new ItemEntity(
                    level, chest.getX() + 4.5, chest.getY() + 1, chest.getZ() + 0.5, new ItemStack(Items.EMERALD, 2));
            lost.setPickUpDelay(0);
            level.addFreshEntity(lost);
            RestlessKnot.tick(level.getServer());
            check(helper, !lost.isRemoved(), "a full container must let items lie");
            lost.discard();
            level.setBlock(chest, Blocks.AIR.defaultBlockState(), 3);
            Spells spells = Spells.get(level.getServer());
            spells.all(Spells.Kind.KNOT).forEach(spells::remove);
            discard(player);
            helper.succeed();
        });
        tests.put("dusk_salt_keeps_hostiles_from_spawning_of_themselves", helper -> {
            ServerPlayer player = fresh(helper);
            ServerLevel level = helper.getLevel();
            BlockPos at = helper.absolutePos(new BlockPos(2, 0, 2));
            level.setBlock(at, Blocks.STONE.defaultBlockState(), 3);
            Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.NATURAL);
            Cow cow = EntityTypes.COW.create(level, EntitySpawnReason.NATURAL);
            zombie.setPos(Vec3.atBottomCenterOf(at.offset(5, 1, 0)));
            cow.setPos(Vec3.atBottomCenterOf(at.offset(5, 1, 0)));
            check(helper, !DuskSalt.keeps(level, zombie, EntitySpawnReason.NATURAL), "without salt a zombie spawns");
            ItemStack salt = new ItemStack(Content.DUSK_SALT.get(), 2);
            check(helper, useOn(level, player, salt, at).consumesAction() && salt.getCount() == 1, "salt is scattered");
            check(helper, DuskSalt.keeps(level, zombie, EntitySpawnReason.NATURAL), "salt must keep a hostile out");
            check(helper, !DuskSalt.keeps(level, zombie, EntitySpawnReason.SPAWNER), "a spawner must not be stopped");
            check(helper, !DuskSalt.keeps(level, cow, EntitySpawnReason.NATURAL), "a cow is no hostile");
            zombie.setPos(Vec3.atBottomCenterOf(at.offset(30, 1, 0)));
            check(
                    helper,
                    !DuskSalt.keeps(level, zombie, EntitySpawnReason.NATURAL),
                    "far from the salt, nothing keeps it");
            Spells spells = Spells.get(level.getServer());
            spells.all(Spells.Kind.DUSK).forEach(spells::remove);
            discard(player);
            helper.succeed();
        });
        tests.put("sighting_mark_stands_glows_fades_and_is_limited", helper -> {
            ServerPlayer player = fresh(helper);
            ServerLevel level = helper.getLevel();
            BlockPos at = helper.absolutePos(new BlockPos(2, 0, 2));
            level.setBlock(at, Blocks.STONE.defaultBlockState(), 3);
            ItemStack mark = new ItemStack(Content.SIGHTING_MARK.get(), 20);
            mark.set(DataComponents.CUSTOM_NAME, Component.literal("Camp"));
            int max = Config.section("consumables").integer("mark_max");
            for (int i = 0; i < max; i++) {
                check(helper, useOn(level, player, mark, at).consumesAction(), "mark " + i + " must stand");
            }
            check(helper, !useOn(level, player, mark, at).consumesAction(), "no more than the limit");
            check(helper, mark.getCount() == 20 - max, "a refused mark stays in hand");
            AABB around = new AABB(at).inflate(3);
            List<SightingMark> stands = level.getEntitiesOfClass(SightingMark.class, around);
            check(helper, stands.size() == max, "the limit must stand; saw " + stands.size());
            check(
                    helper,
                    stands.get(0).isCurrentlyGlowing() && stands.get(0).hasCustomName(),
                    "a mark must glow and carry its name");
            check(helper, Spells.get(level.getServer()).marks(player.getUUID()) == max, "the marks must be counted");
            SightingMark old = stands.get(0);
            old.setUp(
                    player.getUUID(),
                    level.getGameTime() - Config.section("consumables").integer("mark_ticks") - 1);
            old.tick();
            check(helper, old.isRemoved(), "a mark must fade when its days are over");
            stands.stream().filter(m -> !m.isRemoved()).forEach(SightingMark::discard);
            check(helper, Spells.get(level.getServer()).marks(player.getUUID()) == 0, "gone marks must not count");
            discard(player);
            helper.succeed();
        });
        tests.put("mimic_clay_takes_a_look_and_wears_it_as_a_solid_block", helper -> {
            ServerPlayer player = fresh(helper);
            ServerLevel level = helper.getLevel();
            BlockPos stone = helper.absolutePos(new BlockPos(2, 1, 2));
            level.setBlock(stone, Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(stone.above(), Blocks.AIR.defaultBlockState(), 3);
            BlockPos barrier = helper.absolutePos(new BlockPos(5, 1, 2));
            level.setBlock(barrier, Blocks.BARRIER.defaultBlockState(), 3);
            ItemStack clay = new ItemStack(Content.MIMIC_CLAY.get(), 3);
            check(
                    helper,
                    !useOn(level, player, clay, barrier).consumesAction(),
                    "a block never drawn has no look to take");
            check(helper, !clay.has(Content.LOOK.get()), "the clay must stay plain");
            check(helper, useOn(level, player, clay, stone).consumesAction(), "stone must be pressed");
            check(
                    helper,
                    clay.get(Content.LOOK.get()).state().is(Blocks.STONE),
                    "the clay must hold the look of stone");
            check(helper, clay.getCount() == 3, "pressing must not spend the clay");
            check(helper, useOn(level, player, clay, stone).consumesAction(), "the clay must be set down");
            BlockPos mimic = stone.above();
            check(helper, level.getBlockState(mimic).getBlock() instanceof MimicBlock, "a mimic must stand there");
            MimicBlockEntity entity = (MimicBlockEntity) level.getBlockEntity(mimic);
            check(helper, entity.look().is(Blocks.STONE), "the mimic must wear stone");
            check(helper, clay.getCount() == 2, "setting it down must spend one");
            check(
                    helper,
                    level.getBlockState(mimic).getDestroyProgress(player, level, mimic)
                            == Blocks.STONE.defaultBlockState().getDestroyProgress(player, level, mimic),
                    "it must break as slowly as what it shows");
            check(helper, !entity.revealed(level.getGameTime()), "a mimic must be hidden");
            player.setPos(Vec3.atBottomCenterOf(mimic.offset(3, 0, 0)));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Content.DARK_EYE.get()));
            player.getMainHandItem().use(level, player, InteractionHand.MAIN_HAND);
            check(helper, entity.revealed(level.getGameTime()), "the Dark Eye must show its true face");
            level.getBlockState(mimic).getBlock().playerWillDestroy(level, mimic, level.getBlockState(mimic), player);
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(mimic).inflate(2));
            check(
                    helper,
                    drops.stream()
                            .anyMatch(d -> d.getItem().is(Content.MIMIC_CLAY.get())
                                    && d.getItem().has(Content.LOOK.get())),
                    "a broken mimic must give the pressed clay back");
            drops.forEach(ItemEntity::discard);
            level.setBlock(mimic, Blocks.AIR.defaultBlockState(), 3);
            discard(player);
            helper.succeed();
        });
        tests.put("domovois_porridge_drops_only_whole", helper -> {
            ServerLevel level = helper.getLevel();
            BlockPos at = helper.absolutePos(new BlockPos(4, 2, 4));
            var block = Content.DOMOVOIS_PORRIDGE.get();
            for (int left = 1; left <= 8; left++) {
                var state = block.defaultBlockState().setValue(dev.keelfy.restless.content.DomovoiPorridge.LEFT, left);
                var drops = state.getDrops(new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                        .withParameter(
                                net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
                                Vec3.atCenterOf(at))
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, state)
                        .withParameter(
                                net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL,
                                ItemStack.EMPTY));
                check(helper, drops.size() == (left == 8 ? 1 : 0), "only a whole bowl drops, left=" + left);
            }
            helper.succeed();
        });
        tests.put("domovois_porridge_feeds_growth_in_one_shut_room", helper -> {
            ServerPlayer player = fresh(helper);
            ServerLevel level = helper.getLevel();
            BlockPos at = helper.absolutePos(new BlockPos(4, 2, 4));
            room(helper, at);
            BlockPos floor = at.below();
            ItemStack bowl = new ItemStack(Content.DOMOVOIS_PORRIDGE_ITEM.get(), 3);
            BlockPos open = helper.absolutePos(new BlockPos(30, 0, 30));
            level.setBlock(open, Blocks.STONE.defaultBlockState(), 3);
            check(helper, !useOn(level, player, bowl, open).consumesAction(), "an open place must refuse the bowl");
            check(helper, bowl.getCount() == 3, "a refused bowl stays in hand");
            check(helper, useOn(level, player, bowl, floor).consumesAction(), "a shut, roofed room takes the bowl");
            check(
                    helper,
                    level.getBlockState(at).is(Content.DOMOVOIS_PORRIDGE.get()),
                    "the bowl must stand in the room");
            check(
                    helper,
                    !useOn(level, player, bowl, floor.offset(1, 0, 1)).consumesAction(),
                    "one bowl serves a room");
            // Soul sand with a nether wart on it needs no light to grow; sixty give the bowl a fair chance.
            BlockPos patch = at.offset(6, 0, 0);
            for (int x = 0; x < 6; x++) {
                for (int z = 0; z < 10; z++) {
                    level.setBlock(patch.offset(x, -1, z), Blocks.SOUL_SAND.defaultBlockState(), 3);
                    level.setBlock(patch.offset(x, 0, z), Blocks.NETHER_WART.defaultBlockState(), 3);
                }
            }
            Cow calf = EntityTypes.COW.create(level, EntitySpawnReason.TRIGGERED);
            calf.setBaby(true);
            calf.setPos(Vec3.atBottomCenterOf(at));
            level.addFreshEntity(calf);
            int young = calf.getAge();
            var state = level.getBlockState(at);
            for (int i = 0; i < 300; i++) {
                state.tick(level, at, level.getRandom());
            }
            boolean grown = false;
            for (int x = 0; x < 6 && !grown; x++) {
                for (int z = 0; z < 10 && !grown; z++) {
                    grown = level.getBlockState(patch.offset(x, 0, z))
                                    .getValue(net.minecraft.world.level.block.NetherWartBlock.AGE)
                            > 0;
                }
            }
            check(helper, grown, "the crops around must grow faster");
            check(helper, calf.getAge() > young, "the calf must grow up sooner");
            ItemStack key = new ItemStack(Content.HOUSE_KEY.get());
            key.setDamageValue(10);
            ItemStack mended = Restless.onAnvil(key, new ItemStack(Content.DOMOVOIS_PORRIDGE_ITEM.get()));
            check(
                    helper,
                    mended.getDamageValue()
                            == 10 - Config.section("consumables").integer("porridge_repair"),
                    "the anvil must mend the key with the bowl");
            check(
                    helper,
                    Restless.onAnvil(
                                    new ItemStack(Content.HOUSE_KEY.get()),
                                    new ItemStack(Content.DOMOVOIS_PORRIDGE_ITEM.get()))
                            .isEmpty(),
                    "a whole key needs no mending");
            calf.discard();
            discard(player);
            helper.succeed();
        });
    }

    /** A shut, roofed room of three by three round {@code at}, which is the middle of the floor's cell above it. */
    private static void room(GameTestHelper helper, BlockPos at) {
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

    private static ServerPlayer fresh(GameTestHelper helper) {
        ServerPlayer player = HauntEventGameTests.connected(helper);
        helper.getLevel().addNewPlayer(player); // scheduled steps and spells look for players in the level
        return player;
    }

    private static void discard(ServerPlayer player) {
        player.discard();
    }

    private static void sanityChannel(ServerPlayer player) {
        net.neoforged.neoforge.network.registration.ChannelAttributes.getOrCreateCommonChannels(
                        player.connection.getConnection(), net.minecraft.network.ConnectionProtocol.PLAY)
                .add(dev.keelfy.restless.haunt.SanityPayload.TYPE.id());
    }

    /** The bed timer is private; a drinker is "deep asleep" once it has run its hundred ticks. */
    private static void sleepTimer(ServerPlayer player, int ticks) {
        try {
            var field = net.minecraft.world.entity.player.Player.class.getDeclaredField("sleepCounter");
            field.setAccessible(true);
            field.setInt(player, ticks);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static InteractionResult useOn(ServerLevel level, ServerPlayer player, ItemStack stack, BlockPos at) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack.useOn(new UseOnContext(
                level,
                player,
                InteractionHand.MAIN_HAND,
                stack,
                new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false)));
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }
}
