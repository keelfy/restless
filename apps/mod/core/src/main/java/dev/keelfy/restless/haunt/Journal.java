package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.haunt.Notebook.Chapter;
import dev.keelfy.restless.haunt.Notebook.Kind;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * What a player has seen, noted down as it happens and read as a book; saved with the world ({@link Notebook}), not on the
 * item. Each kind of note can open the guide chapter about it, so the guide grows with what the player has run into.
 */
public final class Journal {
    private Journal() {}

    /**
     * Notes something down unless the player already has it, and lets them hear the page turn.
     *
     * @param spirit who it is about, if the game knows; the journal only names it once the player has met it
     * @param args components, or anything else, written as text
     */
    public static void note(ServerPlayer player, Kind kind, HauntData.@Nullable Haunting spirit, Object... args) {
        note(player, kind, spirit, null, args);
    }

    /** @param tag see {@link Kind} */
    public static void note(ServerPlayer player, Kind kind, HauntData.@Nullable Haunting spirit, @Nullable String tag, Object... args) {
        List<Component> text = new ArrayList<>();
        for (Object arg : args) {
            text.add(arg instanceof Component c ? c : Component.literal(String.valueOf(arg)));
        }
        HauntData data = HauntData.get(player.level().getServer());
        Notebook grown = data.notebook(player.getUUID()).with(new Notebook.Note(kind,
            Optional.ofNullable(spirit).map(HauntData.Haunting::uuid), Optional.ofNullable(tag), List.copyOf(text)));
        if (grown != null) {
            data.putNotebook(player.getUUID(), grown);
            turnPage(player, Component.translatable("message.restless.journal_written"));
        }
        for (Chapter chapter : chapters(kind)) {
            unlock(player, chapter);
        }
    }

    /** Opens a guide chapter the first time the player runs into what it is about. */
    public static void unlock(ServerPlayer player, Chapter chapter) {
        HauntData data = HauntData.get(player.level().getServer());
        Notebook opened = data.notebook(player.getUUID()).opened(chapter);
        if (opened != null) {
            data.putNotebook(player.getUUID(), opened);
            turnPage(player, Component.translatable("message.restless.journal_chapter", JournalPages.title(chapter)));
        }
    }

    /** A player seen for the first time: the journal opens at its first chapter and, if the config says so, is put in their hands. */
    public static void begin(ServerPlayer player) {
        HauntData data = HauntData.get(player.level().getServer());
        Notebook opened = data.notebook(player.getUUID()).opened(Chapter.START);
        if (opened != null) {
            data.putNotebook(player.getUUID(), opened);
        }
        if (Config.section("journal").flag("give_on_join")) {
            player.getInventory().placeItemBackInInventory(new ItemStack(Content.JOURNAL.get()), Prediction.SERVER_ONLY);
        }
    }

    /** Opens the player's discovered sections in the journal screen. */
    public static void open(ServerPlayer player, ItemStack journal, InteractionHand hand) {
        HauntData data = HauntData.get(player.level().getServer());
        // A player from before the guide still gets its first chapter.
        Notebook started = data.notebook(player.getUUID()).opened(Chapter.START);
        if (started != null) {
            data.putNotebook(player.getUUID(), started);
        }
        player.connection.send(new ClientboundCustomPayloadPacket(
            new JournalPayload(JournalPages.sections(data.notebook(player.getUUID())))));
    }

    private static List<Chapter> chapters(Kind kind) {
        return switch (kind) {
            case PHOTO -> List.of(Chapter.CLUES, Chapter.WEAKNESS);
            case FOOTPRINTS, VOICE, PLANCHETTE -> List.of(Chapter.CLUES);
            case GRAVE, GRAVE_WHISPER -> List.of(Chapter.GRAVES);
            case EVOLVED -> List.of(Chapter.EVOLUTION);
            case RESTED, BANISHED -> List.of(Chapter.ESSENCE);
            case DREAM, OLD -> List.of();
        };
    }

    private static void turnPage(ServerPlayer player, Component message) {
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.6f, 0.8f);
        player.sendSystemMessage(message, true);
    }
}
