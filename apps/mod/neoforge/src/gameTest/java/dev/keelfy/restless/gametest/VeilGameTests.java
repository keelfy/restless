package dev.keelfy.restless.gametest;

import dev.keelfy.restless.divine.Divination;
import dev.keelfy.restless.journal.Notebook;
import dev.keelfy.restless.spirit.Clue;
import dev.keelfy.restless.state.HauntData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/**
 * A veiled spirit gives nothing to the one tool it foils, the others still read it, and the journal notes the silence.
 */
final class VeilGameTests {
    private VeilGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("veil_unseen_hides_from_the_camera_only", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            UUID spirit = spirit(player, "unseen");
            check(helper, Divination.read(player, Clue.SILHOUETTE).isEmpty(), "the camera must catch nothing");
            check(helper, silent(player, spirit), "the journal must note the camera's silence");
            check(helper, Divination.read(player, Clue.VOICE).isPresent(), "the spirit box must still hear it");
            check(helper, Divination.read(player, Clue.PLANCHETTE).isPresent(), "the board must still move");
            helper.succeed();
        });
        tests.put("veil_voiceless_hides_from_the_spirit_box_only", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            UUID spirit = spirit(player, "voiceless");
            check(helper, Divination.read(player, Clue.VOICE).isEmpty(), "the spirit box must hear nothing");
            check(helper, silent(player, spirit), "the journal must note the spirit box's silence");
            check(helper, Divination.read(player, Clue.SILHOUETTE).isPresent(), "the camera must still catch it");
            check(helper, Divination.read(player, Clue.PLANCHETTE).isPresent(), "the board must still move");
            helper.succeed();
        });
        tests.put("veil_light_footed_still_shows_to_other_tools", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            spirit(player, "light_footed");
            check(helper, Divination.read(player, Clue.SILHOUETTE).isPresent(), "the camera must still catch it");
            check(helper, Divination.read(player, Clue.VOICE).isPresent(), "the spirit box must still hear it");
            helper.succeed();
        });
    }

    private static UUID spirit(ServerPlayer player, String veil) {
        HauntData.Haunting born = new HauntData.Haunting(
                UUID.randomUUID(),
                "shade",
                1L,
                List.of("light_eater", "cold_touch", veil),
                0L,
                0.0,
                Long.MAX_VALUE,
                false,
                0,
                0L);
        HauntData data = HauntData.get(player.level().getServer());
        data.put(player.getUUID(), new ArrayList<>(List.of(born)));
        return born.uuid();
    }

    private static boolean silent(ServerPlayer player, UUID spirit) {
        return HauntData.get(player.level().getServer()).notebook(player.getUUID()).notes().stream()
                .anyMatch(n -> n.kind() == Notebook.Kind.SILENCE
                        && n.spirit().filter(spirit::equals).isPresent());
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }
}
