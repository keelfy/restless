package dev.keelfy.restless.gametest;

import dev.keelfy.restless.config.Settings;
import dev.keelfy.restless.content.Content;
import dev.keelfy.restless.journal.Journal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;

/** The player's choices live in their own file and reach core; the tuning data files are a separate thing. */
final class SettingsGameTests {
    private SettingsGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("settings_file_is_written", helper -> {
            Path file = FMLPaths.CONFIGDIR.get().resolve("restless/settings.toml");
            HauntEventGameTests.check(helper, Files.exists(file), "the settings file must be written on first run");
            try {
                String text = Files.readString(file);
                for (String key : new String[] {"hardcore", "swaps", "journal_on_join"}) {
                    HauntEventGameTests.check(helper, text.contains(key), "the settings file must hold " + key);
                }
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
            HauntEventGameTests.check(
                    helper, Settings.get().equals(Settings.DEFAULTS), "an untouched file must mean the defaults");
            helper.succeed();
        });
        tests.put("journal_on_join_follows_the_setting", helper -> {
            Settings before = Settings.get();
            ServerPlayer player = HauntEventGameTests.connected(helper);
            try {
                Settings.use(new Settings(before.hardcore(), before.swaps(), false));
                Journal.begin(player);
                HauntEventGameTests.check(helper, !holdsJournal(player), "a player must get no journal when it is off");
                Settings.use(new Settings(before.hardcore(), before.swaps(), true));
                Journal.begin(player);
                HauntEventGameTests.check(helper, holdsJournal(player), "a player must get a journal when it is on");
            } finally {
                Settings.use(before);
                player.discard();
            }
            helper.succeed();
        });
    }

    private static boolean holdsJournal(ServerPlayer player) {
        return player.getInventory().contains(stack -> stack.is(Content.JOURNAL.get()));
    }
}
