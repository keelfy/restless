package dev.keelfy.restless.banish;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.block.Altar;
import dev.keelfy.restless.divine.Sense;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.haunt.HauntSounds;
import dev.keelfy.restless.haunt.Location;
import dev.keelfy.restless.haunt.Scan;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.haunt.event.HauntTier;
import dev.keelfy.restless.journal.Journal;
import dev.keelfy.restless.journal.Notebook;
import dev.keelfy.restless.spirit.Focus;
import dev.keelfy.restless.spirit.Spirits;
import dev.keelfy.restless.spirit.Story;
import dev.keelfy.restless.spirit.Traits;
import dev.keelfy.restless.spirit.land.Corruption;
import dev.keelfy.restless.spirit.land.Graves;
import dev.keelfy.restless.spirit.life.Echoes;
import dev.keelfy.restless.spirit.life.Evolution;
import dev.keelfy.restless.state.HauntData;
import dev.keelfy.restless.ward.Wards;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Banishing. An altar inside a closed salt loop with lit candles around it takes an offering: the spirit near the
 * player that is weak to its category is called in and goes through the phases of a {@link Circle}: the call, the
 * bargain, the fury (where it acts on the player and snuffs candles) and the leaving. Every candle lit and the offering
 * in place at the end banishes it, and it leaves its essence. A form the spirit's type does not take makes the fury
 * longer. A category no spirit is weak to enrages one of them instead, snuffs every candle and makes it wary: it
 * refuses the altar for a while, and grows toward evolving. Rituals live in memory only, like every {@link Scheduler}
 * task. An Exorcist's Censer in the off hand shortens the fury, keeps the spirit from snuffing candles and from using
 * anything but harmful events, and brings one more essence.
 */
public final class Ritual {
    private Ritual() {}

    /** @return whether the altar took the offering; false leaves everything as it was */
    public static boolean begin(ServerPlayer player, ServerLevel level, BlockPos altar, Altar.Held held) {
        Focus focus = Objects.requireNonNull(held.focus);
        Config.Section config = Config.section("ritual");
        List<BlockPos> candles = new ArrayList<>();
        Scan.each(level, altar, config.integer("candle_radius"), (pos, state) -> {
            if (AbstractCandleBlock.isLit(state)) {
                candles.add(pos.immutable());
            }
        });
        if (!Altar.isComplete(level, altar)
                || candles.size() < config.integer("candles")
                || !Wards.ringed(level, altar)) {
            player.sendSystemMessage(
                    Component.translatable("message.restless.altar_incomplete", config.integer("candles")), true);
            return false;
        }
        List<Sense.Spirit> spirits = Sense.of(player, HauntData.get(level.getServer()));
        if (spirits.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.restless.ritual_silent"), true);
            return false;
        }
        long now = level.getServer().overworld().getGameTime();
        Optional<Sense.Spirit> weak = spirits.stream()
                .filter(s -> Traits.weakness(Traits.of(s.haunting().traits())) == focus)
                .findFirst();
        List<Sense.Spirit> unwary =
                spirits.stream().filter(s -> s.haunting().waryUntil() <= now).toList();
        if (weak.isPresent() ? weak.get().haunting().waryUntil() > now : unwary.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.restless.ritual_wary"), true);
            return false;
        }
        level.setBlock(altar, level.getBlockState(altar).setValue(Altar.HELD, held), 3);
        if (weak.isEmpty()) {
            wrong(player, level, altar, candles, unwary.get(player.getRandom().nextInt(unwary.size())), now);
            return true;
        }
        Sense.Spirit spirit = weak.get();
        boolean offForm = offForm(spirit.haunting(), held);
        boolean censer = censed(player);
        if (censer) {
            HauntData.get(level.getServer())
                    .change(
                            spirit.haunting().uuid(),
                            h -> h.angered(Config.section("relics").num("censer_anger")));
        }
        player.sendSystemMessage(
                Component.translatable(offForm ? "message.restless.ritual_off_form" : "message.restless.ritual_begins"),
                true);
        level.playSound(null, altar, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 0.6f);
        boolean hallowed = hallowed(level, altar, spirit.haunting());
        if (hallowed) {
            player.sendSystemMessage(Component.translatable("message.restless.ritual_hallowed"), false);
        }
        Plan plan =
                plan(spirit.haunting(), held, censer).longerFury(hallowed ? config.integer("hallowed_fury_waves") : 0);
        Circle circle = new Circle(player, level, altar, held, candles, spirit, censer, plan);
        circle.hallowed = hallowed;
        Scheduler.repeat(player, 1, 1, (p, n) -> circle.tick(n + 1));
        return true;
    }

