package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Session;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.phys.Vec3;

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

    private static boolean offhand(LivingEntity entity, Supplier<Item> item) {
        return entity.getItemInHand(InteractionHand.OFF_HAND).is(item.get());
    }

    /** Call before a player takes damage, or deals it; the amount comes back unchanged. */
    public static void hurt(Entity victim, DamageSource source, float amount) {
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
        return looker instanceof Enemy && target.hasEffect(holder(Content.BORROWED_FACE));
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
