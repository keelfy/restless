package dev.keelfy.restless.gametest;

import dev.keelfy.restless.banish.Ritual;
import dev.keelfy.restless.content.block.Altar;
import dev.keelfy.restless.haunt.Director;
import dev.keelfy.restless.spirit.Form;
import dev.keelfy.restless.spirit.Spirits;
import dev.keelfy.restless.spirit.Story;
import dev.keelfy.restless.spirit.land.Graves;
import dev.keelfy.restless.state.HauntData;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/** A spirit born now takes the form its death names; an older one keeps the form of its type. */
final class FormGameTests {
    private FormGameTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("form_comes_from_the_death_for_spirits_born_now", helper -> {
            HauntData data = HauntData.get(helper.getLevel().getServer());
            // A poltergeist's type takes the whole thing; find a seed whose death names another form.
            long seed = 0;
            while (Story.of(seed).form() == Form.WHOLE) {
                seed++;
            }
            Form death = Story.of(seed).form();
            HauntData.Haunting old = spirit(seed);
            check(helper, Spirits.form(data, old) == Form.WHOLE, "an unmarked spirit keeps its type's form");
            check(helper, Ritual.waves(data, old, Altar.Held.IRON) == 12, "the type's form keeps the usual length");
            HauntData.Haunting marked = spirit(seed);
            data.putMarks(marked.uuid(), data.marks(marked.uuid()).withDeathForm());
            check(helper, Spirits.form(data, marked) == death, "a marked spirit takes the form of its death");
            check(helper, Ritual.waves(data, marked, Altar.Held.IRON) == 18, "the type's form is now off form");
            Altar.Held fits = death == Form.BIT ? Altar.Held.IRON_NUGGET : Altar.Held.IRON_CHAIN;
            check(helper, Ritual.waves(data, marked, fits) == 12, "the death's form keeps the usual length");
            String wanted = BuiltInRegistries.ITEM.getKey(Graves.wanted(data, marked)).toString();
            check(
                    helper,
                    Story.of(seed).wants().contains(wanted),
                    "the grave wants a thing of the death's family: " + wanted);
            check(
                    helper,
                    Graves.wanted(data, old) == Graves.wanted(old.seed()),
                    "an older spirit's grave wants what it always did");
            data.putMarks(marked.uuid(), dev.keelfy.restless.state.Marks.NONE);
            helper.succeed();
        });
        tests.put("form_mark_is_given_at_birth", helper -> {
            ServerPlayer player = HauntEventGameTests.connected(helper);
            HauntData data = HauntData.get(helper.getLevel().getServer());
            data.put(player.getUUID(), List.of());
            HauntData.Haunting born = Director.add(player, data, Spirits.POLTERGEIST, 0L);
            check(helper, born != null && data.marks(born.uuid()).deathForm(), "a spirit born now carries the mark");
            data.put(player.getUUID(), List.of());
            player.discard();
            helper.succeed();
        });
    }

    private static HauntData.Haunting spirit(long seed) {
        return new HauntData.Haunting(
                UUID.randomUUID(),
                "poltergeist",
                seed,
                List.of("door_slammer", "meddler", "light_eater"),
                0L,
                0.0,
                Long.MAX_VALUE / 2,
                false,
                0,
                0L);
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        HauntEventGameTests.check(helper, condition, message);
    }
}
