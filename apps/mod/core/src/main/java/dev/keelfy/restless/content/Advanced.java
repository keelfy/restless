package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Anger;
import dev.keelfy.restless.haunt.Sanity;
import dev.keelfy.restless.haunt.Session;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * What the advanced charms do beyond their base charm, where it is more than a few lines in the item itself: the
 * changeling mask's frame, the nightwalk shroud's toggle, the requital wail's mark, blood debt's payout and the
 * magpie's hook. {@link Charms} calls in where a blow, a death or a tick is the trigger. All of it lives in memory
 * only.
 */
public final class Advanced {
    private Advanced() {}

    /** Players whose borrowed face has struck once as a changeling's: the next blow breaks it. */
    private static final Set<UUID> FRAMING = Session.SERVER.keep(new HashSet<>());
    /** Players walking in the night. */
    private static final Set<UUID> NIGHTWALKERS = Session.SERVER.keep(new HashSet<>());
    /** Mobs a requital wail threw back, and the game time they stop taking more. */
    private static final Map<UUID, Long> REQUITED = Session.SERVER.keep(new HashMap<>());
    /** What a blood debt's blow is owed back, if it kills. */
    private record Debt(UUID victim, float amount, long at) {}

    private static final Map<UUID, Debt> DEBTS = Session.SERVER.keep(new HashMap<>());

    private static Config.Section config() {
        return Config.section("advanced");
    }

    // --- Changeling Mask -------------------------------------------------------------------------------------------

    /**
     * Call when a player whose face is on strikes {@code victim}. A changeling's mask keeps its face through the first
     * blow, and the struck mob is framed: the nearest hostile turns on it. @return whether the face holds
     */
    static boolean framed(ServerPlayer attacker, Entity victim) {
        if (carried(attacker, Content.CHANGELING_MASK).isEmpty()
                || !(victim instanceof Mob struck)
                || !FRAMING.add(attacker.getUUID())) {
            FRAMING.remove(attacker.getUUID());
            return false;
        }
        attacker
                .level()
                .getEntitiesOfClass(
                        Mob.class,
                        struck.getBoundingBox().inflate(config().num("mask_radius")),
                        m -> m instanceof Enemy && m != struck)
                .stream()
                .min(Comparator.comparingDouble(m -> m.distanceToSqr(struck)))
                .ifPresent(m -> m.setTarget(struck));
        attacker.sendSystemMessage(Component.translatable("message.restless.mask_framed"), true);
        return true;
    }

    /** A fresh face has struck nothing yet. */
    static void unframe(ServerPlayer player) {
        FRAMING.remove(player.getUUID());
    }

    // --- Nightwalk Shroud ------------------------------------------------------------------------------------------

    /** Toggles the walk; @return whether it is on now. */
    static boolean toggleWalk(ServerPlayer player) {
        boolean on = NIGHTWALKERS.add(player.getUUID());
        if (!on) {
            NIGHTWALKERS.remove(player.getUUID());
        }
        return on;
    }

    /** Whether {@code entity} walks in the night, and so is looked past by hostile things. */
    static boolean walking(Entity entity) {
        return NIGHTWALKERS.contains(entity.getUUID());
    }

    /** Every half second for a player walking in the night: the light ends it, it costs sanity, hostiles lose track. */
    static void walk(ServerPlayer player) {
        if (!NIGHTWALKERS.contains(player.getUUID())) {
            return;
        }
        boolean lit = player.level().getMaxLocalRawBrightness(player.blockPosition()) > config().integer("walk_light");
        if (carried(player, Content.NIGHTWALK_SHROUD).isEmpty() || lit) {
            NIGHTWALKERS.remove(player.getUUID());
            player.sendSystemMessage(Component.translatable("message.restless.walk_ended"), true);
            return;
        }
        Sanity.set(player, Sanity.of(player) - config().num("walk_sanity_per_second") / 2.0);
        for (Mob mob : player.level()
                .getEntitiesOfClass(
                        Mob.class,
                        player.getBoundingBox().inflate(16.0),
                        m -> m instanceof Enemy && m.getTarget() == player)) {
            mob.setTarget(null);
        }
    }

    // --- Requital Wail ---------------------------------------------------------------------------------------------

