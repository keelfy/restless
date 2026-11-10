package dev.keelfy.restless.haunt.harm;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Hardcore;
import dev.keelfy.restless.haunt.Ticks;
import dev.keelfy.restless.haunt.event.HauntEvent;
import dev.keelfy.restless.haunt.event.HauntTier;
import dev.keelfy.restless.spirit.SpiritType;
import dev.keelfy.restless.spirit.life.Anger;
import dev.keelfy.restless.state.HauntData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

/**
 * Gates the events that go past {@code assaults.safe_health}. A brink event needs {@code min_power}; a lethal one also
 * an evolved spirit on its {@code spirit_ids} list, the world's {@code assaults.lethal} setting, and a brink event
 * since the last lethal one, at most once per {@code assaults.lethal_cooldown_days}. After a brink event nothing
 * harmful runs for {@code assaults.mercy_ticks}. Brink and lethal events never run on peaceful.
 */
public final class Bands {
    private Bands() {}

    /** Whether the event may roll for this player and spirit right now; light events are never gated. */
    public static boolean open(
            HauntEvent event, ServerPlayer player, SpiritType spirit, double power, boolean evolved) {
        if (event.tier() != HauntTier.HARMFUL) {
            return true;
        }
        long now = now(player);
        HauntData data = HauntData.get(player.level().getServer());
        Menace menace = data.menace(player.getUUID());
        if (menace.merciful(now)) {
            return false;
        }
        String id = event.id().toString();
        Band band = Band.of(id);
        if (band == Band.THREAT) {
            return true;
        }
        Config.Section config = Config.event(id);
        if (player.level().getDifficulty() == Difficulty.PEACEFUL || power < config.num("min_power")) {
            return false;
        }
        if (band == Band.BRINK) {
            return true;
        }
        Config.Section assaults = Config.section("assaults");
        boolean allowed = switch (assaults.text("lethal")) {
            case "on" -> true;
            case "not_hardcore" -> !Hardcore.on(player.level());
            case "off" -> false;
            default -> throw new IllegalStateException("assaults.lethal must be on, off or not_hardcore");
        };
        return allowed
                && evolved
                && config.texts("spirit_ids").contains(spirit.id())
                && menace.lethalOpen(now, (long) (assaults.num("lethal_cooldown_days") * Ticks.DAY));
    }

    /** Call after an event ran: a brink event warns and grants mercy; a lethal one resets the warning. */
    public static void fired(HauntEvent event, ServerPlayer player) {
        Band band = Band.of(event.id().toString());
        if (band == Band.THREAT) {
            return;
        }
        HauntData data = HauntData.get(player.level().getServer());
        Menace menace = data.menace(player.getUUID());
        long now = now(player);
        data.putMenace(
                player.getUUID(),
                band == Band.BRINK
                        ? menace.brink(now, (long) Config.section("assaults").num("mercy_ticks"))
                        : menace.lethal(now));
    }

    /** Call when a lethal event has just done its killing blow outside {@link #fired}'s start (a delayed one). */
    public static void struck(ServerPlayer player) {
        HauntData data = HauntData.get(player.level().getServer());
        data.putMenace(player.getUUID(), data.menace(player.getUUID()).lethal(now(player)));
    }

    /**
     * Call when a player dies. If a lethal event ran just before, the spirit is satisfied and its anger drops by
     * {@code assaults.death_relief}.
     */
    public static void died(ServerPlayer player) {
        Config.Section assaults = Config.section("assaults");
        Menace menace = HauntData.get(player.level().getServer()).menace(player.getUUID());
        if (menace.lastLethalAt() >= 0
                && now(player) - menace.lastLethalAt() <= assaults.integer("relief_window_ticks")) {
            Anger.soothe(player, assaults.num("death_relief"));
        }
    }

    /** Damage a spirit-made entity may deal to the player: capped at what its band leaves. */
    public static float cap(DamageSource source, ServerPlayer player, float amount) {
        for (Entity maker : new Entity[] {source.getDirectEntity(), source.getEntity()}) {
            Band band = maker == null ? null : Band.of(maker);
            if (band != null) {
                return Math.max(0.0f, Math.min(amount, band.room(player.getHealth())));
            }
        }
        return amount;
    }

    private static long now(ServerPlayer player) {
        return player.level().getServer().overworld().getGameTime();
    }
}
