package dev.keelfy.restless.haunt;

import dev.keelfy.restless.config.Config;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;

/** What a player has learned, written down as it happens and read as a book; saved with the world, not on the item. */
public final class Journal {
    private Journal() {}

    /** Adds the entry unless the player already has it, and lets them hear the page turn. */
    public static void write(ServerPlayer player, Component entry) {
        if (HauntData.get(player.level().getServer()).write(player.getUUID(), entry)) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.6f, 0.8f);
            player.sendSystemMessage(Component.translatable("message.restless.journal_written"), true);
        }
    }

    /** Copies the player's entries into the held journal and opens it like a written book. */
    public static void open(ServerPlayer player, ItemStack journal, InteractionHand hand) {
        List<Component> entries = HauntData.get(player.level().getServer()).journal(player.getUUID());
        int perPage = Config.section("journal").integer("entries_per_page");
        List<Filterable<Component>> pages = new ArrayList<>();
        for (int i = 0; i < entries.size(); i += perPage) {
            Component page = Component.empty();
            for (int j = i; j < Math.min(i + perPage, entries.size()); j++) {
                page = j == i ? entries.get(j).copy() : page.copy().append("\n\n").append(entries.get(j));
            }
            pages.add(Filterable.passThrough(page));
        }
        if (pages.isEmpty()) {
            pages.add(Filterable.passThrough(Component.translatable("item.restless.journal.empty")));
        }
        journal.set(DataComponents.WRITTEN_BOOK_CONTENT,
            new WrittenBookContent(Filterable.passThrough("Journal"), player.getName().getString(), 0, pages, true));
        player.containerMenu.broadcastChanges();
        player.openItemGui(journal, hand);
    }
}
