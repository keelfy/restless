package dev.keelfy.restless.spirit.life;

import dev.keelfy.restless.banish.Requiem;
import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.Ticks;
import dev.keelfy.restless.journal.Journal;
import dev.keelfy.restless.journal.Notebook;
import dev.keelfy.restless.spirit.SpiritType;
import dev.keelfy.restless.spirit.Spirits;
import dev.keelfy.restless.state.HauntData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Spirits the world makes. A personal one appears because of something the player did: every deed goes through
 * {@link #deed}; which spirit it makes and how often is the {@code births.deeds.<key>} config, the rules shared by all
 * deeds are {@code births}. A territorial one appears with a generated structure, through {@link #generated}, by the
 * {@code births.structures.<id>} config. See docs/births.md and docs/places.md.
 */
public final class Births {
    /** What a player can do to get a spirit; {@code key} names it in the config. */
    public enum Deed {
        SLAUGHTER("slaughter"),
        FAR_DEATH("far_death"),
        PET_SLAIN("pet_slain"),
        PET_LOST("pet_lost"),
        WITNESS_DEATH("witness_death"),
        LOST_LONG("lost_long");

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
        if (player.isSpectator() || player.isCreative() || Requiem.released(player)) {
            return BirthRules.Verdict.NOTHING;
        }
        Config.Section config = Config.section("births");
        Config.Section of = config.section("deeds").section(deed.key());
        if (roll && player.getRandom().nextDouble() >= of.num("chance")) {
            return BirthRules.Verdict.NOTHING;
        }
        return bind(player, Spirits.byId(of.text("spirit")).orElseThrow());
    }

    /**
     * A player calls a personal spirit on purpose (an effigy burning down): the deed's rules without its chance. A type
     * they already have is angered, a new one is born, and the cooldown and the cap hold.
     */
    public static BirthRules.Verdict lure(ServerPlayer player, SpiritType spirit) {
        if (player.isSpectator() || player.isCreative() || Requiem.released(player)) {
            return BirthRules.Verdict.NOTHING;
        }
        return bind(player, spirit);
    }

    /** What {@link #lure} would do now, with nothing changed. */
    public static BirthRules.Verdict outlook(ServerPlayer player, SpiritType spirit) {
        if (player.isSpectator() || player.isCreative() || Requiem.released(player)) {
            return BirthRules.Verdict.NOTHING;
        }
        MinecraftServer server = player.level().getServer();
        long now = server.overworld().getGameTime();
        return decide(player, spirit, HauntData.get(server), now, Config.section("births"));
    }

    private static BirthRules.Verdict decide(
            ServerPlayer player, SpiritType spirit, HauntData data, long now, Config.Section config) {
        List<HauntData.Haunting> own = Director.hauntings(player, data, now);
        return BirthRules.decide(
                own.stream().anyMatch(h -> h.spirit().equals(spirit.id())),
                own.size(),
                own.stream().mapToLong(HauntData.Haunting::bornAt).max().orElse(0),
                now,
                (long) (config.num("cooldown_days") * Ticks.DAY),
                config.integer("max_personal"));
    }

    private static BirthRules.Verdict bind(ServerPlayer player, SpiritType spirit) {
        Config.Section config = Config.section("births");
        MinecraftServer server = player.level().getServer();
        HauntData data = HauntData.get(server);
        long now = server.overworld().getGameTime();
        BirthRules.Verdict verdict = decide(player, spirit, data, now, config);
        switch (verdict) {
            case FED -> {
                double by = config.num("repeat_anger");
                HauntData.Haunting same = Director.hauntings(player, data, now).stream()
                        .filter(h -> h.spirit().equals(spirit.id()))
                        .findFirst()
                        .orElseThrow();
                data.change(same.uuid(), h -> h.angered(by));
            }
            case BORN -> {
                HauntData.Haunting born = Director.add(player, data, spirit, now);
                if (born == null) {
                    return BirthRules.Verdict.NOTHING;
                }
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
     * Call once for each structure start of a freshly generated chunk. A configured structure may get a territorial
     * spirit at the centre of its bounding box. The roll is fixed by the world seed, the structure and where it starts
     * ({@link BirthRules#haunted}), so whether a structure is haunted does not depend on what was explored first.
     *
     * @param roll whether the structure's chance is rolled; false for the debug command
     */
    public static BirthRules.Verdict generated(ServerLevel level, StructureStart start, boolean roll) {
        String id = level.registryAccess()
                .lookupOrThrow(Registries.STRUCTURE)
                .getResourceKey(start.getStructure())
                .map(key -> key.identifier().toString())
                .orElse("");
        Config.Section config = Config.section("births");
        Config.Section structures = config.section("structures");
        if (!structures.has(id)) {
            return BirthRules.Verdict.NOTHING;
        }
        Config.Section of = structures.section(id);
        ChunkPos chunk = start.getChunkPos();
        if (roll && !BirthRules.haunted(level.getSeed(), id, chunk.x(), chunk.z(), of.num("chance"))) {
            return BirthRules.Verdict.NOTHING;
        }
        Director.anchor(
                HauntData.get(level.getServer()),
                Spirits.byId(of.text("spirit")).orElseThrow(),
                level,
                start.getBoundingBox().getCenter(),
                level.getServer().overworld().getGameTime());
        return BirthRules.Verdict.BORN;
    }

    /**
     * A territorial spirit of the type stands at {@code at}, unless another one is within
     * {@code births.structure_spacing}. Used by effigies.
     *
     * @return {@link BirthRules.Verdict#BORN} or {@link BirthRules.Verdict#CROWDED}
     */
    public static BirthRules.Verdict place(ServerLevel level, SpiritType spirit, BlockPos at) {
        BirthRules.Verdict verdict = room(level, at);
        if (verdict == BirthRules.Verdict.BORN) {
            Director.anchor(
                    HauntData.get(level.getServer()),
                    spirit,
                    level,
                    at,
                    level.getServer().overworld().getGameTime());
        }
        return verdict;
    }

    /** What {@link #place} would do at {@code at}, with nothing changed. */
    public static BirthRules.Verdict room(ServerLevel level, BlockPos at) {
        Config.Section config = Config.section("births");
        long nearest = HauntData.get(level.getServer())
                .territorialWithin(level.dimension(), at, config.integer("structure_spacing"))
                .stream()
                .mapToLong(t -> (long) t.anchor().distSqr(at))
                .min()
                .orElse(Long.MAX_VALUE);
        return BirthRules.settle(nearest, config.integer("structure_spacing"));
    }

    /**
     * Call at each director check. A player left with no personal spirit gets another of a type that did not just leave
     * after {@code births.successor_min_days} to {@code successor_max_days}; the wait starts the first time they are
     * seen without one. Nothing is born on a released, creative or spectator player.
     */
    public static void successor(ServerPlayer player, HauntData data, long now) {
        HauntData.Successor waiting = data.successor(player.getUUID());
        if (waiting == null || player.isSpectator() || player.isCreative() || Requiem.released(player)) {
            return;
        }
        List<HauntData.Haunting> own = data.get(player.getUUID());
        if (own != null && !own.isEmpty()) {
            data.putSuccessor(player.getUUID(), null);
            return;
        }
        Config.Section config = Config.section("births");
        if (waiting.dueAt() < 0) {
            double days = config.num("successor_min_days")
                    + player.getRandom().nextDouble()
                            * (config.num("successor_max_days") - config.num("successor_min_days"));
            data.putSuccessor(
                    player.getUUID(), new HauntData.Successor(now + (long) (days * Ticks.DAY), waiting.gone()));
            return;
        }
        if (now < waiting.dueAt()) {
            return;
        }
        List<SpiritType> pool = new ArrayList<>(Spirits.personal());
        pool.removeIf(t -> waiting.gone().contains(t.id()));
        if (pool.isEmpty()) {
            pool.addAll(Spirits.personal());
        }
        bind(player, pool.get(player.getRandom().nextInt(pool.size())));
        data.putSuccessor(player.getUUID(), null);
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
