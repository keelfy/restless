package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.spirit.SpiritType;
import dev.keelfy.restless.spirit.Spirits;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * A personal spirit that appears because of something the player did. Every deed goes through {@link #deed}; which
 * spirit it makes and how often is the {@code births.deeds.<key>} config, the rules shared by all deeds are
 * {@code births}. See docs/births.md.
 */
public final class Births {
    /** What a player can do to get a spirit; {@code key} names it in the config. */
    public enum Deed {
        SLAUGHTER("slaughter"),
        FAR_DEATH("far_death"),
        PET_SLAIN("pet_slain"),
        PET_LOST("pet_lost");

        private final String key;

        Deed(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }

        public static Optional<Deed> byKey(String key) {
            return Arrays.stream(values()).filter(d -> d.key.equals(key)).findFirst();
        }
    }

    private Births() {}

    /**
     * Call when the player did {@code deed}.
     *
     * @param roll whether the deed's chance is rolled; false for the debug command
     */
    public static BirthRules.Verdict deed(ServerPlayer player, Deed deed, boolean roll) {
        if (player.isSpectator() || player.isCreative()) {
            return BirthRules.Verdict.NOTHING;
        }
        Config.Section config = Config.section("births");
        Config.Section of = config.section("deeds").section(deed.key());
        if (roll && player.getRandom().nextDouble() >= of.num("chance")) {
            return BirthRules.Verdict.NOTHING;
        }
        SpiritType spirit = Spirits.byId(of.text("spirit")).orElseThrow();
        MinecraftServer server = player.level().getServer();
        HauntData data = HauntData.get(server);
        long now = server.overworld().getGameTime();
        List<HauntData.Haunting> own = Director.hauntings(player, data, now);
        long cooldown = (long) (config.num("cooldown_days") * Ticks.DAY);
        BirthRules.Verdict verdict = BirthRules.decide(
                own.stream().anyMatch(h -> h.spirit().equals(spirit.id())),
                own.size(),
                own.stream().mapToLong(HauntData.Haunting::bornAt).max().orElse(0),
                now,
                cooldown,
                config.integer("max_personal"));
        switch (verdict) {
            case FED -> {
                double by = config.num("repeat_anger");
                HauntData.Haunting same = own.stream()
                        .filter(h -> h.spirit().equals(spirit.id()))
                        .findFirst()
                        .orElseThrow();
                data.change(same.uuid(), h -> h.angered(by));
            }
            case BORN -> {
                HauntData.Haunting born = Director.add(player, data, spirit, now);
                data.change(
                        born.uuid(),
                        h -> h.angered(config.num("start_anger"))
                                .rescheduled(now + config.integer("grace_ticks"))
                                .noticed(1 + player.getRandom().nextInt(config.integer("notice_within"))));
            }
            default -> {}
        }
        return verdict;
    }

    /**
     * Call when a spirit ran an event on the player. A new spirit says nothing when it comes; on one of its first
     * events the journal gets a line about something unknown near the player instead.
     *
     * @return the spirit with its count moved on
     */
    public static HauntData.Haunting noticing(ServerPlayer player, HauntData.Haunting spirit) {
        if (spirit.notice() == 0) {
            return spirit;
        }
        if (spirit.notice() > 1) {
            return spirit.noticed(spirit.notice() - 1);
        }
        Journal.note(player, Notebook.Kind.DREAM, null, Component.translatable("journal.restless.unknown_near"));
        return spirit.noticed(0);
    }
}
