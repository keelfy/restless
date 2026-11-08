package dev.keelfy.restless.content;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.EventTag;
import dev.keelfy.restless.haunt.HauntData;
import dev.keelfy.restless.haunt.HauntEvent;
import dev.keelfy.restless.haunt.Session;
import dev.keelfy.restless.haunt.Wards;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * What the charms do beyond being used: the ones that act on a blow, a death or a hostile's eye. Charms that work while
 * held work from the off hand only, so no more than one answers at a time.
 */
public final class Charms {
    private static final Identifier STONE_MINING = Identifier.fromNamespaceAndPath(Restless.MOD_ID, "drowned_stone");

    private Charms() {}

    public static Holder<MobEffect> holder(Supplier<MobEffect> effect) {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect.get());
    }

    private static boolean shade(@Nullable Entity entity) {
        return entity instanceof LivingEntity living && living.hasEffect(holder(Content.PHASED));
    }

    /** Whether a shade is on either side of this meeting: it strikes nothing and nothing strikes it. */
    public static boolean passes(@Nullable Entity first, @Nullable Entity second) {
        return shade(first) || shade(second);
    }

    /** Whether this damage is a blow between bodies that a shade is part of, so it must not land at all. */
    public static boolean passes(Entity victim, DamageSource source) {
        boolean blow = source.getEntity() != null || source.getDirectEntity() != null;
        return blow && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && passes(victim, source.getEntity());
    }

    private static boolean offhand(Entity entity, Supplier<Item> item) {
        return entity instanceof LivingEntity living
                && living.getItemInHand(InteractionHand.OFF_HAND).is(item.get());
    }

    /** The first stack of {@code item} the player carries anywhere, off hand included; empty if none. */
    static ItemStack carried(Entity entity, Supplier<Item> item) {
        if (!(entity instanceof ServerPlayer player)) {
            return ItemStack.EMPTY;
        }
        if (player.getOffhandItem().is(item.get())) {
            return player.getOffhandItem();
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** The wail a player carries, the requital wail too. */
    private static ItemStack wail(ServerPlayer player) {
        ItemStack wail = carried(player, Content.DEATH_WAIL);
        return wail.isEmpty() ? carried(player, Content.REQUITAL_WAIL) : wail;
    }

    /** What the grudge has stored and until when. */
    private record Stored(float amount, long until) {}

    private static final Map<UUID, Stored> GRUDGES = Session.SERVER.keep(new HashMap<>());

    /** Call before anything takes damage; returns the amount to deal, which a held grudge may raise. */
    public static float hurt(Entity victim, DamageSource source, float amount) {
        // A shade is hit by nothing and hits nothing, but the world still hurts it: a blow is stopped only when someone
        // struck it, and what ignores bodies altogether, the void and the like, still reaches it.
        // A faded player is touched by nothing at all, and touches nothing.
        if ((Relics.faded(victim) || Relics.faded(source.getEntity()))
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return 0.0f;
        }
        if (passes(victim, source)) {
            return 0.0f;
        }
        float dealt = Advanced.requited(victim, amount);
        if (source.getEntity() instanceof ServerPlayer attacker
                && attacker.hasEffect(holder(Content.BORROWED_FACE))
                && !Advanced.framed(attacker, victim)) {
            attacker.removeEffect(holder(Content.BORROWED_FACE));
            attacker.sendSystemMessage(Component.translatable("message.restless.face_off"), true);
        }
        if (victim instanceof ServerPlayer player) {
            ItemStack wail = wail(player);
            Config.Section config = Config.section("charms");
            if (!wail.isEmpty()
                    && Rest.ready(wail, player.level())
                    && player.getHealth() - amount < config.num("wail_health")) {
                scream(player, config);
                Rest.start(player, wail, config.integer("wail_cooldown_ticks"));
            }
        }
        return grudge(victim, source, oiled(victim, source, dealt));
    }

    /**
     * A weapon coated in wraith oil, struck with in the dark, hits harder and through armor, one charge a blow. The
     * armor is ignored by asking for as much more as it is about to take off.
     */
    private static float oiled(Entity victim, DamageSource source, float amount) {
        if (!(source.getEntity() instanceof ServerPlayer attacker)
                || source.getDirectEntity() != attacker
                || !(victim instanceof LivingEntity target)) {
            return amount;
        }
        ItemStack weapon = attacker.getMainHandItem();
        Charges charges = weapon.get(Content.CHARGES.get());
        Config.Section config = Config.section("consumables");
        if (charges == null
                || charges.left() <= 0
                || attacker.level().getMaxLocalRawBrightness(attacker.blockPosition()) > config.integer("oil_light")) {
            return amount;
        }
        if (charges.left() > 1) {
            weapon.set(Content.CHARGES.get(), new Charges(charges.left() - 1));
        } else {
            weapon.remove(Content.CHARGES.get());
        }
        float boosted = amount * (1.0f + (float) config.num("oil_bonus"));
        float soaked = CombatRules.getDamageAfterAbsorb(target, boosted, source, (float) target.getArmorValue(), (float)
                target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        attacker.level()
                .playSound(
                        null, target.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.6f, 1.5f);
        return soaked > 0.0f ? Math.min(boosted * boosted / soaked, boosted * 5.0f) : boosted;
    }

    /** A holder stores what hurts them for a while; their next blow on anyone returns part of it. */
    private static float grudge(Entity victim, DamageSource source, float amount) {
        float dealt = amount;
        long now = victim.level().getGameTime();
        Config.Section config = Config.section("charms");
        if (source.getEntity() instanceof ServerPlayer attacker
                && attacker != victim
                && (offhand(attacker, Content.GRUDGE) || offhand(attacker, Content.BLOOD_DEBT))) {
            Stored stored = GRUDGES.remove(attacker.getUUID());
            if (stored != null && stored.until() > now) {
                boolean debt = offhand(attacker, Content.BLOOD_DEBT);
                float share = (float) (stored.amount() * config.num("grudge_share"));
                dealt += debt ? share : (float) Math.min(config.num("grudge_cap"), share);
                if (debt) {
                    Advanced.owe(attacker, victim, stored.amount());
                }
                attacker.level()
                        .playSound(
                                null,
                                attacker.blockPosition(),
                                SoundEvents.WARDEN_HEARTBEAT,
                                SoundSource.PLAYERS,
                                1.5f,
                                0.6f);
            }
        }
        if (victim instanceof ServerPlayer holder
                && (offhand(holder, Content.GRUDGE) || offhand(holder, Content.BLOOD_DEBT))) {
            Stored old = GRUDGES.get(holder.getUUID());
            float kept = old != null && old.until() > now ? old.amount() : 0.0f;
            GRUDGES.put(holder.getUUID(), new Stored(kept + amount, now + config.integer("grudge_window_ticks")));
        }
        return dealt;
    }

    /** Call every server tick; the charms held in the off hand work from here, a few times a second. */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 10 != 0) {
            return;
        }
        server.getPlayerList().getPlayers().forEach(Charms::held);
    }

    /** What the charms in a player's off hand do for the moment. */
    public static void held(ServerPlayer player) {
        Config.Section config = Config.section("charms");
        Advanced.walk(player);
        Advanced.hook(player);
        boolean stone = offhand(player, Content.DROWNED_STONE) || offhand(player, Content.TIDECALLER);
        if (stone) {
            // Water is its own: breath without end there, and on dry ground the stone is a weight.
            if (player.isInWater()) {
                player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 40, 0, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 40, 0, true, false));
            } else {
                player.addEffect(
                        new MobEffectInstance(MobEffects.SLOWNESS, 40, config.integer("stone_slowness"), true, false));
            }
        }
        AttributeInstance mining = player.getAttribute(Attributes.SUBMERGED_MINING_SPEED);
        if (mining != null) {
            if (stone && player.isUnderWater()) {
                mining.addOrUpdateTransientModifier(new AttributeModifier(
                        STONE_MINING, config.num("stone_mining_bonus"), AttributeModifier.Operation.ADD_VALUE));
            } else {
                mining.removeModifier(STONE_MINING);
            }
        }
    }

    /** Whether a held stone keeps a nix off its holder: a spirit of water does not drown its own. */
    public static boolean spared(ServerPlayer player, HauntEvent event) {
        return event.tags().contains(EventTag.WATER)
                && (offhand(player, Content.DROWNED_STONE) || offhand(player, Content.TIDECALLER));
    }

    /**
     * Call before a spirit's assault lands: a carried wail keens from the spirit, and true means the event must wait
     * the warning out.
     */
    public static boolean warn(ServerPlayer player, @Nullable Vec3 at) {
        if (wail(player).isEmpty()) {
            return false;
        }
        Vec3 from = at == null ? player.position() : at;
        player.level()
                .playSound(null, from.x, from.y, from.z, SoundEvents.GHAST_SCREAM, SoundSource.HOSTILE, 2.0f, 0.5f);
        return true;
    }

    /** Call when a player opens a container or breaks a block: a face is dropped if a hostile close by would see it. */
    public static void noise(ServerPlayer player) {
        if (!player.hasEffect(holder(Content.BORROWED_FACE))) {
            return;
        }
        double radius = Config.section("charms").num("face_noise_radius");
        boolean seen = !player.level()
                .getEntitiesOfClass(
                        Mob.class,
                        player.getBoundingBox().inflate(radius),
                        m -> m instanceof Enemy && m.hasLineOfSight(player))
                .isEmpty();
        if (seen) {
            player.removeEffect(holder(Content.BORROWED_FACE));
            player.sendSystemMessage(Component.translatable("message.restless.face_off"), true);
        }
    }

    /** Call when a player uses a block. */
    public static void used(ServerPlayer player, BlockPos pos) {
        if (player.level().getBlockEntity(pos) instanceof Container
                || player.level().getBlockState(pos).is(Blocks.ENDER_CHEST)) {
            noise(player);
        }
    }

    /**
     * Call when a wagered mob dies and its drops are settled: the coin multiplies them and the experience, or takes
     * them all. Each win lifts the next one's payout, see {@link Luck}.
     */
    public static void drops(@Nullable Entity killer, LivingEntity dead, Collection<ItemEntity> drops) {
        if (!(killer instanceof ServerPlayer player)
                || !(dead instanceof Mob)
                || !player.hasEffect(holder(Content.GAMBLE))) {
            return;
        }
        Config.Section config = Config.section("charms");
        ItemStack devil = carried(player, Content.DEVILS_COIN);
        ItemStack coin = devil.isEmpty() ? carried(player, Content.TRICKSTER_COIN) : devil;
        if (devil.isEmpty()) {
            player.removeEffect(holder(Content.GAMBLE));
        }
        long now = player.level().getGameTime();
        Luck luck = coin.getOrDefault(Content.LUCK.get(), Luck.NONE);
        boolean won = player.getRandom().nextDouble() < config.num("coin_odds");
        int times = luck.payout(now);
        if (won) {
            List<ItemEntity> twins = new ArrayList<>();
            for (ItemEntity drop : drops) {
                for (int i = 1; i < times; i++) {
                    twins.add(new ItemEntity(
                            dead.level(),
                            drop.getX(),
                            drop.getY(),
                            drop.getZ(),
                            drop.getItem().copy()));
                }
            }
            drops.addAll(twins);
            if (dead.level() instanceof ServerLevel level) {
                ExperienceOrb.award(level, dead.position(), dead.getExperienceReward(level, player) * (times - 1));
            }
            luck = luck.won(now, config.integer("coin_streak_cap"));
        } else {
            drops.clear();
            luck = luck.lost(now);
            player.removeEffect(holder(Content.GAMBLE));
        }
        if (!coin.isEmpty()) {
            coin.set(Content.LUCK.get(), luck);
        }
        flip(dead, won, times);
    }

    /** Shows how the wager fell at the dead mob: a gold burst that grows with the payout, or grey smoke. */
    private static void flip(LivingEntity dead, boolean won, int times) {
        if (!(dead.level() instanceof ServerLevel level)) {
            return;
        }
        double y = dead.getY() + dead.getBbHeight() / 2;
        if (won) {
            level.sendParticles(
                    ParticleTypes.TOTEM_OF_UNDYING, dead.getX(), y, dead.getZ(), 12 * times, 0.4, 0.5, 0.4, 0.4);
            level.playSound(null, dead.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.5f);
        } else {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, dead.getX(), y, dead.getZ(), 16, 0.3, 0.4, 0.3, 0.02);
            level.sendParticles(ParticleTypes.SOUL, dead.getX(), y, dead.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
            level.playSound(null, dead.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 0.6f);
        }
    }

    /** Whether a held house key shelters the player from a spirit's event, and wears from it. */
    public static boolean keeps(ServerPlayer player) {
        ItemStack key = player.getOffhandItem();
        if (!Wards.housed(player.level(), player.blockPosition())) {
            return false;
        }
        if (key.is(Content.KEEPERS_KEY.get())) {
            return keeper(player, key);
        }
        if (!key.is(Content.HOUSE_KEY.get())) {
            return false;
        }
        key.hurtAndBreak(1, player, EquipmentSlot.OFFHAND);
        if (key.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.restless.key_broke"), true);
        }
        return true;
    }

    /**
     * A keeper's key shelters the room it was first used in, and never wears there; anywhere else it does nothing. A
     * room is the place the key was bound to and what lies within {@code keeper_radius} of it.
     */
    private static boolean keeper(ServerPlayer player, ItemStack key) {
        net.minecraft.core.GlobalPos room = key.get(Content.KEY_ROOM.get());
        if (room == null) {
            key.set(
                    Content.KEY_ROOM.get(),
                    net.minecraft.core.GlobalPos.of(player.level().dimension(), player.blockPosition()));
            player.sendSystemMessage(Component.translatable("message.restless.key_bound"), true);
            return true;
        }
        double radius = Config.section("advanced").num("keeper_radius");
        return room.dimension() == player.level().dimension()
                && room.pos().distSqr(player.blockPosition()) <= radius * radius;
    }

    private static void scream(ServerPlayer player, Config.Section config) {
        double strength = config.num("wail_knockback");
        boolean requital = !carried(player, Content.REQUITAL_WAIL).isEmpty();
        for (LivingEntity mob : player.level()
                .getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(config.num("wail_radius")),
                        e -> e instanceof Enemy)) {
            Vec3 away = new Vec3(mob.getX() - player.getX(), 0, mob.getZ() - player.getZ()).normalize();
            mob.push(away.x * strength, 0.4, away.z * strength);
            int dazzle = config.integer("wail_dazzle_ticks");
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, dazzle, 2));
            mob.addEffect(new MobEffectInstance(MobEffects.DARKNESS, dazzle, 0));
            if (requital) {
                Advanced.requite(mob);
            }
        }
        player.level()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(2).value(),
                        SoundSource.PLAYERS,
                        1.5f,
                        0.6f);
    }

    /** Call when a hostile is about to take a new target; true means it must not take this one. */
    public static boolean unseen(LivingEntity looker, LivingEntity target) {
        return looker instanceof Enemy
                && (target.hasEffect(holder(Content.BORROWED_FACE))
                        || target.hasEffect(holder(Content.DECOY))
                        || Relics.faded(target)
                        || Advanced.walking(target));
    }

    /**
     * Call when a player dies, before the drops: a filled stash anywhere in the inventory keeps its stack for the
     * player.
     */
    public static void died(ServerPlayer player) {
        ItemStack satchel = player.getOffhandItem();
        if (satchel.is(Content.GRAVE_SATCHEL.get())
                && !keepsInventory(player)
                && Rest.ready(satchel, player.level())
                && !GraveSatchel.held(satchel).isEmpty()) {
            ItemStack kept = satchel.copy();
            kept.set(
                    Content.REST_UNTIL.get(),
                    player.level().getGameTime() + Config.section("advanced").integer("satchel_rest_ticks"));
            HauntData.get(player.level().getServer()).keep(player.getUUID(), kept);
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            return;
        }
        if (keepsInventory(player)) {
            return;
        }
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stash = inventory.getItem(slot);
            if (stash.is(Content.STASH.get()) && !stashed(stash).isEmpty()) {
                HauntData.get(player.level().getServer()).keep(player.getUUID(), stashed(stash));
                inventory.setItem(slot, ItemStack.EMPTY);
                return;
            }
        }
    }

    private static boolean keepsInventory(ServerPlayer player) {
        return player.level().getGameRules().get(GameRules.KEEP_INVENTORY);
    }

    /** What the stash holds; empty if nothing. */
    static ItemStack stashed(ItemStack stash) {
        return stash.getOrDefault(Content.STASHED.get(), ItemContainerContents.EMPTY)
                .copyOne();
    }

    static void give(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.level()
                    .addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                            player.level(), player.getX(), player.getY(), player.getZ(), stack));
        }
    }

    /** Call when a player comes back after dying. */
    public static void respawned(ServerPlayer player) {
        ItemStack kept = HauntData.get(player.level().getServer()).takeKept(player.getUUID());
        if (!kept.isEmpty()) {
            give(player, kept);
            player.sendSystemMessage(Component.translatable("message.restless.stash_kept"), true);
        }
    }
}
