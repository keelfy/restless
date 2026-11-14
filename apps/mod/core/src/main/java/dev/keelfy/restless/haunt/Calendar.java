package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.journal.Journal;
import dev.keelfy.restless.journal.Notebook;
import dev.keelfy.restless.state.Session;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The three dates the game day brings: the full moon (the vanilla moon phase of that day, phase 0), a blood night every
 * {@code calendar.blood_night_days} days and the day of the dead once a {@code calendar.season_days} season. The day of
 * the dead wins over a blood night that falls on it.
 */
public final class Calendar {
    /** Days whose dusk has already been announced, in this world only. */
    private static final Set<Long> ANNOUNCED = Session.SERVER.keep(ConcurrentHashMap.newKeySet());

    /** Vanilla's moon phases, one per day (MoonPhase.COUNT; a plain number so the pure part runs in unit tests). */
    private static final int MOON_PHASES = 8;

    @Nullable
    private static volatile Date forced;

    private Calendar() {}

    /**
     * One game day. A blood night and the day of the dead never fall on the same day.
     *
     * @param day whole days since the world began, the overworld clock's total ticks over {@link Ticks#DAY}
     */
    public record Date(long day, boolean fullMoon, boolean bloodNight, boolean dayOfTheDead) {
        public static Date of(long day) {
            Config.Section config = Config.section("calendar");
            int season = config.integer("season_days");
            int blood = config.integer("blood_night_days");
            boolean dead = Math.floorMod(day, season) == season - 1;
            return new Date(
                    day, Math.floorMod(day, MOON_PHASES) == 0, !dead && Math.floorMod(day, blood) == blood - 1, dead);
        }

        /** Whether this day is any of the three dates. */
        public boolean special() {
            return fullMoon || bloodNight || dayOfTheDead;
        }
    }

    /** The date of the overworld clock now; a date set by {@link #force} wins. */
    public static Date today(MinecraftServer server) {
        Date date = forced;
        return date != null ? date : Date.of(ticks(server) / Ticks.DAY);
    }

    /** Test seam: every {@link #today} answers {@code date} until it is called again with null. */
    public static void force(@Nullable Date date) {
        forced = date;
    }

    private static long ticks(MinecraftServer server) {
        return server.clockManager()
                .getInstance(server.registryAccess().getOrThrow(WorldClocks.OVERWORLD))
                .totalTicks();
    }

    /**
     * Call once per server tick: at dusk of a special date, once, players in the overworld learn of it in the journal,
     * and of a blood night in chat.
     */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % Ticks.SECOND != 0) {
            return;
        }
        Date date = today(server);
        if (!date.special()
                || Math.floorMod(ticks(server), Ticks.DAY) < Config.section("calendar").integer("dusk_tick")
                || !ANNOUNCED.add(date.day())) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level().dimension() != Level.OVERWORLD) {
                continue;
            }
            if (date.bloodNight()) {
                player.sendSystemMessage(Component.translatable("message.restless.blood_night"));
            }
            Journal.unlock(player, Notebook.Chapter.CALENDAR);
        }
    }
}
