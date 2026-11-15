package dev.keelfy.restless.banish;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.block.Altar;
import dev.keelfy.restless.divine.Clues;
import dev.keelfy.restless.divine.Divination;
import dev.keelfy.restless.divine.Sense;
import dev.keelfy.restless.haunt.Scan;
import dev.keelfy.restless.haunt.Scheduler;
import dev.keelfy.restless.journal.Journal;
import dev.keelfy.restless.journal.Notebook;
import dev.keelfy.restless.sanity.Sanity;
import dev.keelfy.restless.spirit.Clue;
import dev.keelfy.restless.spirit.Traits;
import dev.keelfy.restless.spirit.land.Graves;
import dev.keelfy.restless.spirit.life.Echoes;
import dev.keelfy.restless.state.HauntData;
import dev.keelfy.restless.state.Session;
import dev.keelfy.restless.ward.Wards;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.AbstractCandleBlock;

/**
 * Interrogation: a book used on a whole altar inside a closed salt ring with lit candles calls the spirit near the
 * player for as long as a ritual's call lasts, at the cost of {@code interrogation.sanity_cost} sanity. The first
 * question said in chat then (a habit, the desire, or where the grave lies) gets one true answer, from a liar too, and
 * the spirit leaves. An echo and a wary spirit refuse to come.
 */
public final class Interrogation {
    /** Whom each player is asking, until when in overworld game time. */
    private static final Map<UUID, Asking> ASKING = Session.SERVER.keep(new HashMap<>());

    /** A question a player may ask. */
    public enum Question {
        HABIT,
        DESIRE,
        GRAVE
    }

    private record Asking(UUID spirit, BlockPos altar, long until) {}

    private Interrogation() {}

    /** @return whether a spirit came to be asked; false leaves everything as it was */
    public static boolean begin(ServerPlayer player, ServerLevel level, BlockPos altar) {
        Config.Section ritual = Config.section("ritual");
        List<BlockPos> candles = new ArrayList<>();
        Scan.each(level, altar, ritual.integer("candle_radius"), (pos, state) -> {
            if (AbstractCandleBlock.isLit(state)) {
                candles.add(pos.immutable());
            }
        });
        if (!Altar.isComplete(level, altar)
                || candles.size() < ritual.integer("candles")
                || !Wards.ringed(level, altar)) {
            player.sendSystemMessage(
                    Component.translatable("message.restless.altar_incomplete", ritual.integer("candles")), true);
            return false;
        }
        HauntData data = HauntData.get(level.getServer());
        long now = level.getServer().overworld().getGameTime();
        Optional<HauntData.Haunting> spirit = Sense.of(player, data).stream()
                .map(Sense.Spirit::haunting)
                .filter(h -> !Echoes.is(data, h) && h.waryUntil() <= now)
                .findFirst();
        if (spirit.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.restless.ritual_silent"), true);
            return false;
        }
        int ticks = ritual.integer("call_waves") * ritual.integer("wave_ticks");
        Asking asking = new Asking(spirit.get().uuid(), altar.immutable(), now + ticks);
        ASKING.put(player.getUUID(), asking);
        Sanity.set(player, Sanity.of(player) - Config.section("interrogation").num("sanity_cost"));
        level.playSound(null, altar, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8f, 1.4f);
        player.sendSystemMessage(Component.translatable("message.restless.interrogation_begins"), true);
        Scheduler.later(player, ticks, p -> {
            if (ASKING.remove(p.getUUID(), asking)) {
                p.sendSystemMessage(Component.translatable("message.restless.interrogation_unasked"), true);
            }
        });
        return true;
    }

    /** Whether the player has a spirit waiting for their question. */
    public static boolean asking(ServerPlayer player) {
        return ASKING.containsKey(player.getUUID());
    }

    /** A chat line from {@code player}: the first question while a spirit waits gets its answer. */
    public static void said(ServerPlayer player, String line) {
        Asking asking = ASKING.get(player.getUUID());
        Optional<Question> question = question(line);
        if (asking == null || question.isEmpty()) {
            return;
        }
        ASKING.remove(player.getUUID());
        HauntData data = HauntData.get(player.level().getServer());
        Optional<HauntData.Haunting> spirit = data.find(asking.spirit());
        if (spirit.isEmpty() || player.level().getServer().overworld().getGameTime() > asking.until()) {
            return;
        }
        Component answer = answer(player, data, spirit.get(), asking.altar(), question.get())
                .orElse(Component.translatable("message.restless.interrogation_nothing"));
        Scheduler.later(player, Config.section("spirit_box").integer("delay_ticks"), p -> {
            p.sendSystemMessage(Component.translatable("message.restless.interrogation_answer", answer));
            Journal.note(p, Notebook.Kind.ANSWER, spirit.get(), answer);
        });
    }

    /** What the line asks, by the words of {@code interrogation.questions}; empty when it asks none of them. */
    public static Optional<Question> question(String line) {
        Config.Section questions = Config.section("interrogation").section("questions");
        List<String> words = List.of(
                line.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim().split(" "));
        // "What do you want?" asks for the desire, so the habit's plain words are tried last.
        if (heard(questions.texts("desire"), words)) {
            return Optional.of(Question.DESIRE);
        }
        if (heard(questions.texts("grave"), words)) {
            return Optional.of(Question.GRAVE);
        }
        if (heard(questions.texts("habit"), words)) {
            return Optional.of(Question.HABIT);
        }
        return Optional.empty();
    }

    /** Whether a word was said; a listed word ending in {@code *} matches every word it starts. */
    private static boolean heard(List<String> listed, List<String> words) {
        return listed.stream().anyMatch(w -> words.stream()
                .anyMatch(said -> w.endsWith("*") ? said.startsWith(w.substring(0, w.length() - 1)) : said.equals(w)));
    }

    /** The true answer; empty when the spirit has nothing to tell of it. */
    public static Optional<Component> answer(
            ServerPlayer player, HauntData data, HauntData.Haunting spirit, BlockPos altar, Question question) {
        return switch (question) {
            case HABIT -> Clues.reveal(
                            Divination.known(player, spirit),
                            Traits.all(),
                            Clue.PLANCHETTE,
                            new Random(player.getRandom().nextLong()),
                            true)
                    .map(Divination::hint);
            case DESIRE -> Traits.desire(Traits.of(spirit.traits())).map(Divination::hint);
            case GRAVE -> Graves.of(data, spirit.uuid()).map(grave -> direction(altar, grave));
        };
    }

    /** Which way the grave lies from the altar, as one of four quarters. */
    private static Component direction(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        String quarter = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? "east" : "west") : (dz > 0 ? "south" : "north");
        return Component.translatable(
                "message.restless.interrogation_grave",
                Component.translatable("direction.restless." + quarter));
    }
}
