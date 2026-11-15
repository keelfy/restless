package dev.keelfy.restless.spirit.life;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.Scan;
import dev.keelfy.restless.spirit.Trait;
import dev.keelfy.restless.spirit.Traits;
import dev.keelfy.restless.state.HauntData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * What a spirit fears, wants and hates ({@link Trait.Lean}). A fear keeps its events off a player beside the feared
 * block or creature, a desire left lying near its player is taken and calms it, and its player using the hated thing
 * angers it. Water counts for a fear only while it runs, never a still source.
 */
public final class Leanings {
    private static final String WATER = "minecraft:water";
    /** Overworld game time until which each spirit ignores another offering or offence. */
    private static final Map<UUID, Long> calmed = new HashMap<>();

    private static final Map<UUID, Long> offended = new HashMap<>();

    private Leanings() {}

    /** Whether the spirit with these traits fears something beside the player, so its event must not run. */
    public static boolean fears(ServerPlayer player, List<Trait> traits) {
        Optional<Trait> fear = Traits.fear(traits);
        if (fear.isEmpty()) {
            return false;
        }
        List<String> things = fear.get().things();
        ServerLevel level = player.level();
        int radius = Config.section("leanings").integer("fear_radius");
        if (!level.getEntities(player, player.getBoundingBox().inflate(radius), e -> things.contains(id(e)))
                .isEmpty()) {
            return true;
        }
        boolean[] found = {false};
        Scan.each(
                level, player.blockPosition(), radius, (pos, state) -> found[0] |= !found[0] && feared(state, things));
        return found[0];
    }

    private static boolean feared(BlockState state, List<String> things) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        if (id.equals(WATER)) {
            return things.contains(WATER)
                    && !state.getFluidState().isSource()
                    && state.getFluidState().is(Fluids.FLOWING_WATER);
        }
        return things.contains(id);
    }

    private static String id(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }

    /** Call once per server tick: each spirit takes what it wants from beside its player, at most once a while. */
    public static void tick(MinecraftServer server) {
        Config.Section config = Config.section("leanings");
        long now = server.overworld().getGameTime();
        if (now % config.integer("desire_period_ticks") != 0) {
            return;
        }
        server.getPlayerList().getPlayers().forEach(Leanings::offered);
    }

    /** Each spirit of the player that wants something lying beside them takes it and calms down. */
    public static void offered(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        Config.Section config = Config.section("leanings");
        long now = server.overworld().getGameTime();
        HauntData data = HauntData.get(server);
        for (HauntData.Haunting spirit : spirits(player, data, now)) {
            if (calmed.getOrDefault(spirit.uuid(), 0L) > now) {
                continue;
            }
            Optional<Trait> desire = Traits.desire(Traits.of(spirit.traits()));
            if (desire.isPresent() && take(player, desire.get().things())) {
                calmed.put(spirit.uuid(), now + config.integer("desire_cooldown_ticks"));
                change(player, data, spirit.uuid(), h -> h.angered(-config.num("desire_calm")));
            }
        }
    }

    /** Takes one of the wanted items lying beside the player; whether one was there. */
    private static boolean take(ServerPlayer player, List<String> things) {
        ServerLevel level = player.level();
        int radius = Config.section("leanings").integer("desire_radius");
        for (ItemEntity item : level.getEntitiesOfClass(
                ItemEntity.class,
                player.getBoundingBox().inflate(radius),
                e -> things.contains(
                        BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).toString()))) {
            ItemStack stack = item.getItem().copy();
            stack.shrink(1);
            if (stack.isEmpty()) {
                item.discard();
            } else {
                item.setItem(stack);
            }
            level.sendParticles(
                    ParticleTypes.SOUL, item.getX(), item.getY() + 0.3, item.getZ(), 6, 0.2, 0.2, 0.2, 0.01);
            level.playSound(
                    null, item.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.AMBIENT, 1.0f, 0.8f);
            return true;
        }
        return false;
    }

    /**
     * Call when a player uses a block, with what is in their hand: each of their spirits that hates the block or the
     * item takes offence, at most once a while.
     */
    public static void used(ServerPlayer player, BlockState block, ItemStack hand) {
        MinecraftServer server = player.level().getServer();
        Config.Section config = Config.section("leanings");
        long now = server.overworld().getGameTime();
        String blockId = BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString();
        String itemId = BuiltInRegistries.ITEM.getKey(hand.getItem()).toString();
        HauntData data = HauntData.get(server);
        for (HauntData.Haunting spirit : spirits(player, data, now)) {
            Optional<Trait> hate = Traits.hate(Traits.of(spirit.traits()));
            if (hate.isEmpty()
                    || offended.getOrDefault(spirit.uuid(), 0L) > now
                    || !hate.get().things().contains(blockId)
                            && (hand.isEmpty() || !hate.get().things().contains(itemId))) {
                continue;
            }
            offended.put(spirit.uuid(), now + config.integer("hate_cooldown_ticks"));
            change(player, data, spirit.uuid(), h -> h.angered(config.num("hate_anger")));
        }
    }

    /** The player's own spirits and the territorial ones reaching them. */
    private static List<HauntData.Haunting> spirits(ServerPlayer player, HauntData data, long now) {
        List<HauntData.Haunting> all = new ArrayList<>(Director.hauntings(player, data, now));
        data.territorialReaching(player).forEach(t -> all.add(t.haunting()));
        return all;
    }

    private static void change(
            ServerPlayer player, HauntData data, UUID spirit, UnaryOperator<HauntData.Haunting> how) {
        List<HauntData.Haunting> own =
                new ArrayList<>(data.get(player.getUUID()) == null ? List.of() : data.get(player.getUUID()));
        if (own.stream().anyMatch(h -> h.uuid().equals(spirit))) {
            own.replaceAll(h -> h.uuid().equals(spirit) ? how.apply(h) : h);
            data.put(player.getUUID(), own);
            return;
        }
        for (HauntData.Territorial t : data.territorialReaching(player)) {
            if (t.haunting().uuid().equals(spirit)) {
                data.putTerritorial(
                        new HauntData.Territorial(how.apply(t.haunting()), t.dimension(), t.anchor(), t.radius()));
            }
        }
    }
}