    /** Marks a mob the wail threw back: it takes more for a while, and glows. */
    static void requite(LivingEntity mob) {
        int ticks = config().integer("requital_ticks");
        REQUITED.put(mob.getUUID(), mob.level().getGameTime() + ticks);
        mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, true, false));
    }

    /** What a blow on {@code victim} deals: more if the requital wail marked it and the mark has not run out. */
    static float requited(Entity victim, float amount) {
        Long until = REQUITED.get(victim.getUUID());
        if (until == null) {
            return amount;
        }
        if (until <= victim.level().getGameTime()) {
            REQUITED.remove(victim.getUUID());
            return amount;
        }
        return amount * (1.0f + (float) config().num("requital_bonus"));
    }

    // --- Blood Debt ------------------------------------------------------------------------------------------------

    /** Call when a blood debt's blow lands: it costs sanity, and heals the holder if it kills. */
    static void owe(ServerPlayer holder, Entity victim, float stored) {
        Sanity.set(holder, Sanity.of(holder) - config().num("debt_sanity_cost"));
        DEBTS.put(
                holder.getUUID(),
                new Debt(victim.getUUID(), stored, holder.level().getGameTime()));
    }

    /** Call when something dies. */
    public static void killed(@Nullable Entity killer, LivingEntity dead) {
        if (!(killer instanceof ServerPlayer player)) {
            return;
        }
        Debt debt = DEBTS.remove(player.getUUID());
        if (debt != null
                && debt.victim().equals(dead.getUUID())
                && player.level().getGameTime() - debt.at() <= 5) {
            player.heal(debt.amount());
            player.sendSystemMessage(Component.translatable("message.restless.debt_paid"), true);
        }
    }

    // --- Truthseer's Eye -------------------------------------------------------------------------------------------

    /**
     * Whether the next reading of a speaking {@code clue} by {@code player} may not lie: the eye is in the off hand and
     * rested.
     */
    public static boolean truthful(ServerPlayer player, dev.keelfy.restless.spirit.Clue clue) {
        ItemStack eye = player.getOffhandItem();
        return dev.keelfy.restless.haunt.Clues.speaks(clue)
                && eye.is(Content.TRUTHSEERS_EYE.get())
                && Rest.ready(eye, player.level());
    }

    /** The eye has kept one reading honest: it rests for the day. */
    public static void truthSpent(ServerPlayer player) {
        Rest.start(player, player.getOffhandItem(), config().integer("truth_rest_ticks"));
        player.sendSystemMessage(Component.translatable("message.restless.truth_spent"), true);
    }

    // --- Magpie's Hook ---------------------------------------------------------------------------------------------

    /**
     * Every half second for a holder of the hook in the off hand: nearby items come, and what will not fit is sent on.
     */
    static void hook(ServerPlayer player) {
        ItemStack hook = player.getOffhandItem();
        if (!hook.is(Content.MAGPIES_HOOK.get())) {
            return;
        }
        GlobalPos link = hook.get(Content.HOOK_LINK.get());
        Container chest = link == null ? null : container(player, link);
        if (link != null
                && chest == null
                && link.dimension() == player.level().dimension()
                && player.level().isLoaded(link.pos())) {
            hook.remove(Content.HOOK_LINK.get()); // the container is gone
        }
        for (ItemEntity item : player.level()
                .getEntitiesOfClass(
                        ItemEntity.class,
                        player.getBoundingBox().inflate(config().num("hook_radius")),
                        e -> !e.isInLava())) {
            if (fits(player, item.getItem())) {
                item.setDeltaMovement(player.position()
                        .add(0, 0.5, 0)
                        .subtract(item.position())
                        .normalize()
                        .scale(0.5));
                item.needsSync = true;
            } else if (chest != null) {
                ItemStack rest =
                        HopperBlockEntity.addItem(null, chest, item.getItem().copy(), null);
                if (rest.getCount() != item.getItem().getCount()) {
                    item.setItem(rest);
                }
            }
        }
    }

    /** The container the hook is linked to, if it is in reach, in this dimension and still there. */
    private static @Nullable Container container(ServerPlayer player, GlobalPos link) {
        if (link.dimension() != player.level().dimension()
                || link.pos().distSqr(player.blockPosition()) > square(config().integer("hook_link_range"))
                || !player.level().isLoaded(link.pos())) {
            return null;
        }
        return HopperBlockEntity.getContainerAt(player.level(), link.pos());
    }

    private static double square(int n) {
        return (double) n * n;
    }

    private static boolean fits(ServerPlayer player, ItemStack stack) {
        for (int slot = 0; slot < 36; slot++) {
            ItemStack in = player.getInventory().getItem(slot);
            if (in.isEmpty()
                    || (ItemStack.isSameItemSameComponents(in, stack) && in.getCount() < in.getMaxStackSize())) {
                return true;
            }
        }
        return false;
    }

    /** Links the hook in {@code hook} to a container at {@code pos}; false if there is none there. */
    static boolean link(ServerLevel level, ItemStack hook, net.minecraft.core.BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof Container)) {
            return false;
        }
        hook.set(Content.HOOK_LINK.get(), GlobalPos.of(level.dimension(), pos));
        level.playSound(null, pos, SoundEvents.ENDER_CHEST_CLOSE, SoundSource.PLAYERS, 0.8f, 1.4f);
        return true;
    }

    private static ItemStack carried(
            ServerPlayer player, java.util.function.Supplier<net.minecraft.world.item.Item> item) {
        return Charms.carried(player, item);
    }

    /** Anger a devil's wager costs; kept here with the other upgrades' prices. */
    static void tossed(ServerPlayer player) {
        Anger.provoke(player, Anger.Source.DEVIL, player.blockPosition());
    }
}
