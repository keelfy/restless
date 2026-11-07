package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Session;
import dev.keelfy.restless.haunt.Wards;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * What the charms do beyond being used: the ones that act on a blow, a death or a hostile's eye. Charms that work while
 * held work from the off hand only, so no more than one answers at a time.
 */
public final class Charms {
    /** What a stash saved from its holder's death, until the holder is back; lost if the server stops before. */
    private static final Map<UUID, ItemStack> KEPT = Session.SERVER.keep(new HashMap<>());

    private Charms() {}

    public static Holder<MobEffect> holder(Supplier<MobEffect> effect) {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect.get());
    }

    private static boolean offhand(Entity entity, Supplier<Item> item) {
        return entity instanceof LivingEntity living
                && living.getItemInHand(InteractionHand.OFF_HAND).is(item.get());
    }

    /** What the grudge has stored and until when. */
    private record Stored(float amount, long until) {}

    private static final Map<UUID, Stored> GRUDGES = Session.SERVER.keep(new HashMap<>());

    /** Call before anything takes damage; returns the amount to deal, which a held grudge may raise. */
    public static float hurt(Entity victim, DamageSource source, float amount) {
        float dealt = amount;
        if (source.getEntity() instanceof ServerPlayer attacker && attacker.hasEffect(holder(Content.BORROWED_FACE))) {
            attacker.removeEffect(holder(Content.BORROWED_FACE));
            attacker.sendSystemMessage(Component.translatable("message.restless.face_off"), true);
        }
        if (victim instanceof ServerPlayer player && offhand(player, Content.DEATH_WAIL)) {
            Config.Section config = Config.section("charms");
            ItemStack wail = player.getOffhandItem();
            if (!player.getCooldowns().isOnCooldown(wail) && player.getHealth() - amount < config.num("wail_health")) {
                scream(player, config);
                player.getCooldowns().addCooldown(wail, config.integer("wail_cooldown_ticks"));
            }
        }
        return grudge(victim, source, dealt);
    }

    /** A holder stores what hurts them for a while; their next blow on anyone returns part of it. */
    private static float grudge(Entity victim, DamageSource source, float amount) {
        float dealt = amount;
        long now = victim.level().getGameTime();
        Config.Section config = Config.section("charms");
        if (source.getEntity() instanceof ServerPlayer attacker
                && attacker != victim
                && offhand(attacker, Content.GRUDGE)) {
            Stored stored = GRUDGES.remove(attacker.getUUID());
            if (stored != null && stored.until() > now) {
                dealt += (float) Math.min(config.num("grudge_cap"), stored.amount() * config.num("grudge_share"));
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
        if (victim instanceof ServerPlayer holder && offhand(holder, Content.GRUDGE)) {
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
        if (offhand(player, Content.DROWNED_STONE)) {
            // Water is its own: breath without end there, and on dry ground the stone is a weight.
            if (player.isInWater()) {
                player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 40, 0, true, false));
            } else {
                player.addEffect(
                        new MobEffectInstance(MobEffects.SLOWNESS, 40, config.integer("stone_slowness"), true, false));
            }
        }
        if (offhand(player, Content.DARK_CLOAK) && light(player) >= config.integer("cloak_bright_light")) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, false));
        }
    }

    private static int light(Entity entity) {
        return entity.level().getMaxLocalRawBrightness(entity.blockPosition());
    }

    /** Call when a wagered mob dies and its drops are settled: the coin doubles them or takes them all. */
    public static void drops(@Nullable Entity killer, LivingEntity dead, Collection<ItemEntity> drops) {
        if (!(killer instanceof ServerPlayer player)
                || !(dead instanceof Mob)
                || !player.hasEffect(holder(Content.GAMBLE))) {
            return;
        }
        player.removeEffect(holder(Content.GAMBLE));
        boolean won = player.getRandom().nextDouble() < Config.section("charms").num("coin_odds");
        if (won) {
            List<ItemEntity> twins = new ArrayList<>();
            for (ItemEntity drop : drops) {
                twins.add(new ItemEntity(
                        dead.level(),
                        drop.getX(),
                        drop.getY(),
                        drop.getZ(),
                        drop.getItem().copy()));
            }
            drops.addAll(twins);
        } else {
            drops.clear();
        }
        player.sendSystemMessage(
                Component.translatable(won ? "message.restless.coin_won" : "message.restless.coin_lost"), true);
    }

    /** Whether a held house key shelters the player from a spirit's event, and wears from it. */
    public static boolean keeps(ServerPlayer player) {
        ItemStack key = player.getOffhandItem();
        if (!key.is(Content.HOUSE_KEY.get()) || !Wards.housed(player.level(), player.blockPosition())) {
            return false;
        }
        key.hurtAndBreak(1, player, EquipmentSlot.OFFHAND);
        if (key.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.restless.key_broke"), true);
        }
        return true;
    }

    private static void scream(ServerPlayer player, Config.Section config) {
        double strength = config.num("wail_knockback");
        for (LivingEntity mob : player.level()
                .getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(config.num("wail_radius")),
                        e -> e instanceof Enemy)) {
            Vec3 away = new Vec3(mob.getX() - player.getX(), 0, mob.getZ() - player.getZ()).normalize();
            mob.push(away.x * strength, 0.4, away.z * strength);
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
        if (!(looker instanceof Enemy)) {
            return false;
        }
        if (target.hasEffect(holder(Content.BORROWED_FACE)) || target.hasEffect(holder(Content.DECOY))) {
            return true;
        }
        // The cloak hides the holder in the dark from all but what stands close.
        Config.Section config = Config.section("charms");
        return offhand(target, Content.DARK_CLOAK)
                && light(target) <= config.integer("cloak_dark_light")
                && looker.distanceToSqr(target) > config.num("cloak_notice") * config.num("cloak_notice");
    }

    /** Call when a player dies, before the drops: a filled stash in the off hand keeps its stack for the player. */
    public static void died(ServerPlayer player) {
        ItemStack stash = player.getOffhandItem();
        if (!stash.is(Content.STASH.get())) {
            return;
        }
        ItemStack held = stashed(stash);
        if (held.isEmpty()) {
            return;
        }
        KEPT.put(player.getUUID(), held.copy());
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
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
        ItemStack kept = KEPT.remove(player.getUUID());
        if (kept != null) {
            give(player, kept);
            player.sendSystemMessage(Component.translatable("message.restless.stash_kept"), true);
        }
    }
}
