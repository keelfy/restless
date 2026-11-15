package dev.keelfy.restless.gametest;

import dev.keelfy.restless.Restless;
import dev.keelfy.restless.banish.Interrogation;
import dev.keelfy.restless.sanity.Sanity;
import dev.keelfy.restless.sanity.SanityPayload;
import dev.keelfy.restless.spirit.Trait;
import dev.keelfy.restless.spirit.Traits;
import dev.keelfy.restless.state.HauntData;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

/** A book on a ringed altar calls the spirit for one true answer, for a price in sanity. */
final class InterrogationGameTests {
    private static final UUID SPIRIT = UUID.fromString("00000000-0000-0000-0000-0000000000a7");
    /** A liar: the answer must still be true. */
    private static final List<String> TRAITS = List.of("door_slammer", "meddler", "liar", "wants_bread");

    private InterrogationGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("interrogation_costs_sanity_and_takes_one_question", helper -> {
            ServerPlayer player = ready(helper);
            Sanity.set(player, 1.0);
            check(helper, begin(helper, player), "a book on a ringed, lit altar calls the spirit");
            check(helper, Math.abs(Sanity.of(player) - 0.8) < 1e-9, "the call costs 0.2 sanity: " + Sanity.of(player));
            check(helper, Interrogation.asking(player), "the spirit waits for a question");
            Restless.onChat(player, "hello there");
            check(helper, Interrogation.asking(player), "a line that asks nothing is no question");
            Restless.onChat(player, "what do you want?");
            check(helper, !Interrogation.asking(player), "one question, then the spirit leaves");
            player.discard();
            helper.succeed();
        });
        tests.put("interrogation_answers_each_question_truly", helper -> {
            ServerPlayer player = ready(helper);
            HauntData data = HauntData.get(helper.getLevel().getServer());
            HauntData.Haunting spirit = data.find(SPIRIT).orElseThrow();
            BlockPos altar = helper.absolutePos(AltarGameTests.CENTER);
            check(
                    helper,
                    Interrogation.question("what do you do").orElseThrow() == Interrogation.Question.HABIT,
                    "habit question");
            check(
                    helper,
                    Interrogation.question("Что ты хочешь?").orElseThrow() == Interrogation.Question.DESIRE,
                    "desire question in Russian");
            check(
                    helper,
                    Interrogation.question("where is your grave").orElseThrow() == Interrogation.Question.GRAVE,
                    "grave question");
            List<String> hints = Traits.of(TRAITS).stream()
                    .filter(t -> t.kind() == Trait.Kind.HABIT)
                    .map(t -> "trait.restless." + t.id() + ".hint")
                    .toList();
            for (int i = 0; i < 60; i++) {
                String key = key(Interrogation.answer(player, data, spirit, altar, Interrogation.Question.HABIT)
                        .orElseThrow());
                check(helper, hints.contains(key), "a liar still names its own habit: " + key);
            }
            check(
                    helper,
                    key(Interrogation.answer(player, data, spirit, altar, Interrogation.Question.DESIRE)
                                    .orElseThrow())
                            .equals("trait.restless.wants_bread.hint"),
                    "the desire is told");
            check(
                    helper,
                    Interrogation.answer(player, data, spirit, altar, Interrogation.Question.GRAVE).isEmpty(),
                    "no grave yet, nothing to tell");
            data.putGrave(SPIRIT, new HauntData.Grave(altar.offset(2, 0, -60), true));
            Component grave = Interrogation.answer(player, data, spirit, altar, Interrogation.Question.GRAVE)
                    .orElseThrow();
            Object quarter = ((TranslatableContents) grave.getContents()).getArgs()[0];
            check(
                    helper,
                    key((Component) quarter).equals("direction.restless.north"),
                    "the grave lies north: " + key((Component) quarter));
            player.discard();
            helper.succeed();
        });
    }

    private static boolean begin(GameTestHelper helper, ServerPlayer player) {
        return Interrogation.begin(player, helper.getLevel(), helper.absolutePos(AltarGameTests.CENTER));
    }

    private static String key(Component component) {
        return ((TranslatableContents) component.getContents()).getKey();
    }

    private static ServerPlayer ready(GameTestHelper helper) {
        AltarGameTests.place(helper);
        RitualGameTests.ringAndCandles(helper);
        ServerPlayer player = HauntEventGameTests.connected(helper);
        ChannelAttributes.getOrCreateCommonChannels(player.connection.getConnection(), ConnectionProtocol.PLAY)
                .add(SanityPayload.TYPE.id());
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(AltarGameTests.CENTER.above())));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.BOOK));
        HauntData.get(helper.getLevel().getServer())
                .put(
                        player.getUUID(),
                        List.of(new HauntData.Haunting(
                                SPIRIT, "poltergeist", 7L, TRAITS, 0L, 0.0, Long.MAX_VALUE / 2, false, 0, 0L)));
        return player;
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }
}
