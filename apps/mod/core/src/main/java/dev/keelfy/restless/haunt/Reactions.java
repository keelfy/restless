package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.spirit.Trait;
import dev.keelfy.restless.spirit.Traits;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How what a player does feeds their personal spirits. A trait can shorten a spirit's wait by the noise the player
 * makes, and can make the spirit act on the next check after a trigger; either way the {@link Director} still picks and
 * runs the event, so power, tiers and cooldowns apply as always. In memory only.
 */
public final class Reactions {
    private static final Map<UUID, AtomicInteger> SOUNDS = new ConcurrentHashMap<>();
    private static final Set<UUID> DEEP = ConcurrentHashMap.newKeySet();

    private Reactions() {}

    /** Call for every sound a player makes. */
    public static void heard(UUID player) {
        SOUNDS.computeIfAbsent(player, id -> new AtomicInteger()).incrementAndGet();
    }

    public static void clear() {
        SOUNDS.clear();
        DEEP.clear();
    }

    /** The sounds the player made since the last call. */
    static int takeSounds(UUID player) {
        AtomicInteger sounds = SOUNDS.get(player);
        return sounds == null ? 0 : sounds.getAndSet(0);
    }

    /**
     * The spirit's next roll after this check: sooner for every sound its noise-fed traits hear, later while the player
     * sneaks.
     */
    static HauntData.Haunting fed(HauntData.Haunting haunting, int sounds, boolean sneaking) {
        List<Trait> traits = Traits.of(haunting.traits());
        if (traits.stream().noneMatch(t -> t.noise() > 0)) {
            return haunting;
        }
        long shift = sneaking
                ? -Config.section("director").integer("sneak_delay_ticks")
                : (long) Traits.noise(traits, sounds);
        return shift == 0 ? haunting : haunting.rescheduled(haunting.nextEventAt() - shift);
    }

    /** Call once per director check: spots a player going below y 0. */
    static void watch(ServerPlayer player, long now) {
        if (player.getY() >= 0) {
            DEEP.remove(player.getUUID());
        } else if (DEEP.add(player.getUUID())) {
            trigger(player, Trait.Trigger.DEEP, now);
        }
    }

    /** Call when a player uses a block on the server. */
    public static void used(ServerPlayer player, BlockState state) {
        if (state.getBlock() instanceof ChestBlock || state.getBlock() instanceof BarrelBlock) {
            trigger(player, Trait.Trigger.CHEST);
        } else if (state.is(BlockTags.BEDS)) {
            trigger(player, Trait.Trigger.BED);
        }
    }

    public static void trigger(ServerPlayer player, Trait.Trigger trigger) {
        trigger(player, trigger, player.level().getServer().overworld().getGameTime());
    }

    /** Each of the player's spirits whose type or a trait has this trigger has a chance to act on the next check. */
    private static void trigger(ServerPlayer player, Trait.Trigger trigger, long now) {
        HauntData data = HauntData.get(player.level().getServer());
        List<HauntData.Haunting> spirits = new ArrayList<>(Director.hauntings(player, data, now));
        boolean changed = false;
        for (int i = 0; i < spirits.size(); i++) {
            HauntData.Haunting haunting = spirits.get(i);
            if (haunting.nextEventAt() > now
                    && (Director.spirit(haunting).reacts().contains(trigger)
                            || Traits.reacts(Traits.of(haunting.traits()), trigger))
                    && player.getRandom().nextDouble()
                            < Config.section("director").num("react_chance")) {
                spirits.set(i, haunting.rescheduled(now));
                changed = true;
            }
        }
        if (changed) {
            data.put(player.getUUID(), spirits);
        }
    }
}
