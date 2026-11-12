package dev.keelfy.restless.gametest;

import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.divine.Divination;
import dev.keelfy.restless.journal.Codex;
import dev.keelfy.restless.journal.Journal;
import dev.keelfy.restless.journal.JournalPages;
import dev.keelfy.restless.journal.JournalPayload;
import dev.keelfy.restless.journal.Notebook;
import dev.keelfy.restless.photo.DevelopPayload;
import dev.keelfy.restless.photo.Picture;
import dev.keelfy.restless.state.HauntData;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

final class JournalGameTests {
    private JournalGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        registerArchive(tests);
        registerDiary(tests);
        tests.put("photo_develops_once", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            ItemStack photo = new ItemStack(Content.PHOTO.get());
            photo.set(Content.FILM.get(), 42);
            player.getInventory().setItem(0, photo);
            byte[] grey = new byte[Picture.SIZE];
            grey[7] = 99;
            Picture picture = new Picture(grey);

            Divination.develop(player, new DevelopPayload(41, new Picture(new byte[Picture.SIZE])));
            check(helper, !photo.has(Content.PICTURE.get()), "another film's picture must not develop this photo");
            Divination.develop(player, new DevelopPayload(42, picture));
            check(
                    helper,
                    picture.equals(photo.get(Content.PICTURE.get())),
                    "the film's picture must develop the photo");
            check(helper, !photo.has(Content.FILM.get()), "a developed photo must no longer wait for its picture");
            Divination.develop(player, new DevelopPayload(42, new Picture(new byte[Picture.SIZE])));
            check(helper, picture.equals(photo.get(Content.PICTURE.get())), "a photo must develop only once");

            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                    Unpooled.buffer(), helper.getLevel().registryAccess());
            ItemStack.STREAM_CODEC.encode(buffer, photo);
            check(
                    helper,
                    ItemStack.matches(photo, ItemStack.STREAM_CODEC.decode(buffer)),
                    "a developed photo must reach clients unchanged");
            check(
                    helper,
                    Picture.CODEC
                            .parse(
                                    net.minecraft.nbt.NbtOps.INSTANCE,
                                    Picture.CODEC
                                            .encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, picture)
                                            .getOrThrow())
                            .getOrThrow()
                            .equals(picture),
                    "a picture must survive saving");
            helper.succeed();
        });
        tests.put("journal_sorts_notes", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            HauntData data = HauntData.get(helper.getLevel().getServer());
            long now = helper.getLevel().getGameTime();
            HauntData.Haunting shade =
                    HauntData.Haunting.born("shade", now, helper.getLevel().getRandom());

            Journal.begin(player);
            check(
                    helper,
                    data.notebook(player.getUUID()).has(Notebook.Chapter.START),
                    "a new player must start with the first chapter");
            check(
                    helper,
                    player.getInventory().contains(s -> s.is(Content.JOURNAL.get())),
                    "a new player must be given a journal");
            check(
                    helper,
                    keys(player, data).contains("journal.restless.guide.start.1"),
                    "the guide must show the first chapter");

            Journal.note(player, Notebook.Kind.VOICE, shade, "dark");
            List<String> before = keys(player, data);
            check(
                    helper,
                    !before.contains("journal.restless.section.loose")
                            && before.contains("journal.restless.unknown_spirit"),
                    "a clue of an unphotographed spirit must open an unknown spirit's dossier");
            check(
                    helper,
                    before.contains("journal.restless.may_lie"),
                    "a spirit box word must be marked as possibly a lie");
            check(
                    helper,
                    !before.contains("journal.restless.unnamed"),
                    "an unphotographed spirit must not have a kind");
            check(
                    helper,
                    data.notebook(player.getUUID()).has(Notebook.Chapter.CLUES),
                    "the first clue must open the clues chapter");

            Journal.note(
                    player,
                    Notebook.Kind.EVOLVED,
                    shade,
                    "Name",
                    Component.translatable("spirit.restless.shade"),
                    Component.translatable("spirit.restless.shade_evolved"));
            List<String> grown = keys(player, data);
            check(
                    helper,
                    grown.contains("journal.restless.note.evolved_unknown")
                            && !grown.contains("journal.restless.note.evolved")
                            && !grown.contains("spirit.restless.shade_evolved")
                            && !grown.contains("journal.restless.section.chronicle"),
                    "an unphotographed spirit's evolution must be in its dossier without naming a type");

            Journal.tagged(
                    player, Notebook.Kind.PHOTO, shade, "shade", Component.translatable("spirit.restless.shade"));
            List<String> after = keys(player, data);
            check(
                    helper,
                    after.contains("journal.restless.note.evolved")
                            && !after.contains("journal.restless.note.evolved_unknown"),
                    "a photographed spirit's evolution must name the type");
            byte[] grey = new byte[Picture.SIZE];
            grey[5] = 77;
            Picture shot = new Picture(grey);
            Journal.paste(player, shade.uuid(), shot);
            check(
                    helper,
                    JournalPages.sections(data.notebook(player.getUUID())).stream()
                            .filter(section -> section.key().equals("spirits"))
                            .flatMap(section -> section.cards().stream())
                            .anyMatch(
                                    card -> card.picture().filter(shot::equals).isPresent()),
                    "a developed photo must be pasted onto its spirit's card");
            JournalPayload sent = new JournalPayload(JournalPages.sections(data.notebook(player.getUUID())), true);
            check(
                    helper,
                    sent.sections().stream()
                            .map(JournalPages.Section::key)
                            .toList()
                            .equals(List.of("guide", "spirits", "bestiary")),
                    "the screen must receive only discovered sections in order");
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                    Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                JournalPayload.CODEC.encode(buffer, sent);
                JournalPayload received = JournalPayload.CODEC.decode(buffer);
                check(
                        helper,
                        received.sections().stream()
                                .map(JournalPages.Section::key)
                                .toList()
                                .equals(sent.sections().stream()
                                        .map(JournalPages.Section::key)
                                        .toList()),
                        "section order must survive the network codec");
                // Vanilla collapses literal component arguments into strings during decoding. Compare their content.
                List<String> sentKeys = new ArrayList<>();
                sent.sections().forEach(section -> texts(section, sentKeys));
                List<String> screenKeys = new ArrayList<>();
                received.sections().forEach(section -> texts(section, screenKeys));
                check(
                        helper,
                        screenKeys.equals(sentKeys),
                        "journal text and translation arguments must survive the network codec: " + sentKeys + " != "
                                + screenKeys);
                check(
                        helper,
                        received.sections().stream()
                                .flatMap(section -> section.cards().stream())
                                .anyMatch(card ->
                                        card.picture().filter(shot::equals).isPresent()),
                        "the pasted photo must survive the network codec");
                check(
                        helper,
                        screenKeys.contains("journal.restless.may_lie"),
                        "the screen must retain unreliable evidence labels");
            } finally {
                buffer.release();
            }
            check(helper, !after.contains("journal.restless.section.loose"), "a spirit's clues must stay on its pages");
            check(
                    helper,
                    after.contains("journal.restless.unnamed")
                            && !after.contains("journal.restless.unknown_spirit")
                            && after.contains("journal.restless.note.voice"),
                    "once photographed, the dossier must name the kind and keep its earlier clues");
            check(
                    helper,
                    after.contains("journal.restless.bestiary.shade"),
                    "a photographed kind must get a bestiary page");
            check(
                    helper,
                    after.contains("journal.restless.met.shade"),
                    "a bestiary page must say where the kind is met");
            check(
                    helper,
                    data.notebook(player.getUUID()).has(Notebook.Chapter.WEAKNESS),
                    "the first photo must open the altar chapter");

            int notes = data.notebook(player.getUUID()).notes().size();
            Journal.note(player, Notebook.Kind.VOICE, shade, "dark");
            check(
                    helper,
                    data.notebook(player.getUUID()).notes().size() == notes,
                    "the same note must not be written twice");
            data.putNotebook(player.getUUID(), Notebook.EMPTY);
            helper.succeed();
        });
    }

    private static void registerArchive(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("journal_files_banished", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            HauntData data = HauntData.get(helper.getLevel().getServer());
            long now = helper.getLevel().getGameTime();
            HauntData.Haunting first =
                    HauntData.Haunting.born("shade", now, helper.getLevel().getRandom());
            HauntData.Haunting second =
                    HauntData.Haunting.born("shade", now, helper.getLevel().getRandom());
            Component shade = Component.translatable("spirit.restless.shade");
            for (HauntData.Haunting met : List.of(first, second)) {
                Journal.tagged(player, Notebook.Kind.PHOTO, met, "shade", shade);
            }
            Journal.tagged(player, Notebook.Kind.BANISHED, first, "shade", shade, "Marta");
            Journal.tagged(player, Notebook.Kind.BANISHED, null, "phantom", shade, "Nobody");
            Notebook book = data.notebook(player.getUUID());
            int record = book.notes().size() - 2;
            check(helper, book.fits(record, 1), "a banishment must be fileable under any met spirit of its type");
            check(helper, !book.fits(record + 1, 0), "a note about nobody must not be fileable");
            check(
                    helper,
                    JournalPages.sections(book).stream()
                            .noneMatch(section -> section.key().equals("archive")),
                    "nothing is archived until the player files it");
            // The player picks the second spirit, though the banished one was the first: the book takes their word.
            Notebook filed = book.filed(record, 1);
            check(helper, filed != null, "filing must succeed");
            check(helper, !filed.fits(record, 0), "a banishment must be filed once");
            JournalPages.Section archive = JournalPages.sections(filed).stream()
                    .filter(section -> section.key().equals("archive"))
                    .findFirst()
                    .orElseThrow();
            check(helper, archive.cards().size() == 1, "the filed spirit must move to the archive");
            List<String> texts = new ArrayList<>();
            texts(archive, texts);
            check(helper, texts.contains("Marta"), "the archive must name the spirit by the banishment");
            check(
                    helper,
                    JournalPages.sections(filed).stream()
                            .filter(section -> section.key().equals("spirits"))
                            .allMatch(section -> section.cards().size() == 1),
                    "the other spirit must stay among the known ones");
            data.putNotebook(player.getUUID(), Notebook.EMPTY);
            helper.succeed();
        });
    }

    /** Every translation key on the player's pages, so the test does not depend on the server's language. */
    private static List<String> keys(ServerPlayer player, HauntData data) {
        List<String> keys = new ArrayList<>();
        JournalPages.sections(data.notebook(player.getUUID())).forEach(section -> texts(section, keys));
        return keys;
    }

    private static void texts(JournalPages.Section section, List<String> keys) {
        section.cards().forEach(card -> {
            collect(card.heading(), keys);
            card.entries().forEach(entry -> collect(entry.text(), keys));
        });
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
        if (component.getContents() instanceof PlainTextContents literal
                && !literal.text().isEmpty()) {
            keys.add(literal.text());
        }
        component.getSiblings().forEach(sibling -> collect(sibling, keys));
    }

    private static void registerDiary(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("diary_page_teaches_its_recipe", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            Identifier recipe = Identifier.parse("restless:spirit_box");
            ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, recipe);
            ItemStack pages = new ItemStack(Content.DIARY_PAGE.get(), 2);
            pages.set(Content.RECIPE.get(), recipe);
            player.setItemInHand(InteractionHand.MAIN_HAND, pages);
            check(helper, !player.getRecipeBook().contains(key), "the recipe must not be known before reading");

            pages.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            check(helper, player.getRecipeBook().contains(key), "reading a page must teach its recipe");
            check(helper, pages.getCount() == 1, "reading a page must use it up");

            pages.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            check(helper, pages.getCount() == 1, "a page of a recipe already known must be kept");
            helper.succeed();
        });
        tests.put("journal_lists_charms_and_recipes", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            HauntData data = HauntData.get(helper.getLevel().getServer());
            data.putNotebook(player.getUUID(), Notebook.EMPTY);
            check(
                    helper,
                    Codex.of(player, data.notebook(player.getUUID())).charms().isEmpty(),
                    "a journal that knows no spirit must list no charm");

            Journal.tagged(player, Notebook.Kind.PHOTO, null, "shade", Component.literal("Shade"));
            Codex codex = Codex.of(player, data.notebook(player.getUUID()));
            check(helper, made(codex.charms(), Content.DARK_EYE.get()), "a shade photo must open the Dark Lens page");
            check(
                    helper,
                    !made(codex.charms(), Content.TRUTHSEERS_EYE.get()),
                    "an advanced charm must wait for the other essence's spirit");
            check(
                    helper,
                    codex.charms().getFirst().entries().getLast().items().size() == 5,
                    "a recipe row holds each distinct ingredient and the result");

            check(
                    helper,
                    JournalPages.sections(data.notebook(player.getUUID()), codex).stream()
                            .filter(section -> section.key().equals("guide"))
                            .flatMap(section -> section.cards().stream())
                            .flatMap(card -> card.entries().stream())
                            .anyMatch(entry -> !entry.items().isEmpty()
                                    && entry.items().getLast().is(Content.ALTAR_ITEM.get())),
                    "the chapter on the altar must show the altar's recipe");

            Journal.tagged(player, Notebook.Kind.BANISHED, null, "banshee", Component.literal("Banshee"), "Ann");
            check(
                    helper,
                    made(Codex.of(player, data.notebook(player.getUUID())).charms(), Content.TRUTHSEERS_EYE.get()),
                    "a banished banshee must open the page of the charm made with her essence");
            check(helper, codex.recipes().isEmpty(), "no recipe is in the book before a diary page is read");

            Identifier recipe = Identifier.parse("restless:spirit_box");
            ItemStack page = new ItemStack(Content.DIARY_PAGE.get());
            page.set(Content.RECIPE.get(), recipe);
            player.setItemInHand(InteractionHand.MAIN_HAND, page);
            page.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            Codex learned = Codex.of(player, data.notebook(player.getUUID()));
            check(helper, made(learned.recipes(), Content.SPIRIT_BOX_ITEM.get()), "a read page must list its recipe");

            JournalPayload sent =
                    new JournalPayload(JournalPages.sections(data.notebook(player.getUUID()), learned), true);
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                    Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                JournalPayload.CODEC.encode(buffer, sent);
                JournalPayload received = JournalPayload.CODEC.decode(buffer);
                check(
                        helper,
                        received.sections().stream()
                                .map(JournalPages.Section::key)
                                .toList()
                                .containsAll(List.of("charms", "recipes")),
                        "charm and recipe tabs must reach the screen");
            } finally {
                buffer.release();
            }
            data.putNotebook(player.getUUID(), Notebook.EMPTY);
            helper.succeed();
        });
        tests.put("journal_lists_relics_once_all_spirits_are_known", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            HauntData data = HauntData.get(helper.getLevel().getServer());
            data.putNotebook(player.getUUID(), Notebook.EMPTY);
            for (String type : List.of("lost_one", "nix")) {
                Journal.tagged(player, Notebook.Kind.PHOTO, null, type, Component.literal(type));
            }
            check(
                    helper,
                    Codex.of(player, data.notebook(player.getUUID())).relics().isEmpty(),
                    "a relic must wait until every spirit it is made of is known");
            Journal.tagged(player, Notebook.Kind.PHOTO, null, "trickster", Component.literal("trickster"));
            check(
                    helper,
                    made(Codex.of(player, data.notebook(player.getUUID())).relics(), Content.FERRYMANS_COIN.get()),
                    "knowing all three spirits must open the relic's page");
            data.putNotebook(player.getUUID(), Notebook.EMPTY);
            helper.succeed();
        });
        tests.put("diary_pages_turn_up_in_structure_chests", helper -> {
            ServerLevel level = helper.getLevel();
            LootTable chest = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.SIMPLE_DUNGEON);
            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                    .create(LootContextParamSets.CHEST);
            Set<Identifier> taught = new HashSet<>();
            for (long seed = 0; seed < 400; seed++) {
                for (ItemStack loot : chest.getRandomItems(params, seed)) {
                    if (loot.is(Content.DIARY_PAGE.get())) {
                        taught.add(loot.get(Content.RECIPE.get()));
                    }
                }
            }
            check(helper, !taught.isEmpty(), "a dungeon chest must sometimes hold a diary page");
            check(helper, !taught.contains(null), "a page found in a chest must name its recipe");
            helper.succeed();
        });
    }

    /** Whether one of the cards has a recipe that makes this item. */
    private static boolean made(List<JournalPages.Card> cards, net.minecraft.world.item.Item item) {
        return cards.stream()
                .flatMap(card -> card.entries().stream())
                .anyMatch(entry ->
                        !entry.items().isEmpty() && entry.items().getLast().is(item));
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }
}
