package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Settings;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.content.Picture;
import dev.keelfy.restless.haunt.Notebook.Chapter;
import dev.keelfy.restless.haunt.Notebook.Kind;
import dev.keelfy.restless.platform.Platform;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * What a player has seen, noted down as it happens and read as a book; saved with the world ({@link Notebook}), not on
 * the item. Each kind of note can open the guide chapter about it, so the guide grows with what the player has run
 * into.
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
        tagged(player, kind, spirit, null, args);
    }

    /**
     * Notes something for a player who may be away: online they get it as from {@link #note}; offline it waits in their
     * notebook until they read it.
     */
    public static void noteFor(
            MinecraftServer server, UUID player, Kind kind, HauntData.@Nullable Haunting spirit, Object... args) {
        ServerPlayer online = server.getPlayerList().getPlayer(player);
        if (online != null) {
            note(online, kind, spirit, args);
            return;
        }
        HauntData data = HauntData.get(server);
        Notebook grown = data.notebook(player)
                .with(new Notebook.Note(
                        kind,
                        Optional.ofNullable(spirit).map(HauntData.Haunting::uuid),
                        Optional.empty(),
                        Arrays.stream(args)
                                .map(arg -> arg instanceof Component c ? c : Component.literal(String.valueOf(arg)))
                                .toList()));
        if (grown != null) {
            data.putNotebook(player, grown);
        }
    }

    /** @param tag see {@link Kind} */
    public static void tagged(
            ServerPlayer player, Kind kind, HauntData.@Nullable Haunting spirit, @Nullable String tag, Object... args) {
        List<Component> text = new ArrayList<>();
        for (Object arg : args) {
            text.add(arg instanceof Component c ? c : Component.literal(String.valueOf(arg)));
        }
        HauntData data = HauntData.get(player.level().getServer());
        Notebook grown = data.notebook(player.getUUID())
                .with(new Notebook.Note(
                        kind,
                        Optional.ofNullable(spirit).map(HauntData.Haunting::uuid),
                        Optional.ofNullable(tag),
                        List.copyOf(text)));
        if (grown != null) {
            data.putNotebook(player.getUUID(), grown);
            turnPage(player, Component.translatable("message.restless.journal_written"));
            sync(player);
        }
        for (Chapter chapter : chapters(kind)) {
            unlock(player, chapter);
        }
    }

    /** Pastes a developed photograph of a spirit into the player's notebook, in place of the last one. */
    public static void paste(ServerPlayer player, UUID spirit, Picture picture) {
        HauntData data = HauntData.get(player.level().getServer());
        data.putNotebook(player.getUUID(), data.notebook(player.getUUID()).pasted(spirit, picture));
        sync(player);
    }

    /** Opens a guide chapter the first time the player runs into what it is about. */
    public static void unlock(ServerPlayer player, Chapter chapter) {
        HauntData data = HauntData.get(player.level().getServer());
        Notebook opened = data.notebook(player.getUUID()).opened(chapter);
        if (opened != null) {
            data.putNotebook(player.getUUID(), opened);
            turnPage(player, Component.translatable("message.restless.journal_chapter", JournalPages.title(chapter)));
            sync(player);
        }
    }

    /**
     * A player seen for the first time: the journal opens at its first chapter and, if the config says so, is put in
     * their hands.
     */
    public static void begin(ServerPlayer player) {
        HauntData data = HauntData.get(player.level().getServer());
        Notebook opened = data.notebook(player.getUUID()).opened(Chapter.START);
        if (opened != null) {
            data.putNotebook(player.getUUID(), opened);
        }
        if (Settings.get().journalOnJoin()) {
            player.getInventory()
                    .placeItemBackInInventory(new ItemStack(Content.JOURNAL.get()), Prediction.SERVER_ONLY);
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
        show(player, true);
    }

    /**
     * The player filed a banishment under a spirit's dossier. Whether they are the same spirit is not checked: the
     * archive keeps the player's conclusion.
     */
    public static void file(ServerPlayer player, FilePayload payload) {
        HauntData data = HauntData.get(player.level().getServer());
        Notebook filed = data.notebook(player.getUUID()).filed(payload.record(), payload.dossier());
        if (filed != null) {
            data.putNotebook(player.getUUID(), filed);
            turnPage(player, Component.translatable("message.restless.journal_filed"));
            sync(player);
        }
    }

    /** Sends the pages for the book in the player's hands, which is read without opening a screen. */
    public static void sync(ServerPlayer player) {
        if (Platform.get().canReceive(player, JournalPayload.TYPE)) {
            show(player, false);
        }
    }

    private static void show(ServerPlayer player, boolean open) {
        HauntData data = HauntData.get(player.level().getServer());
        Notebook notebook = data.notebook(player.getUUID());
        player.connection.send(new ClientboundCustomPayloadPacket(
                new JournalPayload(JournalPages.sections(notebook, Codex.of(player, notebook)), open)));
    }

    private static List<Chapter> chapters(Kind kind) {
        return switch (kind) {
            case PHOTO -> List.of(Chapter.CLUES, Chapter.WEAKNESS);
            case FOOTPRINTS, VOICE, PLANCHETTE -> List.of(Chapter.CLUES);
            case GRAVE, GRAVE_WHISPER -> List.of(Chapter.GRAVES);
            case EVOLVED -> List.of(Chapter.EVOLUTION);
            case RESTED, BANISHED -> List.of(Chapter.ESSENCE);
            case DREAM, OLD, LAID -> List.of();
        };
    }

    private static void turnPage(ServerPlayer player, Component message) {
        player.level()
                .playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.6f, 0.8f);
        player.sendSystemMessage(message, true);
    }
}
