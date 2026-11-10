package dev.keelfy.restless.content.item.consumable;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.item.charm.Charms;
import dev.keelfy.restless.haunt.Sanity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

/**
 * What the two drinks do: the nix's Still Water turns the weather, the mara's Dreamless Draught lets its drinker sleep
 * through a night alone. The draught works through an effect that lasts until its drinker has slept deep and woken.
 */
public final class Draughts {
    private Draughts() {}

    /**
     * Clears rain and thunder, and the weather runs its course again; drunk sneaking it calls a storm that runs its own
     * course too.
     */
    public static boolean still(ServerPlayer player, ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) {
            player.sendSystemMessage(Component.translatable("message.restless.still_elsewhere"), true);
            return false;
        }
        MinecraftServer server = level.getServer();
        if (player.isShiftKeyDown()) {
            server.setWeatherParameters(0, ServerLevel.THUNDER_DURATION.sample(level.getRandom()), true, true);
            player.sendSystemMessage(Component.translatable("message.restless.still_storm"), true);
        } else {
            server.setWeatherParameters(0, 0, false, false);
            player.sendSystemMessage(Component.translatable("message.restless.still_clear"), true);
        }
        level.playSound(
                null, player.blockPosition(), SoundEvents.AMBIENT_UNDERWATER_EXIT, SoundSource.PLAYERS, 1.0f, 0.7f);
        return true;
    }

    /** Lets the drinker sleep tonight, however near the monsters, and the night go by with them alone in bed. */
    public static boolean dreamless(ServerPlayer player, ServerLevel level) {
        if (player.hasEffect(Charms.holder(Content.DREAMLESS))) {
            player.sendSystemMessage(Component.translatable("message.restless.draught_drunk"), true);
            return false;
        }
        player.addEffect(new MobEffectInstance(Charms.holder(Content.DREAMLESS), DREAM_TICKS, 0, true, true));
        player.sendSystemMessage(Component.translatable("message.restless.draught_taken"), true);
        level.playSound(
                null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 0.5f);
        return true;
    }

    /** How long the draught waits for its drinker to sleep: a day and a night. */
    private static final int DREAM_TICKS = 48000;

    /** Who slept deep under the draught and has not yet woken. */
    private static final Map<UUID, Boolean> SLEPT = dev.keelfy.restless.haunt.Session.SERVER.keep(new HashMap<>());

    /** The sleeping-percentage the rule had before a drinker's deep sleep lowered it for a tick. */
    private static @Nullable Integer rule;

    private static List<ServerPlayer> players(MinecraftServer server) {
        List<ServerPlayer> players = new ArrayList<>();
        server.getAllLevels().forEach(level -> players.addAll(level.players()));
        return players;
    }

    /** Whether the player drank a draught and has not yet slept it off. */
    public static boolean dreamless(Player player) {
        return player.hasEffect(Charms.holder(Content.DREAMLESS));
    }

    /**
     * Call every server tick. A drinker deep asleep counts as everyone: the sleeping-percentage drops to nothing for a
     * tick, which one sleeper fills, and returns after. A drinker who wakes after that sleeps the draught off and gains
     * sanity.
     */
    public static void tick(MinecraftServer server) {
        if (rule != null) {
            server.getGameRules().set(GameRules.PLAYERS_SLEEPING_PERCENTAGE, rule, server);
            rule = null;
        }
        boolean skip = Config.section("consumables").flag("draught_skips_alone");
        for (ServerPlayer player : players(server)) {
            if (!dreamless(player)) {
                SLEPT.remove(player.getUUID());
                continue;
            }
            if (player.isSleepingLongEnough()) {
                SLEPT.put(player.getUUID(), true);
                if (skip && rule == null) {
                    rule = server.getGameRules().get(GameRules.PLAYERS_SLEEPING_PERCENTAGE);
                    server.getGameRules().set(GameRules.PLAYERS_SLEEPING_PERCENTAGE, 0, server);
                }
            } else if (!player.isSleeping() && SLEPT.remove(player.getUUID()) != null) {
                player.removeEffect(Charms.holder(Content.DREAMLESS));
                Sanity.set(
                        player,
                        Sanity.of(player) + Config.section("consumables").num("draught_sanity"));
                player.sendSystemMessage(Component.translatable("message.restless.draught_woke"), true);
            }
        }
    }
}
