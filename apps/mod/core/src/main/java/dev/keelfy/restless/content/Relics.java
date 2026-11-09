package dev.keelfy.restless.content;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.HauntData;
import dev.keelfy.restless.haunt.HauntTier;
import dev.keelfy.restless.haunt.Sanity;
import dev.keelfy.restless.haunt.Scheduler;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * What the relics do beyond being placed or held: the ferryman's coin on a lethal blow, the hoarder's hollow's shared
 * store and the thefts from it, the hollow lantern's one-per-chunk rule and Mara's cradle. The censer works in
 * {@code Ritual}, the hollow lantern's ward in {@code Wards}.
 */
public final class Relics {
    private static final int HOLLOW_SLOTS = 27;

    private Relics() {}

    /** Whether the chunk around {@code at} already holds a hollow lantern. */
    public static boolean hollowLanternIn(LevelReader level, BlockPos at) {
        ChunkAccess chunk = level.getChunk(at);
        var lantern = Content.HOLLOW_LANTERN.get();
        for (int i = 0; i < chunk.getSectionsCount(); i++) {
            LevelChunkSection section = chunk.getSection(i);
            if (!section.maybeHas(state -> state.is(lantern))) {
                continue;
            }
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        if (section.getBlockState(x, y, z).is(lantern)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /** Whether the entity is faded, which nothing touches and which touches nothing. */
    public static boolean faded(@Nullable Entity entity) {
        return entity instanceof LivingEntity living && living.hasEffect(Charms.holder(Content.FADED));
    }

    /**
     * Call when a living thing is about to die: a player with a ferryman's coin in the off hand that has rested fades
     * instead, with half health and no sanity left. A totem is used first (it acts before the death), and what ignores
     * life itself, the void and the like, is not turned. True means the death must not happen.
     */
    public static boolean ferried(LivingEntity dying, DamageSource source) {
        if (!(dying instanceof ServerPlayer player) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        ItemStack coin = Worn.held(player, Content.FERRYMANS_COIN);
        if (coin.isEmpty() || !Rest.ready(coin, player.level())) {
            return false;
        }
        Config.Section config = Config.section("relics");
        int ticks = config.integer("ferry_fade_ticks");
        player.removeAllEffects();
        player.clearFire();
        player.setHealth((float) (player.getMaxHealth() * config.num("ferry_health")));
        player.addEffect(new MobEffectInstance(Charms.holder(Content.FADED), ticks, 0, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, true, false));
        Sanity.set(player, 0.0);
        Rest.start(player, coin, config.integer("ferry_rest_ticks"));
        ServerLevel level = player.level();
        level.sendParticles(
                ParticleTypes.SOUL, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.5f, 0.5f);
        player.sendSystemMessage(Component.translatable("message.restless.ferried"), true);
        return true;
    }

    /** The player's hoarder's hollows share this one store; every change to it is saved at once. */
    public static SimpleContainer hollow(ServerPlayer player) {
        HauntData data = HauntData.get(player.level().getServer());
        var owner = player.getUUID();
        SimpleContainer store = new SimpleContainer(HOLLOW_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                List<ItemStack> stacks = new ArrayList<>();
                for (int i = 0; i < getContainerSize(); i++) {
                    stacks.add(getItem(i));
                }
                data.putHollow(owner, ItemContainerContents.fromItems(stacks));
            }
        };
        NonNullList<ItemStack> held = NonNullList.withSize(HOLLOW_SLOTS, ItemStack.EMPTY);
        data.hollow(owner).copyInto(held);
        for (int i = 0; i < HOLLOW_SLOTS; i++) {
            store.setItem(i, held.get(i));
        }
        return store;
    }

    /** A hoarder takes one stack from the player's hollows; true if it found one. */
    public static boolean pilfer(ServerPlayer player) {
        SimpleContainer store = hollow(player);
        List<Integer> full = new ArrayList<>();
        for (int i = 0; i < store.getContainerSize(); i++) {
            if (!store.getItem(i).isEmpty()) {
                full.add(i);
            }
        }
        if (full.isEmpty()) {
            return false;
        }
        store.setItem(full.get(player.getRandom().nextInt(full.size())), ItemStack.EMPTY);
        player.sendSystemMessage(Component.translatable("message.restless.hollow_robbed"), true);
        return true;
    }

    /** Call when a player lies down in a cradle at {@code head}; it is their spawn point, and Mara may come. */
    public static void slept(ServerPlayer player, BlockPos head) {
        HauntData data = HauntData.get(player.level().getServer());
        data.putCradle(player.getUUID(), GlobalPos.of(player.level().dimension(), head));
        Config.Section config = Config.section("relics");
        if (player.getRandom().nextDouble() < config.num("cradle_mara_chance")) {
            Scheduler.later(player, config.integer("cradle_wake_delay_ticks"), Relics::mara);
        }
    }

    private static void mara(ServerPlayer player) {
        if (!player.isSleeping()) {
            return;
        }
        List<HauntData.Haunting> spirits =
                HauntData.get(player.level().getServer()).get(player.getUUID());
        if (spirits != null) {
            spirits.stream()
                    .filter(h -> h.spirit().equals("mara"))
                    .findFirst()
                    .ifPresent(h -> Director.act(player, h, null, HauntTier.LIGHT));
        }
    }

    private static boolean inCradle(ServerPlayer player) {
        return player.getSleepingPos()
                .map(pos -> player.level().getBlockState(pos).is(Content.MARAS_CRADLE.get()))
                .orElse(false);
    }

    /** Call when a player wakes: a night in a cradle leaves them with all their sanity. */
    public static void woke(ServerPlayer player) {
        if (inCradle(player)) {
            Sanity.set(player, 1.0);
        }
    }

    /** Call when the sleepers would skip the night: it does not pass when every one of them lies in a cradle. */
    public static boolean keepsNight(ServerLevel level) {
        List<ServerPlayer> asleep = level.players().stream()
                .filter(p -> !p.isSpectator() && p.isSleeping())
                .toList();
        return !asleep.isEmpty() && asleep.stream().allMatch(Relics::inCradle);
    }

    /** Call when a cradle's head half is gone. */
    public static void cradleBroken(ServerLevel level, BlockPos head) {
        HauntData.get(level.getServer()).cradleBroken(GlobalPos.of(level.dimension(), head));
    }

    /**
     * Call when a player's spawn is settled: a spawn point on a cradle that was broken holds once, at the place it
     * stood. Null leaves the game's choice.
     */
    public static @Nullable TeleportTransition respawn(ServerPlayer player, TeleportTransition chosen) {
        if (!chosen.missingRespawnBlock()) {
            return null;
        }
        GlobalPos lost = HauntData.get(player.level().getServer()).takeLost(player.getUUID());
        ServerLevel level = lost == null ? null : player.level().getServer().getLevel(lost.dimension());
        if (level == null) {
            return null;
        }
        BlockPos at = lost.pos();
        while (!level.noCollision(player.getType().getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(at)))
                && at.getY() < level.getMaxY()) {
            at = at.above();
        }
        return new TeleportTransition(
                level, Vec3.atBottomCenterOf(at), Vec3.ZERO, player.getYRot(), 0.0f, TeleportTransition.DO_NOTHING);
    }
}
