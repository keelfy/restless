package dev.keelfy.restless.gametest;

import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.haunt.HauntData;
import dev.keelfy.restless.haunt.Journal;
import dev.keelfy.restless.haunt.JournalPages;
import dev.keelfy.restless.haunt.JournalPayload;
import dev.keelfy.restless.haunt.Notebook;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;

final class JournalGameTests {
    private JournalGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("journal_sorts_notes", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            HauntData data = HauntData.get(helper.getLevel().getServer());
            long now = helper.getLevel().getGameTime();
            HauntData.Haunting shade = HauntData.Haunting.born("shade", now, helper.getLevel().getRandom());

            Journal.begin(player);
            check(helper, data.notebook(player.getUUID()).has(Notebook.Chapter.START), "a new player must start with the first chapter");
            check(helper, player.getInventory().contains(s -> s.is(Content.JOURNAL.get())), "a new player must be given a journal");
            check(helper, keys(player, data).contains("journal.restless.guide.start.1"), "the guide must show the first chapter");

            Journal.note(player, Notebook.Kind.VOICE, shade, "dark");
            List<String> before = keys(player, data);
            check(helper, before.contains("journal.restless.section.loose"), "a clue of an unmet spirit must go to loose notes");
            check(helper, before.contains("journal.restless.may_lie"), "a spirit box word must be marked as possibly a lie");
            check(helper, !before.contains("journal.restless.unnamed"), "an unmet spirit must not get its own pages");
            check(helper, data.notebook(player.getUUID()).has(Notebook.Chapter.CLUES), "the first clue must open the clues chapter");

            Journal.note(player, Notebook.Kind.PHOTO, shade, "shade", Component.translatable("spirit.restless.shade"), "it feeds on light",
                Component.translatable("focus.restless.glowstone"));
            List<String> after = keys(player, data);
            JournalPayload sent = new JournalPayload(JournalPages.sections(data.notebook(player.getUUID())));
            check(helper, sent.sections().stream().map(JournalPages.Section::key).toList()
                .equals(List.of("guide", "spirits", "bestiary")), "the screen must receive only discovered sections in order");
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                JournalPayload.CODEC.encode(buffer, sent);
                JournalPayload received = JournalPayload.CODEC.decode(buffer);
                check(helper, received.sections().stream().map(JournalPages.Section::key).toList()
                    .equals(sent.sections().stream().map(JournalPages.Section::key).toList()), "section order must survive the network codec");
                // Vanilla collapses literal component arguments into strings during decoding. Compare their content.
                List<String> sentKeys = new ArrayList<>();
                sent.sections().forEach(section -> section.pages().forEach(text -> collect(text, sentKeys)));
                List<String> screenKeys = new ArrayList<>();
                received.sections().forEach(section -> section.pages().forEach(text -> collect(text, screenKeys)));
                check(helper, screenKeys.equals(sentKeys), "journal text and translation arguments must survive the network codec: " + sentKeys + " != " + screenKeys);
                check(helper, screenKeys.contains("journal.restless.may_lie"), "the screen must retain unreliable evidence labels");
            } finally {
                buffer.release();
            }
            check(helper, !after.contains("journal.restless.section.loose"), "once met, the spirit's earlier clues must move to its pages");
            check(helper, after.contains("journal.restless.unnamed") && after.contains("journal.restless.note.voice"),
                "the met spirit's pages must hold its earlier clues");
            check(helper, after.contains("journal.restless.bestiary.shade"), "a photographed kind must get a bestiary page");
            check(helper, data.notebook(player.getUUID()).has(Notebook.Chapter.WEAKNESS), "the first photo must open the altar chapter");

            int notes = data.notebook(player.getUUID()).notes().size();
            Journal.note(player, Notebook.Kind.VOICE, shade, "dark");
            check(helper, data.notebook(player.getUUID()).notes().size() == notes, "the same note must not be written twice");
            data.putNotebook(player.getUUID(), Notebook.EMPTY);
            helper.succeed();
        });
    }

    /** Every translation key on the player's pages, so the test does not depend on the server's language. */
    private static List<String> keys(ServerPlayer player, HauntData data) {
        List<String> keys = new ArrayList<>();
        JournalPages.of(data.notebook(player.getUUID())).forEach(page -> collect(page, keys));
        return keys;
    }

    private static void collect(Component component, List<String> keys) {
        if (component.getContents() instanceof TranslatableContents translatable) {
            keys.add(translatable.getKey());
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component inner) {
                    collect(inner, keys);
                } else {
                    keys.add(String.valueOf(arg));
                }
            }
        }
        if (component.getContents() instanceof PlainTextContents literal && !literal.text().isEmpty()) {
            keys.add(literal.text());
        }
        component.getSiblings().forEach(sibling -> collect(sibling, keys));
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }
}