    /** Whether the spirit's type does not take this offering's form. */
    static boolean offForm(HauntData.Haunting spirit, Altar.Held held) {
        return Spirits.byId(spirit.spirit())
                .filter(type -> type.form() != held.form)
                .isPresent();
    }

    /** Whether the player holds the Exorcist's Censer in the off hand. */
    static boolean censed(ServerPlayer player) {
        return player.getOffhandItem().is(Content.EXORCISTS_CENSER.get());
    }

    /** How many waves the ritual runs: longer against a grown spirit, and for a form it does not take. */
    public static int waves(HauntData.Haunting spirit, Altar.Held held) {
        return waves(spirit, held, false);
    }

    /** As {@link #waves(HauntData.Haunting, Altar.Held)}; a censer shortens the fury. */
    public static int waves(HauntData.Haunting spirit, Altar.Held held, boolean censer) {
        return plan(spirit, held, censer).total();
    }

    /**
     * How long each phase runs. A grown spirit and a form it does not take lengthen the fury; a censer cuts its base
     * length.
     */
    public static Plan plan(HauntData.Haunting spirit, Altar.Held held, boolean censer) {
        Config.Section config = Config.section("ritual");
        int fury = (censer ? Config.section("relics").integer("censer_fury_waves") : config.integer("fury_waves"))
                + (spirit.evolved() ? Config.section("evolution").integer("extra_waves") : 0)
                + (offForm(spirit, held) ? config.integer("off_form_waves") : 0);
        return new Plan(
                config.integer("call_waves"), config.integer("bargain_waves"), fury, config.integer("leaving_waves"));
    }

    /** Whether the altar stands near the spirit's gravestone, or the ritual begins around midnight. */
    private static boolean hallowed(ServerLevel level, BlockPos altar, HauntData.Haunting spirit) {
        Config.Section config = Config.section("ritual");
        double grave = Graves.of(HauntData.get(level.getServer()), spirit.uuid())
                .map(pos -> Math.sqrt(pos.distSqr(altar)))
                .orElse(Double.POSITIVE_INFINITY);
        return Hallow.hallowed(
                grave,
                config.num("hallowed_grave_radius"),
                level.getOverworldClockTime(),
                config.integer("midnight_from"),
                config.integer("midnight_to"));
    }

    /** Dense vanilla soot follows the offering's bob; its task ends with the ritual, including player departure. */
    static void smoke(ServerLevel level, BlockPos altar, Altar.Held held) {
        BlockState state = level.getBlockState(altar);
        if (!state.is(Content.ALTAR.get()) || state.getValue(Altar.HELD) != held || !Altar.isComplete(level, altar)) {
            return;
        }
        double height = 1.12 + Math.sin((level.getGameTime() % 24000) * 0.06) * 0.035;
        level.sendParticles(
                ParticleTypes.LARGE_SMOKE,
                altar.getX() + 0.5,
                altar.getY() + height,
                altar.getZ() + 0.5,
                4,
                0.22,
                0.10,
                0.22,
                0.008);
    }

