package dev.keelfy.restless.gametest;

import dev.keelfy.restless.config.Config;
import dev.keelfy.restless.divine.Divination;
import dev.keelfy.restless.spirit.Clue;
import dev.keelfy.restless.spirit.Story;
import dev.keelfy.restless.spirit.Trait;
import dev.keelfy.restless.state.HauntData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/** Each tool tells its own thing: the box the desire and the story, the board a habit, the camera the type. */
final class ToolGameTests {
    private ToolGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("tools_each_read_their_own_axis", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            HauntData.Haunting spirit = spirit(player, List.of("light_eater", "rain_walker", "wants_milk"));
            List<String> story = storyWords(spirit);
            boolean desire = false;
            boolean told = false;
            for (int i = 0; i < 120; i++) {
                var voice = Divination.read(player, Clue.VOICE).orElseThrow();
                if (voice.trait() == null) {
                    told = true;
                    check(helper, story.contains(voice.word()), "a story word must come from the death: " + voice);
                } else {
                    desire = true;
                    check(
                            helper,
                            voice.trait().kind() == Trait.Kind.LEANING,
                            "the box hears only leanings: " + voice.trait().id());
                }
                var board = Divination.read(player, Clue.PLANCHETTE).orElseThrow();
                check(
                        helper,
                        board.trait().kind() == Trait.Kind.HABIT,
                        "the board spells only habits: " + board.trait().id());
            }
            check(helper, desire && told, "the box must tell both the desire and the story");
            player.discard();
            helper.succeed();
        });
        tests.put("tools_box_tells_only_the_story_without_a_leaning", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            HauntData.Haunting spirit = spirit(player, List.of("light_eater", "cold_touch"));
            List<String> story = storyWords(spirit);
            for (int i = 0; i < 40; i++) {
                var voice = Divination.read(player, Clue.VOICE).orElseThrow();
                check(helper, voice.trait() == null, "a spirit with no leaning speaks only of its death");
                check(helper, story.contains(voice.word()), "the word must be the death's: " + voice.word());
            }
            player.discard();
            helper.succeed();
        });
    }

    private static List<String> storyWords(HauntData.Haunting spirit) {
        return Config.section("story").section("words").texts(Story.of(spirit.seed()).cause());
    }

    private static HauntData.Haunting spirit(ServerPlayer player, List<String> traits) {
        HauntData.Haunting born = new HauntData.Haunting(
                UUID.randomUUID(), "shade", 7L, traits, 0L, 0.0, Long.MAX_VALUE, false, 0, 0L);
        HauntData.get(player.level().getServer()).put(player.getUUID(), new ArrayList<>(List.of(born)));
        return born;
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }
}