    /** @param extra essences on top of the usual: the censer's and a hallowed ritual's */
    static void banish(ServerPlayer player, ServerLevel level, BlockPos altar, Sense.Spirit spirit, int extra) {
        clear(level, altar);
        HauntData data = HauntData.get(level.getServer());
        // A censer's circle never lost a candle to the spirit, and a candle lost any other way fails the ritual first.
        int essences = essences(
                        spirit.haunting().evolved(),
                        Corruption.spots(data, spirit.haunting().uuid()),
                        Config.section("ritual").integer("blight_reward_spots"))
                + extra;
        data.change(spirit.haunting().uuid(), h -> null);
        Location.forget(spirit.haunting().uuid());
        Echoes.banished(player, spirit.haunting(), altar);
        Supplier<Item> essence = Content.ESSENCE.get(spirit.haunting().spirit());
        if (essence != null) {
            level.addFreshEntity(new ItemEntity(
                    level,
                    altar.getX() + 0.5,
                    altar.getY() + 1.0,
                    altar.getZ() + 0.5,
                    new ItemStack(essence.get(), essences)));
        }
        level.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                altar.getX() + 0.5,
                altar.getY() + 1.0,
                altar.getZ() + 0.5,
                40,
                0.5,
                1.0,
                0.5,
                0.05);
        level.playSound(null, altar, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0f, 0.6f);
        Component type = Evolution.name(spirit.haunting());
        player.sendSystemMessage(
                essences > 1
                        ? Component.translatable("message.restless.banished_more", type, essences)
                        : Component.translatable("message.restless.banished", type),
                true);
        Journal.tagged(
                player,
                Notebook.Kind.BANISHED,
                spirit.haunting(),
                spirit.haunting().spirit() + (spirit.haunting().evolved() ? "_evolved" : ""),
                type,
                Story.of(spirit.haunting().seed()).name());
    }

    /**
     * What a banishing leaves: one essence, one more for a spirit that had evolved (its ritual is longer), one more for
     * a territorial spirit whose blight had grown to {@code blightSpots} spots.
     */
    public static int essences(boolean evolved, int blightSpots, int blightReward) {
        return 1 + (evolved ? 1 : 0) + (blightSpots >= blightReward ? 1 : 0);
    }

    static void fail(ServerPlayer player, ServerLevel level, BlockPos altar, Sense.Spirit spirit) {
        clear(level, altar);
        HauntData.get(level.getServer())
                .change(
                        spirit.haunting().uuid(),
                        h -> h.angered(Config.section("ritual").num("fail_anger")));
        level.playSound(null, altar, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.0f, 0.5f);
        player.sendSystemMessage(Component.translatable("message.restless.ritual_failed"), true);
    }

    /**
     * The wrong category: the spirit eats the offering, its anger peaks, it strikes at once, every candle goes out, and
     * it keeps away from altars for a while and comes of age sooner.
     */
    private static void wrong(
            ServerPlayer player,
            ServerLevel level,
            BlockPos altar,
            List<BlockPos> candles,
            Sense.Spirit spirit,
            long now) {
        Config.Section config = Config.section("ritual");
        clear(level, altar);
        for (BlockPos pos : candles) {
            BlockState candle = level.getBlockState(pos);
            if (AbstractCandleBlock.isLit(candle)) {
                AbstractCandleBlock.extinguish(null, candle, level, pos);
            }
        }
        HauntData.Haunting angered = spirit.haunting().angered(1.0);
        HauntData.get(level.getServer())
                .change(
                        angered.uuid(),
                        h -> h.angered(1.0)
                                .wary(now + config.integer("wary_ticks"))
                                .aged(config.integer("wrong_age_ticks")));
        Config.Tone refused = config.tone("wrong_sound");
        level.playSound(
                null, altar, HauntSounds.OFFERING_REFUSED, SoundSource.HOSTILE, refused.volume(), refused.pitch());
        player.sendSystemMessage(Component.translatable("message.restless.wrong_focus"), true);
        Director.act(player, angered, spirit.at(), HauntTier.HARMFUL);
    }

    static void clear(ServerLevel level, BlockPos altar) {
        BlockState state = level.getBlockState(altar);
        if (state.is(Content.ALTAR.get())) {
            level.setBlock(altar, state.setValue(Altar.HELD, Altar.Held.EMPTY), 3);
        }
    }
}
